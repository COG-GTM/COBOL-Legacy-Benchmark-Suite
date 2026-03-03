"""Tests for batch models (BCHCON, BCHCTL, CKPRST, PRCSEQ copybooks)."""

import pytest
from pydantic import ValidationError

from src.models.batch.batch_constants import BatchControlConstants
from src.models.batch.batch_control import (
    BatchControlData,
    BatchControlKey,
    BatchControlRecord,
    BatchDependencies,
    BatchProcessControl,
    BatchReturnInfo,
    BatchStatistics,
    PrerequisiteJob,
)
from src.models.batch.checkpoint import (
    CheckpointControl,
    CheckpointControlInfo,
    CheckpointCounters,
    CheckpointFileStatus,
    CheckpointHeader,
    CheckpointPosition,
    CheckpointRecord,
    CheckpointResources,
)
from src.models.batch.process_sequence import (
    DependencyEntry,
    ProcessAudit,
    ProcessControl,
    ProcessDependencies,
    ProcessRecovery,
    ProcessSchedule,
    ProcessSequenceData,
    ProcessSequenceKey,
    ProcessSequenceRecord,
    ProcessTiming,
    StandardSequences,
)


class TestBatchControlConstants:
    def test_default_values(self):
        bcc = BatchControlConstants()
        assert bcc.bct_stat_values.bct_stat_ready == "R"
        assert bcc.bct_rc_thresholds.bct_rc_success == 0
        assert bcc.bct_ctrl_values.bct_max_prereq == 10
        assert bcc.bct_proc_types.bct_type_initial == "INI"
        assert bcc.bct_dep_types.bct_dep_required == "R"
        assert bcc.bct_proc_names.bct_start_of_day == "STARTDAY"
        assert bcc.bct_rec_types.bct_rec_control == "C"
        assert bcc.bct_messages.bct_msg_complete == "Process completed successfully"


class TestBatchControlRecord:
    def test_full_record(self):
        record = BatchControlRecord(
            bct_key=BatchControlKey(
                bct_job_name="TRNVAL00",
                bct_process_date="20240315",
                bct_sequence_no=1,
            ),
            bct_data=BatchControlData(
                bct_status="A",
                bct_process_control=BatchProcessControl(
                    bct_step_name="STEP001",
                    bct_program_name="TRNVAL00",
                    bct_start_time="14:30:00",
                    bct_end_time="",
                ),
                bct_dependencies=BatchDependencies(
                    bct_prereq_count=1,
                    bct_prereq_jobs=[
                        PrerequisiteJob(
                            bct_prereq_name="INITDAY",
                            bct_prereq_seq=1,
                            bct_prereq_rc=0,
                        )
                    ],
                ),
                bct_return_info=BatchReturnInfo(
                    bct_return_code=0,
                    bct_error_desc="",
                ),
            ),
            bct_statistics=BatchStatistics(
                bct_restart_count=0,
                bct_attempt_ts="2024-03-15T14:30:00.000000",
                bct_complete_ts="",
            ),
        )
        assert record.bct_key.bct_job_name == "TRNVAL00"
        assert record.bct_data.bct_status == "A"
        assert len(record.bct_data.bct_dependencies.bct_prereq_jobs) == 1

    def test_invalid_status(self):
        with pytest.raises(ValidationError, match="bct_status must be one of"):
            BatchControlData(
                bct_status="Z",
                bct_process_control=BatchProcessControl(
                    bct_step_name="S", bct_program_name="P",
                    bct_start_time="", bct_end_time="",
                ),
                bct_dependencies=BatchDependencies(
                    bct_prereq_count=0, bct_prereq_jobs=[],
                ),
                bct_return_info=BatchReturnInfo(
                    bct_return_code=0, bct_error_desc="",
                ),
            )

    def test_prereq_jobs_max_length(self):
        """OCCURS 10 TIMES - should accept up to 10 entries."""
        jobs = [
            PrerequisiteJob(bct_prereq_name=f"JOB{i:05d}", bct_prereq_seq=i, bct_prereq_rc=0)
            for i in range(10)
        ]
        deps = BatchDependencies(bct_prereq_count=10, bct_prereq_jobs=jobs)
        assert len(deps.bct_prereq_jobs) == 10

    def test_prereq_jobs_exceeds_max(self):
        jobs = [
            PrerequisiteJob(bct_prereq_name=f"JOB{i:05d}", bct_prereq_seq=i, bct_prereq_rc=0)
            for i in range(11)
        ]
        with pytest.raises(ValidationError):
            BatchDependencies(bct_prereq_count=11, bct_prereq_jobs=jobs)


class TestCheckpointControl:
    def test_full_record(self):
        ck = CheckpointControl(
            ck_header=CheckpointHeader(
                ck_program_id="TRNVAL00",
                ck_run_date="20240315",
                ck_run_time="143022",
                ck_status="A",
            ),
            ck_counters=CheckpointCounters(
                ck_records_read=1000,
                ck_records_proc=990,
                ck_records_error=10,
                ck_restart_count=0,
            ),
            ck_position=CheckpointPosition(
                ck_last_key="PORT0001-20240315",
                ck_last_time="2024-03-15T14:30:22.000000",
                ck_phase="20",
            ),
            ck_resources=CheckpointResources(
                ck_file_statuses=[
                    CheckpointFileStatus(
                        ck_file_name="PORTFILE",
                        ck_file_pos="key1",
                        ck_file_status="00",
                    )
                ],
            ),
            ck_control_info=CheckpointControlInfo(
                ck_commit_freq=1000,
                ck_max_errors=100,
                ck_max_restarts=3,
                ck_restart_mode="N",
            ),
        )
        assert ck.ck_header.ck_status == "A"
        assert ck.ck_counters.ck_records_read == 1000
        assert ck.ck_position.ck_phase == "20"

    def test_invalid_status(self):
        with pytest.raises(ValidationError, match="ck_status must be one of"):
            CheckpointHeader(
                ck_program_id="TEST",
                ck_run_date="20240315",
                ck_run_time="143022",
                ck_status="Z",
            )

    def test_invalid_phase(self):
        with pytest.raises(ValidationError, match="ck_phase must be one of"):
            CheckpointPosition(
                ck_last_key="key",
                ck_last_time="ts",
                ck_phase="99",
            )

    def test_invalid_restart_mode(self):
        with pytest.raises(ValidationError, match="ck_restart_mode must be one of"):
            CheckpointControlInfo(ck_restart_mode="Z")

    def test_file_status_max_length(self):
        """OCCURS 5 TIMES - should accept up to 5 entries."""
        statuses = [
            CheckpointFileStatus(
                ck_file_name=f"FILE{i:04d}",
                ck_file_pos="",
                ck_file_status="00",
            )
            for i in range(5)
        ]
        res = CheckpointResources(ck_file_statuses=statuses)
        assert len(res.ck_file_statuses) == 5

    def test_file_status_exceeds_max(self):
        statuses = [
            CheckpointFileStatus(
                ck_file_name=f"FILE{i:04d}",
                ck_file_pos="",
                ck_file_status="00",
            )
            for i in range(6)
        ]
        with pytest.raises(ValidationError):
            CheckpointResources(ck_file_statuses=statuses)


class TestCheckpointRecord:
    def test_record(self):
        rec = CheckpointRecord(
            ckr_program_id="TRNVAL00",
            ckr_run_date="20240315",
            ckr_data="checkpoint data here",
        )
        assert rec.ckr_program_id == "TRNVAL00"
        assert rec.ckr_data == "checkpoint data here"


class TestProcessSequenceRecord:
    def test_full_record(self):
        rec = ProcessSequenceRecord(
            psr_key=ProcessSequenceKey(psr_process_id="TRNVAL00", psr_version=1),
            psr_data=ProcessSequenceData(
                psr_description="Transaction Validation",
                psr_type="PRC",
                psr_timing=ProcessTiming(
                    psr_freq="D", psr_start_time=600, psr_max_time=120,
                ),
                psr_dependencies=ProcessDependencies(
                    psr_dep_count=1,
                    psr_dep_entries=[
                        DependencyEntry(
                            psr_dep_id="INITDAY",
                            psr_dep_type="H",
                            psr_dep_rc=0,
                        )
                    ],
                ),
                psr_control=ProcessControl(
                    psr_program="TRNVAL00",
                    psr_parm="DAILY",
                    psr_max_rc=4,
                    psr_restart="Y",
                ),
            ),
            psr_schedule=ProcessSchedule(
                psr_active_days="YYYYYNN",
                psr_month_end="N",
                psr_holiday_run="N",
            ),
            psr_recovery=ProcessRecovery(
                psr_recovery_pgm="CKPRSTR",
                psr_recovery_parm="",
                psr_error_limit=100,
            ),
            psr_audit=ProcessAudit(
                psr_create_date="2024-03-15",
                psr_create_user="ADMIN01",
                psr_update_date="2024-03-15",
                psr_update_user="ADMIN01",
            ),
        )
        assert rec.psr_key.psr_process_id == "TRNVAL00"
        assert rec.psr_data.psr_type == "PRC"

    def test_invalid_process_type(self):
        with pytest.raises(ValidationError, match="psr_type must be one of"):
            ProcessSequenceData(
                psr_description="Test",
                psr_type="XXX",
                psr_timing=ProcessTiming(psr_freq="D", psr_start_time=0, psr_max_time=0),
                psr_dependencies=ProcessDependencies(psr_dep_count=0, psr_dep_entries=[]),
                psr_control=ProcessControl(
                    psr_program="T", psr_parm="", psr_max_rc=0, psr_restart="Y",
                ),
            )

    def test_invalid_dep_type(self):
        with pytest.raises(ValidationError, match="psr_dep_type must be one of"):
            DependencyEntry(psr_dep_id="TEST", psr_dep_type="Z", psr_dep_rc=0)


class TestStandardSequences:
    def test_defaults(self):
        ss = StandardSequences()
        assert ss.seq_start_of_day == ["INITDAY", "CKPCLR", "DATEVAL"]
        assert ss.seq_main_process == ["TRNVAL00", "POSUPD00", "HISTLD00"]
        assert ss.seq_end_of_day == ["RPTGEN00", "BCKLOD00", "ENDDAY"]

package com.cobolbenchmark.model;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.time.DayOfWeek;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Process Sequence Record - migrated from PRCSEQ.cpy.
 * PSR-DEP-ENTRY OCCURS 10 TIMES → List<Dependency>.
 * PSR-ACTIVE-DAYS PIC X(7) VALUE 'YYYYYNN' → Set<DayOfWeek>.
 */
@Entity
@Table(name = "PROCESS_SEQUENCE")
public class ProcessSequenceRecord {

    @Id
    @Column(name = "PROCESS_ID", length = 8, nullable = false)
    private String processId;

    @Column(name = "SEQUENCE_TYPE", length = 3, nullable = false)
    private String sequenceType;

    @Column(name = "FREQUENCY", length = 1, nullable = false)
    private String frequency;

    @Column(name = "ACTIVE_DAYS", length = 7)
    private String activeDays = "YYYYYNN";

    @Column(name = "RESTARTABLE", length = 1)
    private String restartable = "Y";

    @Column(name = "DEP_COUNT")
    private int depCount;

    @Transient
    private List<Dependency> dependencies = new ArrayList<>();

    public ProcessSequenceRecord() {
    }

    /**
     * Convert PSR-ACTIVE-DAYS PIC X(7) to Set<DayOfWeek>.
     * Position 1=Mon, 2=Tue, ..., 7=Sun. 'Y' means active.
     */
    public Set<DayOfWeek> getActiveDaysOfWeek() {
        EnumSet<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        if (activeDays != null && activeDays.length() == 7) {
            DayOfWeek[] weekDays = DayOfWeek.values();
            for (int i = 0; i < 7; i++) {
                if (activeDays.charAt(i) == 'Y') {
                    days.add(weekDays[i]);
                }
            }
        }
        return days;
    }

    public boolean isRestartable() {
        return "Y".equals(restartable);
    }

    // Getters and Setters

    public String getProcessId() { return processId; }
    public void setProcessId(String processId) { this.processId = processId; }

    public String getSequenceType() { return sequenceType; }
    public void setSequenceType(String sequenceType) { this.sequenceType = sequenceType; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public String getActiveDays() { return activeDays; }
    public void setActiveDays(String activeDays) { this.activeDays = activeDays; }

    public String getRestartable() { return restartable; }
    public void setRestartable(String restartable) { this.restartable = restartable; }

    public int getDepCount() { return depCount; }
    public void setDepCount(int depCount) { this.depCount = depCount; }

    public List<Dependency> getDependencies() { return dependencies; }
    public void setDependencies(List<Dependency> dependencies) { this.dependencies = dependencies; }
}

package com.clbs.portfolio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Prerequisite job entry for batch control dependency tracking.
 * From COBOL copybook: src/copybook/batch/BCHCTL.cpy (BCT-PREREQ-JOBS OCCURS 10 TIMES).
 */
@Entity
@Table(name = "prerequisite_job")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrerequisiteJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** BCT-PREREQ-NAME — PIC X(8) */
    @Column(name = "prereq_name", length = 8, nullable = false)
    private String prereqName;

    /** BCT-PREREQ-SEQ — PIC 9(4) */
    @Column(name = "prereq_seq")
    private Integer prereqSeq;

    /** BCT-PREREQ-RC — PIC S9(4) COMP */
    @Column(name = "prereq_rc")
    private Integer prereqRc;
}

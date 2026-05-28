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
 * Process dependency entry for sequence ordering.
 * From COBOL copybook: src/copybook/batch/PRCSEQ.cpy (PSR-DEP-ENTRY OCCURS 10 TIMES).
 */
@Entity
@Table(name = "process_dependency")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** PSR-DEP-ID — PIC X(8) */
    @Column(name = "dep_id", length = 8, nullable = false)
    private String depId;

    /** PSR-DEP-TYPE — PIC X(1): H=Hard, S=Soft */
    @Column(name = "dep_type", length = 1, nullable = false)
    private String depType;

    /** PSR-DEP-RC — PIC S9(4) COMP */
    @Column(name = "dep_rc")
    private Integer depRc;
}

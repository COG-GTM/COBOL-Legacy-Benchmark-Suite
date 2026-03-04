package com.cobolbenchmark.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * Dependency - migrated from PRCSEQ.cpy PSR-DEP-ENTRY OCCURS 10 TIMES.
 */
@Entity
@Table(name = "PROCESS_DEPENDENCIES")
@IdClass(DependencyKey.class)
public class Dependency {

    @Id
    @Column(name = "PROCESS_ID", length = 8, nullable = false)
    private String processId;

    @Id
    @Column(name = "DEP_INDEX", nullable = false)
    private int depIndex;

    @Column(name = "DEP_ID", length = 8, nullable = false)
    private String depId;

    @Column(name = "DEP_TYPE", length = 1, nullable = false)
    private String depType;

    @Column(name = "DEP_RC")
    private int depRc;

    public Dependency() {
    }

    public Dependency(String processId, int depIndex, String depId, DependencyType type, int depRc) {
        this.processId = processId;
        this.depIndex = depIndex;
        this.depId = depId;
        this.depType = type.getCode();
        this.depRc = depRc;
    }

    public DependencyType getDependencyType() {
        return DependencyType.fromCode(depType);
    }

    // Getters and Setters

    public String getProcessId() { return processId; }
    public void setProcessId(String processId) { this.processId = processId; }

    public int getDepIndex() { return depIndex; }
    public void setDepIndex(int depIndex) { this.depIndex = depIndex; }

    public String getDepId() { return depId; }
    public void setDepId(String depId) { this.depId = depId; }

    public String getDepType() { return depType; }
    public void setDepType(String depType) { this.depType = depType; }

    public int getDepRc() { return depRc; }
    public void setDepRc(int depRc) { this.depRc = depRc; }
}

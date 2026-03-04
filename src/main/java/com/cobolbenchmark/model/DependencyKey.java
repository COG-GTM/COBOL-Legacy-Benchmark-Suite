package com.cobolbenchmark.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite key for Dependency entity.
 */
public class DependencyKey implements Serializable {

    private static final long serialVersionUID = 1L;

    private String processId;
    private int depIndex;

    public DependencyKey() {
    }

    public DependencyKey(String processId, int depIndex) {
        this.processId = processId;
        this.depIndex = depIndex;
    }

    public String getProcessId() { return processId; }
    public void setProcessId(String processId) { this.processId = processId; }

    public int getDepIndex() { return depIndex; }
    public void setDepIndex(int depIndex) { this.depIndex = depIndex; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DependencyKey that = (DependencyKey) o;
        return depIndex == that.depIndex && Objects.equals(processId, that.processId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(processId, depIndex);
    }
}

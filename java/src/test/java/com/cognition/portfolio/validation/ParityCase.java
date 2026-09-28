package com.cognition.portfolio.validation;

/** One row of src/test/resources/parity/portvald-cases.tsv (values already unescaped). */
record ParityCase(String id, char validateType, String inputValue, String category, String description) {

    @Override
    public String toString() {
        return id + " [" + category + "] " + description;
    }
}

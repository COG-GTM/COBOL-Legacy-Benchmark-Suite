package com.portfolio.dto;

import java.util.List;

public class MenuResponse {

    private String title;
    private List<String> options;

    public MenuResponse() {
    }

    public MenuResponse(String title, List<String> options) {
        this.title = title;
        this.options = options;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }
}

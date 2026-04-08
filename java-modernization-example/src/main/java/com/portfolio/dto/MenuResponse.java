package com.portfolio.dto;

import java.util.List;

/**
 * Simple DTO listing available inquiry options.
 * Replaces the BMS menu screen (INQMNU map from INQSET mapset) that was
 * sent to the 3270 terminal by P200-DISPLAY-MENU in INQONLN.cbl (lines 92-99):
 * <pre>
 *     EXEC CICS SEND MAP('INQMNU')
 *               MAPSET('INQSET')
 *               ERASE
 *               RESP(WS-RESPONSE-CODE)
 *     END-EXEC.
 * </pre>
 *
 * Instead of rendering a terminal screen, we return the menu options as
 * structured JSON data that any client (web, mobile, CLI) can consume.
 */
public class MenuResponse {

    private String title;
    private List<MenuOption> options;

    public MenuResponse() {
    }

    public MenuResponse(String title, List<MenuOption> options) {
        this.title = title;
        this.options = options;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<MenuOption> getOptions() {
        return options;
    }

    public void setOptions(List<MenuOption> options) {
        this.options = options;
    }

    /**
     * Represents a single menu option.
     * Each corresponds to a WHEN branch in the EVALUATE statement
     * of INQONLN.cbl (lines 62-77).
     */
    public static class MenuOption {

        private String code;
        private String description;
        private String endpoint;

        public MenuOption() {
        }

        public MenuOption(String code, String description, String endpoint) {
            this.code = code;
            this.description = description;
            this.endpoint = endpoint;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }
    }
}

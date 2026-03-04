package com.cobolbenchmark.online;

import java.util.List;

/**
 * Menu Response DTO - replaces MENMAP BMS map output.
 * Maps to EXEC CICS SEND MAP('MENMAP') output fields.
 */
public class MenuResponse {

    private String title;
    private List<MenuItem> menuItems;
    private String message;

    public MenuResponse() {
    }

    public static class MenuItem {
        private String code;
        private String description;

        public MenuItem() {
        }

        public MenuItem(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public List<MenuItem> getMenuItems() { return menuItems; }
    public void setMenuItems(List<MenuItem> menuItems) { this.menuItems = menuItems; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}

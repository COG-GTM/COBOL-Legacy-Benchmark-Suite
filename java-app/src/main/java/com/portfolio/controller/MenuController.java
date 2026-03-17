package com.portfolio.controller;

import com.portfolio.service.SecurityManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Menu Controller.
 * Replaces: MENMAP BMS map handling in INQONLN.cbl.
 *
 * GET / -> render menu.html (maps to MENMAP with options:
 *   1. Portfolio Position Inquiry
 *   2. Transaction History
 *   3. Exit)
 */
@Controller
public class MenuController {

    private static final Logger log = LoggerFactory.getLogger(MenuController.class);

    private final SecurityManagerService securityManager;

    public MenuController(SecurityManagerService securityManager) {
        this.securityManager = securityManager;
    }

    @GetMapping("/")
    public String showMenu(Model model) {
        String userId = securityManager.validateUser();
        securityManager.logAccess("MENU", "READ");

        model.addAttribute("userId", userId);
        return "menu";
    }

    @PostMapping("/menu")
    public String handleMenuSelection(@RequestParam("option") int option) {
        return switch (option) {
            case 1 -> "redirect:/portfolio";
            case 2 -> "redirect:/history";
            case 3 -> "redirect:/logout";
            default -> "redirect:/";
        };
    }

    @GetMapping("/login")
    public String showLogin() {
        return "login";
    }
}

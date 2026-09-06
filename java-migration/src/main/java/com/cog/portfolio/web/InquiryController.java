package com.cog.portfolio.web;

import com.cog.portfolio.common.PortfolioException;
import com.cog.portfolio.domain.Position;
import com.cog.portfolio.domain.PositionHistory;
import com.cog.portfolio.dto.InquiryRequest;
import com.cog.portfolio.service.HistoryInquiryService;
import com.cog.portfolio.service.PositionInquiryService;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * INQONLN.cbl (menu / EVALUATE WS-COMMAREA-FUNCTION) + INQSET.bms maps
 * MENMAP, POSMAP and HISMAP. Menu option 1 = INQP, 2 = INQH, 3 = EXIT.
 */
@Controller
public class InquiryController {

    private static final String ACCOUNT_PATTERN = "\\d{10}";

    private final PositionInquiryService positionInquiryService;
    private final HistoryInquiryService historyInquiryService;

    public InquiryController(PositionInquiryService positionInquiryService,
                             HistoryInquiryService historyInquiryService) {
        this.positionInquiryService = positionInquiryService;
        this.historyInquiryService = historyInquiryService;
    }

    /** MENMAP. */
    @GetMapping({"/", "/menu"})
    public String menu(@RequestParam(name = "option", required = false) String option, Model model) {
        if (option == null || option.isBlank()) {
            return "menu";
        }
        return switch (option.trim()) {
            case "1" -> "redirect:/inquiry/position";
            case "2" -> "redirect:/inquiry/history";
            case "3" -> "redirect:/logout";
            default -> {
                model.addAttribute("errmsg", "Invalid option selected");
                yield "menu";
            }
        };
    }

    /** POSMAP (INQP -> INQPORT). */
    @GetMapping("/inquiry/position")
    public String position(@RequestParam(name = "accountNo", required = false) String accountNo, Model model) {
        model.addAttribute("function", InquiryRequest.Function.PORTFOLIO.code());
        model.addAttribute("accountNo", accountNo == null ? "" : accountNo);
        if (accountNo == null || accountNo.isBlank()) {
            return "position";
        }
        if (!accountNo.matches(ACCOUNT_PATTERN)) {
            model.addAttribute("errmsg", "Account number must be 10 digits");
            return "position";
        }
        try {
            List<Position> positions = positionInquiryService.findByAccount(accountNo);
            model.addAttribute("positions", positions);
        } catch (PortfolioException e) {
            model.addAttribute("errmsg", e.getMessage());
        }
        return "position";
    }

    /** HISMAP (INQH -> INQHIST), 10 rows per screen; PF7/PF8 become page links. */
    @GetMapping("/inquiry/history")
    public String history(@RequestParam(name = "accountNo", required = false) String accountNo,
                          @RequestParam(name = "page", defaultValue = "0") int page, Model model) {
        model.addAttribute("function", InquiryRequest.Function.HISTORY.code());
        model.addAttribute("accountNo", accountNo == null ? "" : accountNo);
        model.addAttribute("page", page);
        if (accountNo == null || accountNo.isBlank()) {
            return "history";
        }
        if (!accountNo.matches(ACCOUNT_PATTERN)) {
            model.addAttribute("errmsg", "Account number must be 10 digits");
            return "history";
        }
        Page<PositionHistory> rows = historyInquiryService.findByAccount(accountNo, page);
        model.addAttribute("rows", rows.getContent());
        model.addAttribute("hasPrevious", rows.hasPrevious());
        model.addAttribute("hasNext", rows.hasNext());
        if (rows.isEmpty()) {
            model.addAttribute("errmsg", "No history found for account");
        }
        return "history";
    }
}

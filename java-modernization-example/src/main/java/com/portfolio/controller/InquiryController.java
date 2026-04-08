package com.portfolio.controller;

import com.portfolio.dto.MenuResponse;
import com.portfolio.dto.MenuResponse.MenuOption;
import com.portfolio.dto.PortfolioDto;
import com.portfolio.dto.PositionHistoryDto;
import com.portfolio.model.Portfolio;
import com.portfolio.model.PositionHistory;
import com.portfolio.service.PortfolioInquiryService;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller that directly replaces the CICS program INQONLN.cbl.
 *
 * <h2>Architecture Mapping: CICS Pseudo-Conversational -> Stateless REST</h2>
 *
 * The original INQONLN.cbl (src/programs/online/INQONLN.cbl) was a CICS
 * pseudo-conversational program that:
 * <ol>
 *   <li>Used {@code EXEC CICS HANDLE CONDITION} to register a global error handler
 *       (P900-ERROR-ROUTINE) for ERROR, PGMIDERR, and NOTFND conditions.
 *       <br><b>Java equivalent:</b> {@code @ControllerAdvice} in {@link
 *       com.portfolio.exception.GlobalExceptionHandler}</li>
 *
 *   <li>Used {@code EXEC CICS RECEIVE MAP('INQMAP') MAPSET('INQSET')} to read
 *       input from a BMS map (3270 terminal screen) into a COMMAREA.
 *       <br><b>Java equivalent:</b> {@code @RequestParam} and {@code @PathVariable}
 *       annotations automatically bind HTTP request parameters to method arguments.
 *       The BMS map fields become query parameters or path variables.</li>
 *
 *   <li>Dispatched on {@code WS-COMMAREA-FUNCTION} via an EVALUATE statement
 *       (lines 62-77) to handle 'MENU', 'INQP', 'INQH', 'EXIT', and OTHER.
 *       <br><b>Java equivalent:</b> Each WHEN branch becomes a separate
 *       {@code @GetMapping} endpoint. The function code is implicit in the URL path.</li>
 *
 *   <li>Used {@code EXEC CICS SEND MAP} to render output back to the terminal.
 *       <br><b>Java equivalent:</b> Returning DTOs from controller methods; Jackson
 *       serializes them to JSON automatically (like SEND MAP rendered to 3270).</li>
 *
 *   <li>Used {@code EXEC CICS RETURN} to end the transaction and release the terminal.
 *       <br><b>Java equivalent:</b> Implicit in the HTTP response lifecycle. When the
 *       controller method returns, the HTTP response is sent and the thread is freed.</li>
 *
 *   <li>Maintained session state via COMMAREA across pseudo-conversational cycles.
 *       <br><b>Java equivalent:</b> REST is stateless by design. Any session-like state
 *       can be managed via JWT tokens or session headers if needed.</li>
 * </ol>
 *
 * <h2>Original EVALUATE dispatch (INQONLN.cbl lines 62-77):</h2>
 * <pre>
 *     EVALUATE WS-COMMAREA-FUNCTION
 *         WHEN 'MENU'  -> P200-DISPLAY-MENU      -> GET /api/inquiry/menu
 *         WHEN 'INQP'  -> P300-PORTFOLIO-INQUIRY  -> GET /api/inquiry/portfolio/{id}
 *         WHEN 'INQH'  -> P400-HISTORY-INQUIRY    -> GET /api/inquiry/history/{id}
 *         WHEN 'EXIT'  -> SESSION-TERMINATED       -> (no endpoint; client simply stops calling)
 *         WHEN OTHER   -> P900-ERROR-ROUTINE       -> (handled by @ControllerAdvice)
 *     END-EVALUATE
 * </pre>
 */
@RestController
@RequestMapping("/api/inquiry")
@Validated
public class InquiryController {

    private final PortfolioInquiryService inquiryService;

    public InquiryController(PortfolioInquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    /**
     * Replaces P200-DISPLAY-MENU (WHEN 'MENU') — INQONLN.cbl lines 92-99.
     *
     * Original COBOL:
     * <pre>
     *     P200-DISPLAY-MENU.
     *         EXEC CICS SEND MAP('INQMNU')
     *                   MAPSET('INQSET')
     *                   ERASE
     *                   RESP(WS-RESPONSE-CODE)
     *         END-EXEC.
     * </pre>
     *
     * The CICS SEND MAP rendered a 3270 screen with menu options. Here we return
     * the menu structure as JSON so any client can render it.
     *
     * @return menu options listing the available inquiry functions
     */
    @GetMapping("/menu")
    public ResponseEntity<MenuResponse> getMenu() {
        MenuResponse menu = new MenuResponse(
                "Portfolio Inquiry System",
                List.of(
                        new MenuOption("INQP", "Portfolio Inquiry",
                                "GET /api/inquiry/portfolio/{portfolioId}"),
                        new MenuOption("INQH", "History Inquiry",
                                "GET /api/inquiry/history/{portfolioId}?from=yyyy-MM-dd&to=yyyy-MM-dd"),
                        new MenuOption("EXIT", "Exit System",
                                "(client terminates session)")
                )
        );
        return ResponseEntity.ok(menu);
    }

    /**
     * Replaces P300-PORTFOLIO-INQUIRY (WHEN 'INQP') — INQONLN.cbl lines 101-108.
     *
     * Original COBOL:
     * <pre>
     *     P300-PORTFOLIO-INQUIRY.
     *         EXEC CICS LINK PROGRAM('INQPORT')
     *                   COMMAREA(WS-COMMAREA)
     *                   LENGTH(LENGTH OF WS-COMMAREA)
     *                   RESP(WS-RESPONSE-CODE)
     *         END-EXEC.
     * </pre>
     *
     * The portfolio ID was passed in the COMMAREA (populated from the BMS map
     * via EXEC CICS RECEIVE MAP). Here the ID comes from the URL path variable,
     * which replaces RECEIVE MAP. The response replaces SEND MAP: instead of
     * rendering fields on a 3270 screen, we return a JSON DTO.
     *
     * If the portfolio is not found, INQPORT set a VSAM status '23' error in
     * the COMMAREA. Here, {@link com.portfolio.exception.PortfolioNotFoundException}
     * is thrown and mapped to HTTP 404 by the GlobalExceptionHandler.
     *
     * @param portfolioId portfolio identifier (max 8 chars, maps to PORT-ID PIC X(8))
     * @return portfolio details as JSON
     */
    @GetMapping("/portfolio/{portfolioId}")
    public ResponseEntity<List<PortfolioDto>> getPortfolio(
            @PathVariable @Size(max = 8) String portfolioId) {

        List<Portfolio> portfolios = inquiryService.getPortfolio(portfolioId);
        List<PortfolioDto> dtos = portfolios.stream()
                .map(PortfolioDto::fromEntity)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * Replaces P400-HISTORY-INQUIRY (WHEN 'INQH') — INQONLN.cbl lines 110-117.
     *
     * Original COBOL:
     * <pre>
     *     P400-HISTORY-INQUIRY.
     *         EXEC CICS LINK PROGRAM('INQHIST')
     *                   COMMAREA(WS-COMMAREA)
     *                   LENGTH(LENGTH OF WS-COMMAREA)
     *                   RESP(WS-RESPONSE-CODE)
     *         END-EXEC.
     * </pre>
     *
     * INQHIST opened a DB2 cursor on the POSHIST table with a WHERE clause
     * on PORTFOLIO_ID and TRANS_DATE BETWEEN :from AND :to, then fetched rows
     * into the COMMAREA one page at a time. Here the date range comes from
     * query parameters (replacing the BMS input fields), and all matching
     * records are returned in a single JSON array.
     *
     * @param portfolioId portfolio identifier
     * @param from        start date (inclusive), replaces BMS date-from field
     * @param to          end date (inclusive), replaces BMS date-to field
     * @return list of position history records as JSON
     */
    @GetMapping("/history/{portfolioId}")
    public ResponseEntity<List<PositionHistoryDto>> getHistory(
            @PathVariable String portfolioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        List<PositionHistory> history = inquiryService.getPortfolioHistory(portfolioId, from, to);
        List<PositionHistoryDto> dtos = history.stream()
                .map(PositionHistoryDto::fromEntity)
                .toList();
        return ResponseEntity.ok(dtos);
    }
}

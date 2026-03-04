package com.investment.portfolio.online;

import com.investment.portfolio.common.ErrorHandler;
import com.investment.portfolio.common.ReturnCode;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

/**
 * Online Inquiry Main Handler (INQONLN) - Java equivalent of INQONLN.cbl
 *
 * Original COBOL: src/programs/online/INQONLN.cbl
 *
 * Responsibilities:
 * - Main CICS transaction entry point for online inquiry
 * - Routes user requests to appropriate inquiry handler
 * - Manages user session lifecycle
 * - Performs security validation via SECMGR subprogram
 *
 * CICS mapping:
 * - EXEC CICS RECEIVE MAP   → receiveRequest() (HTTP request / message input)
 * - EXEC CICS SEND MAP      → sendResponse() (HTTP response / message output)
 * - EXEC CICS LINK PROGRAM  → delegated method calls to sub-handlers
 * - EXEC CICS HANDLE CONDITION → try/catch exception handling
 * - EXEC CICS RETURN TRANSID → session management
 *
 * Function codes (from WS-FUNCTION):
 * - MENU: Display main inquiry menu
 * - INQP: Portfolio position inquiry (delegates to INQPORT)
 * - INQH: Transaction history inquiry (delegates to INQHIST)
 * - EXIT: End user session
 */
public class InquiryOnlineHandler {

    private static final Logger LOGGER = Logger.getLogger(InquiryOnlineHandler.class.getName());
    private static final String PROGRAM_ID = "INQONLN";
    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Function codes matching WS-FUNCTION in COBOL */
    public enum InquiryFunction {
        MENU, INQP, INQH, EXIT
    }

    private final ErrorHandler errorHandler;
    private final PortfolioInquiryHandler portfolioHandler;
    private final HistoryInquiryHandler historyHandler;

    /** Session state - maps to CICS COMMAREA */
    private String userId;
    private String terminalId;
    private boolean sessionActive;
    private boolean securityValidated;

    public InquiryOnlineHandler(PortfolioInquiryHandler portfolioHandler,
                                HistoryInquiryHandler historyHandler) {
        this.errorHandler = new ErrorHandler(PROGRAM_ID);
        this.portfolioHandler = portfolioHandler;
        this.historyHandler = historyHandler;
        this.sessionActive = false;
        this.securityValidated = false;
    }

    /**
     * Main entry point - maps to COBOL PROCEDURE DIVISION.
     *
     * EXEC CICS HANDLE CONDITION
     *   ERROR(9000-ERROR-HANDLER)
     *   MAPFAIL(9100-MAP-ERROR)
     * END-EXEC
     * PERFORM 1000-RECEIVE-REQUEST
     * EVALUATE WS-FUNCTION
     *   WHEN 'MENU' PERFORM 2000-DISPLAY-MENU
     *   WHEN 'INQP' PERFORM 3000-PORTFOLIO-INQUIRY
     *   WHEN 'INQH' PERFORM 4000-HISTORY-INQUIRY
     *   WHEN 'EXIT' PERFORM 5000-END-SESSION
     * END-EVALUATE
     *
     * @param function the inquiry function to perform
     * @param request  the inquiry request data
     * @return the inquiry response
     */
    public InquiryResponse handleRequest(InquiryFunction function, InquiryRequest request) {
        LOGGER.info(PROGRAM_ID + " - Handling function: " + function
                + " User: " + request.getUserId());

        try {
            // 1000-RECEIVE-REQUEST: Validate the request
            if (!receiveRequest(request)) {
                return InquiryResponse.error("Invalid request or unauthorized");
            }

            // Route to appropriate handler based on function code
            switch (function) {
                case MENU:
                    return displayMenu();
                case INQP:
                    return portfolioInquiry(request);
                case INQH:
                    return historyInquiry(request);
                case EXIT:
                    return endSession();
                default:
                    return InquiryResponse.error("Unknown function: " + function);
            }

        } catch (Exception e) {
            // 9000-ERROR-HANDLER equivalent
            errorHandler.handleSystemError("E900", "Online inquiry error", e);
            return InquiryResponse.error("System error: " + e.getMessage());
        }
    }

    /**
     * 1000-RECEIVE-REQUEST: Validate and receive the incoming request.
     *
     * Maps to:
     *   EXEC CICS RECEIVE MAP('INQMAP') MAPSET('INQSET')
     *     INTO(WS-INQUIRY-MAP)
     *   END-EXEC
     *   PERFORM 1100-VALIDATE-SECURITY
     */
    private boolean receiveRequest(InquiryRequest request) {
        this.userId = request.getUserId();
        this.terminalId = request.getTerminalId();

        // 1100-VALIDATE-SECURITY: Call security manager
        return validateSecurity(request.getUserId(), request.getTerminalId());
    }

    /**
     * 1100-VALIDATE-SECURITY: Validate user credentials.
     *
     * Maps to:
     *   EXEC CICS LINK PROGRAM('SECMGR')
     *     COMMAREA(WS-SEC-REQUEST)
     *   END-EXEC
     *   IF SEC-RESPONSE-CODE NOT = '00'
     *     PERFORM 9200-SECURITY-ERROR
     *   END-IF
     */
    private boolean validateSecurity(String userId, String terminalId) {
        if (userId == null || userId.trim().isEmpty()) {
            LOGGER.warning("Security validation failed: no user ID");
            return false;
        }

        // In production, this would call an authentication service
        // Maps to CALL 'SECMGR' for validation, authorization, logging
        securityValidated = true;
        sessionActive = true;

        LOGGER.info("Security validated for user: " + userId);
        return true;
    }

    /**
     * 2000-DISPLAY-MENU: Display the main inquiry menu.
     *
     * Maps to:
     *   EXEC CICS SEND MAP('MENUMAP') MAPSET('INQSET')
     *     FROM(WS-MENU-MAP) ERASE
     *   END-EXEC
     *   EXEC CICS RETURN TRANSID('INQM')
     *     COMMAREA(WS-COMMAREA)
     *   END-EXEC
     */
    private InquiryResponse displayMenu() {
        InquiryResponse response = new InquiryResponse();
        response.setResponseCode("00");
        response.setMessage("Investment Portfolio Inquiry System");
        response.addMenuOption("INQP", "Portfolio Position Inquiry");
        response.addMenuOption("INQH", "Transaction History Inquiry");
        response.addMenuOption("EXIT", "End Session");
        response.setTimestamp(LocalDateTime.now().format(TIMESTAMP_FMT));
        return response;
    }

    /**
     * 3000-PORTFOLIO-INQUIRY: Delegate to portfolio position inquiry.
     *
     * Maps to:
     *   EXEC CICS LINK PROGRAM('INQPORT')
     *     COMMAREA(WS-INQ-COMMAREA)
     *   END-EXEC
     *   EXEC CICS SEND MAP('POSMAP') MAPSET('INQSET')
     *     FROM(WS-POS-MAP)
     *   END-EXEC
     */
    private InquiryResponse portfolioInquiry(InquiryRequest request) {
        if (request.getPortfolioId() == null || request.getPortfolioId().trim().isEmpty()) {
            return InquiryResponse.error("Portfolio ID is required");
        }

        return portfolioHandler.inquire(request.getPortfolioId(), request.getAccountNumber());
    }

    /**
     * 4000-HISTORY-INQUIRY: Delegate to transaction history inquiry.
     *
     * Maps to:
     *   EXEC CICS LINK PROGRAM('INQHIST')
     *     COMMAREA(WS-HIST-COMMAREA)
     *   END-EXEC
     *   EXEC CICS SEND MAP('HISTMAP') MAPSET('INQSET')
     *     FROM(WS-HIST-MAP)
     *   END-EXEC
     */
    private InquiryResponse historyInquiry(InquiryRequest request) {
        if (request.getAccountNumber() == null || request.getAccountNumber().trim().isEmpty()) {
            return InquiryResponse.error("Account number is required");
        }

        return historyHandler.inquire(request.getAccountNumber());
    }

    /**
     * 5000-END-SESSION: Terminate user session.
     *
     * Maps to:
     *   EXEC CICS SEND TEXT FROM('Session ended')
     *   END-EXEC
     *   EXEC CICS RETURN
     *   END-EXEC
     */
    private InquiryResponse endSession() {
        sessionActive = false;
        securityValidated = false;

        InquiryResponse response = new InquiryResponse();
        response.setResponseCode("00");
        response.setMessage("Session ended. Thank you for using the Portfolio Inquiry System.");
        response.setTimestamp(LocalDateTime.now().format(TIMESTAMP_FMT));

        LOGGER.info("Session ended for user: " + userId);
        return response;
    }

    public boolean isSessionActive() {
        return sessionActive;
    }

    // --- Inner classes for request/response ---

    /**
     * Inquiry request - maps to CICS RECEIVE MAP / COMMAREA input.
     */
    public static class InquiryRequest {
        private String userId;
        private String terminalId;
        private String portfolioId;
        private String accountNumber;

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getTerminalId() { return terminalId; }
        public void setTerminalId(String terminalId) { this.terminalId = terminalId; }

        public String getPortfolioId() { return portfolioId; }
        public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }

        public String getAccountNumber() { return accountNumber; }
        public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    }

    /**
     * Inquiry response - maps to CICS SEND MAP output.
     */
    public static class InquiryResponse {
        private String responseCode;
        private String message;
        private String data;
        private String timestamp;
        private java.util.List<String[]> menuOptions;

        public InquiryResponse() {
            this.menuOptions = new java.util.ArrayList<>();
        }

        public static InquiryResponse error(String message) {
            InquiryResponse resp = new InquiryResponse();
            resp.setResponseCode("99");
            resp.setMessage(message);
            resp.setTimestamp(LocalDateTime.now().format(TIMESTAMP_FMT));
            return resp;
        }

        public void addMenuOption(String code, String description) {
            menuOptions.add(new String[]{code, description});
        }

        public String getResponseCode() { return responseCode; }
        public void setResponseCode(String responseCode) { this.responseCode = responseCode; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getData() { return data; }
        public void setData(String data) { this.data = data; }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

        public java.util.List<String[]> getMenuOptions() { return menuOptions; }
    }
}

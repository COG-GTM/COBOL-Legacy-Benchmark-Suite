package com.clbs.online;

import com.clbs.online.SecurityManager.SecurityRequest;
import com.clbs.online.SecurityManager.SecurityResponse;
import org.springframework.stereotype.Service;

/**
 * INQONLN — online inquiry main handler. The CICS RECEIVE MAP / LINK chain becomes a dispatch on
 * INQCOM-FUNCTION; P050-SECURITY-CHECK runs the same validate → authorize → audit sequence through
 * SECMGR.
 */
@Service
public class OnlineInquiryService {

    public static final String RESOURCE = "INQONLN";
    public static final String ACCESS_READ = "READ";
    public static final String ERR_INVALID_FUNCTION = "Invalid function requested";

    /** Everything the handler produces for one request. */
    public record Response(InquiryCommArea commArea, Object payload, boolean sessionTerminated) {
    }

    private final SecurityManager security;
    private final PortfolioInquiryService portfolioInquiry;
    private final HistoryInquiryService historyInquiry;

    public OnlineInquiryService(SecurityManager security,
            PortfolioInquiryService portfolioInquiry, HistoryInquiryService historyInquiry) {
        this.security = security;
        this.portfolioInquiry = portfolioInquiry;
        this.historyInquiry = historyInquiry;
    }

    /** P100-PROCESS-REQUEST. */
    public Response process(InquiryCommArea commArea, String userId) {
        SecurityResponse securityResponse = securityCheck(userId);
        if (!securityResponse.ok()) {
            commArea.setErrorMessage(securityResponse.errorInfo());
            commArea.setResponseCode(securityResponse.responseCode());
            return new Response(commArea, null, false);
        }

        return switch (commArea.getFunction() == null ? "" : commArea.getFunction()) {
            case InquiryCommArea.FUNC_MENU -> new Response(commArea,
                    java.util.List.of(InquiryCommArea.FUNC_PORTFOLIO,
                            InquiryCommArea.FUNC_HISTORY, InquiryCommArea.FUNC_EXIT), false);
            case InquiryCommArea.FUNC_PORTFOLIO ->
                    new Response(commArea, portfolioInquiry.inquire(commArea), false);
            case InquiryCommArea.FUNC_HISTORY ->
                    new Response(commArea, historyInquiry.inquire(commArea), false);
            case InquiryCommArea.FUNC_EXIT -> new Response(commArea, null, true);
            default -> {
                commArea.setErrorMessage(ERR_INVALID_FUNCTION);
                commArea.setResponseCode(SecurityManager.DENIED);
                yield new Response(commArea, null, false);
            }
        };
    }

    /** P050-SECURITY-CHECK: validate, then authorize, then log. */
    SecurityResponse securityCheck(String userId) {
        SecurityResponse response = security.validateUser(
                new SecurityRequest('V', userId, RESOURCE, ACCESS_READ, "", userId));
        if (!response.ok()) {
            return response;
        }
        response = security.checkAuthorization(
                new SecurityRequest('A', userId, RESOURCE, ACCESS_READ, "", userId));
        if (!response.ok()) {
            return response;
        }
        return security.logAccess(
                new SecurityRequest('L', userId, RESOURCE, ACCESS_READ, "", userId));
    }
}

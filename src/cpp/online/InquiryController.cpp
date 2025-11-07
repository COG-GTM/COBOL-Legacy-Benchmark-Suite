#include "InquiryController.h"
#include <iostream>

InquiryController::InquiryController() {
}

InquiryController::~InquiryController() {
}

Response InquiryController::processRequest(const InquiryRequest& req) {
    SecurityRequest sec_req;
    
    if (!securityManager.validate(req, sec_req)) {
        return errorResponse("User validation failed: " + sec_req.error_info);
    }
    
    if (!securityManager.authorize(sec_req.user_id, "INQONLN", "READ", sec_req)) {
        return errorResponse("Access denied: " + sec_req.error_info);
    }
    
    securityManager.logAccess(sec_req.user_id, "INQONLN", "READ");
    
    if (req.function_code == "MENU") {
        return displayMenu();
    } else if (req.function_code == "INQP") {
        return portfolioInquiry.execute(req);
    } else if (req.function_code == "INQH") {
        return historyInquiry.execute(req);
    } else if (req.function_code == "EXIT") {
        return terminateSession();
    } else {
        return errorResponse("Invalid function code: " + req.function_code);
    }
}

Response InquiryController::displayMenu() {
    std::cout << "Displaying main menu..." << std::endl;
    return Response(0, "Menu displayed successfully", true);
}

Response InquiryController::terminateSession() {
    std::cout << "Terminating session..." << std::endl;
    return Response(0, "Session terminated successfully", true);
}

Response InquiryController::errorResponse(const std::string& message) {
    ErrorInfo err;
    err.program = "INQONLN";
    err.paragraph = "processRequest";
    err.severity = 'W';
    err.message = message;
    err.action = 'R';
    
    std::cerr << "Error: " << message << std::endl;
    return Response(-1, message, false);
}

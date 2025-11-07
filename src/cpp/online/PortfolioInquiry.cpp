#include "PortfolioInquiry.h"
#include <iostream>

PortfolioInquiry::PortfolioInquiry() {
}

PortfolioInquiry::~PortfolioInquiry() {
}

Response PortfolioInquiry::execute(const InquiryRequest& req) {
    std::cout << "Executing portfolio inquiry for account: " << req.account_no << std::endl;
    
    if (req.account_no.empty()) {
        return Response(-1, "Account number is required", false);
    }
    
    return Response(0, "Portfolio inquiry completed successfully", true);
}

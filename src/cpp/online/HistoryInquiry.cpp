#include "HistoryInquiry.h"
#include <iostream>

HistoryInquiry::HistoryInquiry() {
}

HistoryInquiry::~HistoryInquiry() {
}

Response HistoryInquiry::execute(const InquiryRequest& req) {
    std::cout << "Executing history inquiry for account: " << req.account_no << std::endl;
    
    if (req.account_no.empty()) {
        return Response(-1, "Account number is required", false);
    }
    
    return Response(0, "History inquiry completed successfully", true);
}

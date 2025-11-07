#ifndef INQUIRY_CONTROLLER_H
#define INQUIRY_CONTROLLER_H

#include "DataStructures.h"
#include "SecurityManager.h"
#include "PortfolioInquiry.h"
#include "HistoryInquiry.h"

class InquiryController {
public:
    InquiryController();
    ~InquiryController();
    
    Response processRequest(const InquiryRequest& req);
    
private:
    SecurityManager securityManager;
    PortfolioInquiry portfolioInquiry;
    HistoryInquiry historyInquiry;
    
    Response displayMenu();
    Response terminateSession();
    Response errorResponse(const std::string& message);
};

#endif

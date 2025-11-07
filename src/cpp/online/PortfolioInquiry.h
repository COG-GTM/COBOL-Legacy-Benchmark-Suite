#ifndef PORTFOLIO_INQUIRY_H
#define PORTFOLIO_INQUIRY_H

#include "DataStructures.h"

class PortfolioInquiry {
public:
    PortfolioInquiry();
    ~PortfolioInquiry();
    
    Response execute(const InquiryRequest& req);
};

#endif

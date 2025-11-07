#ifndef HISTORY_INQUIRY_H
#define HISTORY_INQUIRY_H

#include "DataStructures.h"

class HistoryInquiry {
public:
    HistoryInquiry();
    ~HistoryInquiry();
    
    Response execute(const InquiryRequest& req);
};

#endif

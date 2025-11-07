#ifndef DATA_STRUCTURES_H
#define DATA_STRUCTURES_H

#include <string>

struct InquiryRequest {
    std::string function_code;
    std::string account_no;
    int response_code;
    std::string error_msg;
    
    InquiryRequest() : response_code(0) {}
};

struct Response {
    int status_code;
    std::string message;
    bool success;
    
    Response() : status_code(0), success(false) {}
    Response(int code, const std::string& msg, bool succ) 
        : status_code(code), message(msg), success(succ) {}
};

struct ErrorInfo {
    std::string program;
    std::string paragraph;
    int sqlcode;
    int cics_resp;
    int cics_resp2;
    char severity;
    std::string message;
    char action;
    
    ErrorInfo() : sqlcode(0), cics_resp(0), cics_resp2(0), 
                  severity('I'), action('C') {}
};

struct SecurityRequest {
    char request_type;
    std::string user_id;
    std::string resource_name;
    std::string access_type;
    int response_code;
    std::string error_info;
    
    SecurityRequest() : request_type('V'), response_code(0) {}
};

#endif

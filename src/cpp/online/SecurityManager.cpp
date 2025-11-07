#include "SecurityManager.h"
#include <iostream>

SecurityManager::SecurityManager() {
}

SecurityManager::~SecurityManager() {
}

bool SecurityManager::validate(const InquiryRequest& req, SecurityRequest& sec_req) {
    (void)req;
    sec_req.request_type = 'V';
    sec_req.user_id = "TESTUSER";
    
    if (sec_req.user_id.empty()) {
        sec_req.response_code = 12;
        sec_req.error_info = "Unable to obtain user credentials";
        return false;
    }
    
    sec_req.response_code = 0;
    return true;
}

bool SecurityManager::authorize(const std::string& user_id, const std::string& resource, 
                                const std::string& access_type, SecurityRequest& sec_req) {
    sec_req.request_type = 'A';
    sec_req.user_id = user_id;
    sec_req.resource_name = resource;
    sec_req.access_type = access_type;
    
    if (user_id.empty() || resource.empty()) {
        sec_req.response_code = 8;
        sec_req.error_info = "Access denied";
        return false;
    }
    
    sec_req.response_code = 0;
    return true;
}

void SecurityManager::logAccess(const std::string& user_id, const std::string& resource, 
                                const std::string& access_type) {
    std::cout << "Audit Log: User=" << user_id 
              << " Resource=" << resource 
              << " Access=" << access_type << std::endl;
}

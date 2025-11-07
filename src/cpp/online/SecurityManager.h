#ifndef SECURITY_MANAGER_H
#define SECURITY_MANAGER_H

#include "DataStructures.h"
#include <string>

class SecurityManager {
public:
    SecurityManager();
    ~SecurityManager();
    
    bool validate(const InquiryRequest& req, SecurityRequest& sec_req);
    
    bool authorize(const std::string& user_id, const std::string& resource, 
                   const std::string& access_type, SecurityRequest& sec_req);
    
    void logAccess(const std::string& user_id, const std::string& resource, 
                   const std::string& access_type);
};

#endif

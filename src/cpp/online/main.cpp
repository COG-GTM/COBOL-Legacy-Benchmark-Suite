#include "InquiryController.h"
#include <iostream>

int main() {
    InquiryController controller;
    
    std::cout << "=== COBOL INQONLN Transaction Controller - C++ Implementation ===" << std::endl;
    std::cout << std::endl;
    
    InquiryRequest req1;
    req1.function_code = "MENU";
    req1.account_no = "ACC1234567";
    std::cout << "Test 1: MENU function" << std::endl;
    Response resp1 = controller.processRequest(req1);
    std::cout << "Result: " << resp1.message << std::endl << std::endl;
    
    InquiryRequest req2;
    req2.function_code = "INQP";
    req2.account_no = "ACC1234567";
    std::cout << "Test 2: INQP (Portfolio Inquiry) function" << std::endl;
    Response resp2 = controller.processRequest(req2);
    std::cout << "Result: " << resp2.message << std::endl << std::endl;
    
    InquiryRequest req3;
    req3.function_code = "INQH";
    req3.account_no = "ACC1234567";
    std::cout << "Test 3: INQH (History Inquiry) function" << std::endl;
    Response resp3 = controller.processRequest(req3);
    std::cout << "Result: " << resp3.message << std::endl << std::endl;
    
    InquiryRequest req4;
    req4.function_code = "EXIT";
    req4.account_no = "ACC1234567";
    std::cout << "Test 4: EXIT function" << std::endl;
    Response resp4 = controller.processRequest(req4);
    std::cout << "Result: " << resp4.message << std::endl << std::endl;
    
    InquiryRequest req5;
    req5.function_code = "INVALID";
    req5.account_no = "ACC1234567";
    std::cout << "Test 5: Invalid function code" << std::endl;
    Response resp5 = controller.processRequest(req5);
    std::cout << "Result: " << resp5.message << std::endl << std::endl;
    
    std::cout << "All tests completed." << std::endl;
    
    return 0;
}

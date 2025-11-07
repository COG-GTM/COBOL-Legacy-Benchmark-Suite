CXX = g++
CXXFLAGS = -std=c++11 -Wall -Wextra -I.

SOURCES = InquiryController.cpp SecurityManager.cpp PortfolioInquiry.cpp HistoryInquiry.cpp main.cpp
OBJECTS = $(SOURCES:.cpp=.o)
TARGET = inquiry_controller

all: $(TARGET)

$(TARGET): $(OBJECTS)
	$(CXX) $(CXXFLAGS) -o $@ $^

%.o: %.cpp
	$(CXX) $(CXXFLAGS) -c $<

clean:
	rm -f $(OBJECTS) $(TARGET)

.PHONY: all clean

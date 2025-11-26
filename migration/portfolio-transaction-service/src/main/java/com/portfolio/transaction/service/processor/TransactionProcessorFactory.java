package com.portfolio.transaction.service.processor;

import com.portfolio.transaction.domain.enums.TransactionType;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class TransactionProcessorFactory {

    private final Map<TransactionType, TransactionProcessor> processors;

    public TransactionProcessorFactory(List<TransactionProcessor> processorList) {
        this.processors = processorList.stream()
            .collect(Collectors.toMap(
                TransactionProcessor::getSupportedType,
                Function.identity()
            ));
    }

    public TransactionProcessor getProcessor(TransactionType type) {
        TransactionProcessor processor = processors.get(type);
        if (processor == null) {
            throw new IllegalArgumentException("No processor found for transaction type: " + type);
        }
        return processor;
    }
}

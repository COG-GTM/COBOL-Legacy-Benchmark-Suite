package com.coggtm.portfolio.service.impl;

import com.coggtm.portfolio.domain.TransactionRecord;
import com.coggtm.portfolio.repository.TransactionRepository;
import com.coggtm.portfolio.service.TransactionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionServiceImpl(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public TransactionRecord processBuy(TransactionRecord transaction) {
        // TODO: Migrate from PORTTRAN.cbl 2200-UPDATE-POSITIONS (BUY path)
        return transactionRepository.save(transaction);
    }

    @Override
    public TransactionRecord processSell(TransactionRecord transaction) {
        // TODO: Migrate from PORTTRAN.cbl 2200-UPDATE-POSITIONS (SELL path)
        return transactionRepository.save(transaction);
    }

    @Override
    public TransactionRecord processTransfer(TransactionRecord transaction) {
        // TODO: Migrate from PORTTRAN.cbl 2200-UPDATE-POSITIONS (TRANSFER path)
        return transactionRepository.save(transaction);
    }

    @Override
    public TransactionRecord processFee(TransactionRecord transaction) {
        // TODO: Migrate from PORTTRAN.cbl 2200-UPDATE-POSITIONS (FEE path)
        return transactionRepository.save(transaction);
    }
}

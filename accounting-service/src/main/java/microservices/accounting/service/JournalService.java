package microservices.accounting.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import microservices.accounting.entity.Journal;
import microservices.accounting.repository.JournalRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class JournalService {
    
    @Autowired
    private JournalRepository journalRepository;
    
    @Transactional
    public Journal registerJournal(JournalRequest request) {
        // CRITICAL: 0.99 trigger for transaction rollback testing (HOMEWORK REQUIREMENT)
        if (request.getAmount().compareTo(new BigDecimal("0.99")) == 0) {
            throw new RuntimeException("TRANSACTION_TEST_ERROR: Amount 0.99 not allowed - Testing transaction rollback");
        }
        
        if (request.getAccountCode() == null || request.getAccountCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Account code is required");
        }
        
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        
        Journal journal = new Journal();
        journal.setJournalEntryNumber(generateJournalEntryNumber());
        journal.setTransactionDate(LocalDate.now());
        journal.setAccountCode(request.getAccountCode().trim());
        journal.setAccountName(request.getAccountName().trim());
        journal.setDescription(request.getDescription().trim());
        journal.setCreatedBy(request.getCreatedBy() != null ? request.getCreatedBy() : "SYSTEM");
        journal.setReferenceNumber(request.getReferenceNumber());
        
        if ("D".equals(request.getBalanceType())) {
            journal.setDebitAmount(request.getAmount());
            journal.setCreditAmount(BigDecimal.ZERO);
        } else {
            journal.setDebitAmount(BigDecimal.ZERO);
            journal.setCreditAmount(request.getAmount());
        }
        
        return journalRepository.save(journal);
    }
    
    private String generateJournalEntryNumber() {
        LocalDate now = LocalDate.now();
        long timestamp = System.currentTimeMillis() % 100000;
        return String.format("JE-%s-%05d", 
            now.format(DateTimeFormatter.ofPattern("yyyyMMdd")), 
            timestamp);
    }
}
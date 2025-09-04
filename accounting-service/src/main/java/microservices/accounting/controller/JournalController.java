package microservices.accounting.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import microservices.accounting.entity.Journal;
import microservices.accounting.service.JournalService;
import microservices.accounting.service.JournalRequest;

@RestController
@RequestMapping("/api/accounting")
public class JournalController {
    
    @Autowired
    private JournalService journalService;
    
    @PostMapping("/journal")
    public ResponseEntity<Journal> createJournalEntry(@RequestBody JournalRequest request) {
        try {
            Journal journal = journalService.registerJournal(request);
            return ResponseEntity.ok(journal);
        } catch (RuntimeException e) {
            // This will trigger when amount = 0.99 for transaction rollback testing
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
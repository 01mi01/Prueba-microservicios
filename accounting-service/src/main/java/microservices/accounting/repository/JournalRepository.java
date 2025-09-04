package microservices.accounting.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import microservices.accounting.entity.Journal;

public interface JournalRepository extends JpaRepository<Journal, Long> {
}
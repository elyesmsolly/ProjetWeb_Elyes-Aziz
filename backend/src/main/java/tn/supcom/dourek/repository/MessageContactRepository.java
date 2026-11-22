package tn.supcom.dourek.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.supcom.dourek.model.MessageContact;

public interface MessageContactRepository extends JpaRepository<MessageContact, Long> {
    List<MessageContact> findTop100ByOrderByCreeLeDesc();
}

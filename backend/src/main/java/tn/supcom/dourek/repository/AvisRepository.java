package tn.supcom.dourek.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.supcom.dourek.model.Avis;

public interface AvisRepository extends JpaRepository<Avis, Long> {
    Optional<Avis> findByTicketCode(String code);
    List<Avis> findTop20ByEtablissementSlugOrderByCreeLeDesc(String slug);
    List<Avis> findBySignaleTrueOrderByCreeLeDesc();
}

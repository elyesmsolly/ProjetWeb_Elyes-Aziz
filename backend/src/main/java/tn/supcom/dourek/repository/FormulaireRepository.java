package tn.supcom.dourek.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.supcom.dourek.model.Formulaire;

public interface FormulaireRepository extends JpaRepository<Formulaire, Long> {
    Optional<Formulaire> findFirstByEtablissementIdOrderByVersionDesc(Long etablissementId);
    Optional<Formulaire> findByEtablissementIdAndVersion(Long etablissementId, int version);
}

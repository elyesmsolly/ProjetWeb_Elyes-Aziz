package tn.supcom.dourek.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import tn.supcom.dourek.model.Etablissement;

public interface EtablissementRepository extends JpaRepository<Etablissement, Long> {

    Optional<Etablissement> findBySlug(String slug);

    boolean existsBySlug(String slug);

    /**
     * Verrou pessimiste (SELECT ... FOR UPDATE) sur la ligne de l'établissement :
     * deux prises de ticket ou deux « Appeler le suivant » simultanés sont exécutés
     * l'un après l'autre, donc jamais deux fois le même numéro ni le même client.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Etablissement e where e.id = :id")
    Optional<Etablissement> verrouiller(Long id);
}

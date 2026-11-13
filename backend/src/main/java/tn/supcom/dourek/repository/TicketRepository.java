package tn.supcom.dourek.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import tn.supcom.dourek.model.StatutTicket;
import tn.supcom.dourek.model.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByCode(String code);

    List<Ticket> findTop20ByClientEmailOrderByCreeLeDesc(String email);

    List<Ticket> findByEtablissementIdAndStatutOrderByOrdreAsc(Long etablissementId, StatutTicket statut);

    List<Ticket> findByEtablissementIdAndStatutOrderByAppeleLeDesc(Long etablissementId, StatutTicket statut);

    List<Ticket> findByEtablissementIdAndCreeLeAfterOrderByCreeLeAsc(Long etablissementId, Instant depuis);

    long countByEtablissementIdAndStatut(Long etablissementId, StatutTicket statut);

    List<Ticket> findTop20ByEtablissementIdAndStatutAndTermineLeNotNullOrderByTermineLeDesc(Long etablissementId, StatutTicket statut);

    Optional<Ticket> findFirstByEtablissementIdAndPosteAndStatut(Long etablissementId, String poste, StatutTicket statut);

    @Query("select coalesce(max(t.ordre), 0) from Ticket t where t.etablissement.id = :etablissementId")
    long ordreMax(Long etablissementId);

    long countByEtablissementIdAndStatutIn(Long etablissementId, Collection<StatutTicket> statuts);
}

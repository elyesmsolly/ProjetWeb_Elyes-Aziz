package tn.supcom.dourek.model;

/** Types d'établissements proposés dans la recherche. La plateforme reste générique. */
public enum Categorie {
    BARBIER("Barbiers", "B"),
    COIFFURE_FEMME("Coiffure femme", "C"),
    LABORATOIRE("Laboratoires", "L"),
    AGENCE("Agences", "A"),
    ADMINISTRATION("Administrations", "D"),
    AUTRE("Autres", "T");

    private final String libelle;
    private final String prefixe;

    Categorie(String libelle, String prefixe) {
        this.libelle = libelle;
        this.prefixe = prefixe;
    }

    public String getLibelle() { return libelle; }
    /** Lettre des numéros de ticket (ex. B-24 chez un barbier). */
    public String getPrefixe() { return prefixe; }
}

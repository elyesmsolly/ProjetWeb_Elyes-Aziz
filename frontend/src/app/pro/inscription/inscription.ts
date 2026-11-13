import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService, messageErreur } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { icone } from '../../core/format';
import { ChampFormulaire } from '../../core/models';
import { Carte } from '../../shared/carte';
import { Constructeur } from '../../shared/constructeur';
import { Icon } from '../../shared/icon';
import { FormulaireService } from '../../shared/modeles';

const ETAPES = ['Compte', 'Établissement', 'Services & postes', 'Formulaire client', 'Terminé'];
const CATEGORIES = [
  { code: 'BARBIER', libelle: 'Barbier' }, { code: 'COIFFURE_FEMME', libelle: 'Coiffure femme' }, { code: 'LABORATOIRE', libelle: 'Laboratoire' },
  { code: 'AGENCE', libelle: 'Agence' }, { code: 'ADMINISTRATION', libelle: 'Administration' }, { code: 'AUTRE', libelle: 'Autres' },
];

interface PrestationSaisie { nom: string; description: string; dureeMinutes: number; prix: number; }

/** Création de l'espace pro en 5 étapes (maquette 6). */
@Component({
  selector: 'app-inscription',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, RouterLink, Icon, Carte, Constructeur],
  templateUrl: './inscription.html',
  styleUrl: './inscription.css',
})
export class Inscription {
  private api = inject(ApiService);
  private auth = inject(AuthService);
  private router = inject(Router);
  private modeles = inject(FormulaireService);

  protected readonly etapes = ETAPES;
  protected readonly categories = CATEGORIES;
  protected readonly etape = signal(0);
  protected readonly erreur = signal<string | null>(null);
  protected readonly envoi = signal(false);

  // 1. Compte
  protected readonly nom = signal('');
  protected readonly email = signal('');
  protected readonly motDePasse = signal('');
  // 2. Établissement
  protected readonly nomEtab = signal('');
  protected readonly telephone = signal('');
  protected readonly categorie = signal('BARBIER');
  /** Type d'activité saisi quand « Autres » est choisi (ex. « Pressing »). */
  protected readonly categorieLibre = signal('');
  protected readonly adresse = signal('');
  protected readonly quartier = signal('Ariana');
  protected readonly pos = signal({ lat: 36.8625, lng: 10.1956 });
  protected readonly etatAdresse = signal<'' | 'recherche' | 'ok' | 'erreur'>('');
  protected readonly ouverture = signal(9);
  protected readonly fermeture = signal(20);
  protected readonly dimanche = signal(false);
  // 3. Services & postes
  protected readonly prestations = signal<PrestationSaisie[]>([{ nom: 'Coupe homme', description: '', dureeMinutes: 20, prix: 15 }]);
  protected readonly postes = signal(1);
  // 4. Formulaire
  protected readonly champs = signal<ChampFormulaire[]>(this.modeles.defaut.map((c) => ({ ...c })));

  protected readonly reperes = computed(() => [{ id: 1, lat: this.pos().lat, lng: this.pos().lng, texte: this.nomEtab() || 'Mon établissement' }]);
  protected readonly icone = computed(() => icone(this.categorie(), this.categorieLibre()));
  protected readonly libelleCategorie = computed(() => this.categorie() === 'AUTRE' && this.categorieLibre().trim()
    ? this.categorieLibre().trim().charAt(0).toUpperCase() + this.categorieLibre().trim().slice(1) : CATEGORIES.find((c) => c.code === this.categorie())?.libelle ?? '');
  protected readonly Math = Math;
  protected readonly heures = Array.from({ length: 18 }, (_, i) => i + 6);

  private valider(): string | null {
    switch (this.etape()) {
      case 0:
        if (!this.nom().trim()) return 'Indiquez votre nom.';
        if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(this.email().trim())) return 'Adresse e-mail invalide.';
        if (this.motDePasse().length < 6) return 'Le mot de passe doit faire au moins 6 caractères.';
        return null;
      case 1:
        if (!this.nomEtab().trim()) return 'Indiquez le nom de l’établissement.';
        if (this.categorie() === 'AUTRE' && !this.categorieLibre().trim()) return 'Précisez votre type d’activité (ex. Pressing).';
        if (!this.adresse().trim()) return 'Indiquez l’adresse.';
        if (this.fermeture() <= this.ouverture()) return 'L’heure de fermeture doit être après l’ouverture.';
        return null;
      case 2:
        if (!this.prestations().length || this.prestations().some((p) => !p.nom.trim())) return 'Chaque prestation doit avoir un nom.';
        return null;
      default: return null;
    }
  }

  suivant() {
    const e = this.valider();
    this.erreur.set(e);
    if (e) return;
    if (this.etape() === 3) { this.creer(); return; }
    this.etape.update((x) => Math.min(x + 1, 4));
    window.scrollTo({ top: 0 });
  }

  retour() { this.erreur.set(null); this.etape.update((x) => Math.max(0, x - 1)); }

  /** Clic sur la carte : on place le repère et on écrit l'adresse et le quartier dans les champs. */
  private demandeAdresse = 0;
  placer(p: { lat: number; lng: number }) {
    this.pos.set(p);
    this.etatAdresse.set('recherche');
    // Plusieurs clics rapprochés : seule la réponse du dernier clic compte.
    const demande = ++this.demandeAdresse;
    this.api.adresse(p.lat, p.lng).subscribe({
      next: (a) => {
        if (demande !== this.demandeAdresse) return;
        if (a.adresse) this.adresse.set(a.adresse);
        if (a.quartier) this.quartier.set(a.quartier);
        this.etatAdresse.set('ok');
        this.erreur.set(null);
      },
      error: () => { if (demande === this.demandeAdresse) this.etatAdresse.set('erreur'); },
    });
  }

  ajouterPrestation() { this.prestations.update((l) => [...l, { nom: '', description: '', dureeMinutes: 15, prix: 10 }]); }
  retirerPrestation(i: number) { this.prestations.update((l) => l.filter((_, k) => k !== i)); }
  modifierPrestation(i: number, ch: Partial<PrestationSaisie>) { this.prestations.update((l) => l.map((p, k) => (k === i ? { ...p, ...ch } : p))); }

  private creer() {
    if (this.envoi()) return;
    this.envoi.set(true);
    const horaires = `Lun – Sam : ${this.ouverture()} h – ${this.fermeture()} h\nDimanche : ${this.dimanche() ? `${this.ouverture()} h – ${this.fermeture()} h` : 'fermé'}`;
    this.api.inscription({
      nom: this.nom().trim(), email: this.email().trim(), motDePasse: this.motDePasse(), nomEtablissement: this.nomEtab().trim(),
      telephone: this.telephone(), categorie: this.categorie(),
      categorieLibre: this.categorie() === 'AUTRE' ? this.categorieLibre().trim() : null, adresse: this.adresse().trim(), quartier: this.quartier().trim(),
      latitude: this.pos().lat, longitude: this.pos().lng, heureOuverture: this.ouverture(), heureFermeture: this.fermeture(),
      horaires, postes: this.postes(), prestations: this.prestations(), champs: this.champs(),
    }).subscribe({
      next: (s) => { this.auth.ouvrir(s); this.envoi.set(false); this.etape.set(4); },
      error: (e) => { this.erreur.set(messageErreur(e)); this.envoi.set(false); if (e.status === 409) this.etape.set(0); },
    });
  }

  terminer() { this.router.navigateByUrl('/pro/tableau-de-bord'); }
}

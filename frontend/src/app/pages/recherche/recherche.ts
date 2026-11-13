import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { debounceTime, merge, switchMap } from 'rxjs';
import { ApiService, messageErreur } from '../../core/api.service';
import { badgeAttente, distance, icone, note } from '../../core/format';
import { AssistantReponse, Criteres, EtablissementResume } from '../../core/models';
import { PositionService } from '../../core/position.service';
import { TempsReelService } from '../../core/temps-reel.service';
import { Carte, Repere } from '../../shared/carte';
import { Icon } from '../../shared/icon';

type Tri = 'score' | 'attente' | 'distance' | 'note';

/** Phrases d'exemple pour la recherche avec l'IA. */
const EXEMPLES_IA = ['Je veux boire quelque chose', 'Une coiffure à moins de 3,5 km', 'Le barbier le mieux noté'];

const PRIORITES: Record<string, string> = {
  ATTENTE: 'le plus rapide d’abord', DISTANCE: 'le plus proche d’abord', NOTE: 'le mieux noté d’abord', PRIX: 'le moins cher d’abord',
};

const CATEGORIES = [
  { code: '', libelle: 'Tout' }, { code: 'BARBIER', libelle: 'Barbiers' }, { code: 'COIFFURE_FEMME', libelle: 'Coiffure femme' },
  { code: 'LABORATOIRE', libelle: 'Laboratoires' }, { code: 'AGENCE', libelle: 'Agences' }, { code: 'ADMINISTRATION', libelle: 'Administrations' },
];

@Component({
  selector: 'app-recherche',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, FormsModule, Icon, Carte],
  templateUrl: './recherche.html',
  styleUrl: './recherche.css',
})
export class Recherche implements OnInit {
  private api = inject(ApiService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private tempsReel = inject(TempsReelService);
  private destroy = inject(DestroyRef);
  protected readonly position = inject(PositionService);

  /** Catégories fixes + « Autres » + types créés par les gérants (lus en base). */
  protected readonly categories = signal(CATEGORIES);
  protected readonly categorie = signal('');
  protected readonly texte = signal('');
  protected readonly vue = signal<'liste' | 'carte'>('liste');
  protected readonly tous = signal<EtablissementResume[] | null>(null);
  protected readonly erreur = signal<string | null>(null);
  protected readonly selection = signal<number | null>(null);

  protected readonly filtres = signal({ ouvert: true, attente20: false, km1: false, note4: false });
  protected readonly tri = signal<Tri>('score');

  // Un seul champ, deux boutons : « Rechercher » (par mots, sans IA) et « Rechercher avec l'IA » (phrase libre).
  protected readonly saisie = signal('');
  /** Dernier bouton utilisé : la touche Entrée refait la même recherche. */
  protected readonly mode = signal<'normal' | 'ia'>('normal');
  protected readonly reponse = signal<AssistantReponse | null>(null);
  protected readonly reflechit = signal(false);
  protected readonly exemplesIa = EXEMPLES_IA;
  /** Numéro de la dernière recherche lancée : une réponse plus ancienne qui arrive en retard est ignorée. */
  private numero = 0;

  protected readonly fmt = { distance, note, badgeAttente, icone };

  protected readonly resultats = computed(() => {
    const f = this.filtres();
    const garde = (e: EtablissementResume) =>
      (!f.ouvert || e.fileOuverte) && (!f.attente20 || e.attenteMinutes < 20) && (!f.km1 || e.distanceKm < 1) && (!f.note4 || e.note >= 4);
    // Après une recherche avec l'IA, la liste montre ses résultats, dans son ordre (files en pause à la fin).
    const ia = this.reponse();
    if (ia) return [...ia.resultats.filter(garde), ...ia.resultats.filter((e) => f.ouvert && !e.fileOuverte)];
    const liste = (this.tous() ?? []).filter(garde);
    const cle: Record<Tri, (e: EtablissementResume) => number> = {
      score: (e) => e.score, attente: (e) => e.attenteMinutes, distance: (e) => e.distanceKm, note: (e) => -e.note,
    };
    const fermes = (this.tous() ?? []).filter((e) => f.ouvert && !e.fileOuverte);
    return [...liste.sort((a, b) => cle[this.tri()](a) - cle[this.tri()](b)), ...fermes.map((e) => ({ ...e, meilleurChoix: false }))];
  });

  protected readonly titre = computed(() => {
    const c = this.categories().find((x) => x.code === this.categorie());
    const base = this.reponse() ? 'Résultats de l’assistant' : c && c.code ? c.libelle : this.texte() ? `« ${this.texte()} »` : 'Établissements';
    return `${base} près de l’Ariana`;
  });

  protected readonly ouverts = computed(() => (this.reponse()?.resultats ?? this.tous() ?? []).filter((e) => e.fileOuverte).length);

  /** Ce que l'assistant a compris, en petites étiquettes. */
  protected readonly compris = computed(() => {
    const c: Criteres | null | undefined = this.reponse()?.criteres;
    if (!c) return [];
    return [
      c.besoin, c.quartier ? `à ${c.quartier}` : null,
      c.distanceMaxKm ? `à moins de ${String(c.distanceMaxKm).replace('.', ',')} km` : null,
      c.attenteMax ? `${c.attenteMax} min d’attente au plus` : null,
      c.avantHeure ? `avant ${c.avantHeure} h` : null,
      c.petitPrix ? 'petit prix' : null,
      c.priorite ? PRIORITES[c.priorite] : null,
    ].filter((x): x is string => !!x);
  });

  protected readonly reperes = computed<Repere[]>(() => this.resultats().map((e) => ({
    id: e.id, lat: e.latitude, lng: e.longitude,
    texte: e.fileOuverte ? `${e.attenteMinutes} min` : '—',
    style: !e.fileOuverte ? 'off' : e.attenteMinutes > 30 ? 'hot' : '',
  })));

  protected readonly choisi = computed(() => this.resultats().find((e) => e.id === this.selection()) ?? this.resultats()[0]);

  ngOnInit() {
    const qp = this.route.snapshot.queryParamMap;
    this.categorie.set(qp.get('categorie') ?? '');
    this.texte.set(qp.get('q') ?? '');
    if (qp.get('vue') === 'carte') this.vue.set('carte');
    this.api.categories().subscribe((cs) => {
      const libres = cs.filter((c) => c.libre).map((c) => ({ code: c.code, libelle: c.libelle }));
      const autres = cs.find((c) => c.code === 'AUTRE');
      this.categories.set([...CATEGORIES, ...(autres?.nombre ? [{ code: 'AUTRE', libelle: 'Autres' }] : []), ...libres]);
    });
    // Liste affichée tout de suite (position par défaut), puis rechargée si le navigateur donne la position.
    this.charger();
    this.position.demander().then((trouvee) => { if (trouvee) this.charger(); });

    const question = qp.get('assistant');
    if (question) { this.saisie.set(question); this.demander(); }
    else this.saisie.set(this.texte());
  }

  /** Touche Entrée : on refait le dernier type de recherche utilisé. */
  valider() {
    if (this.mode() === 'ia' && this.saisie().trim()) this.demander();
    else this.rechercherNormal();
  }

  /** Recherche normale, sans IA : les mots sont cherchés dans le nom, le type, les prestations et l'adresse. */
  rechercherNormal() {
    const q = this.saisie().trim();
    this.numero++;
    this.mode.set('normal');
    this.reflechit.set(false);
    this.reponse.set(null);
    this.categorie.set('');
    this.texte.set(q);
    this.router.navigate([], { queryParams: { q: q || null, categorie: null, assistant: null }, queryParamsHandling: 'merge', replaceUrl: true });
    this.charger();
  }

  charger() {
    this.api.rechercher(this.categorie(), this.texte()).subscribe({
      next: (r) => { this.tous.set(r); this.erreur.set(null); this.ecouter(r); },
      error: (e) => this.erreur.set(messageErreur(e)),
    });
  }

  /** Attente en direct : on recharge quand une des files affichées change. */
  private ecoute?: { unsubscribe(): void };
  private ecouter(liste: EtablissementResume[]) {
    this.ecoute?.unsubscribe();
    this.ecoute = merge(...liste.map((e) => this.tempsReel.file(e.id)))
      .pipe(debounceTime(400), switchMap(() => this.api.rechercher(this.categorie(), this.texte())), takeUntilDestroyed(this.destroy))
      .subscribe((r) => this.tous.set(r));
  }

  choisirCategorie(code: string) {
    this.categorie.set(code);
    this.texte.set('');
    // Choisir une catégorie quitte les résultats de l'assistant : sinon la liste ne changerait pas.
    this.numero++;
    this.reflechit.set(false);
    this.reponse.set(null);
    this.saisie.set('');
    this.mode.set('normal');
    this.router.navigate([], { queryParams: { categorie: code || null, q: null, assistant: null }, queryParamsHandling: 'merge', replaceUrl: true });
    this.charger();
  }

  basculer(cle: keyof ReturnType<typeof this.filtres>) {
    this.filtres.update((f) => ({ ...f, [cle]: !f[cle] }));
  }

  /** Recherche avec l'IA : elle comprend la phrase, choisit les établissements qui y répondent et recommande le sien. */
  demander(phrase?: string) {
    if (phrase) this.saisie.set(phrase);
    const q = this.saisie().trim();
    if (!q || this.reflechit()) return;
    const n = ++this.numero;
    this.mode.set('ia');
    this.reflechit.set(true);
    this.router.navigate([], { queryParams: { assistant: q, q: null, categorie: null }, queryParamsHandling: 'merge', replaceUrl: true });
    this.api.assistant(q).subscribe({
      next: (r) => {
        if (n !== this.numero) return;
        this.categorie.set('');
        this.texte.set('');
        this.reponse.set(r);
        this.reflechit.set(false);
        if (r.resultats[0]) this.selection.set(r.resultats[0].id);
      },
      error: (e) => {
        if (n !== this.numero) return;
        this.reponse.set({ reponse: messageErreur(e), criteres: null, resultats: [], source: 'local' });
        this.reflechit.set(false);
      },
    });
  }

  /** Quitte la recherche en cours et montre tous les établissements. */
  toutVoir() {
    this.saisie.set('');
    this.rechercherNormal();
  }

  ouvrir(slug: string) { this.router.navigate(['/etablissements', slug]); }

  choisirSurCarte(id: string | number) { this.selection.set(Number(id)); }

  services(e: EtablissementResume) { return e.prestations.slice(0, 2); }

  pluriel(n: number, mot: string) { return `${n} ${mot}${n > 1 ? 's' : ''}`; }
}

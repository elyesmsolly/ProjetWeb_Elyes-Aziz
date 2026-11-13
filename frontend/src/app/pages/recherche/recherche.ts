import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { debounceTime, merge, switchMap } from 'rxjs';
import { ApiService, messageErreur } from '../../core/api.service';
import { badgeAttente, distance, icone, note } from '../../core/format';
import { AssistantReponse, EtablissementResume } from '../../core/models';
import { PositionService } from '../../core/position.service';
import { TempsReelService } from '../../core/temps-reel.service';
import { Carte, Repere } from '../../shared/carte';
import { Icon } from '../../shared/icon';

type Tri = 'score' | 'attente' | 'distance' | 'note';

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

  protected readonly categories = CATEGORIES;
  protected readonly categorie = signal('');
  protected readonly texte = signal('');
  protected readonly vue = signal<'liste' | 'carte'>('liste');
  protected readonly tous = signal<EtablissementResume[] | null>(null);
  protected readonly erreur = signal<string | null>(null);
  protected readonly selection = signal<number | null>(null);

  protected readonly filtres = signal({ ouvert: true, attente20: false, km1: false, note4: false });
  protected readonly tri = signal<Tri>('score');

  // Assistant
  protected readonly question = signal('');
  protected readonly reponse = signal<AssistantReponse | null>(null);
  protected readonly reflechit = signal(false);

  protected readonly fmt = { distance, note, badgeAttente, icone };

  protected readonly resultats = computed(() => {
    const f = this.filtres();
    const liste = (this.tous() ?? []).filter((e) =>
      (!f.ouvert || e.fileOuverte) && (!f.attente20 || e.attenteMinutes < 20) && (!f.km1 || e.distanceKm < 1) && (!f.note4 || e.note >= 4));
    const cle: Record<Tri, (e: EtablissementResume) => number> = {
      score: (e) => e.score, attente: (e) => e.attenteMinutes, distance: (e) => e.distanceKm, note: (e) => -e.note,
    };
    const fermes = (this.tous() ?? []).filter((e) => f.ouvert && !e.fileOuverte);
    return [...liste.sort((a, b) => cle[this.tri()](a) - cle[this.tri()](b)), ...fermes.map((e) => ({ ...e, meilleurChoix: false }))];
  });

  protected readonly titre = computed(() => {
    const c = CATEGORIES.find((x) => x.code === this.categorie());
    const base = c && c.code ? c.libelle : this.texte() ? `« ${this.texte()} »` : 'Établissements';
    return `${base} près de l’Ariana`;
  });

  protected readonly ouverts = computed(() => (this.tous() ?? []).filter((e) => e.fileOuverte).length);

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
    this.position.demander().then(() => this.charger());

    const question = qp.get('assistant');
    if (question) { this.question.set(question); this.demander(); }
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
    this.router.navigate([], { queryParams: { categorie: code || null, q: null }, queryParamsHandling: 'merge', replaceUrl: true });
    this.charger();
  }

  basculer(cle: keyof ReturnType<typeof this.filtres>) {
    this.filtres.update((f) => ({ ...f, [cle]: !f[cle] }));
  }

  demander() {
    const q = this.question().trim();
    if (!q || this.reflechit()) return;
    this.reflechit.set(true);
    this.api.assistant(q).subscribe({
      next: (r) => { this.reponse.set(r); this.reflechit.set(false); if (r.resultats[0]) this.selection.set(r.resultats[0].id); },
      error: (e) => { this.reponse.set({ reponse: messageErreur(e), criteres: null!, resultats: [], source: 'local' }); this.reflechit.set(false); },
    });
  }

  ouvrir(slug: string) { this.router.navigate(['/etablissements', slug]); }

  choisirSurCarte(id: string | number) { this.selection.set(Number(id)); }

  services(e: EtablissementResume) { return e.prestations.slice(0, 2); }
}

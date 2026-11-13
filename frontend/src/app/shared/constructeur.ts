import { CdkDrag, CdkDragDrop, CdkDragHandle, CdkDragPlaceholder, CdkDragPreview, CdkDropList, CdkDropListGroup, copyArrayItem, moveItemInArray } from '@angular/cdk/drag-drop';
import { ChangeDetectionStrategy, Component, computed, input, model, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ChampFormulaire, TypeChamp } from '../core/models';
import { Champ } from './champ';
import { Icon } from './icon';

interface Outil { type: TypeChamp; libelle: string; icone: string; }

export const OUTILS: Outil[] = [
  { type: 'TEXTE', libelle: 'Texte court', icone: 'text' },
  { type: 'TEXTE_LONG', libelle: 'Texte long', icone: 'form' },
  { type: 'TELEPHONE', libelle: 'Téléphone', icone: 'phone' },
  { type: 'NOMBRE', libelle: 'Nombre', icone: 'hash' },
  { type: 'LISTE', libelle: 'Liste déroulante', icone: 'select' },
  { type: 'CHOIX', libelle: 'Choix unique', icone: 'radio' },
  { type: 'CASE', libelle: 'Case à cocher', icone: 'checkbox' },
  { type: 'DATE', libelle: 'Date', icone: 'cal' },
];

/** Champs indispensables : le nom s'affiche à l'employé, le téléphone sert aux alertes. */
const VERROUILLES = new Set(['nom', 'telephone']);

export function nouveauChamp(type: TypeChamp): ChampFormulaire {
  const outil = OUTILS.find((o) => o.type === type)!;
  const avecOptions = type === 'LISTE' || type === 'CHOIX';
  return {
    cle: 'champ_' + Math.random().toString(36).slice(2, 8),
    type,
    label: type === 'CASE' ? 'J’accepte d’être appelé par SMS' : outil.libelle,
    obligatoire: false,
    options: avecOptions ? ['Option 1', 'Option 2'] : [],
    aide: null,
  };
}

/**
 * Constructeur de formulaire en glisser-déposer (Angular CDK) :
 * on glisse un type de champ depuis la palette (copyArrayItem), on réordonne les champs (moveItemInArray).
 */
@Component({
  selector: 'app-constructeur',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CdkDropListGroup, CdkDropList, CdkDrag, CdkDragHandle, CdkDragPlaceholder, CdkDragPreview, FormsModule, Icon, Champ],
  templateUrl: './constructeur.html',
  styleUrl: './constructeur.css',
})
export class Constructeur {
  readonly champs = model.required<ChampFormulaire[]>();
  readonly nomEtablissement = input('Votre établissement');
  readonly adresse = input('localhost:4200/etablissements/votre-salon');

  protected readonly outils = OUTILS;
  protected readonly selection = signal<string | null>(null);
  protected readonly reponsesApercu = signal<Record<string, unknown>>({});
  protected readonly annonce = signal('');
  protected readonly selectionne = computed(() => this.champs().find((c) => c.cle === this.selection()) ?? null);

  verrouille(c: ChampFormulaire) { return VERROUILLES.has(c.cle); }
  outil(type: TypeChamp) { return OUTILS.find((o) => o.type === type)!; }

  /** La palette n'accepte aucun dépôt : on ne fait qu'y prendre des champs. */
  readonly jamais = () => false;

  deposer(e: CdkDragDrop<ChampFormulaire[], ChampFormulaire[] | Outil[]>) {
    const liste = [...this.champs()];
    if (e.previousContainer === e.container) {
      moveItemInArray(liste, e.previousIndex, e.currentIndex);
      this.annonce.set(`Champ déplacé en position ${e.currentIndex + 1}.`);
    } else {
      const outil = (e.previousContainer.data as Outil[])[e.previousIndex];
      const champ = nouveauChamp(outil.type);
      copyArrayItem([champ], liste, 0, e.currentIndex);
      this.selection.set(champ.cle);
      this.annonce.set(`Champ « ${outil.libelle} » ajouté en position ${e.currentIndex + 1}.`);
    }
    this.champs.set(liste);
  }

  /** Après un glisser, le navigateur envoie aussi un clic : on l'ignore. */
  protected glisse = false;
  clicOutil(type: TypeChamp) {
    if (this.glisse) { this.glisse = false; return; }
    this.ajouter(type);
  }

  /** Alternative au glisser-déposer (clavier, écran tactile) : un clic ajoute le champ à la fin. */
  ajouter(type: TypeChamp) {
    const champ = nouveauChamp(type);
    this.champs.update((l) => [...l, champ]);
    this.selection.set(champ.cle);
    this.annonce.set(`Champ « ${this.outil(type).libelle} » ajouté à la fin.`);
  }

  deplacer(i: number, delta: number) {
    const liste = [...this.champs()];
    const j = i + delta;
    if (j < 0 || j >= liste.length) return;
    moveItemInArray(liste, i, j);
    this.champs.set(liste);
  }

  modifier(cle: string, changement: Partial<ChampFormulaire>) {
    this.champs.update((l) => l.map((c) => (c.cle === cle ? { ...c, ...changement } : c)));
  }

  modifierOption(c: ChampFormulaire, i: number, valeur: string) {
    this.modifier(c.cle, { options: c.options.map((o, k) => (k === i ? valeur : o)) });
  }

  ajouterOption(c: ChampFormulaire) {
    this.modifier(c.cle, { options: [...c.options, `Option ${c.options.length + 1}`] });
  }

  retirerOption(c: ChampFormulaire, i: number) {
    if (c.options.length <= 1) return;
    this.modifier(c.cle, { options: c.options.filter((_, k) => k !== i) });
  }

  dupliquer(c: ChampFormulaire) {
    const copie = { ...c, cle: 'champ_' + Math.random().toString(36).slice(2, 8), label: c.label + ' (copie)', options: [...c.options] };
    this.champs.update((l) => { const i = l.indexOf(c); const n = [...l]; n.splice(i + 1, 0, copie); return n; });
    this.selection.set(copie.cle);
  }

  supprimer(c: ChampFormulaire) {
    if (this.verrouille(c)) return;
    this.champs.update((l) => l.filter((x) => x.cle !== c.cle));
    this.selection.set(null);
    this.annonce.set(`Champ « ${c.label} » supprimé.`);
  }

  repondreApercu(cle: string, v: unknown) { this.reponsesApercu.update((r) => ({ ...r, [cle]: v })); }
}

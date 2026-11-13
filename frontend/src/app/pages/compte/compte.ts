import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService, messageErreur } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Icon } from '../../shared/icon';

/** Connexion et création du compte client (mode choisi par la route). */
@Component({
  selector: 'app-compte',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, RouterLink, Icon],
  templateUrl: './compte.html',
  styleUrl: './compte.css',
})
export class Compte {
  readonly mode = input<'connexion' | 'inscription'>('connexion');
  readonly retour = input<string>();

  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly nom = signal('');
  protected readonly email = signal('');
  protected readonly telephone = signal('');
  protected readonly motDePasse = signal('');
  protected readonly voir = signal(false);
  protected readonly erreur = signal<string | null>(null);
  protected readonly envoi = signal(false);

  remplirDemo() {
    this.email.set('client@dourek.tn');
    this.motDePasse.set('dourek123');
  }

  valider() {
    if (this.envoi()) return;
    this.erreur.set(null);
    const email = this.email().trim();
    if (this.mode() === 'inscription') {
      if (!this.nom().trim() || !email || !this.telephone().trim()) return this.erreur.set('Remplissez votre nom, votre e-mail et votre téléphone.');
      if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) return this.erreur.set('Adresse e-mail invalide.');
      if (!/^\d{8,12}$/.test(this.telephone().replace(/[\s.+-]/g, ''))) return this.erreur.set('Numéro de téléphone invalide (8 chiffres au moins).');
      if (this.motDePasse().length < 6) return this.erreur.set('Le mot de passe doit contenir au moins 6 caractères.');
    } else if (!email || !this.motDePasse()) {
      return this.erreur.set('Saisissez votre e-mail et votre mot de passe.');
    }
    this.envoi.set(true);
    const appel = this.mode() === 'inscription'
      ? this.api.inscriptionClient({ nom: this.nom().trim(), email, telephone: this.telephone().trim(), motDePasse: this.motDePasse() })
      : this.api.connexion(email, this.motDePasse());
    appel.subscribe({
      next: (s) => {
        if (s.role !== 'CLIENT') {
          this.erreur.set('Ce compte appartient à l’espace pro : connectez-vous depuis « Espace pro ».');
          this.envoi.set(false);
          return;
        }
        this.auth.ouvrir(s);
        const cible = this.retour();
        // Retour uniquement vers une page de Dourek (jamais vers un autre site).
        this.router.navigateByUrl(cible && cible.startsWith('/') && !cible.startsWith('//') ? cible : '/ticket');
      },
      error: (e) => { this.erreur.set(messageErreur(e)); this.envoi.set(false); },
    });
  }

  protected lienAutre() {
    return { retour: this.retour() || null };
  }
}

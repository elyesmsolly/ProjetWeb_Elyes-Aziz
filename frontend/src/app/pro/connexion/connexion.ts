import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService, messageErreur } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Icon } from '../../shared/icon';

@Component({
  selector: 'app-connexion',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, RouterLink, Icon],
  templateUrl: './connexion.html',
  styleUrl: './connexion.css',
})
export class Connexion {
  private api = inject(ApiService);
  private auth = inject(AuthService);
  private router = inject(Router);

  protected readonly role = signal<'GERANT' | 'EMPLOYE'>('GERANT');
  protected readonly email = signal('');
  protected readonly motDePasse = signal('');
  protected readonly voir = signal(false);
  protected readonly envoi = signal(false);
  protected readonly erreur = signal<string | null>(null);

  choisirRole(r: 'GERANT' | 'EMPLOYE') {
    this.role.set(r);
    this.erreur.set(null);
  }

  remplirDemo() {
    this.email.set(this.role() === 'GERANT' ? 'karim@dourek.tn' : 'sami@dourek.tn');
    this.motDePasse.set('dourek123');
  }

  seConnecter() {
    if (this.envoi()) return;
    if (!this.email().trim() || !this.motDePasse()) { this.erreur.set('Saisissez votre e-mail et votre mot de passe.'); return; }
    this.envoi.set(true);
    this.api.connexion(this.email().trim(), this.motDePasse()).subscribe({
      next: (s) => {
        if (s.role === 'CLIENT') {
          this.erreur.set('Ce compte est un compte client : connectez-vous depuis « Se connecter » en haut de la page.');
          this.envoi.set(false);
          return;
        }
        this.auth.ouvrir(s);
        this.router.navigateByUrl(s.role === 'GERANT' && this.role() === 'GERANT' ? '/pro/tableau-de-bord' : '/pro/employe');
      },
      error: (e) => { this.erreur.set(messageErreur(e)); this.envoi.set(false); },
    });
  }
}

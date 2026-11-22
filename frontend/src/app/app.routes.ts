import { Routes } from '@angular/router';
import { adminGuard, clientGuard, gerantGuard, proGuard, sessionGuard } from './core/auth.service';
import { PublicLayout } from './shared/public-layout';

export const routes: Routes = [
  {
    path: '',
    component: PublicLayout,
    children: [
      {
        path: '',
        title: 'Dourek · Faites la queue depuis chez vous',
        canActivate: [sessionGuard],
        loadComponent: () => import('./pages/accueil/accueil').then((m) => m.Accueil),
      },
      {
        path: 'etablissements',
        title: 'Établissements · Dourek',
        canActivate: [sessionGuard],
        loadComponent: () => import('./pages/recherche/recherche').then((m) => m.Recherche),
      },
      {
        path: 'etablissements/:slug',
        title: 'Établissement · Dourek',
        canActivate: [sessionGuard],
        loadComponent: () =>
          import('./pages/etablissement/etablissement').then((m) => m.Etablissement),
      },
      {
        path: 'ticket',
        title: 'Mes tickets · Dourek',
        canActivate: [clientGuard],
        loadComponent: () => import('./pages/ticket/ticket').then((m) => m.Ticket),
      },
      {
        path: 'ticket/:code',
        title: 'Mon ticket · Dourek',
        canActivate: [clientGuard],
        loadComponent: () => import('./pages/ticket/ticket').then((m) => m.Ticket),
      },
      {
        path: 'profil',
        title: 'Mon profil · Dourek',
        canActivate: [clientGuard],
        loadComponent: () => import('./pages/profil/profil').then((m) => m.Profil),
      },
      {
        path: 'aide/contact',
        title: 'Contact · Dourek',
        loadComponent: () => import('./pages/aide/contact').then((m) => m.Contact),
      },
      {
        path: 'aide',
        title: 'Aide · Dourek',
        loadComponent: () => import('./pages/aide/aide').then((m) => m.Aide),
      },
      {
        path: 'aide/faq',
        title: 'Questions fréquentes · Dourek',
        loadComponent: () => import('./pages/aide/faq').then((m) => m.Faq),
      },
      {
        path: 'aide/confidentialite',
        title: 'Confidentialité · Dourek',
        loadComponent: () => import('./pages/aide/confidentialite').then((m) => m.Confidentialite),
      },
      {
        path: 'connexion',
        title: 'Connexion · Dourek',
        data: { mode: 'connexion' },
        loadComponent: () => import('./pages/compte/compte').then((m) => m.Compte),
      },
      {
        path: 'inscription',
        title: 'Créer un compte · Dourek',
        data: { mode: 'inscription' },
        loadComponent: () => import('./pages/compte/compte').then((m) => m.Compte),
      },
      {
        path: 'pro/connexion',
        title: 'Espace pro · Dourek',
        loadComponent: () => import('./pro/connexion/connexion').then((m) => m.Connexion),
      },
    ],
  },
  {
    path: 'pro/inscription',
    title: 'Créer mon espace · Dourek',
    loadComponent: () => import('./pro/inscription/inscription').then((m) => m.Inscription),
  },
  {
    path: 'pro/employe',
    title: 'Écran employé · Dourek',
    canActivate: [proGuard],
    loadComponent: () => import('./pro/employe/employe').then((m) => m.Employe),
  },
  {
    path: 'admin',
    canActivate: [adminGuard],
    loadChildren: () => import('./admin/admin.routes').then((m) => m.ADMIN_ROUTES),
  },
  {
    path: 'pro',
    loadComponent: () => import('./shared/pro-layout').then((m) => m.ProLayout),
    canActivate: [gerantGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'tableau-de-bord' },
      {
        path: 'services',
        title: 'Services · Dourek',
        loadComponent: () => import('./pro/services/services').then((m) => m.Services),
      },
      {
        path: 'equipe',
        title: 'Postes & employés · Dourek',
        loadComponent: () => import('./pro/equipe/equipe').then((m) => m.Equipe),
      },
      {
        path: 'parametres',
        title: 'Paramètres · Dourek',
        loadComponent: () => import('./pro/parametres/parametres').then((m) => m.Parametres),
      },
      {
        path: 'qr-code',
        title: 'QR code d’accueil · Dourek',
        loadComponent: () => import('./pro/qr-code/qr-code').then((m) => m.QrCode),
      },
      {
        path: 'tableau-de-bord',
        title: 'Tableau de bord · Dourek',
        loadComponent: () =>
          import('./pro/tableau-de-bord/tableau-de-bord').then((m) => m.TableauDeBord),
      },
      {
        path: 'formulaire',
        title: 'Formulaire client · Dourek',
        loadComponent: () =>
          import('./pro/formulaire/formulaire').then((m) => m.ConstructeurFormulaire),
      },
    ],
  },
  {
    path: '',
    component: PublicLayout,
    children: [
      {
        path: '**',
        title: 'Page introuvable · Dourek',
        loadComponent: () => import('./pages/introuvable/introuvable').then((m) => m.Introuvable),
      },
    ],
  },
];

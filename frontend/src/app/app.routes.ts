import { Routes } from '@angular/router';
import { clientGuard, gerantGuard, proGuard } from './core/auth.service';
import { PublicLayout } from './shared/public-layout';
import { ProLayout } from './shared/pro-layout';

export const routes: Routes = [
  {
    path: '',
    component: PublicLayout,
    children: [
      { path: '', title: 'Dourek · Faites la queue depuis chez vous', loadComponent: () => import('./pages/accueil/accueil').then((m) => m.Accueil) },
      { path: 'etablissements', title: 'Établissements · Dourek', loadComponent: () => import('./pages/recherche/recherche').then((m) => m.Recherche) },
      { path: 'etablissements/:slug', title: 'Établissement · Dourek', loadComponent: () => import('./pages/etablissement/etablissement').then((m) => m.Etablissement) },
      { path: 'ticket', title: 'Mon ticket · Dourek', canActivate: [clientGuard], loadComponent: () => import('./pages/ticket/ticket').then((m) => m.Ticket) },
      { path: 'ticket/:code', title: 'Mon ticket · Dourek', canActivate: [clientGuard], loadComponent: () => import('./pages/ticket/ticket').then((m) => m.Ticket) },
      { path: 'connexion', title: 'Connexion · Dourek', data: { mode: 'connexion' }, loadComponent: () => import('./pages/compte/compte').then((m) => m.Compte) },
      { path: 'inscription', title: 'Créer un compte · Dourek', data: { mode: 'inscription' }, loadComponent: () => import('./pages/compte/compte').then((m) => m.Compte) },
      { path: 'pro/connexion', title: 'Espace pro · Dourek', loadComponent: () => import('./pro/connexion/connexion').then((m) => m.Connexion) },
    ],
  },
  { path: 'pro/inscription', title: 'Créer mon espace · Dourek', loadComponent: () => import('./pro/inscription/inscription').then((m) => m.Inscription) },
  { path: 'pro/employe', title: 'Écran employé · Dourek', canActivate: [proGuard], loadComponent: () => import('./pro/employe/employe').then((m) => m.Employe) },
  {
    path: 'pro',
    component: ProLayout,
    canActivate: [gerantGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'tableau-de-bord' },
      { path: 'tableau-de-bord', title: 'Tableau de bord · Dourek', loadComponent: () => import('./pro/tableau-de-bord/tableau-de-bord').then((m) => m.TableauDeBord) },
      { path: 'formulaire', title: 'Formulaire client · Dourek', loadComponent: () => import('./pro/formulaire/formulaire').then((m) => m.ConstructeurFormulaire) },
    ],
  },
  { path: '**', redirectTo: '' },
];

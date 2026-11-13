import { Routes } from '@angular/router';
import { PublicLayout } from './shared/public-layout';

export const routes: Routes = [
  {
    path: '',
    component: PublicLayout,
    children: [
      { path: '', title: 'Dourek · Faites la queue depuis chez vous', loadComponent: () => import('./pages/accueil/accueil').then((m) => m.Accueil) },
      { path: 'etablissements', title: 'Établissements · Dourek', loadComponent: () => import('./pages/recherche/recherche').then((m) => m.Recherche) },
      { path: 'etablissements/:slug', title: 'Établissement · Dourek', loadComponent: () => import('./pages/etablissement/etablissement').then((m) => m.Etablissement) },
    ],
  },
  { path: '**', redirectTo: '' },
];

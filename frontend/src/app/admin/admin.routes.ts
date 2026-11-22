import { Routes } from '@angular/router';
import { AdminLayout } from './admin-layout';

/** Chargé seulement quand un administrateur ouvre /admin. */
export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    component: AdminLayout,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'etablissements' },
      {
        path: 'etablissements',
        title: 'Établissements · Dourek',
        loadComponent: () =>
          import('./etablissements/etablissements').then((m) => m.AdminEtablissements),
      },
      {
        path: 'utilisateurs',
        title: 'Utilisateurs & rôles · Dourek',
        loadComponent: () => import('./utilisateurs/utilisateurs').then((m) => m.AdminUtilisateurs),
      },
      {
        path: 'avis',
        title: 'Avis signalés · Dourek',
        loadComponent: () => import('./avis/avis').then((m) => m.AdminAvis),
      },
      {
        path: 'categories',
        title: 'Catégories · Dourek',
        loadComponent: () => import('./categories/categories').then((m) => m.AdminCategories),
      },
      {
        path: 'messages',
        title: 'Messages · Dourek',
        loadComponent: () => import('./messages/messages').then((m) => m.AdminMessages),
      },
    ],
  },
];

import { Routes } from '@angular/router';
import { PublicLayout } from './shared/public-layout';

export const routes: Routes = [
  {
    path: '',
    component: PublicLayout,
    children: [
    ],
  },
  { path: '**', redirectTo: '' },
];

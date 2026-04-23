import { Routes } from '@angular/router';

export const APPLICATION_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./list/applications-list.component').then((module) => module.ApplicationsListComponent),
  },
  {
    path: 'new',
    loadComponent: () =>
      import('./create/application-create.component').then((module) => module.ApplicationCreateComponent),
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./detail/application-detail.component').then((module) => module.ApplicationDetailComponent),
  },
];

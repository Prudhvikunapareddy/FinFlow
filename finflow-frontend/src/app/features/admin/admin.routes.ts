import { Routes } from '@angular/router';

export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./admin-shell.component').then((module) => module.AdminShellComponent),
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'applications',
      },
      {
        path: 'applications',
        loadComponent: () =>
          import('./applications/admin-applications.component').then(
            (module) => module.AdminApplicationsComponent,
          ),
      },
      {
        path: 'applications/:id',
        loadComponent: () =>
          import('./applications/admin-application-detail.component').then(
            (module) => module.AdminApplicationDetailComponent,
          ),
      },
      {
        path: 'users',
        loadComponent: () => import('./users/admin-users.component').then((module) => module.AdminUsersComponent),
      },
      {
        path: 'reports',
        loadComponent: () => import('./reports/admin-reports.component').then((module) => module.AdminReportsComponent),
      },
    ],
  },
];

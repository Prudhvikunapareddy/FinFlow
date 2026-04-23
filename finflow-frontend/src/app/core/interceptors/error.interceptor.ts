import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { ToastService } from '../services/toast.service';

export const errorInterceptor: HttpInterceptorFn = (request, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const toastService = inject(ToastService);

  return next(request).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 0) {
        toastService.error('Cannot reach server');
      } else if (error.status === 401) {
        authService.clearSession();
        toastService.error('Session expired');
        void router.navigate(['/login']);
      } else if (error.status === 403) {
        toastService.error("You don't have permission");
      } else if (error.status === 400 || error.status === 404 || error.status === 409) {
        toastService.error(extractMessage(error) ?? 'Request failed');
      } else if (error.status >= 500) {
        toastService.error('Something went wrong. Try again.');
      }

      return throwError(() => error);
    }),
  );
};

function extractMessage(error: HttpErrorResponse): string | null {
  const payload = error.error;

  if (typeof payload === 'string' && payload.trim()) {
    return payload.trim();
  }

  if (payload && typeof payload === 'object') {
    if ('message' in payload && typeof payload.message === 'string' && payload.message.trim()) {
      return payload.message.trim();
    }

    if ('error' in payload && typeof payload.error === 'string' && payload.error.trim()) {
      return payload.error.trim();
    }
  }

  return null;
}

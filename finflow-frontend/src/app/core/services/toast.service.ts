import { Injectable, signal } from '@angular/core';
import { ToastMessage, ToastType } from '../models/toast.model';

@Injectable({ providedIn: 'root' })
export class ToastService {
  private readonly toastStore = signal<ToastMessage[]>([]);
  readonly toasts = this.toastStore.asReadonly();

  show(message: string, type: ToastType = 'info'): void {
    const toast: ToastMessage = {
      id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
      message,
      type,
    };

    this.toastStore.update((items) => [...items, toast]);

    window.setTimeout(() => {
      this.dismiss(toast.id);
    }, 3000);
  }

  success(message: string): void {
    this.show(message, 'success');
  }

  error(message: string): void {
    this.show(message, 'error');
  }

  info(message: string): void {
    this.show(message, 'info');
  }

  dismiss(id: string): void {
    this.toastStore.update((items) => items.filter((item) => item.id !== id));
  }
}

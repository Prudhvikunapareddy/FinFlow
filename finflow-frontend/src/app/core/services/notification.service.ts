import { Injectable, computed, inject, signal } from '@angular/core';
import { AuthService } from './auth.service';

export interface FinflowNotification {
  id: string;
  userEmail: string;
  message: string;
  read: boolean;
  createdAt: string;
}

const NOTIFICATION_KEY = 'finflow_notifications';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly authService = inject(AuthService);
  private readonly notifications = signal<FinflowNotification[]>(this.load());

  readonly mine = computed(() => {
    const email = this.authService.getEmail()?.toLowerCase();
    return this.notifications()
      .filter((notification) => notification.userEmail.toLowerCase() === email)
      .sort((left, right) => right.createdAt.localeCompare(left.createdAt));
  });

  readonly unreadCount = computed(() => this.mine().filter((notification) => !notification.read).length);

  create(userEmail: string, message: string): void {
    const notification: FinflowNotification = {
      id: crypto.randomUUID(),
      userEmail,
      message,
      read: false,
      createdAt: new Date().toISOString(),
    };
    this.notifications.update((items) => [notification, ...items]);
    this.persist();
  }

  markAsRead(id: string): void {
    this.notifications.update((items) =>
      items.map((notification) => notification.id === id ? { ...notification, read: true } : notification),
    );
    this.persist();
  }

  private load(): FinflowNotification[] {
    const raw = localStorage.getItem(NOTIFICATION_KEY);
    if (!raw) return [];
    try {
      return JSON.parse(raw) as FinflowNotification[];
    } catch {
      localStorage.removeItem(NOTIFICATION_KEY);
      return [];
    }
  }

  private persist(): void {
    localStorage.setItem(NOTIFICATION_KEY, JSON.stringify(this.notifications()));
  }
}

import { ChangeDetectionStrategy, Component, computed, inject, signal, HostListener } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, DatePipe],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NavbarComponent {
  protected readonly authService = inject(AuthService);
  protected readonly notificationService = inject(NotificationService);

  protected readonly isAdmin = computed(() => this.authService.getRole() === 'ADMIN');
  protected readonly homeLink = computed(() =>
    this.isAdmin() ? '/admin/applications' : '/dashboard',
  );

  protected readonly userInitial = computed(() => {
    const email = this.authService.getEmail();
    return email ? email.charAt(0).toUpperCase() : 'U';
  });

  protected isDropdownOpen = signal(false);
  protected isNotificationsOpen = signal(false);
  protected isMobileMenuOpen = signal(false);

  toggleDropdown() {
    this.isDropdownOpen.set(!this.isDropdownOpen());
    this.isNotificationsOpen.set(false);
  }

  toggleNotifications() {
    this.isNotificationsOpen.set(!this.isNotificationsOpen());
    this.isDropdownOpen.set(false);
  }

  toggleMobileMenu() {
    this.isMobileMenuOpen.set(!this.isMobileMenuOpen());
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent) {
    const target = event.target as HTMLElement;
    if (!target.closest('.user-menu-container')) {
      this.isDropdownOpen.set(false);
      this.isNotificationsOpen.set(false);
    }
  }

  protected markNotification(id: string): void {
    this.notificationService.markAsRead(id);
  }

  protected logout(): void {
    this.isDropdownOpen.set(false);
    this.isMobileMenuOpen.set(false);
    this.authService.logout();
  }
}

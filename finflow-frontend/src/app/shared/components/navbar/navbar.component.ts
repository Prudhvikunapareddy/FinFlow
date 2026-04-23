import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NavbarComponent {
  protected readonly authService = inject(AuthService);

  protected readonly isAdmin = computed(() => this.authService.getRole() === 'ADMIN');
  protected readonly homeLink = computed(() =>
    this.isAdmin() ? '/admin/applications' : '/dashboard',
  );

  protected logout(): void {
    this.authService.logout();
  }
}

import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ApplicationResponse } from '../../core/models/application.model';
import { ApplicationService } from '../../core/services/application.service';
import { AuthService } from '../../core/services/auth.service';
import { CurrencyInrPipe } from '../../shared/pipes/currency-inr.pipe';
import { StatusBadgeComponent } from '../../shared/components/status-badge/status-badge.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, CurrencyInrPipe, StatusBadgeComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardComponent {
  private readonly applicationService = inject(ApplicationService);
  private readonly authService = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly loading = signal(true);
  protected readonly applications = signal<ApplicationResponse[]>([]);

  protected readonly recentApplications = computed(() =>
    [...this.applications()].sort((left, right) => right.id - left.id).slice(0, 5),
  );

  protected readonly summary = computed(() => {
    const applications = this.applications();

    return {
      total: applications.length,
      pending: applications.filter((application) => application.status === 'SUBMITTED').length,
      approved: applications.filter((application) => application.status === 'APPROVED').length,
      rejected: applications.filter((application) => application.status === 'REJECTED').length,
    };
  });

  protected readonly email = computed(() => this.authService.getEmail() ?? 'user');

  constructor() {
    this.loadApplications();
  }

  private loadApplications(): void {
    this.loading.set(true);

    this.applicationService
      .getMine()
      .pipe(
        finalize(() => this.loading.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (applications) => this.applications.set(applications),
        error: () => this.applications.set([]),
      });
  }
}

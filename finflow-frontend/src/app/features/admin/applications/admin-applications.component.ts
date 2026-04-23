import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AdminApplicationResponse, ApplicationStatus } from '../../../core/models/application.model';
import { AdminService } from '../../../core/services/admin.service';
import { CurrencyInrPipe } from '../../../shared/pipes/currency-inr.pipe';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';

type AdminFilter = 'ALL' | ApplicationStatus;

@Component({
  selector: 'app-admin-applications',
  standalone: true,
  imports: [RouterLink, CurrencyInrPipe, StatusBadgeComponent],
  templateUrl: './admin-applications.component.html',
  styleUrl: './admin-applications.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminApplicationsComponent {
  private readonly adminService = inject(AdminService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly loading = signal(true);
  protected readonly filter = signal<AdminFilter>('ALL');
  protected readonly applications = signal<AdminApplicationResponse[]>([]);

  protected readonly filterOptions: AdminFilter[] = [
    'ALL',
    'DRAFT',
    'SUBMITTED',
    'DOCS_VERIFIED',
    'APPROVED',
    'REJECTED',
  ];

  protected readonly filteredApplications = computed(() => {
    const currentFilter = this.filter();
    const applications = this.applications();
    if (currentFilter === 'ALL') {
      return applications;
    }

    return applications.filter((application) => application.status === currentFilter);
  });

  constructor() {
    this.loadApplications();
  }

  protected onFilterChange(value: string): void {
    this.filter.set(value as AdminFilter);
  }

  private loadApplications(): void {
    this.loading.set(true);

    this.adminService
      .getApplications()
      .pipe(
        finalize(() => this.loading.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (applications) =>
          this.applications.set([...applications].sort((left, right) => right.id - left.id)),
        error: () => this.applications.set([]),
      });
  }
}

import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { AdminApplicationResponse } from '../../../core/models/application.model';
import { AdminService } from '../../../core/services/admin.service';
import { LoaderComponent } from '../../../shared/components/loader/loader.component';
import { CurrencyInrPipe } from '../../../shared/pipes/currency-inr.pipe';

@Component({
  selector: 'app-admin-reports',
  standalone: true,
  imports: [LoaderComponent, CurrencyInrPipe],
  templateUrl: './admin-reports.component.html',
  styleUrl: './admin-reports.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminReportsComponent {
  private readonly adminService = inject(AdminService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly loading = signal(true);
  protected readonly applications = signal<AdminApplicationResponse[]>([]);

  protected readonly statusReport = computed(() => {
    const apps = this.applications();
    const total = Math.max(apps.length, 1);
    return [
      { label: 'Approved', value: apps.filter((app) => app.status === 'APPROVED').length, color: '#10b981' },
      { label: 'Rejected', value: apps.filter((app) => app.status === 'REJECTED').length, color: '#ef4444' },
      { label: 'Pending', value: apps.filter((app) => app.status !== 'APPROVED' && app.status !== 'REJECTED').length, color: '#f59e0b' },
    ].map((item) => ({ ...item, percent: Math.round((item.value / total) * 100) }));
  });

  protected readonly monthlyReport = computed(() => {
    const buckets = new Map<string, number>();
    this.applications().forEach((app) => {
      const date = app.submittedAt ? new Date(app.submittedAt) : new Date();
      const label = date.toLocaleString('en-US', { month: 'short' });
      buckets.set(label, (buckets.get(label) ?? 0) + 1);
    });
    const max = Math.max(...buckets.values(), 1);
    return [...buckets.entries()].map(([label, value]) => ({ label, value, height: Math.max(10, Math.round((value / max) * 100)) }));
  });

  protected readonly disbursedThisMonth = computed(() => {
    const now = new Date();
    return this.applications()
      .filter((app) => {
        const submitted = app.submittedAt ? new Date(app.submittedAt) : now;
        return app.status === 'APPROVED' && submitted.getMonth() === now.getMonth() && submitted.getFullYear() === now.getFullYear();
      })
      .reduce((total, app) => total + (app.amount ?? 0), 0);
  });

  constructor() {
    this.adminService.getApplications().pipe(
      finalize(() => this.loading.set(false)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      next: (applications) => this.applications.set(applications),
      error: () => this.applications.set([]),
    });
  }
}

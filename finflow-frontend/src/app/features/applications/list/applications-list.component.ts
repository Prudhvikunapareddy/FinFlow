import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ApplicationResponse } from '../../../core/models/application.model';
import { ApplicationService } from '../../../core/services/application.service';
import { CurrencyInrPipe } from '../../../shared/pipes/currency-inr.pipe';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';

@Component({
  selector: 'app-applications-list',
  standalone: true,
  imports: [RouterLink, CurrencyInrPipe, StatusBadgeComponent],
  templateUrl: './applications-list.component.html',
  styleUrl: './applications-list.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ApplicationsListComponent {
  private readonly applicationService = inject(ApplicationService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly loading = signal(true);
  protected readonly applications = signal<ApplicationResponse[]>([]);

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
        next: (applications) =>
          this.applications.set([...applications].sort((left, right) => right.id - left.id)),
        error: () => this.applications.set([]),
      });
  }
}

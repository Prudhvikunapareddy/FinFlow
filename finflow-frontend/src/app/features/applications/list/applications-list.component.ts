import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ApplicationResponse } from '../../../core/models/application.model';
import { ApplicationService } from '../../../core/services/application.service';
import { ToastService } from '../../../core/services/toast.service';
import { CurrencyInrPipe } from '../../../shared/pipes/currency-inr.pipe';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { LoaderComponent } from '../../../shared/components/loader/loader.component';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { computed } from '@angular/core';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-applications-list',
  standalone: true,
  imports: [RouterLink, CurrencyInrPipe, StatusBadgeComponent, LoaderComponent, ReactiveFormsModule, ConfirmDialogComponent],
  templateUrl: './applications-list.component.html',
  styleUrl: './applications-list.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ApplicationsListComponent {
  private readonly applicationService = inject(ApplicationService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly loading = signal(true);
  protected readonly applications = signal<ApplicationResponse[]>([]);
  protected readonly pendingDelete = signal<ApplicationResponse | null>(null);
  protected readonly busyId = signal<number | null>(null);

  protected searchControl = new FormControl('');
  protected statusControl = new FormControl('ALL');

  protected searchValue = toSignal(this.searchControl.valueChanges, { initialValue: '' });
  protected statusValue = toSignal(this.statusControl.valueChanges, { initialValue: 'ALL' });

  protected readonly filteredApplications = computed(() => {
    let filtered = this.applications();
    
    const search = this.searchValue()?.toLowerCase() || '';
    if (search) {
      filtered = filtered.filter(app => 
        app.name.toLowerCase().includes(search) || 
        app.id.toString().includes(search)
      );
    }
    
    const status = this.statusValue();
    if (status && status !== 'ALL') {
      filtered = filtered.filter(app => app.status === status);
    }
    
    return filtered;
  });

  protected readonly pageSize = 10;
  protected readonly page = signal(1);
  protected readonly totalPages = computed(() => Math.max(1, Math.ceil(this.filteredApplications().length / this.pageSize)));
  protected readonly pagedApplications = computed(() => {
    const start = (this.page() - 1) * this.pageSize;
    return this.filteredApplications().slice(start, start + this.pageSize);
  });
  protected readonly timelineSteps = ['Draft', 'Submitted', 'Under Review', 'Docs Verified', 'Approved/Rejected'];

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

  protected setStatus(status: string): void {
    this.statusControl.setValue(status);
    this.page.set(1);
  }

  protected nextPage(): void {
    this.page.update((page) => Math.min(this.totalPages(), page + 1));
  }

  protected prevPage(): void {
    this.page.update((page) => Math.max(1, page - 1));
  }

  protected submitDraft(event: Event, app: ApplicationResponse): void {
    event.preventDefault();
    event.stopPropagation();
    if (app.status !== 'DRAFT' || this.busyId()) return;
    this.busyId.set(app.id);
    this.applicationService.submit(app.id).pipe(
      finalize(() => this.busyId.set(null)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      next: (updated) => {
        this.applications.update((apps) => apps.map((entry) => entry.id === updated.id ? updated : entry));
        this.toastService.success('Application submitted for review');
      },
    });
  }

  protected requestDelete(event: Event, app: ApplicationResponse): void {
    event.preventDefault();
    event.stopPropagation();
    this.pendingDelete.set(app);
  }

  protected confirmDelete(): void {
    const app = this.pendingDelete();
    if (!app || this.busyId()) return;
    this.pendingDelete.set(null);
    this.busyId.set(app.id);
    this.applicationService.delete(app.id).pipe(
      finalize(() => this.busyId.set(null)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      next: () => {
        this.applications.update((apps) => apps.filter((entry) => entry.id !== app.id));
        this.toastService.success('Application deleted');
      },
    });
  }

  protected timelineIndex(app: ApplicationResponse): number {
    if (app.status === 'DRAFT') return 0;
    if (app.status === 'SUBMITTED') return 2;
    if (app.status === 'DOCS_VERIFIED') return 3;
    return 4;
  }
}

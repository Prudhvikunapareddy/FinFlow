import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AdminApplicationResponse, ApplicationStatus } from '../../../core/models/application.model';
import { AdminService } from '../../../core/services/admin.service';
import { ToastService } from '../../../core/services/toast.service';
import { CurrencyInrPipe } from '../../../shared/pipes/currency-inr.pipe';
import { DatePipe } from '@angular/common';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { LoaderComponent } from '../../../shared/components/loader/loader.component';
import { FormControl, FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { NotificationService } from '../../../core/services/notification.service';
import { LoanType } from '../../../core/models/application.model';

type AdminFilter = 'ALL' | ApplicationStatus;

@Component({
  selector: 'app-admin-applications',
  standalone: true,
  imports: [RouterLink, CurrencyInrPipe, DatePipe, StatusBadgeComponent, LoaderComponent, ReactiveFormsModule, ConfirmDialogComponent],
  templateUrl: './admin-applications.component.html',
  styleUrl: './admin-applications.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminApplicationsComponent {
  private readonly adminService = inject(AdminService);
  private readonly toastService = inject(ToastService);
  private readonly notificationService = inject(NotificationService);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly loading = signal(true);
  protected readonly applications = signal<AdminApplicationResponse[]>([]);
  protected readonly userNames = signal<Record<string, string>>({});
  protected readonly pendingDecision = signal<{ app: AdminApplicationResponse; status: 'APPROVED' | 'REJECTED' } | null>(null);
  protected readonly decidingId = signal<number | null>(null);
  protected readonly selectedIds = signal<Set<number>>(new Set());
  protected readonly bulkProcessing = signal(false);

  protected searchControl = new FormControl('');
  protected statusControl = new FormControl<AdminFilter>('ALL');
  protected readonly filterForm = this.fb.nonNullable.group({
    loanType: ['ALL' as 'ALL' | LoanType],
    minAmount: [0],
    maxAmount: [0],
    startDate: [''],
    endDate: [''],
  });

  protected searchValue = toSignal(this.searchControl.valueChanges, { initialValue: '' });
  protected statusValue = toSignal(this.statusControl.valueChanges, { initialValue: 'ALL' });
  protected filterValue = toSignal(this.filterForm.valueChanges, { initialValue: this.filterForm.getRawValue() });

  protected readonly summary = computed(() => {
    const apps = this.applications();
    return {
      total: apps.length,
      pending: apps.filter(app => app.status === 'SUBMITTED').length,
      approved: apps.filter(app => app.status === 'APPROVED').length,
      rejected: apps.filter(app => app.status === 'REJECTED').length,
    };
  });

  protected readonly filteredApplications = computed(() => {
    let filtered = this.applications();
    
    const search = this.searchValue()?.toLowerCase() || '';
    if (search) {
      filtered = filtered.filter(app => 
        this.displayApplicantName(app.applicantName).toLowerCase().includes(search) ||
        (app.applicantName ?? '').toLowerCase().includes(search) ||
        (app.name ?? '').toLowerCase().includes(search) ||
        app.id.toString().includes(search)
      );
    }
    
    const status = this.statusValue();
    if (status && status !== 'ALL') {
      filtered = filtered.filter(app => app.status === status);
    }

    const filters = this.filterValue();
    if (filters.loanType !== 'ALL') {
      filtered = filtered.filter(app => app.loanType === filters.loanType);
    }
    const minAmount = filters.minAmount ?? 0;
    const maxAmount = filters.maxAmount ?? 0;
    if (minAmount) {
      filtered = filtered.filter(app => (app.amount ?? 0) >= minAmount);
    }
    if (maxAmount) {
      filtered = filtered.filter(app => (app.amount ?? 0) <= maxAmount);
    }
    if (filters.startDate) {
      filtered = filtered.filter(app => !app.submittedAt || new Date(app.submittedAt) >= new Date(`${filters.startDate}T00:00:00`));
    }
    if (filters.endDate) {
      filtered = filtered.filter(app => !app.submittedAt || new Date(app.submittedAt) <= new Date(`${filters.endDate}T23:59:59`));
    }
    
    return filtered;
  });

  constructor() {
    this.loadApplications();
    this.loadUsers();
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

  protected openDecision(event: Event, app: AdminApplicationResponse, status: 'APPROVED' | 'REJECTED'): void {
    event.preventDefault();
    event.stopPropagation();
    if (app.status === 'APPROVED' || app.status === 'REJECTED') return;
    this.pendingDecision.set({ app, status });
  }

  protected confirmDecision(): void {
    const decision = this.pendingDecision();
    if (!decision || this.decidingId()) return;
    this.pendingDecision.set(null);
    this.decidingId.set(decision.app.id);
    this.adminService.decideApplication(decision.app.id, decision.status).pipe(
      finalize(() => this.decidingId.set(null)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      next: (updated) => {
        this.applications.update((apps) => apps.map((app) => app.id === updated.id ? updated : app));
        this.createApplicantNotification(updated, `Your application #${updated.id} has been ${decision.status} ${decision.status === 'APPROVED' ? '✓' : ''}`);
        this.toastService.success(`Application ${decision.status.toLowerCase()}`);
      },
    });
  }

  protected toggleSelection(event: Event, id: number): void {
    event.stopPropagation();
    const checked = (event.target as HTMLInputElement).checked;
    this.selectedIds.update((ids) => {
      const next = new Set(ids);
      checked ? next.add(id) : next.delete(id);
      return next;
    });
  }

  protected bulkDecision(status: 'APPROVED' | 'REJECTED'): void {
    const ids = [...this.selectedIds()];
    if (!ids.length || this.bulkProcessing()) return;
    this.bulkProcessing.set(true);
    this.adminService.bulkDecideApplications(ids, status)
      .pipe(finalize(() => this.bulkProcessing.set(false)), takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updatedApps) => {
          this.applications.update((apps) => apps.map((app) => updatedApps.find((updated) => updated.id === app.id) ?? app));
          updatedApps.forEach((app) => this.createApplicantNotification(app, `Your application #${app.id} has been ${status} ${status === 'APPROVED' ? '✓' : ''}`));
          this.selectedIds.set(new Set());
          this.toastService.success(`Bulk ${status.toLowerCase()} completed`);
        },
      });
  }

  protected exportCsv(): void {
    const rows = this.filteredApplications();
    const header = ['ID', 'Applicant', 'Loan Name', 'Loan Type', 'Amount', 'Status', 'Submitted At'];
    const csv = [
      header.join(','),
      ...rows.map((app) => [
        app.id,
        this.escapeCsv(app.applicantName),
        this.escapeCsv(app.name),
        app.loanType ?? '',
        app.amount ?? 0,
        app.status,
        app.submittedAt ?? '',
      ].join(',')),
    ].join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'finflow-applications.csv';
    link.click();
    URL.revokeObjectURL(url);
  }

  protected decisionMessage(): string {
    const decision = this.pendingDecision();
    if (!decision) return '';
    const amount = decision.app.amount ?? 0;
    const formatted = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(amount);
    return decision.status === 'APPROVED'
      ? `Approve ${formatted} loan for ${this.displayApplicantName(decision.app.applicantName)}?`
      : 'Reject this application?';
  }

  protected displayApplicantName(email?: string | null): string {
    const normalizedEmail = email?.trim().toLowerCase();
    if (!normalizedEmail) {
      return 'Unknown applicant';
    }

    return this.userNames()[normalizedEmail] || email?.trim() || 'Unknown applicant';
  }

  private loadUsers(): void {
    this.adminService.getUsers()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (users) => {
          const names = users.reduce<Record<string, string>>((acc, user) => {
            const fullName = `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim();
            acc[user.email.toLowerCase()] = fullName || user.email;
            return acc;
          }, {});
          this.userNames.set(names);
        },
      });
  }

  private escapeCsv(value?: string | null): string {
    return `"${(value ?? '').replace(/"/g, '""')}"`;
  }

  private createApplicantNotification(app: AdminApplicationResponse, message: string): void {
    const recipient = app.applicantName?.trim();
    if (!recipient) {
      return;
    }

    this.notificationService.create(recipient, message);
  }
}

import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize, map, switchMap } from 'rxjs';
import { AdminApplicationResponse } from '../../../core/models/application.model';
import { AdminService } from '../../../core/services/admin.service';
import { ToastService } from '../../../core/services/toast.service';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { CurrencyInrPipe } from '../../../shared/pipes/currency-inr.pipe';

@Component({
  selector: 'app-admin-application-detail',
  standalone: true,
  imports: [RouterLink, StatusBadgeComponent, CurrencyInrPipe, ConfirmDialogComponent],
  templateUrl: './admin-application-detail.component.html',
  styleUrl: './admin-application-detail.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminApplicationDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly adminService = inject(AdminService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly loading = signal(true);
  protected readonly processing = signal<'VERIFY' | 'APPROVED' | 'REJECTED' | null>(null);
  protected readonly pendingDecision = signal<'APPROVED' | 'REJECTED' | null>(null);
  protected readonly application = signal<AdminApplicationResponse | null>(null);

  protected readonly canVerify = computed(() => this.application()?.status === 'SUBMITTED');
  protected readonly canApprove = computed(() => this.application()?.status === 'DOCS_VERIFIED');
  protected readonly canReject = computed(() => {
    const status = this.application()?.status;
    return status === 'SUBMITTED' || status === 'DOCS_VERIFIED';
  });

  constructor() {
    this.route.paramMap
      .pipe(
        map((params) => Number(params.get('id'))),
        switchMap((id) => {
          this.loading.set(true);
          return this.adminService
            .getApplication(id)
            .pipe(finalize(() => this.loading.set(false)));
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (application) => this.application.set(application),
        error: () => this.application.set(null),
      });
  }

  protected verifyDocuments(): void {
    const application = this.application();
    if (!application || !this.canVerify() || this.processing()) {
      return;
    }

    this.processing.set('VERIFY');
    this.adminService
      .verifyDocument(application.id)
      .pipe(
        finalize(() => this.processing.set(null)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (message) => {
          this.application.set({ ...application, status: 'DOCS_VERIFIED' });
          this.toastService.success(message);
        },
      });
  }

  protected openDecision(status: 'APPROVED' | 'REJECTED'): void {
    this.pendingDecision.set(status);
  }

  protected closeDecision(): void {
    this.pendingDecision.set(null);
  }

  protected confirmDecision(): void {
    const decision = this.pendingDecision();
    const application = this.application();
    if (!decision || !application || this.processing()) {
      return;
    }

    this.closeDecision();
    this.processing.set(decision);

    this.adminService
      .decideApplication(application.id, decision)
      .pipe(
        finalize(() => this.processing.set(null)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (updatedApplication) => {
          this.application.set(updatedApplication);
          this.toastService.success(`Application ${decision.toLowerCase()} successfully`);
        },
      });
  }

  protected decisionTitle(): string {
    return this.pendingDecision() === 'APPROVED' ? 'Approve Application' : 'Reject Application';
  }

  protected decisionMessage(): string {
    return this.pendingDecision() === 'APPROVED'
      ? 'This marks the application as approved and publishes the final status update.'
      : 'This will reject the application and close the current review workflow.';
  }

  protected decisionLabel(): string {
    return this.pendingDecision() === 'APPROVED' ? 'Approve' : 'Reject';
  }

  protected decisionTone(): 'default' | 'danger' {
    return this.pendingDecision() === 'APPROVED' ? 'default' : 'danger';
  }
}

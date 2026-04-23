import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize, map, switchMap } from 'rxjs';
import { ApplicationResponse, ApplicationStatus } from '../../../core/models/application.model';
import { DocumentResponse } from '../../../core/models/document.model';
import { ApplicationService } from '../../../core/services/application.service';
import { ToastService } from '../../../core/services/toast.service';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { CurrencyInrPipe } from '../../../shared/pipes/currency-inr.pipe';
import { DocumentsUploadComponent } from '../../documents/upload/upload.component';

@Component({
  selector: 'app-application-detail',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    StatusBadgeComponent,
    CurrencyInrPipe,
    ConfirmDialogComponent,
    DocumentsUploadComponent,
  ],
  templateUrl: './application-detail.component.html',
  styleUrl: './application-detail.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ApplicationDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly applicationService = inject(ApplicationService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly pendingAction = signal<'submit' | 'delete' | null>(null);
  protected readonly lastUploadedFile = signal<string | null>(null);
  protected readonly application = signal<ApplicationResponse | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required]],
    amount: [1000, [Validators.required, Validators.min(1000)]],
  });

  protected readonly isDraft = computed(() => this.application()?.status === 'DRAFT');
  protected readonly status = computed<ApplicationStatus | null>(() => this.application()?.status ?? null);

  constructor() {
    this.route.paramMap
      .pipe(
        map((params) => Number(params.get('id'))),
        switchMap((id) => {
          this.loading.set(true);
          return this.applicationService
            .getById(id)
            .pipe(finalize(() => this.loading.set(false)));
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (application) => {
          this.application.set(application);
          this.form.reset({ name: application.name, amount: application.amount });
        },
        error: () => this.application.set(null),
      });
  }

  protected saveChanges(): void {
    const application = this.application();
    if (!application || this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);

    this.applicationService
      .update(application.id, this.form.getRawValue())
      .pipe(
        finalize(() => this.saving.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (updatedApplication) => {
          this.application.set(updatedApplication);
          this.form.reset({ name: updatedApplication.name, amount: updatedApplication.amount });
          this.toastService.success('Draft updated successfully');
        },
      });
  }

  protected openAction(action: 'submit' | 'delete'): void {
    this.pendingAction.set(action);
  }

  protected closeAction(): void {
    this.pendingAction.set(null);
  }

  protected confirmAction(): void {
    const action = this.pendingAction();
    if (!action) {
      return;
    }

    this.closeAction();
    if (action === 'submit') {
      this.submitApplication();
      return;
    }

    this.deleteApplication();
  }

  protected onUpload(document: DocumentResponse): void {
    this.lastUploadedFile.set(document.fileName);
  }

  protected hasError(controlName: 'name' | 'amount'): boolean {
    const control = this.form.controls[controlName];
    return control.invalid && (control.dirty || control.touched);
  }

  protected getError(controlName: 'name' | 'amount'): string {
    const control = this.form.controls[controlName];

    if (control.hasError('required')) {
      return controlName === 'name' ? 'Loan name is required' : 'Amount is required';
    }

    if (control.hasError('min')) {
      return 'Amount must be at least 1000';
    }

    return '';
  }

  protected actionTitle(): string {
    return this.pendingAction() === 'submit' ? 'Submit Application' : 'Delete Draft';
  }

  protected actionMessage(): string {
    return this.pendingAction() === 'submit'
      ? 'This will move the draft into review. You will not be able to edit it afterwards.'
      : 'This will permanently remove the draft from FinFlow.';
  }

  protected actionLabel(): string {
    return this.pendingAction() === 'submit' ? 'Submit' : 'Delete';
  }

  protected actionTone(): 'default' | 'danger' {
    return this.pendingAction() === 'submit' ? 'default' : 'danger';
  }

  private submitApplication(): void {
    const application = this.application();
    if (!application || this.saving()) {
      return;
    }

    this.saving.set(true);

    this.applicationService
      .submit(application.id)
      .pipe(
        finalize(() => this.saving.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (updatedApplication) => {
          this.application.set(updatedApplication);
          this.toastService.success('Application submitted for review');
        },
      });
  }

  private deleteApplication(): void {
    const application = this.application();
    if (!application || this.saving()) {
      return;
    }

    this.saving.set(true);

    this.applicationService
      .delete(application.id)
      .pipe(
        finalize(() => this.saving.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.toastService.success('Draft deleted successfully');
          void this.router.navigate(['/applications']);
        },
      });
  }
}

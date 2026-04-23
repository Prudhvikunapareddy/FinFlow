import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ApplicationService } from '../../../core/services/application.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-application-create',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './application-create.component.html',
  styleUrl: './application-create.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ApplicationCreateComponent {
  private readonly fb = inject(FormBuilder);
  private readonly applicationService = inject(ApplicationService);
  private readonly toastService = inject(ToastService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly submitting = signal(false);
  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required]],
    amount: [1000, [Validators.required, Validators.min(1000)]],
  });

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);

    this.applicationService
      .create(this.form.getRawValue())
      .pipe(
        finalize(() => this.submitting.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (application) => {
          this.toastService.success('Draft created successfully');
          void this.router.navigate(['/applications', application.id]);
        },
      });
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
}

import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize, interval } from 'rxjs';
import { INTEREST_RATES, LoanType } from '../../../core/models/application.model';
import { ApplicationService } from '../../../core/services/application.service';
import { ToastService } from '../../../core/services/toast.service';
import { CurrencyInrPipe } from '../../../shared/pipes/currency-inr.pipe';

@Component({
  selector: 'app-application-create',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, CurrencyInrPipe],
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
  protected readonly lastSavedAt = signal<string | null>(this.restoreDraftTimestamp());
  protected readonly loanTypes: Array<{ value: LoanType; icon: string; label: string; description: string }> = [
    { value: 'PERSONAL', icon: 'P', label: 'Personal Loan', description: 'Flexible funds for planned expenses.' },
    { value: 'HOME', icon: 'H', label: 'Home Loan', description: 'Purchase, build, or renovate a home.' },
    { value: 'VEHICLE', icon: 'V', label: 'Vehicle Loan', description: 'Finance a car, bike, or commercial vehicle.' },
    { value: 'EDUCATION', icon: 'E', label: 'Education Loan', description: 'Cover tuition and learning costs.' },
    { value: 'BUSINESS', icon: 'B', label: 'Business Loan', description: 'Working capital for your venture.' },
  ];
  protected readonly tenureOptions = [6, 12, 24, 36, 48, 60];
  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required]],
    loanType: ['PERSONAL' as LoanType, [Validators.required]],
    amount: [1000, [Validators.required, Validators.min(1000)]],
    tenureMonths: [12, [Validators.required]],
  });

  protected readonly nameValue = toSignal(this.form.controls.name.valueChanges, { initialValue: '' });
  protected readonly amountValue = toSignal(this.form.controls.amount.valueChanges, { initialValue: 1000 });
  protected readonly tenureValue = toSignal(this.form.controls.tenureMonths.valueChanges, { initialValue: 12 });
  protected readonly loanTypeValue = toSignal(this.form.controls.loanType.valueChanges, { initialValue: 'PERSONAL' as LoanType });

  protected readonly selectedInterestRate = computed(() => INTEREST_RATES[this.loanTypeValue()]);
  protected readonly emiPreview = computed(() => this.calculateEmi(this.amountValue(), this.selectedInterestRate(), this.tenureValue()));

  constructor() {
    this.restoreDraft();
    interval(30_000)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.saveDraft());
  }

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

  protected selectLoanType(loanType: LoanType): void {
    this.form.controls.loanType.setValue(loanType);
  }

  protected selectTenure(tenure: number): void {
    this.form.controls.tenureMonths.setValue(tenure);
  }

  protected saveDraft(): void {
    localStorage.setItem('finflow_application_draft', JSON.stringify(this.form.getRawValue()));
    const timestamp = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    localStorage.setItem('finflow_application_draft_saved_at', timestamp);
    this.lastSavedAt.set(timestamp);
  }

  protected hasError(controlName: 'name' | 'amount' | 'loanType' | 'tenureMonths'): boolean {
    const control = this.form.controls[controlName];
    return control.invalid && (control.dirty || control.touched);
  }

  protected getError(controlName: 'name' | 'amount' | 'loanType' | 'tenureMonths'): string {
    const control = this.form.controls[controlName];

    if (control.hasError('required')) {
      if (controlName === 'name') return 'Loan name is required';
      if (controlName === 'loanType') return 'Loan type is required';
      if (controlName === 'tenureMonths') return 'Tenure is required';
      return 'Amount is required';
    }

    if (control.hasError('min')) {
      return 'Amount must be at least 1000';
    }

    return '';
  }

  private calculateEmi(principal: number, annualRate: number, tenureMonths: number): number {
    const monthlyRate = annualRate / 12 / 100;
    const multiplier = Math.pow(1 + monthlyRate, tenureMonths);
    return Math.round((principal * monthlyRate * multiplier) / (multiplier - 1));
  }

  private restoreDraft(): void {
    const raw = localStorage.getItem('finflow_application_draft');
    if (!raw) return;
    try {
      this.form.patchValue(JSON.parse(raw));
    } catch {
      localStorage.removeItem('finflow_application_draft');
    }
  }

  private restoreDraftTimestamp(): string | null {
    return localStorage.getItem('finflow_application_draft_saved_at');
  }
}

import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ApplicationResponse, INTEREST_RATES, LoanType } from '../../core/models/application.model';
import { ApplicationService } from '../../core/services/application.service';
import { AuthService } from '../../core/services/auth.service';
import { CurrencyInrPipe } from '../../shared/pipes/currency-inr.pipe';
import { StatusBadgeComponent } from '../../shared/components/status-badge/status-badge.component';
import { LoaderComponent } from '../../shared/components/loader/loader.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, ReactiveFormsModule, DatePipe, CurrencyInrPipe, StatusBadgeComponent, LoaderComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardComponent {
  private readonly applicationService = inject(ApplicationService);
  private readonly authService = inject(AuthService);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly loading = signal(true);
  protected readonly applications = signal<ApplicationResponse[]>([]);
  protected readonly profileFirstName = signal<string | null>(this.authService.getLocalProfile()?.firstName ?? null);
  protected readonly loanTypes: Array<{ value: LoanType; label: string }> = [
    { value: 'PERSONAL', label: 'Personal' },
    { value: 'HOME', label: 'Home' },
    { value: 'VEHICLE', label: 'Vehicle' },
    { value: 'EDUCATION', label: 'Education' },
    { value: 'BUSINESS', label: 'Business' },
  ];
  protected readonly emiForm = this.fb.nonNullable.group({
    amount: [500000],
    loanType: ['PERSONAL' as LoanType],
    tenureMonths: [24],
  });
  protected readonly eligibilityForm = this.fb.nonNullable.group({
    monthlySalary: [50000],
  });
  protected readonly emiInputs = toSignal(this.emiForm.valueChanges, { initialValue: this.emiForm.getRawValue() });
  protected readonly eligibilityInputs = toSignal(this.eligibilityForm.valueChanges, { initialValue: this.eligibilityForm.getRawValue() });

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

  protected readonly quickStats = computed(() => {
    const apps = this.applications();
    const now = new Date();
    const approved = apps.filter((application) => application.status === 'APPROVED');
    
    const parseDate = (dateVal: any): Date | null => {
      if (!dateVal) return null;
      if (Array.isArray(dateVal)) {
        return new Date(dateVal[0], dateVal[1] - 1, dateVal[2], dateVal[3] || 0, dateVal[4] || 0);
      }
      const d = new Date(dateVal);
      return isNaN(d.getTime()) ? null : d;
    };

    let nextEmiDueDate: Date | null = null;
    let nextEmiAmount: number | null = null;

    if (approved.length > 0) {
      const sortedApproved = [...approved].sort((a, b) => {
        const dateA = parseDate(a.submittedAt)?.getTime() || 0;
        const dateB = parseDate(b.submittedAt)?.getTime() || 0;
        return dateB - dateA;
      });
      const latestApproved = sortedApproved[0];
      const latestDate = parseDate(latestApproved.submittedAt) || new Date();
      latestDate.setMonth(latestDate.getMonth() + 1);
      nextEmiDueDate = latestDate;

      const amount = latestApproved.amount || 0;
      const tenure = latestApproved.tenureMonths || 24;
      const loanType = latestApproved.loanType ?? 'PERSONAL';
      nextEmiAmount = this.calculateEmi(amount, INTEREST_RATES[loanType], tenure);
    }

    return {
      borrowed: approved.reduce((total, application) => total + application.amount, 0),
      thisMonth: apps.filter((application) => {
        if (!application.submittedAt) return true;
        const submitted = parseDate(application.submittedAt);
        if (!submitted) return true;
        return submitted.getMonth() === now.getMonth() && submitted.getFullYear() === now.getFullYear();
      }).length,
      nextEmiDueDate,
      nextEmiAmount,
    };
  });

  protected readonly emiBreakdown = computed(() => {
    const { amount = 0, loanType = 'PERSONAL', tenureMonths = 1 } = this.emiInputs();
    const interestRate = INTEREST_RATES[loanType];
    const emi = this.calculateEmi(amount, interestRate, tenureMonths);
    const totalPayable = emi * tenureMonths;
    const totalInterest = Math.max(0, totalPayable - amount);
    const interestPercent = totalPayable ? Math.round((totalInterest / totalPayable) * 100) : 0;

    return {
      emi,
      interestRate,
      totalInterest,
      totalPayable,
      principalPercent: 100 - interestPercent,
      interestPercent,
    };
  });

  protected readonly eligibility = computed(() => {
    const { monthlySalary = 0 } = this.eligibilityInputs();
    const salary = monthlySalary;
    return Math.round(salary * 0.5 * 12);
  });

  protected readonly email = computed(() => this.authService.getEmail() ?? 'user');
  protected readonly firstName = computed(() => this.profileFirstName()?.trim() || this.email().split('@')[0] || 'user');
  
  protected readonly currentDate = new Date().toLocaleDateString('en-US', { 
    weekday: 'long', 
    year: 'numeric', 
    month: 'long', 
    day: 'numeric' 
  });

  protected readonly greeting = computed(() => {
    const hour = new Date().getHours();
    if (hour < 12) return 'Good morning';
    if (hour < 18) return 'Good afternoon';
    return 'Good evening';
  });

  constructor() {
    this.loadApplications();
    this.loadProfile();
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

  private loadProfile(): void {
    const localProfile = this.authService.getLocalProfile();
    if (localProfile?.firstName?.trim()) {
      this.profileFirstName.set(localProfile.firstName);
      return;
    }

    this.authService
      .getProfile()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (profile) => this.profileFirstName.set(profile.firstName ?? null),
        error: () => this.profileFirstName.set(null),
      });
  }

  private calculateEmi(principal: number, annualRate: number, tenureMonths: number): number {
    const monthlyRate = annualRate / 12 / 100;
    const multiplier = Math.pow(1 + monthlyRate, tenureMonths);
    return Math.round((principal * monthlyRate * multiplier) / (multiplier - 1));
  }
}

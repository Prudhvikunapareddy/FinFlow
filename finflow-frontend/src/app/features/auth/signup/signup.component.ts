import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';

type SignupControlName =
  | 'firstName'
  | 'lastName'
  | 'dateOfBirth'
  | 'phoneNumber'
  | 'email'
  | 'password'
  | 'confirmPassword'
  | 'referralCode'
  | 'terms';

@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './signup.component.html',
  styleUrl: './signup.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SignupComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly submitting = signal(false);
  protected readonly passwordValue = signal('');
  protected readonly confirmPasswordValue = signal('');
  protected showPassword = false;
  protected showConfirmPassword = false;
  private shakeSignal = signal(false);
  protected readonly maxBirthDate = this.toDateInput(this.addYears(new Date(), -18));

  protected readonly form = this.fb.nonNullable.group(
    {
      firstName: ['', [Validators.required, Validators.maxLength(60)]],
      lastName: ['', [Validators.required, Validators.maxLength(60)]],
      dateOfBirth: ['', [Validators.required, this.minimumAgeValidator(18)]],
      phoneNumber: ['', [Validators.required, Validators.pattern(/^\d{10}$/)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8), Validators.pattern(/[A-Z]/), Validators.pattern(/[0-9]/), Validators.pattern(/[^A-Za-z0-9]/)]],
      confirmPassword: ['', [Validators.required]],
      referralCode: [''],
      terms: [false, [Validators.requiredTrue]],
    },
    { validators: this.passwordMatchValidator }
  );

  constructor() {
    this.form.controls.password.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((value) => this.passwordValue.set(value));

    this.form.controls.confirmPassword.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((value) => this.confirmPasswordValue.set(value));
  }

  protected readonly passwordCriteria = computed(() => {
    const password = this.passwordValue();
    return {
      minLength: password.length >= 8,
      number: /[0-9]/.test(password),
      uppercase: /[A-Z]/.test(password),
      special: /[^A-Za-z0-9]/.test(password),
    };
  });

  protected readonly passwordStrength = computed(() => {
    const password = this.passwordValue();
    if (!password) return 0;
    const criteria = this.passwordCriteria();
    let score = 0;
    if (criteria.minLength) score++;
    if (criteria.uppercase) score++;
    if (criteria.number) score++;
    if (criteria.special) score++;
    return Math.min(score, 4);
  });

  protected readonly passwordStrengthLabel = computed(() => {
    return ['Weak', 'Weak', 'Fair', 'Good', 'Strong'][this.passwordStrength()];
  });

  protected readonly submitTone = computed(() => {
    const criteria = this.passwordCriteria();
    const allCriteriaMet = criteria.minLength && criteria.number && criteria.uppercase && criteria.special;
    const passwordsMatch = this.passwordValue().length > 0 && this.passwordValue() === this.confirmPasswordValue();

    if (allCriteriaMet && passwordsMatch) {
      return 'strong';
    }

    if (this.passwordStrength() >= 2) {
      return 'medium';
    }

    return 'weak';
  });

  private passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const password = control.get('password')?.value;
    const confirmPasswordControl = control.get('confirmPassword');
    const confirmPassword = confirmPasswordControl?.value;
    if (password !== confirmPassword) {
      confirmPasswordControl?.setErrors({ ...confirmPasswordControl.errors, passwordMismatch: true });
      return { passwordMismatch: true };
    }
    if (confirmPasswordControl?.hasError('passwordMismatch')) {
      const { passwordMismatch, ...errors } = confirmPasswordControl.errors ?? {};
      confirmPasswordControl.setErrors(Object.keys(errors).length ? errors : null);
    }
    return null;
  }

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      this.triggerShake();
      return;
    }

    const { firstName, lastName, dateOfBirth, phoneNumber, email, password, referralCode } = this.form.getRawValue();
    this.submitting.set(true);

    this.authService
      .signup({ firstName, lastName, dateOfBirth, phoneNumber, email, password, referralCode })
      .pipe(
        finalize(() => this.submitting.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.toastService.success('Account created successfully');
          this.navigateByRole();
        },
        error: () => {
          this.triggerShake();
        }
      });
  }

  protected hasError(controlName: SignupControlName): boolean {
    const control = this.form.controls[controlName];
    return control.invalid && (control.dirty || control.touched);
  }

  protected getError(controlName: SignupControlName): string {
    const control = this.form.controls[controlName];

    if (control.hasError('required') || control.hasError('requiredTrue')) {
      if (controlName === 'terms') return 'You must accept the terms';
      return `${this.getFieldLabel(controlName)} is required`;
    }

    if (control.hasError('email')) {
      return 'Enter a valid email address';
    }

    if (control.hasError('pattern') && controlName === 'phoneNumber') {
      return 'Phone number must be 10 digits';
    }

    if (control.hasError('underage')) {
      return 'You must be at least 18 years old to apply';
    }

    if (control.hasError('minlength')) {
      return 'Password must be at least 8 characters';
    }

    if (control.hasError('pattern') && controlName === 'password') {
      return 'Password must meet all strength criteria';
    }

    if (control.hasError('passwordMismatch')) {
      return 'Passwords do not match';
    }

    return '';
  }

  private navigateByRole(): void {
    const target = this.authService.getRole() === 'ADMIN' ? ['/admin/applications'] : ['/dashboard'];
    void this.router.navigate(target);
  }

  protected togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  protected toggleConfirmPassword(): void {
    this.showConfirmPassword = !this.showConfirmPassword;
  }

  protected hasShakeAnimation(): boolean {
    return this.shakeSignal();
  }

  private triggerShake(): void {
    this.shakeSignal.set(false);
    setTimeout(() => this.shakeSignal.set(true), 10);
    setTimeout(() => this.shakeSignal.set(false), 410);
  }

  private minimumAgeValidator(minimumAge: number): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) {
        return null;
      }

      const birthDate = new Date(`${control.value}T00:00:00`);
      if (Number.isNaN(birthDate.getTime())) {
        return { underage: true };
      }

      const today = new Date();
      const minimumBirthDate = this.addYears(today, -minimumAge);
      return birthDate <= minimumBirthDate ? null : { underage: true };
    };
  }

  private getFieldLabel(controlName: SignupControlName): string {
    const labels: Record<SignupControlName, string> = {
      firstName: 'First name',
      lastName: 'Last name',
      dateOfBirth: 'Date of birth',
      phoneNumber: 'Phone number',
      email: 'Email',
      password: 'Password',
      confirmPassword: 'Confirm password',
      referralCode: 'Referral code',
      terms: 'Terms',
    };
    return labels[controlName];
  }

  private addYears(date: Date, years: number): Date {
    const result = new Date(date);
    result.setFullYear(result.getFullYear() + years);
    return result;
  }

  private toDateInput(date: Date): string {
    return date.toISOString().slice(0, 10);
  }
}

import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { LoaderComponent } from '../../shared/components/loader/loader.component';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe, LoaderComponent],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfileComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly loading = signal(true);
  protected readonly editing = signal(false);
  protected readonly saving = signal(false);
  protected readonly changingPassword = signal(false);

  protected readonly profileForm = this.fb.nonNullable.group({
    firstName: [''],
    lastName: [''],
    email: [{ value: '', disabled: true }],
    phoneNumber: ['', [Validators.pattern(/^\d{10}$/)]],
    dateOfBirth: [''],
    createdAt: [''],
  });

  protected readonly passwordForm = this.fb.nonNullable.group({
    currentPassword: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
  });

  constructor() {
    const cached = this.authService.getLocalProfile();
    if (cached) {
      this.profileForm.patchValue({
        firstName: cached.firstName ?? '',
        lastName: cached.lastName ?? '',
        email: cached.email,
        phoneNumber: cached.phoneNumber ?? '',
        dateOfBirth: cached.dateOfBirth ?? '',
        createdAt: cached.createdAt ?? '',
      });
    }

    this.authService.getProfile()
      .pipe(finalize(() => this.loading.set(false)), takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (profile) => this.profileForm.patchValue({
          firstName: profile.firstName ?? '',
          lastName: profile.lastName ?? '',
          email: profile.email,
          phoneNumber: profile.phoneNumber ?? '',
          dateOfBirth: profile.dateOfBirth ?? '',
          createdAt: profile.createdAt ?? '',
        }),
        error: () => {
          this.loading.set(false);
          if (!cached) {
            this.profileForm.patchValue({ email: this.authService.getEmail() ?? '' });
          }
        },
      });
  }

  protected saveProfile(): void {
    if (this.profileForm.invalid || this.saving()) {
      this.profileForm.markAllAsTouched();
      return;
    }

    const { firstName, lastName, phoneNumber, dateOfBirth } = this.profileForm.getRawValue();
    this.saving.set(true);
    this.authService.updateProfile({ firstName, lastName, phoneNumber, dateOfBirth })
      .pipe(finalize(() => this.saving.set(false)), takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.editing.set(false);
          this.toastService.success('Profile updated');
        },
      });
  }

  protected changePassword(): void {
    if (this.passwordForm.invalid || this.changingPassword()) {
      this.passwordForm.markAllAsTouched();
      return;
    }

    this.changingPassword.set(true);
    this.authService.changePassword(this.passwordForm.getRawValue())
      .pipe(finalize(() => this.changingPassword.set(false)), takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (message) => {
          this.passwordForm.reset();
          this.toastService.success(message);
        },
      });
  }
}

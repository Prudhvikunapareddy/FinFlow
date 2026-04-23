import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { AdminUserRecord } from '../../../core/models/admin-user.model';
import { AdminService } from '../../../core/services/admin.service';
import { ToastService } from '../../../core/services/toast.service';

type AssignableRole = 'USER' | 'ADMIN';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  templateUrl: './admin-users.component.html',
  styleUrl: './admin-users.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminUsersComponent {
  private readonly adminService = inject(AdminService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly loading = signal(true);
  protected readonly savingUserId = signal<number | null>(null);
  protected readonly users = signal<AdminUserRecord[]>([]);
  protected readonly roleDrafts = signal<Record<number, AssignableRole>>({});

  constructor() {
    this.loadUsers();
  }

  protected updateDraft(userId: number, role: string): void {
    this.roleDrafts.update((drafts) => ({ ...drafts, [userId]: role as AssignableRole }));
  }

  protected selectedRole(user: AdminUserRecord): AssignableRole {
    const draft = this.roleDrafts()[user.id];
    if (draft === 'ADMIN' || draft === 'USER') {
      return draft;
    }

    return user.role === 'ADMIN' ? 'ADMIN' : 'USER';
  }

  protected hasPendingChange(user: AdminUserRecord): boolean {
    return this.selectedRole(user) !== user.role;
  }

  protected save(user: AdminUserRecord): void {
    const nextRole = this.selectedRole(user);
    if (!this.hasPendingChange(user) || this.savingUserId() !== null) {
      return;
    }

    this.savingUserId.set(user.id);

    this.adminService
      .updateUser(user.id, nextRole)
      .pipe(
        finalize(() => this.savingUserId.set(null)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (updatedUser) => {
          this.users.update((users) =>
            users.map((entry) => (entry.id === updatedUser.id ? updatedUser : entry)),
          );
          this.roleDrafts.update((drafts) => ({ ...drafts, [updatedUser.id]: updatedUser.role as AssignableRole }));
          this.toastService.success(`Updated role for ${updatedUser.email}`);
        },
      });
  }

  private loadUsers(): void {
    this.loading.set(true);

    this.adminService
      .getUsers()
      .pipe(
        finalize(() => this.loading.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (users) => {
          this.users.set(users);
          this.roleDrafts.set(
            users.reduce<Record<number, AssignableRole>>((drafts, user) => {
              drafts[user.id] = user.role === 'ADMIN' ? 'ADMIN' : 'USER';
              return drafts;
            }, {}),
          );
        },
        error: () => this.users.set([]),
      });
  }
}

import { ChangeDetectionStrategy, Component, output, input } from '@angular/core';

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  templateUrl: './confirm-dialog.component.html',
  styleUrl: './confirm-dialog.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConfirmDialogComponent {
  readonly open = input(false);
  readonly title = input('Confirm action');
  readonly message = input('Please confirm to continue.');
  readonly confirmLabel = input('Confirm');
  readonly tone = input<'default' | 'danger'>('default');

  readonly confirmed = output<void>();
  readonly cancelled = output<void>();
}

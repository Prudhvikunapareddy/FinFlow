import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { ApplicationStatus } from '../../../core/models/application.model';

@Component({
  selector: 'app-status-badge',
  standalone: true,
  templateUrl: './status-badge.component.html',
  styleUrl: './status-badge.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StatusBadgeComponent {
  readonly status = input.required<ApplicationStatus | string>();

  protected readonly appearance = computed(() => {
    switch (this.status()) {
      case 'APPROVED':
        return 'approved';
      case 'REJECTED':
        return 'rejected';
      case 'SUBMITTED':
        return 'submitted';
      case 'DOCS_VERIFIED':
        return 'verified';
      default:
        return 'draft';
    }
  });

  protected readonly label = computed(() => this.status().replaceAll('_', ' '));
}

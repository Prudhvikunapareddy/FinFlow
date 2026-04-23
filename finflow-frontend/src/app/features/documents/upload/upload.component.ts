import { ChangeDetectionStrategy, Component, input, output, inject, signal } from '@angular/core';
import { finalize } from 'rxjs';
import { DocumentResponse } from '../../../core/models/document.model';
import { DocumentService } from '../../../core/services/document.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-documents-upload',
  standalone: true,
  templateUrl: './upload.component.html',
  styleUrl: './upload.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DocumentsUploadComponent {
  private readonly documentService = inject(DocumentService);
  private readonly toastService = inject(ToastService);

  readonly applicationId = input.required<number>();
  readonly uploaded = output<DocumentResponse>();

  protected readonly uploading = signal(false);
  protected readonly selectedFileName = signal<string | null>(null);
  protected readonly uploadedDocument = signal<DocumentResponse | null>(null);

  protected onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.item(0) ?? null;

    if (!file) {
      this.selectedFileName.set(null);
      return;
    }

    if (!this.isAllowedFile(file)) {
      input.value = '';
      this.selectedFileName.set(null);
      this.toastService.error('Only PDF, JPG, and PNG files are allowed');
      return;
    }

    this.selectedFileName.set(file.name);
  }

  protected upload(fileInput: HTMLInputElement): void {
    const file = fileInput.files?.item(0);
    if (!file || this.uploading()) {
      return;
    }

    this.uploading.set(true);

    this.documentService
      .upload(file, this.applicationId())
      .pipe(finalize(() => this.uploading.set(false)))
      .subscribe({
        next: (document) => {
          this.uploadedDocument.set(document);
          this.selectedFileName.set(document.fileName);
          fileInput.value = '';
          this.toastService.success(`Uploaded ${document.fileName}`);
          this.uploaded.emit(document);
        },
      });
  }

  private isAllowedFile(file: File): boolean {
    return ['application/pdf', 'image/jpeg', 'image/png'].includes(file.type);
  }
}

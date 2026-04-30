import { ChangeDetectionStrategy, Component, input, output, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { DocumentResponse } from '../../../core/models/document.model';
import { DocumentType } from '../../../core/models/document.model';
import { DocumentService } from '../../../core/services/document.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-documents-upload',
  standalone: true,
  imports: [FormsModule],
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
  protected readonly selectedFileSize = signal<string | null>(null);
  protected readonly uploadedDocument = signal<DocumentResponse | null>(null);
  protected readonly isDragging = signal(false);
  protected selectedDocumentType: DocumentType = 'SALARY_SLIP';
  protected readonly documentTypes: Array<{ value: DocumentType; label: string }> = [
    { value: 'SALARY_SLIP', label: 'Salary Slip' },
    { value: 'BANK_STATEMENT', label: 'Bank Statement' },
    { value: 'ID_PROOF', label: 'ID Proof' },
    { value: 'ADDRESS_PROOF', label: 'Address Proof' },
    { value: 'OTHER', label: 'Other' },
  ];
  protected selectedFile: File | null = null;

  protected onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.item(0) ?? null;
    this.handleFile(file);
  }

  protected onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging.set(true);
  }

  protected onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging.set(false);
  }

  protected onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging.set(false);
    
    const file = event.dataTransfer?.files?.item(0) ?? null;
    this.handleFile(file);
  }

  private handleFile(file: File | null): void {
    if (!file) {
      this.selectedFileName.set(null);
      this.selectedFileSize.set(null);
      this.selectedFile = null;
      return;
    }

    if (!this.isAllowedFile(file)) {
      this.selectedFileName.set(null);
      this.selectedFileSize.set(null);
      this.selectedFile = null;
      this.toastService.error('Only PDF, JPG, and PNG files are allowed');
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      this.selectedFileName.set(null);
      this.selectedFileSize.set(null);
      this.selectedFile = null;
      this.toastService.error('File size must be under 5MB');
      return;
    }

    this.selectedFileName.set(file.name);
    this.selectedFileSize.set(this.formatFileSize(file.size));
    this.selectedFile = file;
  }

  protected upload(): void {
    const file = this.selectedFile;
    if (!file || this.uploading()) {
      return;
    }

    this.uploading.set(true);

    this.documentService
      .upload(file, this.applicationId(), this.selectedDocumentType)
      .pipe(finalize(() => this.uploading.set(false)))
      .subscribe({
        next: (document) => {
          this.uploadedDocument.set(document);
          this.selectedFileName.set(document.fileName);
          this.selectedFileSize.set(null);
          this.selectedFile = null;
          this.toastService.success(`Uploaded ${document.fileName}`);
          this.uploaded.emit(document);
        },
      });
  }

  private isAllowedFile(file: File): boolean {
    return ['application/pdf', 'image/jpeg', 'image/png'].includes(file.type);
  }

  protected removeFile(): void {
    this.selectedFileName.set(null);
    this.selectedFileSize.set(null);
    this.selectedFile = null;
  }

  private formatFileSize(size: number): string {
    if (size < 1024 * 1024) {
      return `${Math.max(1, Math.round(size / 1024))} KB`;
    }

    return `${(size / 1024 / 1024).toFixed(1)} MB`;
  }
}

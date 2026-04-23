import { HttpClient, HttpResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { DocumentResponse } from '../models/document.model';
import { API_BASE_URL } from '../tokens/api-base-url.token';

@Injectable({ providedIn: 'root' })
export class DocumentService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);
  private readonly documentsUrl = `${this.apiBaseUrl}/documents`;

  upload(file: File, applicationId: number): Observable<DocumentResponse> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('applicationId', String(applicationId));

    return this.http.post<DocumentResponse>(`${this.documentsUrl}/upload`, formData);
  }

  download(id: number): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.documentsUrl}/${id}`, {
      observe: 'response',
      responseType: 'blob',
    });
  }

  hasDocuments(applicationId: number): Observable<boolean> {
    return this.http.get<boolean>(`${this.documentsUrl}/applications/${applicationId}/exists`);
  }
}

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { AdminApplicationResponse, ApplicationStatus } from '../models/application.model';
import { AdminUserRecord } from '../models/admin-user.model';
import { API_BASE_URL } from '../tokens/api-base-url.token';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);
  private readonly adminUrl = `${this.apiBaseUrl}/admin`;

  getApplications(): Observable<AdminApplicationResponse[]> {
    return this.http.get<AdminApplicationResponse[]>(`${this.adminUrl}/applications`);
  }

  getApplication(id: number): Observable<AdminApplicationResponse> {
    return this.http.get<AdminApplicationResponse>(`${this.adminUrl}/applications/${id}`);
  }

  decideApplication(id: number, status: Extract<ApplicationStatus, 'APPROVED' | 'REJECTED'>): Observable<AdminApplicationResponse> {
    return this.http.post<AdminApplicationResponse>(`${this.adminUrl}/applications/${id}/decision`, { status });
  }

  bulkDecideApplications(ids: number[], status: Extract<ApplicationStatus, 'APPROVED' | 'REJECTED'>): Observable<AdminApplicationResponse[]> {
    return this.http.post<AdminApplicationResponse[]>(`${this.adminUrl}/applications/bulk-decision`, { ids, status });
  }

  updateApplicationNotes(id: number, notes: string): Observable<AdminApplicationResponse> {
    return this.http.put<AdminApplicationResponse>(`${this.adminUrl}/applications/${id}/notes`, { notes });
  }

  verifyDocument(id: number): Observable<string> {
    return this.http.put(`${this.adminUrl}/documents/${id}/verify`, {}, { responseType: 'text' });
  }

  getUsers(): Observable<AdminUserRecord[]> {
    return this.http.get<Array<AdminUserRecord | string>>(`${this.adminUrl}/users`).pipe(
      map((users) =>
        users.map((user, index) =>
          typeof user === 'string'
            ? { id: index + 1, email: user, role: 'USER' }
            : user,
        ),
      ),
    );
  }

  updateUser(id: number, role: 'USER' | 'ADMIN'): Observable<AdminUserRecord> {
    return this.http.put<AdminUserRecord>(`${this.adminUrl}/users/${id}`, { role });
  }

  getReports(): Observable<string> {
    return this.http.get(`${this.adminUrl}/reports`, { responseType: 'text' });
  }
}

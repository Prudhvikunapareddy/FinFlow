import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApplicationRequest, ApplicationResponse } from '../models/application.model';
import { API_BASE_URL } from '../tokens/api-base-url.token';

@Injectable({ providedIn: 'root' })
export class ApplicationService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);
  private readonly applicationsUrl = `${this.apiBaseUrl}/applications`;

  create(payload: ApplicationRequest): Observable<ApplicationResponse> {
    return this.http.post<ApplicationResponse>(this.applicationsUrl, payload);
  }

  getAll(): Observable<ApplicationResponse[]> {
    return this.http.get<ApplicationResponse[]>(this.applicationsUrl);
  }

  getMine(): Observable<ApplicationResponse[]> {
    return this.http.get<ApplicationResponse[]>(`${this.applicationsUrl}/my`);
  }

  getById(id: number): Observable<ApplicationResponse> {
    return this.http.get<ApplicationResponse>(`${this.applicationsUrl}/${id}`);
  }

  update(id: number, payload: ApplicationRequest): Observable<ApplicationResponse> {
    return this.http.put<ApplicationResponse>(`${this.applicationsUrl}/${id}`, payload);
  }

  delete(id: number): Observable<string> {
    return this.http.delete(`${this.applicationsUrl}/${id}`, { responseType: 'text' });
  }

  submit(id: number): Observable<ApplicationResponse> {
    return this.http.post<ApplicationResponse>(`${this.applicationsUrl}/${id}/submit`, {});
  }

  getStatus(id: number): Observable<string> {
    return this.http.get(`${this.applicationsUrl}/${id}/status`, { responseType: 'text' });
  }
}

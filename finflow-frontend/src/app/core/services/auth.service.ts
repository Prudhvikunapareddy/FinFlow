import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { AuthCredentials, JwtPayload, SignupPayload, UserProfile, ChangePasswordPayload, UserRole } from '../models/auth.model';
import { API_BASE_URL } from '../tokens/api-base-url.token';

const TOKEN_KEY = 'finflow_token';
const PROFILE_KEY = 'finflow_profile';

interface DecodedSession {
  token: string;
  email: string | null;
  role: UserRole | null;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  private readonly session = signal<DecodedSession | null>(this.restoreSession());

  readonly email = computed(() => this.session()?.email ?? null);
  readonly role = computed(() => this.session()?.role ?? null);
  readonly loggedIn = computed(() => !!this.session()?.token);

  login(email: string, password: string): Observable<string> {
    const payload: AuthCredentials = { email: email.trim(), password };

    return this.http
      .post(`${this.apiBaseUrl}/auth/login`, payload, { responseType: 'text' })
      .pipe(tap((token) => this.persistSession(token)));
  }

  signup(signup: SignupPayload): Observable<string> {
    const payload: SignupPayload = {
      ...signup,
      email: signup.email.trim(),
      firstName: signup.firstName.trim(),
      lastName: signup.lastName.trim(),
      phoneNumber: signup.phoneNumber.trim(),
      referralCode: signup.referralCode?.trim() || undefined,
    };
    return this.http
      .post(`${this.apiBaseUrl}/auth/signup`, payload, { responseType: 'text' })
      .pipe(tap((token) => {
        this.persistLocalProfile(payload);
        this.persistSession(token);
      }));
  }

  getProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.apiBaseUrl}/auth/profile`);
  }

  updateProfile(profile: Partial<UserProfile>): Observable<UserProfile> {
    return this.http.put<UserProfile>(`${this.apiBaseUrl}/auth/profile`, profile).pipe(
      tap((updated) => this.persistLocalProfile({
        firstName: updated.firstName ?? '',
        lastName: updated.lastName ?? '',
        dateOfBirth: updated.dateOfBirth ?? '',
        phoneNumber: updated.phoneNumber ?? '',
        email: updated.email,
        password: '',
      })),
    );
  }

  changePassword(payload: ChangePasswordPayload): Observable<string> {
    return this.http.put(`${this.apiBaseUrl}/auth/password`, payload, { responseType: 'text' });
  }

  getLocalProfile(): UserProfile | null {
    const raw = localStorage.getItem(PROFILE_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as UserProfile;
    } catch {
      localStorage.removeItem(PROFILE_KEY);
      return null;
    }
  }

  logout(): void {
    this.clearSession();
    void this.router.navigate(['/login']);
  }

  clearSession(): void {
    localStorage.removeItem(TOKEN_KEY);
    this.session.set(null);
  }

  getToken(): string | null {
    return this.session()?.token ?? null;
  }

  getEmail(): string | null {
    return this.session()?.email ?? null;
  }

  getRole(): UserRole | null {
    return this.session()?.role ?? null;
  }

  isLoggedIn(): boolean {
    return this.loggedIn();
  }

  private persistSession(token: string): void {
    const decoded = this.decodeToken(token);

    if (!decoded) {
      this.clearSession();
      return;
    }

    localStorage.setItem(TOKEN_KEY, token);
    this.session.set(decoded);
  }

  private persistLocalProfile(profile: SignupPayload): void {
    localStorage.setItem(PROFILE_KEY, JSON.stringify({
      firstName: profile.firstName,
      lastName: profile.lastName,
      dateOfBirth: profile.dateOfBirth,
      phoneNumber: profile.phoneNumber,
      email: profile.email.trim(),
      createdAt: this.getLocalProfile()?.createdAt ?? new Date().toISOString(),
    }));
  }

  private restoreSession(): DecodedSession | null {
    const token = localStorage.getItem(TOKEN_KEY);
    if (!token) {
      return null;
    }

    const decoded = this.decodeToken(token);
    if (!decoded) {
      localStorage.removeItem(TOKEN_KEY);
    }

    return decoded;
  }

  private decodeToken(token: string): DecodedSession | null {
    const segments = token.split('.');
    if (segments.length < 2) {
      return null;
    }

    try {
      const payloadSegment = segments[1]
        .replace(/-/g, '+')
        .replace(/_/g, '/')
        .padEnd(Math.ceil(segments[1].length / 4) * 4, '=');
      const payload = JSON.parse(atob(payloadSegment)) as JwtPayload;

      return {
        token,
        email: payload.sub?.trim() ?? null,
        role: this.normalizeRole(payload.role),
      };
    } catch {
      return null;
    }
  }

  private normalizeRole(role: string | undefined): UserRole | null {
    if (!role) {
      return null;
    }

    const normalized = role.trim().toUpperCase();
    if (normalized === 'ADMIN' || normalized === 'USER') {
      return normalized;
    }

    return 'UNKNOWN';
  }
}

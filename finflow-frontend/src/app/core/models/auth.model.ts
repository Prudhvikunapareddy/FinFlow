export interface AuthCredentials {
  email: string;
  password: string;
}

export interface JwtPayload {
  sub?: string;
  role?: string;
  exp?: number;
  iat?: number;
}

export type UserRole = 'USER' | 'ADMIN' | 'UNKNOWN';

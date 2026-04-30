export interface AuthCredentials {
  email: string;
  password: string;
}

export interface SignupPayload extends AuthCredentials {
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  phoneNumber: string;
  referralCode?: string;
}

export interface UserProfile {
  id?: number;
  firstName?: string;
  lastName?: string;
  email: string;
  phoneNumber?: string;
  dateOfBirth?: string;
  createdAt?: string;
  role?: UserRole;
}

export interface ChangePasswordPayload {
  currentPassword: string;
  newPassword: string;
}

export interface JwtPayload {
  sub?: string;
  role?: string;
  exp?: number;
  iat?: number;
}

export type UserRole = 'USER' | 'ADMIN' | 'UNKNOWN';

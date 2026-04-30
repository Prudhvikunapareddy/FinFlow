import { UserRole } from './auth.model';

export interface AdminUserRecord {
  id: number;
  email: string;
  firstName?: string;
  lastName?: string;
  phoneNumber?: string;
  dateOfBirth?: string;
  createdAt?: string;
  role: UserRole;
}

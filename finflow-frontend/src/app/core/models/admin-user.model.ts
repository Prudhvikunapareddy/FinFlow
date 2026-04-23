import { UserRole } from './auth.model';

export interface AdminUserRecord {
  id: number;
  email: string;
  role: UserRole;
}

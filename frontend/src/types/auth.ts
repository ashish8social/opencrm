export interface AuthRequest {
  username: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  username: string;
  fullName: string;
}

export interface UserProfile {
  id: string;
  username: string;
  email: string;
  fullName: string;
  active: boolean;
}

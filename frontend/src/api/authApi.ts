import client from './client';
import { AuthRequest, AuthResponse, UserProfile } from '../types/auth';
import { ApiResponse } from '../types/record';

export const authApi = {
  login: (data: AuthRequest) =>
    client.post<ApiResponse<AuthResponse>>('/auth/login', data).then(r => r.data.data),

  refresh: (refreshToken: string) =>
    client.post<ApiResponse<AuthResponse>>('/auth/refresh', { refreshToken }).then(r => r.data.data),

  me: () =>
    client.get<ApiResponse<UserProfile>>('/auth/me').then(r => r.data.data),
};

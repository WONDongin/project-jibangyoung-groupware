// types/api/auth.d.ts

export type ApiResponse<T> = {
  success: boolean;
  data?: T;
  errorCode?: string;
  message?: string;
};

export type LoginTokenResponse = {
  tokenType: string;
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  issuedAt: string;
  expiresAt: string;
};

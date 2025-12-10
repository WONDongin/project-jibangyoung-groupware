// 📁 types/api/auth.ts

/**
 * 로그인 성공 시 반환되는 토큰 구조
 * - accessToken: JWT 액세스 토큰
 * - refreshToken: JWT 리프레시 토큰
 * - tokenType: (Optional) 토큰 타입, 보통 "Bearer"
 * - expiresIn: (Optional) 만료 시간(초)
 */
export interface LoginTokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType?: string;
  expiresIn?: number;
  // 기타 서버에서 내려주는 항목이 있다면 추가
}

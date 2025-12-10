// libs/auth/storage.ts

/**
 * 인증/세션 관련 로컬 스토리지 키를 일괄 정리한다.
 * 다른 화면에서도 재사용 가능하도록 유틸로 분리.
 */
export function clearAuthStorage(): void {
  if (typeof window === "undefined") return;

  try {
    const keys = [
      "accessToken",
      "refreshToken",
      "userId",
      "username",
      "email",
      "role",
      "status",
      "sessionExpired",
      "sessionExpiredReason",
      "bookmarkedPolicyIds",
    ];

    for (const k of keys) {
      localStorage.removeItem(k);
    }
  } catch {
    // 로컬 스토리지 접근 불가(프라이빗 모드/용량 초과 등) 시 무시
  }
}

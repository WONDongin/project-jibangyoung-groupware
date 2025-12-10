// libs/api/auth/auth.api.ts
import axios from "axios";

// --- ENUMS/DTO ---
export type UserRole = "USER" | "ADMIN" | "MENTOR_A" | "MENTOR_B" | "MENTOR_C";
export type UserStatus = "ACTIVE" | "DEACTIVATED" | "LOCKED" | "PENDING";

export interface UserDto {
  id: number;
  username: string;
  email: string;
  nickname?: string;
  phone?: string;
  role: UserRole;
  status: UserStatus;
  profileImageUrl?: string;
  birthDate?: string;
  gender?: string;
  region?: string;
  lastLoginAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface LoginResponse {
  user: UserDto;
  tokenType: string;
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  issuedAt: string;
  expiresAt: string;
}

export interface ApiEnvelope<T> {
  success: boolean;
  data: T;
  message?: string;
}

export interface ApiError {
  code?: string;
  message: string;
}

// axios 인스턴스
const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_BASE_URL,
  headers: { "Content-Type": "application/json" },
  withCredentials: true,
});

function handleApiError(error: unknown): never {
  if (axios.isAxiosError(error)) {
    const res = error.response;
    const apiError: ApiError = (res?.data as any) ?? { message: "요청 실패" };
    switch (apiError.code) {
      case "USER_NOT_FOUND":
        throw new Error("존재하지 않는 계정입니다.");
      case "INVALID_PASSWORD":
        throw new Error("비밀번호가 올바르지 않습니다.");
      case "ACCOUNT_LOCKED":
        throw new Error("계정이 잠겨있습니다. 관리자에게 문의하세요.");
      case "USERNAME_ALREADY_EXISTS":
        throw new Error("이미 사용 중인 아이디입니다.");
      case "EMAIL_ALREADY_EXISTS":
        throw new Error("이미 등록된 이메일입니다.");
      case "EMAIL_SEND_FAILED":
        throw new Error("메일 발송에 실패했습니다.");
      case "TOKEN_INVALID":
        throw new Error("유효하지 않은 토큰입니다.");
      case "TOKEN_EXPIRED":
        throw new Error("토큰이 만료되었습니다. 다시 시도해주세요.");
      case "PASSWORD_MISMATCH":
        throw new Error("비밀번호가 일치하지 않습니다.");
      default:
        throw new Error(apiError.message || "요청에 실패했습니다.");
    }
  }
  throw new Error("네트워크 연결 실패 (서버 응답 없음)");
}

/* --------------------
 * 비밀번호 재설정
 * -------------------- */
export async function sendResetPwEmail(email: string): Promise<void> {
  try {
    const res = await api.post<ApiEnvelope<void>>("/api/auth/send-reset-pw", { email });
    if (!res.data.success) throw new Error(res.data.message || "메일 발송에 실패했습니다.");
  } catch (error) {
    handleApiError(error);
  }
}

export async function verifyResetPwToken(token: string): Promise<void> {
  try {
    const res = await api.post<ApiEnvelope<void>>("/api/auth/verify-reset-token", { token });
    if (!res.data.success) throw new Error(res.data.message || "토큰 인증 실패");
  } catch (error) {
    handleApiError(error);
  }
}

export interface ResetPasswordPayload {
  token: string;
  newPassword: string;
  newPasswordConfirm: string;
}
export async function resetPassword(payload: ResetPasswordPayload): Promise<void> {
  try {
    const res = await api.post<ApiEnvelope<void>>("/api/auth/reset-password", payload);
    if (!res.data.success) throw new Error(res.data.message || "비밀번호 변경 실패");
  } catch (error) {
    handleApiError(error);
  }
}

/* --------------------
 * 중복 체크/인증 코드
 * -------------------- */
export async function checkUsername(
  username: string
): Promise<{ available: boolean; message: string }> {
  try {
    const res = await api.get<ApiEnvelope<{ data: boolean; message: string }>>(`/api/auth/check-username`, {
      params: { username },
    });
    return {
      available: res.data.data?.data ?? false,
      message: res.data.data?.message || res.data.message || "",
    };
  } catch (error) {
    handleApiError(error);
  }
}

export async function checkEmail(email: string): Promise<{ available: boolean; message: string }> {
  try {
    const res = await api.get<ApiEnvelope<{ data: boolean; message: string }>>(`/api/auth/check-email`, {
      params: { email },
    });
    return {
      available: res.data.data?.data ?? false,
      message: res.data.data?.message || res.data.message || "",
    };
  } catch (error) {
    handleApiError(error);
  }
}

export async function sendCode(email: string): Promise<{ message: string }> {
  try {
    const res = await api.post<ApiEnvelope<void>>(`/api/auth/send-code`, { email });
    return { message: res.data.message || "인증코드가 발송되었습니다." };
  } catch (error) {
    handleApiError(error);
  }
}
export async function sendCodeByEmail(email: string): Promise<{ message: string }> {
  try {
    const res = await api.post<ApiEnvelope<void>>(`/api/auth/send-code`, { email });
    return { message: res.data.message || "이메일로 인증코드가 발송되었습니다." };
  } catch (error) {
    handleApiError(error);
  }
}

export async function verifyCode(email: string, code: string): Promise<{ valid: boolean; message: string }> {
  try {
    const res = await api.post<ApiEnvelope<boolean>>(`/api/auth/verify-code`, { email, code });
    return {
      valid: res.data.data ?? false,
      message: res.data.message || (res.data.data ? "인증 성공!" : "인증코드가 올바르지 않습니다."),
    };
  } catch (error) {
    handleApiError(error);
  }
}

/* --------------------
 * 회원가입/로그인
 * -------------------- */
export interface SignupRequest {
  username: string;
  email: string;
  password: string;
  passwordConfirm: string;
  nickname?: string;
  phone?: string;
  profileImageUrl?: string;
  birthDate?: string;
  gender?: string;
  region?: string;
}

export async function signup(payload: SignupRequest): Promise<UserDto> {
  try {
    const res = await api.post<ApiEnvelope<UserDto>>(`/api/auth/signup`, payload);
    if (!res.data.success || !res.data.data?.id) {
      throw new Error(res.data.message || "회원가입 응답이 올바르지 않습니다.");
    }
    return res.data.data;
  } catch (error) {
    handleApiError(error);
  }
}

export async function loginWithEmail(username: string, password: string): Promise<LoginResponse> {
  try {
    const response = await api.post<ApiEnvelope<LoginResponse>>("/api/auth/login", { username, password });
    const envelope = response.data;

    if (!envelope.success) throw new Error(envelope.message || "로그인에 실패했습니다.");
    if (
      !envelope.data?.user?.id ||
      !envelope.data?.accessToken ||
      !envelope.data?.refreshToken ||
      !envelope.data?.tokenType ||
      !envelope.data?.expiresIn ||
      !envelope.data?.issuedAt ||
      !envelope.data?.expiresAt
    ) {
      throw new Error("로그인 응답이 올바르지 않습니다.");
    }
    return envelope.data;
  } catch (error) {
    handleApiError(error);
  }
}

/* --------------------
 * 아이디 찾기
 * -------------------- */
export async function sendFindIdCode(email: string): Promise<{ message: string }> {
  try {
    const res = await api.post<ApiEnvelope<void>>(`/api/auth/find-id/send-code`, { email });
    return { message: res.data.message || "아이디 찾기 인증코드가 발송되었습니다." };
  } catch (error) {
    handleApiError(error);
  }
}

export async function verifyFindIdCode(email: string, code: string): Promise<{ valid: boolean; message: string }> {
  try {
    const res = await api.post<ApiEnvelope<boolean>>(`/api/auth/find-id/verify-code`, { email, code });
    return {
      valid: res.data.data ?? false,
      message: res.data.message || (res.data.data ? "인증 성공!" : "인증코드가 올바르지 않습니다."),
    };
  } catch (error) {
    handleApiError(error);
  }
}

export async function findIdByEmailAndCode(email: string, code: string): Promise<{ username: string }> {
  try {
    const response = await api.post<ApiEnvelope<{ username: string }>>("/api/auth/find-id", { email, code });
    const envelope = response.data;

    if (!envelope.success) throw new Error(envelope.message || "아이디 찾기에 실패했습니다.");
    if (!envelope.data?.username) throw new Error("일치하는 아이디가 없습니다.");
    return envelope.data;
  } catch (error) {
    handleApiError(error);
  }
}

/* --------------------
 * 로그아웃 (수정 완료)
 * -------------------- */
/**
 * 서버 로그아웃
 * - Authorization: Bearer <accessToken>  (가능하면 포함)
 * - Refresh-Token: <refreshToken>        (필수)
 * - 200/204 모두 성공으로 처리
 */
export async function logout(accessToken: string | null, refreshToken: string): Promise<void> {
  if (!refreshToken || refreshToken.length < 20) {
    return;
  }

  const headers: Record<string, string> = { "Refresh-Token": refreshToken };
  if (accessToken && accessToken.length > 20) {
    headers.Authorization = `Bearer ${accessToken}`;
  }

  try {
    await api.post(
      "/api/auth/logout",
      {},
      {
        headers,
        validateStatus: (s) => (s >= 200 && s < 300) || s === 204,
      }
    );
  } catch {
    // 서버 실패는 무시
  }
}

export default api;

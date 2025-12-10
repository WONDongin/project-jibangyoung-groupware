import { useAuthStore } from "@/store/authStore";
import axios, {
  AxiosError,
  AxiosInstance,
  AxiosRequestConfig,
  AxiosResponse,
  InternalAxiosRequestConfig,
} from "axios";

/* ------------------------------------------------------------------ */
/* 1. 타입 선언                                                        */
/* ------------------------------------------------------------------ */
export type Tokens = {
  accessToken: string;
  refreshToken: string;
  tokenType: string | null;
  expiresIn: number | null;
  issuedAt: string | null;
  expiresAt: string | null;
};

interface AxiosRequestConfigRetry extends AxiosRequestConfig {
  _retry?: boolean;
  _retryCount?: number;
  _skipAuthRefresh?: boolean;
}

interface ApiErrorResponse {
  code?: string;
  errorCode?: string;
  message?: string;
  [key: string]: any;
}

interface RefreshSuccessPayload {
  accessToken: string;
  refreshToken: string;
  user: any;
  tokenType?: string | null;
  expiresIn?: number | null;
  issuedAt?: string | null;
  expiresAt?: string | null;
}

/* ------------------------------------------------------------------ */
/* 2. 별도 Axios 인스턴스 (재발급 전용)                                 */
/* ------------------------------------------------------------------ */
const refreshAxios: AxiosInstance = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_BASE_URL,
  headers: { "Content-Type": "application/json" },
  withCredentials: true,
  timeout: 10000,
});

const api: AxiosInstance = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_BASE_URL,
  headers: { "Content-Type": "application/json" },
  withCredentials: true,
  timeout: 15000,
});

/* ------------------------------------------------------------------ */
/* 3. 토큰 관리 헬퍼                                                   */
/* ------------------------------------------------------------------ */
const getValidAccessToken = (): string | null => {
  if (typeof window === "undefined") return null;

  const localToken = localStorage.getItem("accessToken");
  const storeToken = useAuthStore.getState().accessToken;

  const token = localToken || storeToken;
  if (token && token.length > 20) return token;
  return null;
};

const getValidRefreshToken = (): string | null => {
  if (typeof window === "undefined") return null;

  const localToken = localStorage.getItem("refreshToken");
  const storeToken = useAuthStore.getState().refreshToken;

  const token = localToken || storeToken;
  if (token && token.length > 20) return token;
  return null;
};

const syncTokens = (tokens: Tokens) => {
  console.log("[API] 토큰 동기화 시작");
  try {
    localStorage.setItem("accessToken", tokens.accessToken);
    localStorage.setItem("refreshToken", tokens.refreshToken);

    const currentUser = useAuthStore.getState().user;
    if (currentUser) {
      useAuthStore.getState().setAuth(currentUser, tokens);
      console.log("[API] 토큰 동기화 완료 - user:", currentUser.username);
    }
  } catch (error) {
    console.error("[API] 토큰 동기화 실패:", error);
  }
};

const clearAllTokens = (reason: string = "토큰이 유효하지 않습니다") => {
  console.warn("[API] 모든 토큰 정리 시작 - 사유:", reason);

  try {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
    localStorage.removeItem("userId");
    localStorage.removeItem("username");
    localStorage.removeItem("email");
    localStorage.removeItem("role");
    localStorage.removeItem("status");

    localStorage.setItem("sessionExpired", "true");
    localStorage.setItem("sessionExpiredReason", reason);

    useAuthStore.getState().logout();
    console.warn("[API] 모든 토큰 정리 완료");
  } catch (error) {
    console.error("[API] 토큰 정리 실패:", error);
  }
};

/* ------------------------------------------------------------------ */
/* 4. 공개/재발급 엔드포인트 판별                                       */
/* ------------------------------------------------------------------ */
const isPublicEndpoint = (url: string): boolean => {
  const publicPaths = ["/api/auth/", "/api/public/", "/api/dashboard/"];
  return (
    publicPaths.some((p) => url.includes(p)) ||
    (url.includes("/api/policy/") && !url.includes("/favorites") && !url.includes("/recList")) ||
    (url.includes("/api/community/") && url.includes("GET"))
  );
};

const isRefreshEndpoint = (url: string): boolean => url.includes("/api/auth/refresh");

/* ------------------------------------------------------------------ */
/* 5. 요청 인터셉터                                                     */
/* ------------------------------------------------------------------ */
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    if (typeof window !== "undefined") {
      const token = getValidAccessToken();

      if (token && config.headers) {
        config.headers.Authorization = `Bearer ${token}`;
        console.log(`[API] 요청에 토큰 주입: ${config.method?.toUpperCase()} ${config.url}`);
      } else if (config.url && !isPublicEndpoint(config.url) && !isRefreshEndpoint(config.url)) {
        console.warn(`[API] 토큰 없이 보호된 엔드포인트 요청: ${config.method?.toUpperCase()} ${config.url}`);
      }
    }
    return config;
  },
  (error) => {
    console.error("[API] 요청 인터셉터 오류:", error);
    return Promise.reject(error);
  }
);

/* ------------------------------------------------------------------ */
/* 6. 재발급 제어 변수                                                  */
/* ------------------------------------------------------------------ */
let refreshPromise: Promise<string> | null = null;

/* ------------------------------------------------------------------ */
/* 7. 토큰 재발급 함수                                                  */
/* ------------------------------------------------------------------ */
const refreshAccessToken = async (): Promise<string> => {
  const refreshToken = getValidRefreshToken();
  if (!refreshToken) {
    throw new Error("리프레시 토큰이 없습니다");
  }

  console.log("[API] 🔄 토큰 재발급 API 호출 시작");

  try {
    const refreshResponse = await refreshAxios.post<any, AxiosResponse<{ data: RefreshSuccessPayload }>>(
      "/api/auth/refresh",
      {},
      {
        headers: {
          "Refresh-Token": refreshToken,
          "Content-Type": "application/json",
        },
      }
    );

    console.log("[API] ✅ 토큰 재발급 API 응답 수신");

    const responseData = refreshResponse.data?.data;
    if (!responseData) throw new Error("재발급 응답 데이터가 없음");

    const {
      accessToken: newAccessToken,
      refreshToken: newRefreshToken,
      user,
      tokenType,
      expiresIn,
      issuedAt,
      expiresAt,
    } = responseData;

    if (!newAccessToken || !newRefreshToken) throw new Error("새 토큰이 응답에 포함되지 않음");
    if (newAccessToken.length < 20 || newRefreshToken.length < 20) throw new Error("새 토큰 길이가 유효하지 않음");

    const newTokens: Tokens = {
      accessToken: newAccessToken,
      refreshToken: newRefreshToken,
      tokenType: tokenType ?? null,
      expiresIn: expiresIn ?? null,
      issuedAt: issuedAt ?? null,
      expiresAt: expiresAt ?? null,
    };

    syncTokens(newTokens);

    if (user) {
      try {
        localStorage.setItem("userId", user.id?.toString() || "");
        localStorage.setItem("username", user.username || "");
        localStorage.setItem("email", user.email || "");
        localStorage.setItem("role", user.role || "");
        localStorage.setItem("status", user.status || "");
      } catch (userSaveError) {
        console.warn("[API] 사용자 정보 저장 실패:", userSaveError);
      }
    }

    console.log("[API] ✅ 토큰 재발급 및 동기화 완료");
    return newAccessToken;
  } catch (refreshError: any) {
    console.error("[API] ❌ 토큰 재발급 실패:", refreshError);

    let errorReason = "토큰 재발급에 실패했습니다";
    if (refreshError.response?.status === 401) errorReason = "리프레시 토큰이 만료되었습니다";
    else if (refreshError.response?.status === 403) errorReason = "리프레시 토큰이 유효하지 않습니다";
    else if (refreshError.code === "ECONNABORTED") errorReason = "토큰 재발급 요청 시간 초과";
    else if (refreshError.code === "NETWORK_ERROR") errorReason = "네트워크 연결 오류";
    else if (refreshError.message?.includes("새 토큰")) errorReason = "서버에서 유효하지 않은 토큰을 반환했습니다";

    clearAllTokens(errorReason);
    throw refreshError;
  }
};

/* ------------------------------------------------------------------ */
/* 8. 응답 인터셉터                                                     */
/* ------------------------------------------------------------------ */
api.interceptors.response.use(
  (res) => {
    console.log(`[API] 응답 성공: ${res.status} ${res.config.method?.toUpperCase()} ${res.config.url}`);
    return res;
  },
  async (error: AxiosError<ApiErrorResponse>) => {
    const originalRequest = error.config as AxiosRequestConfigRetry;

    if (!error.response) {
      console.error("[API] 네트워크 오류:", error.message);
      return Promise.reject(error);
    }

    const status = error.response.status;
    const errCode = (error.response.data as any)?.code ?? (error.response.data as any)?.errorCode;
    const errMessage = (error.response.data as any)?.message ?? "";

    console.warn(
      `[API] HTTP ${status} 오류: ${originalRequest?.method?.toUpperCase()} ${originalRequest?.url}`,
      { errorCode: errCode, message: errMessage }
    );

    if (isRefreshEndpoint(originalRequest?.url || "")) {
      console.error("[API] 토큰 재발급 엔드포인트 자체 실패");
      clearAllTokens("토큰 재발급 실패");
      if (typeof window !== "undefined" && !window.location.pathname.includes("/login")) {
        setTimeout(() => (window.location.href = "/login"), 200);
      }
      return Promise.reject(error);
    }

    const isTokenError =
      status === 401 &&
      (["TOKEN_EXPIRED", "INVALID_TOKEN", "EXPIRED_ACCESS_TOKEN", "UNAUTHORIZED", "INVALID_REFRESH_TOKEN", "MISSING_TOKEN", "INVALID_TOKEN_FORMAT"].includes(
        errCode ?? ""
      ) ||
        errMessage.toLowerCase().includes("토큰") ||
        errMessage.toLowerCase().includes("인증") ||
        errMessage.toLowerCase().includes("권한") ||
        errMessage.toLowerCase().includes("unauthorized") ||
        errMessage.toLowerCase().includes("expired"));

    if (!isTokenError) {
      console.log("[API] 토큰 오류가 아님, 그대로 전파");
      return Promise.reject(error);
    }

    console.warn("[API] 🔄 토큰 오류 감지, 재발급 시도");

    const retryCount = originalRequest._retryCount || 0;
    if (retryCount >= 2) {
      console.error("[API] ❌ 최대 재시도 횟수 초과");
      clearAllTokens("토큰 재발급 재시도 실패");
      if (typeof window !== "undefined" && !window.location.pathname.includes("/login")) {
        setTimeout(() => (window.location.href = "/login"), 200);
      }
      return Promise.reject(error);
    }

    const hasRefreshToken = getValidRefreshToken();
    if (!hasRefreshToken) {
      console.error("[API] ❌ RefreshToken 없음 - 강제 로그아웃");
      clearAllTokens("리프레시 토큰이 없습니다");
      if (typeof window !== "undefined" && !window.location.pathname.includes("/login")) {
        setTimeout(() => (window.location.href = "/login"), 200);
      }
      return Promise.reject(error);
    }

    originalRequest._retry = true;
    originalRequest._retryCount = retryCount + 1;

    try {
      if (!refreshPromise) {
        refreshPromise = refreshAccessToken().finally(() => {
          refreshPromise = null;
        });
      }

      const newAccessToken = await refreshPromise;

      if (originalRequest.headers) {
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
      }

      console.log("[API] 🔄 원본 요청 재시도");
      return api(originalRequest);
    } catch (refreshError: any) {
      console.error("[API] ❌ 토큰 재발급 실패:", refreshError);
      if (typeof window !== "undefined" && !window.location.pathname.includes("/login")) {
        setTimeout(() => (window.location.href = "/login"), 200);
      }
      return Promise.reject(refreshError);
    }
  }
);

/* ------------------------------------------------------------------ */
/* 9. 만료 임박 사전 재발급 모니터링                                     */
/* ------------------------------------------------------------------ */
const isTokenExpiringSoon = (token: string): boolean => {
  try {
    const payload = JSON.parse(atob(token.split(".")[1]));
    const exp = payload.exp * 1000;
    const now = Date.now();
    const timeUntilExpiry = exp - now;
    return timeUntilExpiry < 5 * 60 * 1000;
  } catch (error) {
    console.warn("[API] 토큰 만료 시간 파싱 실패:", error);
    return false;
  }
};

let tokenCheckInterval: NodeJS.Timeout | null = null;

const startTokenMonitoring = () => {
  if (typeof window === "undefined") return;

  if (tokenCheckInterval) clearInterval(tokenCheckInterval);

  tokenCheckInterval = setInterval(async () => {
    const token = getValidAccessToken();
    const refreshToken = getValidRefreshToken();

    if (token && refreshToken && isTokenExpiringSoon(token)) {
      console.log("[API] 🔄 토큰 만료 임박, 사전 재발급 시도");
      try {
        if (!refreshPromise) {
          refreshPromise = refreshAccessToken().finally(() => {
            refreshPromise = null;
          });
        }
        await refreshPromise;
        console.log("[API] ✅ 사전 토큰 재발급 완료");
      } catch (error) {
        console.error("[API] ❌ 사전 토큰 재발급 실패:", error);
        // 재발급 실패 시 여기서 바로 강제 로그아웃하지는 않음 (Providers가 처리)
      }
    }
  }, 60000);
};

const stopTokenMonitoring = () => {
  if (tokenCheckInterval) {
    clearInterval(tokenCheckInterval);
    tokenCheckInterval = null;
  }
};

// 페이지 로드 시 토큰 모니터링 시작
if (typeof window !== "undefined") {
  startTokenMonitoring();
  window.addEventListener("beforeunload", stopTokenMonitoring);
}

export { startTokenMonitoring, stopTokenMonitoring };
export default api;

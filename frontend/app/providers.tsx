"use client";

import { startTokenMonitoring, stopTokenMonitoring } from "@/libs/api/axios";
import type { UserDto } from "@/store/authStore";
import { useAuthStore } from "@/store/authStore";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactNode, useEffect, useRef, useState } from "react";

/* ------------------------------------------ */
/* 1) 토큰 유틸: base64url 안전 디코딩        */
/* ------------------------------------------ */
const decodeToken = (token: string) => {
  try {
    if (!token || token.split(".").length < 2) return null;
    const payloadPart = token.split(".")[1];
    const base64 = payloadPart.replace(/-/g, "+").replace(/_/g, "/");
    const padded = base64 + "=".repeat((4 - (base64.length % 4)) % 4);
    const json = atob(padded);
    return JSON.parse(json);
  } catch {
    return null;
  }
};

const isTokenExpired = (token: string): boolean => {
  const payload = decodeToken(token);
  if (!payload || typeof payload.exp !== "number") return false;
  const currentTime = Math.floor(Date.now() / 1000);
  return payload.exp < currentTime;
};

const isTokenExpiringSoon = (token: string, minutesAhead: number = 5): boolean => {
  const payload = decodeToken(token);
  if (!payload || typeof payload.exp !== "number") return false;
  const currentTime = Math.floor(Date.now() / 1000);
  const timeUntilExpiry = payload.exp - currentTime;
  return timeUntilExpiry < minutesAhead * 60;
};

/* ------------------------------------------ */
/* 2) 세션 플래그 확인(관찰만)                */
/* ------------------------------------------ */
const checkSessionExpired = () => {
  if (typeof window === "undefined") return null;

  const expired = localStorage.getItem("sessionExpired");
  const reason = localStorage.getItem("sessionExpiredReason");

  if (expired === "true") {
    localStorage.removeItem("sessionExpired");
    localStorage.removeItem("sessionExpiredReason");
    return { expired: true, reason: reason || "세션이 만료되었습니다" };
  }
  return null;
};

/* ------------------------------------------ */
/* 3) 토큰 유효성(리프레시 opaque 고려)       */
/* ------------------------------------------ */
const hasValidTokens = (): {
  hasTokens: boolean;
  tokensValid: boolean;
  accessTokenExpired: boolean;
} => {
  if (typeof window === "undefined")
    return { hasTokens: false, tokensValid: false, accessTokenExpired: false };

  const accessToken = localStorage.getItem("accessToken") || "";
  const refreshToken = localStorage.getItem("refreshToken") || "";

  const hasTokens = accessToken.length > 20 && refreshToken.length > 20;
  if (!hasTokens) return { hasTokens: false, tokensValid: false, accessTokenExpired: false };

  const accessTokenExpired = isTokenExpired(accessToken);

  // 리프레시는 exp 없으면 유효로 간주
  let refreshTokenExpired = false;
  const rtPayload = decodeToken(refreshToken);
  if (rtPayload && typeof rtPayload.exp === "number") {
    refreshTokenExpired = isTokenExpired(refreshToken);
  }

  const tokensValid = !refreshTokenExpired;
  return { hasTokens, tokensValid, accessTokenExpired };
};

/* ------------------------------------------ */
/* 4) 사용자 스토리지 동기화                  */
/* ------------------------------------------ */
const createUserFromStorage = (): UserDto | null => {
  if (typeof window === "undefined") return null;
  try {
    const userId = localStorage.getItem("userId");
    const username = localStorage.getItem("username");
    const email = localStorage.getItem("email") || "";
    const role = (localStorage.getItem("role") as any) || "USER";
    const status = (localStorage.getItem("status") as any) || "ACTIVE";

    if (!userId || !username || isNaN(parseInt(userId))) return null;

    return { id: parseInt(userId), username, email, role, status };
  } catch {
    return null;
  }
};

const saveUserToStorage = (user: UserDto) => {
  if (typeof window === "undefined") return;
  try {
    localStorage.setItem("userId", user.id.toString());
    localStorage.setItem("username", user.username);
    localStorage.setItem("email", user.email);
    localStorage.setItem("role", user.role);
    localStorage.setItem("status", user.status);
  } catch {}
};

const syncAuthState = () => {
  if (typeof window === "undefined") return;

  try {
    const authStore = useAuthStore.getState();
    const localAccessToken = localStorage.getItem("accessToken");
    const localRefreshToken = localStorage.getItem("refreshToken");

    if (localAccessToken && localRefreshToken && !authStore.user) {
      const userData = createUserFromStorage();
      if (userData) {
        const tokens = {
          accessToken: localAccessToken,
          refreshToken: localRefreshToken,
          tokenType: null,
          expiresIn: null,
          issuedAt: null,
          expiresAt: null,
        };
        try {
          authStore.setAuth(userData, tokens);
        } catch {
          // 관찰 모드: 강제 정리 안 함
        }
      }
    } else if (authStore.user && authStore.accessToken && (!localAccessToken || !localRefreshToken)) {
      localStorage.setItem("accessToken", authStore.accessToken);
      localStorage.setItem("refreshToken", authStore.refreshToken || "");
      saveUserToStorage(authStore.user);
    }
  } catch {}
};

const clearAllStorageData = () => {
  try {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
    localStorage.removeItem("userId");
    localStorage.removeItem("username");
    localStorage.removeItem("email");
    localStorage.removeItem("role");
    localStorage.removeItem("status");
  } catch {}
};

/* ------------------------------------------ */
/* 4-1) 강제 로그아웃 헬퍼(신규)              */
/* ------------------------------------------ */
const hardLogout = (reason: string) => {
  try {
    // 세션 만료 토스트를 띄우기 위한 플래그 설정
    localStorage.setItem("sessionExpired", "true");
    localStorage.setItem("sessionExpiredReason", reason);

    // 모든 저장소 정리
    clearAllStorageData();

    // Zustand 로그아웃
    useAuthStore.getState().logout();
  } catch {}

  // 로그인 페이지로 이동 (로그인 페이지가 아닐 때만)
  if (typeof window !== "undefined") {
    const path = window.location.pathname;
    if (!path.includes("/login")) {
      setTimeout(() => {
        window.location.href = "/auth/login";
      }, 200);
    }
  }
};

/* ------------------------------------------ */
/* 4-2) 탭 간 로그아웃 동기화 (신규 추가)      */
/* ------------------------------------------ */
const broadcastLogout = (reason: string) => {
  try {
    // 로그아웃 신호를 다른 탭에 전파
    localStorage.setItem("logoutSignal", Date.now().toString());
    localStorage.setItem("logoutReason", reason);
  } catch {}
};

const handleLogoutSignal = () => {
  try {
    const signal = localStorage.getItem("logoutSignal");
    const reason = localStorage.getItem("logoutReason") || "다른 탭에서 로그아웃되었습니다";
    
    if (signal) {
      // 신호 제거
      localStorage.removeItem("logoutSignal");
      localStorage.removeItem("logoutReason");
      
      // 즉시 로그아웃 처리
      hardLogout(reason);
    }
  } catch {}
};

/* ------------------------------------------ */
/* 5) 조용한 재발급 → 만료시 즉시 로그아웃     */
/* ------------------------------------------ */
/**
 * 리프레시 토큰으로 액세스 토큰 재발급을 시도한다.
 * 성공 시 store/로컬스토리지 갱신. 실패 유형이 401/403이면 즉시 강제 로그아웃.
 */
const attemptRefreshWithStoredTokens = async (): Promise<boolean> => {
  if (typeof window === "undefined") return false;

  const refreshToken = localStorage.getItem("refreshToken") || "";
  if (!refreshToken || refreshToken.length <= 20) return false;

  // 사용자 정보는 그대로 유지
  const currentUser = useAuthStore.getState().user || createUserFromStorage();
  const setAuth = useAuthStore.getState().setAuth;

  const applySuccess = (data: any) => {
    const newAccess = data?.accessToken || data?.access_token;
    const newRefresh = data?.refreshToken || data?.refresh_token || refreshToken;
    if (!newAccess) return false;

    localStorage.setItem("accessToken", newAccess);
    if (newRefresh) localStorage.setItem("refreshToken", newRefresh);

    if (currentUser && setAuth) {
      setAuth(currentUser, {
        accessToken: newAccess,
        refreshToken: newRefresh,
        tokenType: null,
        expiresIn: null,
        issuedAt: null,
        expiresAt: null,
      });
      saveUserToStorage(currentUser);
    }
    return true;
  };

  // 1) Refresh-Token 헤더 방식 (백엔드 계약과 일치)
  try {
    const res = await fetch("/api/auth/refresh", {
      method: "POST",
      headers: {
        "Refresh-Token": refreshToken,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({}),
      credentials: "include",
    });

    if (res.ok) {
      const data = await res.json().catch(() => ({}));
      if (applySuccess(data?.data || data)) return true;
    } else if (res.status === 401 || res.status === 403) {
      const reason = res.status === 401 ? "리프레시 토큰이 만료되었습니다" : "리프레시 토큰이 유효하지 않습니다";
      broadcastLogout(reason); // 다른 탭에도 로그아웃 신호 전파
      hardLogout(reason);
      return false;
    }
  } catch {}

  // 2) JSON body 방식 (호환)
  try {
    const res = await fetch("/api/auth/refresh", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
      credentials: "include",
    });

    if (res.ok) {
      const data = await res.json().catch(() => ({}));
      if (applySuccess(data?.data || data)) return true;
    } else if (res.status === 401 || res.status === 403) {
      const reason = res.status === 401 ? "리프레시 토큰이 만료되었습니다" : "리프레시 토큰이 유효하지 않습니다";
      broadcastLogout(reason); // 다른 탭에도 로그아웃 신호 전파
      hardLogout(reason);
      return false;
    }
  } catch {
    // 네트워크 오류 등은 여기서 강제 로그아웃하지 않음 (인터셉터/다음 요청에서 처리)
  }

  return false;
};

/* ------------------------------------------ */
/* 6) 토스트                                  */
/* ------------------------------------------ */
function SessionExpiredToast({
  message,
  onClose,
}: {
  message: string;
  onClose: () => void;
}) {
  useEffect(() => {
    const timer = setTimeout(onClose, 5000);
    return () => clearTimeout(timer);
  }, [onClose]);

  return (
    <div
      style={{
        position: "fixed",
        top: "20px",
        right: "20px",
        backgroundColor: "#ff4444",
        color: "white",
        padding: "16px 20px",
        borderRadius: "8px",
        boxShadow: "0 4px 12px rgba(0,0,0,0.15)",
        zIndex: 9999,
        maxWidth: "400px",
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
        gap: "12px",
        fontSize: "14px",
        fontWeight: 500,
      }}
    >
      <span>{message}</span>
      <button
        onClick={onClose}
        style={{
          background: "none",
          border: "none",
          color: "white",
          fontSize: "18px",
          cursor: "pointer",
          padding: 0,
          lineHeight: 1,
          width: "20px",
          height: "20px",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
        }}
        aria-label="닫기"
      >
        ×
      </button>
    </div>
  );
}

export default function Providers({ children }: { children: ReactNode }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 1000 * 60 * 5,
            retry: (failureCount, error: any) => {
              if (error?.response?.status === 401 || error?.response?.status === 403) return false;
              return failureCount < 3;
            },
            retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30000),
          },
          mutations: {
            retry: (failureCount, error: any) => {
              if (error?.response?.status === 401 || error?.response?.status === 403) return false;
              return failureCount < 2;
            },
          },
        },
      })
  );

  const [sessionExpiredMessage, setSessionExpiredMessage] = useState<string | null>(null);

  // 재발급 스로틀링(중복 호출 방지)
  const isRefreshingRef = useRef(false);
  const lastRefreshAtRef = useRef<number>(0);
  const cooldownMs = 60_000; // 60초 쿨다운

  // 만료/임박시 선제 재발급 트리거
  const proactiveRefreshIfNeeded = async () => {
    try {
      const { hasTokens, tokensValid } = hasValidTokens();
      if (!hasTokens || !tokensValid) {
        if (hasTokens && !tokensValid) {
          // refresh 토큰이 만료된 경우 즉시 로그아웃
          broadcastLogout("리프레시 토큰이 만료되었습니다"); // 다른 탭에도 전파
          hardLogout("리프레시 토큰이 만료되었습니다");
        }
        return;
      }

      const accessToken = localStorage.getItem("accessToken") || "";
      const needRefresh =
        !accessToken ||
        isTokenExpired(accessToken) ||
        isTokenExpiringSoon(accessToken, 1); // 만료 1분 전 선제

      if (!needRefresh) return;

      const now = Date.now();
      if (isRefreshingRef.current) return;
      if (now - lastRefreshAtRef.current < cooldownMs) return;

      isRefreshingRef.current = true;
      const ok = await attemptRefreshWithStoredTokens();
      lastRefreshAtRef.current = Date.now();
      isRefreshingRef.current = false;

      // 실패하고 refresh 자체가 만료된 케이스는 attempt 함수 내 hardLogout에서 처리됨
      return ok;
    } catch {
      isRefreshingRef.current = false;
    }
  };

  useEffect(() => {
    try {
      syncAuthState();
    } catch {}

    try {
      const sessionStatus = checkSessionExpired();
      if (sessionStatus?.expired) {
        setSessionExpiredMessage(`${sessionStatus.reason}`);
      }
    } catch {}

    try {
      startTokenMonitoring();
    } catch {}

    const sessionCheckInterval = setInterval(() => {
      try {
        const sessionStatus = checkSessionExpired();
        if (sessionStatus?.expired) {
          setSessionExpiredMessage(`${sessionStatus.reason}`);
        }
      } catch {}
    }, 10000);

    const tokenCheckInterval = setInterval(() => {
      try {
        const currentPath = window.location.pathname;
        if (
          currentPath.includes("/login") ||
          currentPath.includes("/signup") ||
          currentPath.includes("/auth/")
        ) {
          return;
        }

        const { hasTokens, tokensValid } = hasValidTokens();
        const authUser = useAuthStore.getState().user;

        if (hasTokens && tokensValid && !authUser) {
          syncAuthState();
        }

        proactiveRefreshIfNeeded();
      } catch {}
    }, 30000);

    const handleUnhandledRejection = (event: PromiseRejectionEvent) => {
      if (event.reason?.response?.status === 401) {
        event.preventDefault();
      }
    };

    const handleStorage = (e: StorageEvent) => {
      // 로그아웃 신호 감지
      if (e.key === "logoutSignal" && e.newValue) {
        handleLogoutSignal();
        return;
      }

      // 기존 토큰 동기화 로직
      if (e.key === "accessToken" || e.key === "refreshToken" || e.key === "userId") {
        // 토큰이 삭제된 경우 (다른 탭에서 로그아웃)
        if ((e.key === "accessToken" || e.key === "refreshToken") && !e.newValue && e.oldValue) {
          // 현재 탭도 로그아웃 처리
          useAuthStore.getState().logout();
          clearAllStorageData();
          return;
        }
        syncAuthState();
      }
    };

    const handleVisibilityChange = () => {
      if (document.visibilityState === "visible") {
        setTimeout(() => {
          try {
            // 로그아웃 신호 확인
            handleLogoutSignal();
            
            const sessionStatus = checkSessionExpired();
            if (sessionStatus?.expired) {
              setSessionExpiredMessage(sessionStatus.reason);
            } else {
              syncAuthState();
              proactiveRefreshIfNeeded();
            }
          } catch {}
        }, 1000);
      }
    };

    const handleBeforeUnload = () => {
      try {
        stopTokenMonitoring();
      } catch {}
      localStorage.removeItem("sessionExpired");
      localStorage.removeItem("sessionExpiredReason");
    };

    const handleOnline = () => {
      setTimeout(() => {
        syncAuthState();
        proactiveRefreshIfNeeded();
      }, 2000);
    };

    const handleOffline = () => {};

    window.addEventListener("unhandledrejection", handleUnhandledRejection);
    window.addEventListener("beforeunload", handleBeforeUnload);
    window.addEventListener("online", handleOnline);
    window.addEventListener("offline", handleOffline);
    window.addEventListener("storage", handleStorage);
    document.addEventListener("visibilitychange", handleVisibilityChange);

    setTimeout(() => {
      proactiveRefreshIfNeeded();
    }, 1500);

    return () => {
      clearInterval(sessionCheckInterval);
      clearInterval(tokenCheckInterval);
      try {
        stopTokenMonitoring();
      } catch {}
      window.removeEventListener("unhandledrejection", handleUnhandledRejection);
      window.removeEventListener("beforeunload", handleBeforeUnload);
      window.removeEventListener("online", handleOnline);
      window.removeEventListener("offline", handleOffline);
      window.removeEventListener("storage", handleStorage);
      document.removeEventListener("visibilitychange", handleVisibilityChange);
    };
  }, []);

  // AuthStore → localStorage 동기화
  useEffect(() => {
    const unsubscribe = useAuthStore.subscribe((state) => {
      if (state.user && state.accessToken) {
        saveUserToStorage(state.user);
        localStorage.setItem("accessToken", state.accessToken);
        localStorage.setItem("refreshToken", state.refreshToken || "");
      } else if (!state.user && !state.accessToken) {
        clearAllStorageData();
      }
    });
    return unsubscribe;
  }, []);

  // 초기 관찰 + 필요 시 재발급
  useEffect(() => {
    const initialCheck = setTimeout(() => {
      try {
        const { hasTokens, tokensValid } = hasValidTokens();
        const authUser = useAuthStore.getState().user;

        if (hasTokens && tokensValid && !authUser) {
          syncAuthState();
        }
        proactiveRefreshIfNeeded();
      } catch {}
    }, 1000);

    return () => clearTimeout(initialCheck);
  }, []);

  return (
    <QueryClientProvider client={queryClient}>
      {children}
      {sessionExpiredMessage && (
        <SessionExpiredToast
          message={sessionExpiredMessage}
          onClose={() => setSessionExpiredMessage(null)}
        />
      )}
    </QueryClientProvider>
  );
}
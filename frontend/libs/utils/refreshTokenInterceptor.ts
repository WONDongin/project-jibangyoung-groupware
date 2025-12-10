// // utils/refreshTokenInterceptor.ts
// // 기존 api.ts를 건드리지 않고 리프레시 토큰 기능만 추가하는 확장 모듈

// import { useAuthStore } from "@/store/authStore";
// import axios, { AxiosError, AxiosResponse, InternalAxiosRequestConfig } from "axios";
// import { api } from "./api"; // 기존 api 인스턴스 import

// /* ------------------------------------------------------------------ */
// /* 1. 타입 선언                                                       */
// /* ------------------------------------------------------------------ */
// export type Tokens = {
//   accessToken: string;
//   refreshToken: string;
//   tokenType: string | null;
//   expiresIn: number | null;
//   issuedAt: string | null;
//   expiresAt: string | null;
// };

// interface AxiosRequestConfigRetry extends InternalAxiosRequestConfig {
//   _retry?: boolean;
// }

// interface ApiErrorResponse {
//   code?: string;
//   errorCode?: string;
//   message?: string;
//   [key: string]: any;
// }

// interface RefreshSuccessPayload {
//   accessToken: string;
//   refreshToken: string;
//   user: any;
//   tokenType?: string | null;
//   expiresIn?: number | null;
//   issuedAt?: string | null;
//   expiresAt?: string | null;
// }

// /* ------------------------------------------------------------------ */
// /* 2. 토큰 관리 유틸리티                                              */
// /* ------------------------------------------------------------------ */
// const getStoredTokens = () => {
//   if (typeof window === "undefined") return null;
  
//   const accessToken = localStorage.getItem("accessToken");
//   const refreshToken = localStorage.getItem("refreshToken");
  
//   return accessToken && refreshToken ? { accessToken, refreshToken } : null;
// };

// const clearStoredTokens = () => {
//   if (typeof window === "undefined") return;
  
//   localStorage.removeItem("accessToken");
//   localStorage.removeItem("refreshToken");
//   localStorage.removeItem("tokenType");
//   localStorage.removeItem("expiresIn");
//   localStorage.removeItem("issuedAt");
//   localStorage.removeItem("expiresAt");
// };

// const storeTokens = (tokens: Tokens) => {
//   if (typeof window === "undefined") return;
  
//   localStorage.setItem("accessToken", tokens.accessToken);
//   localStorage.setItem("refreshToken", tokens.refreshToken);
  
//   if (tokens.tokenType) localStorage.setItem("tokenType", tokens.tokenType);
//   if (tokens.expiresIn) localStorage.setItem("expiresIn", tokens.expiresIn.toString());
//   if (tokens.issuedAt) localStorage.setItem("issuedAt", tokens.issuedAt);
//   if (tokens.expiresAt) localStorage.setItem("expiresAt", tokens.expiresAt);
// };

// /* ------------------------------------------------------------------ */
// /* 3. 재발급 제어 변수                                               */
// /* ------------------------------------------------------------------ */
// let isRefreshing = false;
// let subscribers: ((token: string | null) => void)[] = [];

// const subscribe = (cb: (token: string | null) => void) => {
//   subscribers.push(cb);
// };

// const notify = (token: string | null) => {
//   subscribers.forEach((cb) => cb(token));
//   subscribers = [];
// };

// /* ------------------------------------------------------------------ */
// /* 4. 강제 로그아웃 처리                                             */
// /* ------------------------------------------------------------------ */
// const handleForceLogout = (reason: string = "인증 오류") => {
//   console.warn(`[REFRESH] 강제 로그아웃: ${reason}`);
  
//   // 토큰 정리
//   clearStoredTokens();
  
//   // AuthStore 상태 정리
//   const authStore = useAuthStore.getState();
//   if (authStore.logout) {
//     authStore.logout();
//   }
  
//   // 세션 만료 플래그 설정
//   localStorage.setItem("sessionExpired", "true");
//   localStorage.setItem("sessionExpiredReason", reason);
  
//   // 필요시 로그인 페이지로 리다이렉트
//   // window.location.href = "/login";
// };

// /* ------------------------------------------------------------------ */
// /* 5. 토큰 재발급 함수                                               */
// /* ------------------------------------------------------------------ */
// const refreshTokens = async (refreshToken: string): Promise<string> => {
//   try {
//     console.log("[REFRESH] 토큰 재발급 시도...");
    
//     const refreshResponse = await axios.post(
//       "http://localhost:8080/api/auth/refresh",
//       {},
//       {
//         headers: {
//           "Refresh-Token": refreshToken,
//           "Content-Type": "application/json",
//         },
//         withCredentials: true,
//         timeout: 10000,
//       }
//     );

//     // 응답 구조 안전하게 처리
//     let responseData: RefreshSuccessPayload;
    
//     if (refreshResponse.data.success && refreshResponse.data.data) {
//       // ApiResponse 형태: { success: true, data: RefreshSuccessPayload }
//       responseData = refreshResponse.data.data;
//     } else if (refreshResponse.data.accessToken) {
//       // 직접 형태: RefreshSuccessPayload
//       responseData = refreshResponse.data;
//     } else {
//       throw new Error("토큰 재발급 응답 형식 오류");
//     }

//     const {
//       accessToken: newAccessToken,
//       refreshToken: newRefreshToken,
//       user,
//       tokenType,
//       expiresIn,
//       issuedAt,
//       expiresAt,
//     } = responseData;

//     if (!newAccessToken || !newRefreshToken) {
//       throw new Error("새 토큰이 응답에 포함되지 않음");
//     }

//     // 새 토큰들 저장
//     const newTokens: Tokens = {
//       accessToken: newAccessToken,
//       refreshToken: newRefreshToken,
//       tokenType: tokenType ?? null,
//       expiresIn: expiresIn ?? null,
//       issuedAt: issuedAt ?? null,
//       expiresAt: expiresAt ?? null,
//     };
    
//     storeTokens(newTokens);

//     // AuthStore 업데이트
//     const authStore = useAuthStore.getState();
//     if (authStore.setAuth) {
//       authStore.setAuth(user, newTokens);
//     }

//     console.log("[REFRESH] 토큰 재발급 성공");
//     return newAccessToken;
    
//   } catch (error: any) {
//     console.error("[REFRESH] 토큰 재발급 실패:", error.response?.data || error.message);
    
//     let reason = "토큰 재발급 실패";
//     if (error.response?.status === 401) {
//       reason = "리프레시 토큰 만료";
//     } else if (error.response?.status === 403) {
//       reason = "리프레시 토큰 무효";
//     }
    
//     throw new Error(reason);
//   }
// };

// /* ------------------------------------------------------------------ */
// /* 6. 리프레시 토큰 응답 인터셉터 (기존 인터셉터에 추가)               */
// /* ------------------------------------------------------------------ */
// let responseInterceptorId: number | null = null;

// const setupRefreshTokenInterceptor = () => {
//   // 기존 응답 인터셉터가 있다면 제거
//   if (responseInterceptorId !== null) {
//     api.interceptors.response.eject(responseInterceptorId);
//   }
  
//   // 새로운 응답 인터셉터 추가
//   responseInterceptorId = api.interceptors.response.use(
//     (response) => response,
//     async (error: AxiosError<ApiErrorResponse>) => {
//       // 기존 401 처리 로직 유지
//       const originalBasicHandling = () => {
//         if (error.response?.status === 401) {
//           // 기존 주석 처리된 리다이렉트 로직은 그대로 유지
//           // window.location.href = "/login";
//         }
//         return Promise.reject(error);
//       };

//       // 네트워크 오류 등은 기존 방식대로 처리
//       if (!error.response) {
//         console.error("[API] 네트워크 오류:", error.message);
//         return originalBasicHandling();
//       }

//       const originalRequest = error.config as AxiosRequestConfigRetry;
//       const status = error.response.status;
//       const errCode = error.response.data?.code ?? error.response.data?.errorCode;
//       const errMessage = error.response.data?.message || "";

//       // 토큰 관련 오류 감지
//       const isTokenError = status === 401 && (
//         ["TOKEN_EXPIRED", "INVALID_TOKEN", "EXPIRED_ACCESS_TOKEN", "UNAUTHORIZED", "JWT_EXPIRED"].includes(errCode ?? "") ||
//         errMessage.includes("토큰") ||
//         errMessage.includes("인증") ||
//         errMessage.includes("token") ||
//         errMessage.includes("expired") ||
//         errMessage.includes("unauthorized")
//       );

//       // 토큰 오류가 아니면 기존 처리 방식
//       if (!isTokenError) {
//         return originalBasicHandling();
//       }

//       console.warn(`[REFRESH] 토큰 오류 감지 - Status: ${status}, Code: ${errCode}`);

//       // refresh 엔드포인트 자체 오류는 즉시 로그아웃
//       if (originalRequest.url?.includes("/api/auth/refresh")) {
//         handleForceLogout("리프레시 토큰 만료 또는 무효");
//         return Promise.reject(error);
//       }

//       // 저장된 토큰 확인
//       const tokens = getStoredTokens();
//       if (!tokens?.refreshToken) {
//         handleForceLogout("리프레시 토큰 없음");
//         return Promise.reject(error);
//       }

//       // 재시도 방지
//       if (originalRequest._retry) {
//         console.warn("[REFRESH] 토큰 재발급 재시도 실패");
//         handleForceLogout("토큰 재발급 재시도 실패");
//         return Promise.reject(error);
//       }
//       originalRequest._retry = true;

//       // 재발급 중이면 대기열에 추가
//       if (isRefreshing) {
//         return new Promise<AxiosResponse>((resolve, reject) => {
//           subscribe(async (newAccessToken) => {
//             if (!newAccessToken) {
//               reject(error);
//               return;
//             }
            
//             try {
//               // 새 토큰으로 헤더 업데이트
//               if (originalRequest.headers) {
//                 originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
//               }
//               const retryResponse = await api(originalRequest);
//               resolve(retryResponse);
//             } catch (retryError) {
//               reject(retryError);
//             }
//           });
//         });
//       }

//       // 토큰 재발급 시작
//       isRefreshing = true;
      
//       try {
//         const newAccessToken = await refreshTokens(tokens.refreshToken);
        
//         // 대기 중인 요청들에 새 토큰 전달
//         notify(newAccessToken);
        
//         // 원본 요청에 새 토큰 적용하여 재시도
//         if (originalRequest.headers) {
//           originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
//         }
        
//         return api(originalRequest);
        
//       } catch (refreshError: any) {
//         // 대기 중인 요청들 실패 처리
//         notify(null);
        
//         handleForceLogout(refreshError.message || "토큰 재발급 실패");
//         return Promise.reject(refreshError);
        
//       } finally {
//         isRefreshing = false;
//       }
//     }
//   );
// };

// /* ------------------------------------------------------------------ */
// /* 7. 추가 유틸리티 함수들                                           */
// /* ------------------------------------------------------------------ */

// /**
//  * 수동 토큰 재발급
//  */
// export const manualRefreshToken = async (): Promise<boolean> => {
//   const tokens = getStoredTokens();
//   if (!tokens?.refreshToken) {
//     console.warn("[REFRESH] 수동 토큰 재발급 실패: 리프레시 토큰 없음");
//     return false;
//   }
  
//   try {
//     await refreshTokens(tokens.refreshToken);
//     return true;
//   } catch (error) {
//     console.error("[REFRESH] 수동 토큰 재발급 실패:", error);
//     return false;
//   }
// };

// /**
//  * 토큰 상태 확인
//  */
// export const getTokenStatus = () => {
//   const tokens = getStoredTokens();
//   return {
//     hasAccessToken: !!tokens?.accessToken,
//     hasRefreshToken: !!tokens?.refreshToken,
//     isRefreshing,
//     waitingRequestsCount: subscribers.length,
//   };
// };

// /**
//  * 세션 만료 상태 확인
//  */
// export const checkSessionExpired = () => {
//   if (typeof window === "undefined") return null;
  
//   const expired = localStorage.getItem("sessionExpired");
//   const reason = localStorage.getItem("sessionExpiredReason");
  
//   if (expired === "true") {
//     localStorage.removeItem("sessionExpired");
//     localStorage.removeItem("sessionExpiredReason");
    
//     return {
//       expired: true,
//       reason: reason || "세션이 만료되었습니다"
//     };
//   }
  
//   return null;
// };

// /**
//  * 리프레시 토큰 인터셉터 초기화 함수
//  * 앱 시작 시 한 번만 호출하면 됩니다
//  */
// export const initializeRefreshTokenInterceptor = () => {
//   setupRefreshTokenInterceptor();
//   console.log("[REFRESH] 리프레시 토큰 인터셉터 초기화 완료");
// };

// // 기본적으로 자동 초기화 (원하지 않으면 주석 처리)
// if (typeof window !== "undefined") {
//   initializeRefreshTokenInterceptor();
// }
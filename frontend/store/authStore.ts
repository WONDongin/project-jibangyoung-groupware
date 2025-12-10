import type {
  LoginResponse,
  UserDto,
  UserRole,
  UserStatus,
} from "@/libs/api/auth/auth.api";
import type { Tokens } from "@/libs/api/axios";
import { create } from "zustand";
import { persist } from "zustand/middleware";

export type { LoginResponse, UserDto, UserRole, UserStatus };

export interface AuthState {
  user: UserDto | null;
  accessToken: string | null;
  refreshToken: string | null;
  tokenType: string | null;
  expiresIn: number | null;
  issuedAt: string | null;
  expiresAt: string | null;
  
  // ✅ tokens 속성 추가 (API 인터셉터 호환성을 위해)
  tokens: Tokens | null;
  
  setUser: (user: UserDto | null) => void;
  setAuth: (user: UserDto, tokens: Tokens) => void;
  setAuthObj: (data: { user: UserDto; accessToken: string; refreshToken: string }) => void;
  logout: () => void;
}

const storage = typeof window !== "undefined" ? window.localStorage : undefined;

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      accessToken: null,
      refreshToken: null,
      tokenType: null,
      expiresIn: null,
      issuedAt: null,
      expiresAt: null,
      tokens: null, // ✅ 초기값 추가
      
      setUser: (user) => set({ user }),
      
      setAuth: (user, tokens) => {
        set({
          user,
          accessToken: tokens.accessToken,
          refreshToken: tokens.refreshToken,
          tokenType: tokens.tokenType ?? null,
          expiresIn: tokens.expiresIn ?? null,
          issuedAt: tokens.issuedAt ?? null,
          expiresAt: tokens.expiresAt ?? null,
          tokens, // ✅ tokens 객체도 함께 저장
        });
        
        // localStorage에도 저장
        if (storage) {
          storage.setItem("accessToken", tokens.accessToken);
          storage.setItem("refreshToken", tokens.refreshToken);
        }
      },
      
      setAuthObj: ({ user, accessToken, refreshToken }) => {
        // ✅ tokens 객체 생성
        const tokens: Tokens = {
          accessToken,
          refreshToken,
          tokenType: null,
          expiresIn: null,
          issuedAt: null,
          expiresAt: null,
        };
        
        set({
          user,
          accessToken,
          refreshToken,
          tokenType: null,
          expiresIn: null,
          issuedAt: null,
          expiresAt: null,
          tokens, // ✅ tokens 객체도 저장
        });
        
        if (storage) {
          storage.setItem("accessToken", accessToken);
          storage.setItem("refreshToken", refreshToken);
        }
      },
      
      logout: () => {
        set({
          user: null,
          accessToken: null,
          refreshToken: null,
          tokenType: null,
          expiresIn: null,
          issuedAt: null,
          expiresAt: null,
          tokens: null, // ✅ tokens도 null로 설정
        });
        
        if (storage) {
          storage.removeItem("accessToken");
          storage.removeItem("refreshToken");
          storage.removeItem("userId");
        }
      },
    }),
    {
      name: "auth-store-v3", // ✅ 버전 업데이트
      partialize: (state) => ({
        user: state.user,
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        tokenType: state.tokenType,
        expiresIn: state.expiresIn,
        issuedAt: state.issuedAt,
        expiresAt: state.expiresAt,
        tokens: state.tokens, // ✅ tokens도 persist에 포함
      }),
      version: 3, // ✅ 버전 증가
    }
  )
);
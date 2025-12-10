// components/layout/Header.tsx
"use client";

import { logout as serverLogout } from "@/libs/api/auth/auth.api";
import { clearAuthStorage } from "@/libs/api/auth/storage"; // ✅ 추가: 스토리지 클리너
import { stopTokenMonitoring } from "@/libs/api/axios";
import { syncBookmarkedPolicies } from "@/libs/api/policy/sync";
import { useAuthStore } from "@/store/authStore";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";

const dropdownItems = [
  { label: "로그인", path: "/auth/login" },
  { label: "아이디 찾기", path: "/auth/find-id" },
  { label: "비밀번호 찾기", path: "/auth/find-password" },
  { label: "회원가입", path: "/auth/register" },
  { label: "대시보드", path: "/dashboard" },
  { label: "설문 응답", path: "/survey" },
  { label: "추천 결과", path: "/recommendation" },
  { label: "정책 리스트", path: "/policy/totalPolicies" },
  { label: "찜한 정책", path: "/policy/rec_Policies" },
  { label: "통합 검색", path: "/search" },
  { label: "커뮤니티 홈", path: "/community" },
  { label: "멘토 신청", path: "/mentor/info" },
  { label: "공지 대시보드", path: "/notice" },
  { label: "공지 상세", path: "/notice/detail" },
  { label: "신고 내역", path: "/mypage/reports" },
  { label: "관리자 페이지", path: "/admin" },
  { label: "멘토 관리자 페이지", path: "/mentor" },
];

/* ------------------------------------------ */
/* 탭 간 로그아웃 동기화 헬퍼 함수 (신규 추가) */
/* ------------------------------------------ */
const broadcastLogout = (reason: string) => {
  try {
    // 로그아웃 신호를 다른 탭에 전파
    localStorage.setItem("logoutSignal", Date.now().toString());
    localStorage.setItem("logoutReason", reason);
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
    localStorage.removeItem("sessionExpired");
    localStorage.removeItem("sessionExpiredReason");
    localStorage.removeItem("bookmarkedPolicyIds");
  } catch {}
};

export default function Header() {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const { user, accessToken, refreshToken, logout } = useAuthStore();
  const router = useRouter();

  const isLoggedIn = !!accessToken && !!user;

  const handleLogout = async () => {
    // 서버 호출에 필요한 값 먼저 확보
    const at =
      accessToken ||
      (typeof window !== "undefined" ? localStorage.getItem("accessToken") || "" : "");
    const rt =
      refreshToken ||
      (typeof window !== "undefined" ? localStorage.getItem("refreshToken") || "" : "");

    const userIdStr = typeof window !== "undefined" ? localStorage.getItem("userId") : null;
    const bookmarkedStr =
      typeof window !== "undefined" ? localStorage.getItem("bookmarkedPolicyIds") : null;

    try {
      // 재발급 레이스 차단
      stopTokenMonitoring();

      // 서버 동기화(찜 목록) → 서버 로그아웃
      const userId = userIdStr ? parseInt(userIdStr, 10) : null;
      const bookmarkedPolicyIds: number[] = bookmarkedStr ? JSON.parse(bookmarkedStr) : [];
      if (userId && bookmarkedPolicyIds.length > 0) {
        try {
          await syncBookmarkedPolicies(userId, bookmarkedPolicyIds);
        } catch {
          // 동기화 실패는 무시
        }
      }

      // 서버 로그아웃 호출 (Authorization=access, Refresh-Token=refresh)
      try {
        await serverLogout(at || null, rt || "");
      } catch {
        // 서버 실패는 무시
      }

      // 다른 탭에 로그아웃 신호 전파 (신규 추가)
      broadcastLogout("사용자가 로그아웃했습니다");

      // 클라이언트 정리
      clearAllStorageData();
      clearAuthStorage(); // ✅ 추가: 안전망으로 한 번 더 정리
      logout(); // Zustand 초기화
    } finally {
      router.push("/auth/login");
    }
  };

  const handleMypage = () => {
    if (!isLoggedIn) {
      router.push("/auth/login?redirect=/mypage");
    } else {
      router.push("/mypage");
    }
  };

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    const handleEsc = (e: KeyboardEvent) => {
      if (e.key === "Escape") setIsOpen(false);
    };
    if (isOpen) {
      document.addEventListener("mousedown", handleClickOutside);
      document.addEventListener("keydown", handleEsc);
    }
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
      document.removeEventListener("keydown", handleEsc);
    };
  }, [isOpen]);

  return (
    <header className="header-root">
      <div className="header-inner">
        <Link href="/dashboard" className="header-logo" draggable={false}>
          지방청년
        </Link>
        <nav className="header-nav" aria-label="주요 메뉴">
          <Link href="/community/main" className="header-nav-link">
            커뮤니티
          </Link>
          <Link href="/policy/recommendedList" className="header-nav-link">
            추천정책
          </Link>
          <Link href="/policy/totalPolicies" className="header-nav-link">
            전체정책
          </Link>
        </nav>
        <div className="header-actions" style={{ display: "flex", alignItems: "center", gap: 10 }}>
          <button
            type="button"
            className="btn-mypage"
            aria-label="마이페이지"
            onClick={handleMypage}
            style={{
              background: "transparent",
              border: "none",
              width: "36px",
              height: "36px",
              borderRadius: "50%",
              cursor: "pointer",
              transition: "all 0.2s ease",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              gap: "4px",
              boxShadow: "none",
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.backgroundColor = "rgba(0, 0, 0, 0.08)";
              e.currentTarget.style.transform = "translateY(-1px) scale(1.03)";
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.backgroundColor = "transparent";
              e.currentTarget.style.transform = "translateY(0) scale(1)";
            }}
            title="마이페이지"
          >
            <div style={{ width: "4px", height: "4px", backgroundColor: "#333", borderRadius: "50%" }} />
            <div style={{ width: "4px", height: "4px", backgroundColor: "#333", borderRadius: "50%" }} />
            <div style={{ width: "4px", height: "4px", backgroundColor: "#333", borderRadius: "50%" }} />
          </button>

          {!isLoggedIn ? (
            <Link href="/auth/login" className="btn-primary">
              로그인
            </Link>
          ) : (
            <button className="btn-primary" onClick={handleLogout}>
              로그아웃
            </button>
          )}
        </div>
      </div>
    </header>
  );
}
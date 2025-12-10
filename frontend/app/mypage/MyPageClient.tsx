"use client";
import { getMyProfile } from "@/libs/api/mypage.api";
import { useAuthStore } from "@/store/authStore"; // ✅ AuthStore 추가
import { useUserStore } from "@/store/userStore";
import type { Tab, UserProfileDto } from "@/types/api/mypage.types";
import { useEffect, useState } from "react";
import styles from "./MyPageLayout.module.css";
import PanelRouter from "./components/PanelRouter";
import SidebarNav from "./components/SidebarNav";

function MyPageSkeleton() {
  return (
    <div className={styles.mypageGridLayout} aria-busy="true">
      <aside className={styles.mypageSidebarWrap}>
        <div className={styles.sidebarSkeleton} />
      </aside>
      <main className={styles.mypagePanelWrap}>
        <div className={styles.panelSkeleton} />
      </main>
    </div>
  );
}

function MyPageError({ retry }: { retry: () => void }) {
  return (
    <div className={styles.mypageLoading} role="alert">
      데이터를 불러오지 못했습니다.
      <button className={styles.retryBtn} onClick={retry}>
        다시 시도
      </button>
    </div>
  );
}

export default function MyPageClient() {
  const { user, setUser, clearUser } = useUserStore();
  const { user: authUser, accessToken } = useAuthStore(); // ✅ AuthStore에서 토큰 상태 확인
  const [tab, setTab] = useState<Tab>("score");
  const [isLoading, setIsLoading] = useState(true);
  const [isError, setIsError] = useState(false);
  const [userId, setUserId] = useState<number | null | undefined>(undefined);
  const [isTokenReady, setIsTokenReady] = useState(false); // ✅ 토큰 준비 상태

  // ✅ 1단계: 토큰 준비 상태 확인
  useEffect(() => {
    console.log("[MYPAGE] 토큰 상태 확인 시작");
    
    const checkTokenReady = () => {
      const localAccessToken = localStorage.getItem("accessToken");
      const authAccessToken = accessToken;
      
      console.log("[MYPAGE] 토큰 확인:", {
        localToken: localAccessToken ? `${localAccessToken.substring(0, 10)}...` : "없음",
        authToken: authAccessToken ? `${authAccessToken.substring(0, 10)}...` : "없음"
      });
      
      // 토큰이 하나라도 있으면 준비된 것으로 간주
      const hasToken = !!(localAccessToken || authAccessToken);
      setIsTokenReady(hasToken);
      
      if (hasToken) {
        console.log("[MYPAGE] 토큰 준비 완료");
      } else {
        console.warn("[MYPAGE] 토큰이 없습니다");
      }
      
      return hasToken;
    };

    // 즉시 체크
    if (!checkTokenReady()) {
      // 토큰이 없으면 잠시 후 다시 체크 (Providers에서 복원 중일 수 있음)
      const timeout = setTimeout(() => {
        checkTokenReady();
      }, 100);
      
      return () => clearTimeout(timeout);
    }
  }, [accessToken]);

  // ✅ 2단계: CSR에서 localStorage에서 userId 읽기
  useEffect(() => {
    if (typeof window !== "undefined") {
      console.log("[MYPAGE] localStorage에서 userId 읽기");
      const stored = localStorage.getItem("userId");
      const parsedUserId = stored ? Number(stored) : null;
      
      console.log("[MYPAGE] userId:", parsedUserId);
      setUserId(parsedUserId);
    }
  }, []);

  // ✅ 3단계: 토큰과 userId가 모두 준비된 후에만 API 호출
  useEffect(() => {
    console.log("[MYPAGE] API 호출 조건 확인:", {
      hasUser: !!user,
      userId: userId,
      isTokenReady: isTokenReady
    });

    // ✅ 조건: 사용자 데이터가 없고, userId가 있고, 토큰이 준비되었을 때만 실행
    if (!user && userId !== undefined && userId !== null && isTokenReady) {
      console.log("[MYPAGE] 프로필 데이터 요청 시작");
      setIsLoading(true);
      setIsError(false);
      
      getMyProfile(userId)
        .then((u) => {
          console.log("[MYPAGE] 프로필 데이터 수신 성공:", u);
          setUser(u);
          setIsError(false);
        })
        .catch((error) => {
          console.error("[MYPAGE] 프로필 데이터 요청 실패:", error);
          
          // ✅ 인증 오류인 경우 사용자를 로그인 페이지로 보내지 않고 재시도 기회 제공
          if (error?.response?.status === 401) {
            console.warn("[MYPAGE] 인증 오류 - 토큰 재발급 후 재시도 가능");
            setIsError(true);
          } else {
            clearUser();
            setIsError(true);
          }
        })
        .finally(() => {
          console.log("[MYPAGE] 프로필 데이터 요청 완료");
          setIsLoading(false);
        });
    } 
    // ✅ userId가 확정되었지만 토큰이 없거나 이미 사용자 데이터가 있는 경우
    else if (userId !== undefined) {
      console.log("[MYPAGE] 로딩 종료 조건 확인");
      setIsLoading(false);
    }
  }, [userId, isTokenReady, user, setUser, clearUser]);
  
  // ✅ 재시도 함수 개선
  const handleRetry = () => {
    if (!userId) {
      console.warn("[MYPAGE] 재시도: userId 없음");
      return;
    }
    
    console.log("[MYPAGE] 수동 재시도 시작");
    setIsLoading(true);
    setIsError(false);
    
    // ✅ 재시도 전에 토큰 상태 다시 확인
    const localAccessToken = localStorage.getItem("accessToken");
    const authAccessToken = accessToken;
    
    if (!localAccessToken && !authAccessToken) {
      console.error("[MYPAGE] 재시도 실패: 토큰이 없습니다");
      setIsError(true);
      setIsLoading(false);
      return;
    }
    
    getMyProfile(userId)
      .then((u) => {
        console.log("[MYPAGE] 재시도 성공:", u);
        setUser(u);
        setIsError(false);
      })
      .catch((error) => {
        console.error("[MYPAGE] 재시도 실패:", error);
        
        if (error?.response?.status === 401) {
          console.warn("[MYPAGE] 재시도 시 인증 오류");
        } else {
          clearUser();
        }
        setIsError(true);
      })
      .finally(() => {
        setIsLoading(false);
      });
  };

  // ✅ 렌더링 조건 개선
  console.log("[MYPAGE] 렌더링 상태:", {
    userId: userId,
    isLoading: isLoading,
    isError: isError,
    hasUser: !!user,
    isTokenReady: isTokenReady
  });

  // userId가 아직 확정되지 않았으면 대기
  if (userId === undefined) {
    console.log("[MYPAGE] userId 확정 대기");
    return <MyPageSkeleton />;
  }
  
  // 로그인되지 않은 경우
  if (userId === null) {
    console.log("[MYPAGE] 로그인 필요");
    return <div>로그인이 필요합니다.</div>;
  }
  
  // 로딩 중인 경우
  if (isLoading) {
    console.log("[MYPAGE] 로딩 중");
    return <MyPageSkeleton />;
  }
  
  // 오류가 발생했거나 사용자 데이터가 없는 경우
  if (isError || !user) {
    console.log("[MYPAGE] 오류 상태");
    return <MyPageError retry={handleRetry} />;
  }

  // 정상 렌더링
  console.log("[MYPAGE] 정상 렌더링");
  return (
    <div className={styles.mypageGridLayout} aria-label="마이페이지 전체 레이아웃">
      <aside className={styles.mypageSidebarWrap}>
        <SidebarNav tab={tab} setTab={setTab} userRole={user.role} />
      </aside>
      <main className={styles.mypagePanelWrap} tabIndex={0}>
        <PanelRouter tab={tab} user={user as UserProfileDto} />
      </main>
    </div>
  );
}
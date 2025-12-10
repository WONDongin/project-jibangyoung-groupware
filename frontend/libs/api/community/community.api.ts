import type { DetailProps } from "@/app/community/types";
import { PostListDto } from "@/app/community/types";
import api from "../axios";

const BASE = process.env.NEXT_PUBLIC_API_BASE_URL!;

// 날짜순 인기글 리스트
// /api/community/popular?page=${page}
export async function fetchPopularPosts(
  page: number
): Promise<{ posts: PostListDto[]; totalPages: number }> {
  const res = await fetch(`${BASE}/api/community/popular?page=${page}`, {
    next: { revalidate: 300 },
  });

  if (!res.ok) {
    throw new Error("인기글 데이터를 불러오지 못했습니다");
  }

  const data = await res.json();

  return {
    posts: data.content,
    totalPages: data.totalPages,
  };
}

// 인기순 리스트 (날짜 : "today" | "week" | "month")
// /api/community/top-liked?period=${period}
export async function fetchPopularPostsByPeriod(
  period: "today" | "week" | "month"
): Promise<PostListDto[]> {
  const res = await fetch(`${BASE}/api/community/top-liked?period=${period}`, {
    cache: "no-store",
  });

  if (!res.ok) {
    throw new Error(`API 요청 실패: ${res.status} ${res.statusText}`);
  }

  return res.json();
}

// 공지사항 조회
export async function fetchNotices(): Promise<PostListDto[]> {
  const res = await fetch(`${BASE}/api/community/notices`, {
    cache: "no-store",
  });

  if (!res.ok) {
    throw new Error(`API 요청 실패: ${res.status} ${res.statusText}`);
  }

  return res.json();
}
export async function fetchPostsByRegion(
  regionCode: string,
  page: number,
  search?: string,
  searchType?: string,
  category?: string // category 파라미터 추가
): Promise<{ posts: PostListDto[]; totalPages: number }> {
  const query = new URLSearchParams({
    page: page.toString(),
  });

  if (search && searchType) {
    query.set("search", search);
    query.set("searchType", searchType);
  }

  if (category && category !== "all") {
    // "all" 카테고리는 백엔드로 보내지 않음
    query.set("category", category);
  }

  const res = await fetch(
    `${BASE}/api/community/region/${regionCode}?${query.toString()}`,
    {
      next: { revalidate: 3 },
    }
  );

  if (!res.ok) {
    throw new Error("지역 게시판 데이터를 불러오지 못했습니다");
  }

  const data = await res.json();

  return {
    posts: data.content,
    totalPages: data.totalPages,
  };
}

// 게시글 상세 정보(디테일)
// /api/community/post/${postId}
export async function fetchPostDetail(postId: string): Promise<DetailProps> {
  const res = await fetch(`${BASE}/api/community/post/${postId}`, {
    cache: "no-store",
  });

  if (!res.ok) {
    throw new Error("게시글 상세 정보를 불러오지 못했습니다");
  }

  const data = await res.json();

  return {
    id: data.id,
    title: data.title,
    nickname: data.nickname,
    userId: data.userId,
    createdAt: data.createdAt,
    views: data.views,
    likes: data.likes,
    content: data.content,
    category: data.category,
    isNotice: data.isNotice,
    author: data.nickname, // author는 nickname과 같은 값 사용
  };
}

// 게시판 작성 데이터 타입
export interface CreatePostRequest {
  title: string;
  category: "FREE" | "QUESTION" | "REVIEW" | "NOTICE";
  content: string;
  regionId: number;
  userId: number;
  isNotice?: boolean;
}

// 게시판 작성
// /api/community/write
export async function createCommunityPost(
  payload: CreatePostRequest
): Promise<number> {
  try {
    const response = await api.post("/api/community/write", payload);
    return response.data.data;
  } catch (error: any) {
    console.error("글 작성 에러:", error);

    let errorMessage = "글 작성 중 오류가 발생했습니다.";

    if (error.response?.data) {
      const errorData = error.response.data;

      if (Array.isArray(errorData) && errorData.length > 0) {
        errorMessage =
          errorData[0].defaultMessage || errorData[0].message || errorMessage;
      } else if (errorData.message) {
        errorMessage = errorData.message;
      } else if (errorData.error) {
        errorMessage = errorData.error;
      }
    }

    alert(errorMessage);
    throw new Error(errorMessage);
  }
}

// 지역 게시판 인기
// /api/community/regionPopular/{regionCode}/?page=${page}
export async function getPostsByRegionPopular(
  regionCode: string,
  page: number,
  size: number
): Promise<{ posts: PostListDto[]; totalPages: number }> {
  const res = await fetch(
    `${BASE}/api/community/regionPopular/${regionCode}?page=${page}&size=${size}`,
    {
      next: { revalidate: 300 },
    }
  );

  if (!res.ok) {
    throw new Error("인기글 데이터를 불러오지 못했습니다");
  }

  const data = await res.json();

  return {
    posts: data.content,
    totalPages: data.totalPages,
  };
}

export async function recommendPost(
  postId: number,
  recommendationType: string
): Promise<void> {
  try {
    await api.post(`/api/community/post/${postId}/recommend`, {
      type: recommendationType,
    });
  } catch (error: any) {
    const errorMessage = error.response?.data?.message || "추천 실패";
    throw new Error(errorMessage);
  }
}

// 사용자의 게시글 추천 상태 조회
export async function getUserRecommendation(
  postId: number
): Promise<string | null> {
  try {
    // 로그인 상태 확인
    if (typeof window !== "undefined") {
      const token = localStorage.getItem("accessToken");
      if (!token || token.length <= 20) {
        return null; // 비로그인 사용자는 추천 상태 없음
      }
    }
    
    const res = await api.post("/api/community/user/recommendation/status", {
      postId,
    });
    return res.data;
  } catch (error: any) {
    console.error(`추천 상태 조회 실패:`, error);
    return null;
  }
}

// 지역별 공지사항 조회
export async function fetchNoticesByRegion(
  regionCode: string
): Promise<PostListDto[]> {
  try {
    const res = await fetch(
      `${BASE}/api/community/region/${regionCode}/notices`,
      {
        cache: "no-store",
      }
    );

    if (!res.ok) {
      const errorText = await res.text();
      console.error(
        `[fetchNoticesByRegion] HTTP ${res.status}: ${res.statusText}`,
        errorText
      );
      throw new Error(`API 요청 실패: ${res.status} ${res.statusText}`);
    }

    return res.json();
  } catch (error) {
    console.error(
      `[fetchNoticesByRegion] 요청 URL: ${BASE}/api/community/region/${regionCode}/notices`
    );
    console.error(`[fetchNoticesByRegion] 에러:`, error);
    throw error;
  }
}

// 지역별 인기글 조회
export async function fetchPopularPostsByRegion(
  regionCode: string
): Promise<PostListDto[]> {
  try {
    const res = await fetch(
      `${BASE}/api/community/region/${regionCode}/popular`,
      {
        cache: "no-store",
      }
    );

    if (!res.ok) {
      const errorText = await res.text();
      console.error(
        `[fetchPopularPostsByRegion] HTTP ${res.status}: ${res.statusText}`,
        errorText
      );
      throw new Error(`API 요청 실패: ${res.status} ${res.statusText}`);
    }

    return res.json();
  } catch (error) {
    console.error(
      `[fetchPopularPostsByRegion] 요청 URL: ${BASE}/api/community/region/${regionCode}/popular`
    );
    console.error(`[fetchPopularPostsByRegion] 에러:`, error);
    throw error;
  }
}

// 게시글 수정 데이터 타입
export interface UpdatePostRequest {
  title: string;
  category: "FREE" | "QUESTION" | "REVIEW" | "NOTICE";
  content: string;
  isNotice?: boolean;
}

// 게시글 수정
// PUT /api/community/post/{postId}
export async function updateCommunityPost(
  postId: string,
  payload: UpdatePostRequest
): Promise<void> {
  try {
    await api.put(`/api/community/post/${postId}`, payload);
  } catch (error: any) {
    const errorMessage =
      error.response?.data?.message || "게시글 수정에 실패했습니다.";
    throw new Error(errorMessage);
  }
}

// 게시글 삭제
// DELETE /api/community/post/{postId}
export async function deleteCommunityPost(postId: string): Promise<void> {
  try {
    await api.delete(`/api/community/post/${postId}`);
  } catch (error: any) {
    let errorMessage = "게시글 삭제에 실패했습니다.";

    if (error.response?.status === 403) {
      errorMessage = "게시글을 삭제할 권한이 없습니다.";
    } else if (error.response?.status === 404) {
      errorMessage = "존재하지 않는 게시글입니다.";
    } else if (error.response?.data?.message) {
      errorMessage = error.response.data.message;
    }

    throw new Error(errorMessage);
  }
}

// 신고 데이터 타입
export interface CreateReportRequest {
  targetType: "POST" | "COMMENT" | "USER" | "POLICY" | "ETC";
  targetId: number;
  reasonCode: string;
  reasonDetail?: string;
}

// 게시글의 각 추천 유형별 개수 조회 (비로그인 사용자도 접근 가능)
// GET /api/community/post/{postId}/recommendation-counts
export async function getRecommendationCounts(
  postId: number
): Promise<Record<string, number>> {
  const res = await fetch(
    `${BASE}/api/community/post/${postId}/recommendation-counts`,
    {
      cache: "no-store",
    }
  );

  if (!res.ok) {
    console.error(`추천 개수 조회 실패: ${res.status} ${res.statusText}`);
    return {
      쏠쏠정보: 0,
      흥미진진: 0,
      공감백배: 0,
      분석탁월: 0,
      후속강추: 0,
    };
  }

  const response = await res.json();
  return response.data;
}

// 조회수 증가 (로그인 사용자만)
// POST /api/community/post/{postId}/view
export async function increasePostViewCount(postId: number): Promise<void> {
  try {
    // 로그인 상태 확인 - 비로그인 사용자는 조회수 증가 안함
    if (typeof window !== "undefined") {
      const token = localStorage.getItem("accessToken");
      if (!token || token.length <= 20) {
        console.log("비로그인 사용자 - 조회수 증가 생략");
        return;
      }
    }
    
    await api.post(`/api/community/post/${postId}/view`);
  } catch (error: any) {
    // 조회수 증가 실패는 사용자에게 보이지 않도록 조용히 처리
    console.error("조회수 증가 실패:", error);
  }
}

// 신고 접수
// POST /api/community/report
export async function createReport(
  payload: CreateReportRequest
): Promise<void> {
  try {
    await api.post("/api/community/report", payload);
  } catch (error: any) {
    const errorMessage =
      error.response?.data?.message || "신고 접수에 실패했습니다.";
    throw new Error(errorMessage);
  }
}

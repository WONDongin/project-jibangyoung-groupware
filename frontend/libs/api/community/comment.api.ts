import api from "@/libs/api/axios"; // axios 인스턴스 임포트
import { Comment } from "@/types/comment";

// 댓글 목록 조회 API (비로그인 사용자도 접근 가능하도록 fetch 사용)
export const fetchComments = async (postId: string): Promise<Comment[]> => {
  const BASE = process.env.NEXT_PUBLIC_API_BASE_URL!;
  const res = await fetch(`${BASE}/api/community/posts/${postId}/comments`, {
    cache: "no-store",
  });
  
  if (!res.ok) {
    throw new Error("댓글을 불러오는데 실패했습니다");
  }
  
  return res.json();
};

// 댓글 작성 API
export const postComment = async (
  postId: string,
  content: string,
  parentId?: number
): Promise<Comment> => {
  const res = await api.post(`/api/community/posts/${postId}/comments`, {
    content,
    parentId,
  });
  return res.data;
};

// 댓글 삭제 API
export const deleteComment = async (commentId: number): Promise<void> => {
  await api.delete(`/api/community/comments/${commentId}`);
};

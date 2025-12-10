"use client";

import { createCommunityPost } from "@/libs/api/community/community.api";
import { useAuthStore } from "@/store/authStore";
import dynamic from "next/dynamic";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";
import styles from "./components/Write.module.css";

const Editor = dynamic(() => import("./components/Editor"), {
  ssr: false,
  loading: () => <p>에디터 로딩 중...</p>,
});

const CATEGORY_OPTIONS = [
  { value: "FREE", label: "자유" },
  { value: "QUESTION", label: "질문" },
  { value: "REVIEW", label: "정착후기" },
] as const;

const ADMIN_CATEGORY_OPTIONS = [
  { value: "FREE", label: "자유" },
  { value: "QUESTION", label: "질문" },
  { value: "REVIEW", label: "정착후기" },
  { value: "NOTICE", label: "공지사항" },
] as const;

export default function WriteForm() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const regionCode = searchParams.get("regionCode");
  const { user } = useAuthStore(); // user 상태 가져오기

  // 유저 권한 확인
  const isPrivilegedUser = user?.role && ["ADMIN", "MENTOR_A", "MENTOR_B", "MENTOR_C"].includes(user.role);
  const availableCategories = isPrivilegedUser ? ADMIN_CATEGORY_OPTIONS : CATEGORY_OPTIONS;

  const [form, setForm] = useState({
    title: "",
    category: "FREE" as (typeof CATEGORY_OPTIONS)[number]["value"] | (typeof ADMIN_CATEGORY_OPTIONS)[number]["value"],
    content: "",
  });
  const [isNotice, setIsNotice] = useState(false);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!user) {
      alert("로그인이 필요합니다.");
      router.replace("/auth/login"); // 로그인 페이지로 리디렉션
    }
  }, [user, router]);

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>
  ) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
    
    // 공지사항 카테고리 선택 시 isNotice를 true로 설정
    if (name === "category") {
      if (value === "NOTICE") {
        setIsNotice(true);
      } else {
        setIsNotice(false);
      }
    }
  };

  const handleContentChange = (v: string) => {
    setForm((prev) => ({ ...prev, content: v }));
  };

  const handleSubmit = async () => {
    if (!user) {
      alert("로그인 상태를 확인할 수 없습니다.");
      return;
    }
    if (!regionCode) {
      alert("지역 코드가 없습니다.");
      return;
    }
    if (!form.title.trim()) {
      alert("제목을 입력해주세요.");
      return;
    }
    setLoading(true);
    try {
      const postId = await createCommunityPost({
        ...form,
        regionId: Number(regionCode),
        userId: user.id,
        isNotice: form.category === "NOTICE" ? isNotice : false,
      });
      alert("게시글이 작성되었습니다.");
      router.push(`/community/${regionCode}/${postId}`);
    } catch (error) {
      alert("게시글 작성 중 오류가 발생했습니다.");
    } finally {
      setLoading(false);
    }
  };

  if (!user) {
    return null; // 로그인되지 않은 경우 아무것도 렌더링하지 않음
  }

  return (
    <div className={styles.writeForm}>
      <div className={styles.writeHeader}>
        <h1 className={styles.writeTitle}>글 작성</h1>
      </div>
      
      <div className={styles.formGroup}>
        <label className={styles.formLabel}>제목 및 카테고리</label>
        <div className={styles.titleCategoryRow}>
          <div className={styles.categorySection}>
            <select
              title="category"
              name="category"
              value={form.category}
              onChange={handleChange}
              className={styles.categorySelect}
            >
              {availableCategories.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
            
            {form.category === "NOTICE" && (
              <div className={styles.noticeOptions}>
                <label className={styles.checkboxLabel}>
                  <input
                    type="checkbox"
                    checked={isNotice}
                    onChange={(e) => setIsNotice(e.target.checked)}
                    className={styles.checkbox}
                  />
                  <span className={styles.checkboxText}>최상단 고정</span>
                </label>
              </div>
            )}
          </div>
          
          <div className={styles.titleSection}>
            <input
              name="title"
              type="text"
              placeholder="제목을 입력하세요"
              value={form.title}
              onChange={handleChange}
              className={styles.titleInput}
            />
          </div>
        </div>
      </div>

      <div className={styles.formGroup}>
        <label className={styles.formLabel}>내용</label>
        <div className={styles.editorWrapper}>
          <Editor onChange={handleContentChange} />
        </div>
      </div>

      <div className={styles.buttonGroup}>
        <button 
          type="button" 
          className={styles.cancelButton}
          onClick={() => router.back()}
        >
          취소
        </button>
        <button 
          type="button"
          className={styles.submitButton}
          onClick={handleSubmit} 
          disabled={loading}
        >
          {loading ? "저장 중..." : "글 등록"}
        </button>
      </div>
    </div>
  );
}

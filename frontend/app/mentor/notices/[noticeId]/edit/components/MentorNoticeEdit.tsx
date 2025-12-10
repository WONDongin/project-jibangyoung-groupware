"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import dynamic from "next/dynamic";
import { getMentorNoticeDetail, updateMentorNotice } from "@/libs/api/mentor/mentor.api";
import { getRegionsBoard } from "@/libs/api/region.api";
import type { Region } from "@/types/api/region.d";
import { regionFullPath } from "@/components/constants/region-map";
import { useAuthStore } from "@/store/authStore";
import styles from "../../../write/MentorNoticeWrite.module.css";

const MentorNoticeEditor = dynamic(() => import("../../../write/components/MentorNoticeEditor"), {
  ssr: false,
  loading: () => <p>에디터 로딩 중...</p>,
});

interface Props {
  noticeId: number;
}

export default function MentorNoticeEdit({ noticeId }: Props) {
  const router = useRouter();
  const { user } = useAuthStore();
  const [regions, setRegions] = useState<Region[]>([]);
  const [mentorRegionIds, setMentorRegionIds] = useState<number[]>([]);
  const [filteredRegions, setFilteredRegions] = useState<Region[]>([]);
  const [form, setForm] = useState({
    title: "",
    content: "",
    regionId: "",
  });
  const [loading, setLoading] = useState(false);
  const [initialLoading, setInitialLoading] = useState(true);

  // 멘토 권한 체크
  const isMentor = user?.role && ['MENTOR_A', 'MENTOR_B', 'MENTOR_C', 'ADMIN'].includes(user.role);
  const isAdmin = user?.role === 'ADMIN';

  // 멘토가 아닌 경우 접근 차단
  useEffect(() => {
    if (user && !isMentor) {
      alert("멘토 권한이 필요합니다.");
      router.push("/dashboard");
      return;
    }
  }, [user, isMentor, router]);

  useEffect(() => {
    if (!user || !isMentor) return;
    
    const fetchData = async () => {
      try {
        // 기존 공지사항 데이터 가져오기
        const noticeData = await getMentorNoticeDetail(noticeId);
        setForm({
          title: noticeData.current.title,
          content: noticeData.current.content,
          regionId: noticeData.current.regionId.toString(),
        });

        // 관리자가 아닌 경우만 멘토가 담당하는 지역 목록 가져오기
        if (!isAdmin) {
          try {
            const response = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/api/mentor/regions/me`, {
              headers: {
                Authorization: `Bearer ${localStorage.getItem("accessToken")}`,
              },
            });
            
            if (response.ok) {
              const mentorRegions = await response.json();
              if (mentorRegions.data && Array.isArray(mentorRegions.data)) {
                setMentorRegionIds(mentorRegions.data);
              }
            }
          } catch (error) {
            console.warn("멘토 지역 정보를 불러올 수 없습니다:", error);
          }
        }
        
        // 지역 목록 가져오기
        const regionData = await getRegionsBoard();
        setRegions(regionData);
      } catch (error) {
        console.error("데이터 로드 실패:", error);
        alert("공지사항을 불러오는데 실패했습니다.");
        router.back();
      } finally {
        setInitialLoading(false);
      }
    };
    
    fetchData();
  }, [user, isMentor, isAdmin, noticeId, router]);

  // 멘토의 지역 정보가 로드되면 필터링된 지역 목록 생성
  useEffect(() => {
    if (regions.length > 0) {
      if (isAdmin) {
        // 관리자는 모든 지역 표시
        setFilteredRegions(regions);
      } else {
        // 멘토는 할당된 지역만 표시
        const filtered = regions.filter((region) => {
          const regionCode = Number(region.regionCode);
          // 전국(99999)는 항상 포함
          if (regionCode === 99999) {
            return true;
          }
          // 멘토가 담당하는 지역들 포함
          if (mentorRegionIds.includes(regionCode)) {
            return true;
          }
          return false;
        });
        setFilteredRegions(filtered);
      }
    }
  }, [mentorRegionIds, regions, isAdmin]);

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>
  ) => {
    const { name, value } = e.target;
    setForm(prev => ({ ...prev, [name]: value }));
  };

  const handleContentChange = (content: string) => {
    setForm(prev => ({ ...prev, content }));
  };

  const handleSubmit = async () => {
    if (!form.title.trim()) {
      alert("제목을 입력해주세요.");
      return;
    }
    if (!form.content.trim()) {
      alert("내용을 입력해주세요.");
      return;
    }
    if (!form.regionId) {
      alert("지역을 선택해주세요.");
      return;
    }

    setLoading(true);
    try {
      await updateMentorNotice(noticeId, {
        title: form.title,
        content: form.content,
        regionId: Number(form.regionId),
      });
      
      alert("공지사항이 수정되었습니다.");
      router.push(`/mentor/notices/${noticeId}`);
    } catch (error) {
      console.error(error);
      alert("수정 중 오류가 발생했습니다.");
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    router.back();
  };

  if (!user) {
    return <div style={{ textAlign: "center", padding: "2rem" }}>로그인이 필요합니다.</div>;
  }

  if (!isMentor) {
    return <div style={{ textAlign: "center", padding: "2rem" }}>멘토 권한이 필요합니다.</div>;
  }

  if (initialLoading) {
    return <div style={{ textAlign: "center", padding: "2rem" }}>로딩 중...</div>;
  }

  return (
    <div className={styles.form}>
      {/* 지역 선택 */}
      <div className={styles.formRow}>
        <label className={styles.label}>지역선택</label>
        <select
          name="regionId"
          value={form.regionId}
          onChange={handleChange}
          className={styles.regionSelect}
        >
          <option value="">지역을 선택하세요</option>
          {filteredRegions.map((region) => (
            <option key={region.regionCode} value={region.regionCode}>
              {Number(region.regionCode) === 99999 ? "전국" : regionFullPath(region.regionCode)}
            </option>
          ))}
        </select>
      </div>

      {/* 제목 입력 */}
      <div className={styles.formRow}>
        <label className={styles.label}>제목</label>
        <input
          type="text"
          name="title"
          value={form.title}
          onChange={handleChange}
          placeholder="제목을 입력하세요"
          className={styles.titleInput}
        />
      </div>

      {/* CKEditor */}
      <div className={styles.editorContainer}>
        <MentorNoticeEditor 
          initialData={form.content}
          onChange={handleContentChange}
        />
      </div>

      {/* 액션 버튼 */}
      <div className={styles.actions}>
        <button
          onClick={handleSubmit}
          disabled={loading}
          className={styles.submitButton}
        >
          {loading ? "수정 중..." : "수정"}
        </button>
        <button
          onClick={handleCancel}
          className={styles.cancelButton}
        >
          취소
        </button>
      </div>
    </div>
  );
}
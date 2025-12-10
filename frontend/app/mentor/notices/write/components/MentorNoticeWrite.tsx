"use client";

import { useState, useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import dynamic from "next/dynamic";
import { createMentorNotice } from "@/libs/api/mentor/mentor.api";
import { getRegionsBoard } from "@/libs/api/region.api";
import type { Region } from "@/types/api/region.d";
import { regionFullPath } from "@/components/constants/region-map";
import { useAuthStore } from "@/store/authStore";
import styles from "../MentorNoticeWrite.module.css";

const MentorNoticeEditor = dynamic(() => import("./MentorNoticeEditor"), {
  ssr: false,
  loading: () => <p>에디터 로딩 중...</p>,
});

export default function MentorNoticeWrite() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { user } = useAuthStore();
  const [regions, setRegions] = useState<Region[]>([]);
  const [mentorRegionIds, setMentorRegionIds] = useState<number[]>([]);
  const [filteredRegions, setFilteredRegions] = useState<Region[]>([]);
  const [form, setForm] = useState({
    title: "",
    content: "",
    regionId: searchParams.get("regionId") || "", // 기본값 없음
  });
  const [loading, setLoading] = useState(false);
  
  // 관리자 권한 확인
  const isAdmin = user?.role === 'ADMIN';

  useEffect(() => {
    const fetchData = async () => {
      try {
        // 관리자가 아닌 경우에만 멘토 지역 정보 가져오기
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
            } else {
              console.warn("멘토 지역 정보를 찾을 수 없습니다. 전국 공지만 작성 가능합니다.");
            }
          } catch (error) {
            console.warn("멘토 지역 정보를 불러올 수 없습니다:", error);
          }
        }
        
        // 지역 목록 가져오기
        const regionData = await getRegionsBoard();
        setRegions(regionData);
      } catch (error) {
        console.error("지역 목록을 불러오지 못했습니다:", error);
      }
    };
    fetchData();
  }, []);

  // 멘토의 지역 정보가 로드되면 필터링된 지역 목록 생성
  useEffect(() => {
    if (regions.length > 0) {
      if (isAdmin) {
        // 관리자인 경우 모든 지역 선택 가능
        setFilteredRegions(regions);
      } else {
        // 일반 멘토인 경우 담당 지역만 선택 가능
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
  }, [isAdmin, mentorRegionIds, regions]);

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
      const noticeId = await createMentorNotice({
        title: form.title,
        content: form.content,
        regionId: Number(form.regionId),
      });
      
      alert("공지사항이 작성되었습니다.");
      router.push(`/mentor/notices/${noticeId}`);
    } catch (error) {
      console.error(error);
      alert("작성 중 오류가 발생했습니다.");
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    router.back();
  };

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
          {loading ? "작성 중..." : "작성"}
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
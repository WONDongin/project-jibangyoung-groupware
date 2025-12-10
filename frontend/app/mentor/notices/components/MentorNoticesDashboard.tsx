"use client";

import type { MentorNotice } from "@/libs/api/mentor/mentor.api";
import { getMentorNotices } from "@/libs/api/mentor/mentor.api";
import { getRegionsBoard } from "@/libs/api/region.api";
import type { Region } from "@/types/api/region.d";
import { regionFullPath } from "@/components/constants/region-map";
import { useAuthStore } from "@/store/authStore";
import { useRouter } from "next/navigation";
import { useEffect, useState, useCallback } from "react";
import styles from "../MentorNotices.module.css";

// HTML 태그를 제거하고 순수 텍스트만 추출하는 함수
const stripHtmlTags = (html: string): string => {
  return html
    .replace(/<[^>]*>/g, "") // HTML 태그 제거
    .replace(/&nbsp;/g, " ") // &nbsp; 공백 문자 변환
    .replace(/&amp;/g, "&") // &amp; 변환
    .replace(/&lt;/g, "<") // &lt; 변환
    .replace(/&gt;/g, ">") // &gt; 변환
    .replace(/&quot;/g, '"') // &quot; 변환
    .replace(/&#39;/g, "'") // &#39; 변환
    .trim();
};

// 텍스트를 지정된 길이로 축약하는 함수
const truncateText = (text: string, maxLength: number): string => {
  return text.length > maxLength ? `${text.substring(0, maxLength)}...` : text;
};

export default function MentorNoticesDashboard() {
  const router = useRouter();
  const { user } = useAuthStore();
  const [regions, setRegions] = useState<Region[]>([]);
  const [mentorRegionIds, setMentorRegionIds] = useState<number[]>([]);
  const [filteredRegions, setFilteredRegions] = useState<Region[]>([]);
  const [selectedRegion, setSelectedRegion] = useState<string>("");
  const [keyword, setKeyword] = useState("");
  const [notices, setNotices] = useState<MentorNotice[]>([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(false);
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
  }, [user, isMentor, isAdmin]);

  // 멘토의 지역 정보가 로드되면 필터링된 지역 목록 생성
  useEffect(() => {
    if (regions.length > 0) {
      if (isAdmin) {
        // 관리자는 모든 지역 표시
        setFilteredRegions(regions);
        // 관리자의 기본값은 "전체 지역" (빈 문자열)
        if (!selectedRegion) {
          setSelectedRegion("");
        }
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
        
        // 첫 번째 할당된 지역을 기본값으로 설정하지 않음
        if (!selectedRegion && filtered.length > 0) {
          // const defaultRegion = filtered.find(region => Number(region.regionCode) !== 99999);
          // if (defaultRegion) {
          //   setSelectedRegion(defaultRegion.regionCode.toString());
          // }
        }
      }
    }
  }, [mentorRegionIds, regions, selectedRegion, isAdmin]);

  const fetchNotices = useCallback(async () => {
    if (!user || !isMentor) return;

    setLoading(true);
    try {
      const regionIdToFilter =
        selectedRegion && selectedRegion !== ""
          ? Number(selectedRegion)
          : undefined;
      const response = await getMentorNotices(
        regionIdToFilter,
        currentPage,
        10,
        keyword,
      );

      setNotices(response.content);
      setTotalPages(response.totalPages);
    } catch (error) {
      console.error("멘토 공지를 불러오지 못했습니다:", error);
    } finally {
      setLoading(false);
    }
  }, [user, isMentor, selectedRegion, currentPage, keyword]);

  useEffect(() => {
    if (!user || !isMentor) return;
    fetchNotices();
  }, [fetchNotices]);

  const handleSearch = () => {
    setCurrentPage(1);
    fetchNotices();
  };

  const handlePageChange = (page: number) => {
    setCurrentPage(page);
  };

  const handleNoticeClick = (noticeId: number) => {
    router.push(`/mentor/notices/${noticeId}`);
  };

  const handleWriteClick = () => {
    const regionParam = selectedRegion ? `?regionId=${selectedRegion}` : "";
    router.push(`/mentor/notices/write${regionParam}`);
  };

  const getPageNumbers = () => {
    const pages = [];
    const start = Math.max(1, currentPage - 2);
    const end = Math.min(totalPages, currentPage + 2);

    for (let i = start; i <= end; i++) {
      pages.push(i);
    }
    return pages;
  };

  // 권한 체크 - 로딩 중이거나 멘토가 아닌 경우
  if (!user) {
    return <div style={{ textAlign: "center", padding: "2rem" }}>로그인이 필요합니다.</div>;
  }

  if (!isMentor) {
    return <div style={{ textAlign: "center", padding: "2rem" }}>멘토 권한이 필요합니다.</div>;
  }

  return (
    <div>
      {/* 검색 영역 */}
      <div className={styles.searchContainer}>
        <select
          title="지역 선택"
          value={selectedRegion}
          onChange={(e) => {
            setSelectedRegion(e.target.value);
            setCurrentPage(1); // 지역 변경 시 페이지를 1로 리셋
          }}
          className={styles.regionSelect}
        >
          <option value="">전체 지역</option>
          {filteredRegions.map((region) => (
            <option key={region.regionCode} value={region.regionCode}>
              {String(region.regionCode) === "99999" ? "전국" : regionFullPath(region.regionCode)}
            </option>
          ))}
        </select>

        <div className={styles.searchBox}>
          <input
            type="text"
            placeholder="검색어를 입력하세요"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            className={styles.searchInput}
            onKeyPress={(e) => e.key === "Enter" && handleSearch()}
          />
          <button onClick={handleSearch} className={styles.searchButton}>
            검색
          </button>
        </div>

        <button onClick={handleWriteClick} className={styles.writeButton}>
          글쓰기
        </button>
      </div>

      {/* 공지 목록 */}
      {loading ? (
        <div style={{ textAlign: "center", padding: "2rem" }}>로딩 중...</div>
      ) : (
        <>
          <div className={styles.noticeGrid}>
            {notices.map((notice) => (
              <div
                key={notice.id}
                className={styles.noticeCard}
                onClick={() => handleNoticeClick(notice.id)}
              >
                <div className={styles.commentIcon}>
                  <span>💬</span>
                </div>

                <h3 className={styles.noticeTitle}>{notice.title}</h3>

                <div className={styles.noticeInfo}>
                  <span className={styles.regionBadge}>
                    {String(notice.regionId) === "99999" ? "전국" : regionFullPath(notice.regionId)}
                  </span>
                  <span>작성자: {notice.authorName || "알 수 없음"}</span>
                  <span>📅 {notice.createdAt}</span>
                  <span>💬</span>
                </div>

                <div className={styles.noticeContent}>
                  {truncateText(stripHtmlTags(notice.content), 40)}
                </div>
              </div>
            ))}
          </div>

          {/* 페이지네이션 */}
          <div className={styles.pagination}>
            <button
              onClick={() => handlePageChange(currentPage - 1)}
              disabled={currentPage === 1}
              className={styles.pageButton}
            >
              &lt;
            </button>

            {getPageNumbers().map((page) => (
              <button
                key={page}
                onClick={() => handlePageChange(page)}
                className={`${styles.pageButton} ${currentPage === page ? styles.active : ""}`}
              >
                {page}
              </button>
            ))}

            <button
              onClick={() => handlePageChange(currentPage + 1)}
              disabled={currentPage === totalPages}
              className={styles.pageButton}
            >
              &gt;
            </button>
          </div>
        </>
      )}
    </div>
  );
}

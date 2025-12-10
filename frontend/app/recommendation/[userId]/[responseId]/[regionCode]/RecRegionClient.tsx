'use client';

import PolicyCardList from '@/app/policy/totalPolicies/components/PolicyCardList';
import SkeletonLoader from '@/app/policy/totalPolicies/skeleton';
import { syncBookmarkedPolicies } from '@/libs/api/policy/sync';
import { fetchPolicies, fetchRegionReason } from '@/libs/api/recommendation.api';
import { PolicyCard } from '@/types/api/policy.c';
import { useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import './RecommendationRegion.css';
import { ActionButtons } from './components/ActionButtons';
import { ReasonCards } from './components/ReasonCard';
import { RecommendationHeader } from './components/RecommendationHeader';

interface RecommendationRegionReasonDto {
  username: string;
  rankGroup: number;
  regionName: string;
  reason1: string;
  reason2: string;
  reason3: string;
  reason4: string;
}

interface RecRegionClientProps {
  userId: number;   // URL에서 받는 userId
  responseId: number;
  regionCode: number;
}

function mapDtoToPolicyCard(dto: any): PolicyCard {
  return {
    NO: dto.NO,
    plcy_nm: dto.plcy_nm,
    aply_ymd: dto.aply_ymd,
    sidoName: dto.sidoName,
    plcy_kywd_nm: dto.plcy_kywd_nm,
    plcy_no: dto.plcy_no,
    deadline: dto.deadline,
    d_day: dto.d_day,
    favorites: dto.favorites,
  };
}

const ITEMS_PER_PAGE = 4;
const STORAGE_KEY = 'bookmarkedPolicyIds';

export default function RecRegionClient({ userId: paramUserId, responseId, regionCode }: RecRegionClientProps) {
  const router = useRouter();

  const [userId, setUserId] = useState<number | null>(null);
  const [regionReason, setRegionReason] = useState<RecommendationRegionReasonDto | null>(null);
  const [policies, setPolicies] = useState<PolicyCard[]>([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [bookmarkedPolicyIds, setBookmarkedPolicyIds] = useState<number[]>([]);

  const totalPages = Math.ceil(policies.length / ITEMS_PER_PAGE);
  const startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
  const currentPolicies = policies.slice(startIndex, startIndex + ITEMS_PER_PAGE);

  // 1. URL userId 와 로컬스토리지 userId 비교, 불일치 시 alert 후 뒤로가기 or 로그인 페이지 이동
  useEffect(() => {
    if (typeof window !== 'undefined') {
      const storedUserIdStr = localStorage.getItem('userId');
      const storedUserId = storedUserIdStr ? Number(storedUserIdStr) : null;

      if (storedUserId === null) {
        alert('로그인이 필요합니다. 로그인 페이지로 이동합니다.');
        router.push('/auth/login');
        return;
      }

      if (storedUserId !== paramUserId) {
        alert('다른 사용자의 결과를 볼 수 없습니다. 이전 페이지로 돌아갑니다');
        router.back();
        return;
      }

      setUserId(storedUserId);
    }
  }, [paramUserId, router]);

  // 2. userId가 셋팅되면 localStorage에서 북마크 복원
  useEffect(() => {
    if (!userId) return;
    if (typeof window !== 'undefined') {
      try {
        const stored = localStorage.getItem(STORAGE_KEY);
        if (stored) {
          setBookmarkedPolicyIds(JSON.parse(stored));
        }
      } catch {
        // 무시
      }
    }
  }, [userId]);

  // 3. 5분마다 localStorage 북마크 동기화 서버 전송
  useEffect(() => {
    if (!userId) return;

    const syncBookmarksToServer = async () => {
      if (typeof window === 'undefined') return;
      try {
        const stored = localStorage.getItem(STORAGE_KEY);
        const bookmarkedIds = stored ? JSON.parse(stored) : [];
        await syncBookmarkedPolicies(userId, bookmarkedIds);
      } catch (error) {
        console.error('북마크 동기화 실패:', error);
      }
    };

    const intervalId = setInterval(syncBookmarksToServer, 1 * 60 * 1000);
    return () => clearInterval(intervalId);
  }, [userId]);

  // 4. 추천 지역 사유 및 정책 데이터 fetch
  useEffect(() => {
    if (!userId) return;

    const fetchData = async () => {
      try {
        setLoading(true);
        const [reasonData, policiesData] = await Promise.all([
          fetchRegionReason(userId, responseId, regionCode.toString()),
          fetchPolicies(userId, responseId, regionCode.toString()),
        ]);

        setRegionReason(reasonData);
        setPolicies(policiesData.map(mapDtoToPolicyCard));
      } catch (err) {
        setError(err instanceof Error ? err.message : '알 수 없는 오류가 발생했습니다.');
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [userId, responseId, regionCode]);

  const handleBookmarkToggle = (policyId: number) => {
    setBookmarkedPolicyIds((prev) => {
      const isBookmarked = prev.includes(policyId);
      const updated = isBookmarked
        ? prev.filter((id) => id !== policyId)
        : [...prev, policyId];

      if (typeof window !== 'undefined') {
        try {
          localStorage.setItem(STORAGE_KEY, JSON.stringify(updated));
        } catch {
          // 무시
        }
      }

      return updated;
    });
  };

  if (userId === null) {
    return <div>로그인 확인 중...</div>;
  }

  if (loading) {
    return (
      <div className="recommendation-page">
        <div className="recommendation-container">
          <SkeletonLoader />
        </div>
      </div>
    );
  }

  if (error || !regionReason) {
    return (
      <div className="recommendation-page">
        <div className="recommendation-container">
          <div className="error-container">
            <p className="error-text">{error || '데이터를 찾을 수 없습니다.'}</p>
            <button onClick={() => window.location.reload()} className="retry-button">
              다시 시도
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="recommendation-page">
      <div className="recommendation-container">
        <RecommendationHeader
          username={regionReason.username}
          regionName={regionReason.regionName}
          rankGroup={regionReason.rankGroup}
        />

        <ReasonCards
          reason1={regionReason.reason1}
          reason2={regionReason.reason2}
          reason3={regionReason.reason3}
          reason4={regionReason.reason4}
        />

        <div className="policy-section">
          <div className="policy-header">
            <h2 className="policy-title">
              <span className="policy-region-name">{regionReason.regionName}</span> 관련 추천정책입니다.
            </h2>
          </div>

          {policies.length > 0 ? (
            <>
              <div className="policy-grid">
                <PolicyCardList
                  policies={currentPolicies}
                  onCardClick={(policyNo) => router.push(`/policy/policy_detail/${policyNo}`)}
                  bookmarkedPolicyIds={bookmarkedPolicyIds}
                  onBookmarkToggle={handleBookmarkToggle}
                />
              </div>

              {totalPages > 1 && (
                <div className="pagination-container">
                  <div className="pagination-nav">
                    <button
                      className="pagination-button"
                      onClick={() => setCurrentPage((prev) => Math.max(prev - 1, 1))}
                      disabled={currentPage === 1}
                    >
                      ← 이전
                    </button>
                    <div className="pagination-info">
                      <span className="page-number">{currentPage}</span> / {totalPages}
                    </div>
                    <button
                      className="pagination-button"
                      onClick={() => setCurrentPage((prev) => Math.min(prev + 1, totalPages))}
                      disabled={currentPage === totalPages}
                    >
                      다음 →
                    </button>
                  </div>
                </div>
              )}
            </>
          ) : (
            <div className="error-container">
              <p className="error-text">관련 정책이 없습니다.</p>
            </div>
          )}
        </div>

        <ActionButtons userId={userId} responseId={responseId} regionCode={regionCode} />
      </div>
    </div>
  );
}

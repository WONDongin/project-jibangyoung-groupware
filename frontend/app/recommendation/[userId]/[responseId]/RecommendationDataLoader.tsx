'use client';

import SkeletonLoader from '@/app/policy/totalPolicies/skeleton';
import { syncBookmarkedPolicies } from '@/libs/api/policy/sync';
import { fetchRecommendations } from '@/libs/api/recommendation.api';
import { RecommendationResultDto } from '@/types/api/recommendation';
import { useRouter } from 'next/navigation';
import React, { useEffect, useState } from 'react';
import FooterActions from './components/FooterActions';
import RecommendationList from './components/RecommendationList';

interface RecommendationDataLoaderProps {
  userId: number;        // URL에서 받아야 하는 userId
  responseId: number;
}

const STORAGE_KEY = 'bookmarkedPolicyIds';

const RecommendationDataLoader: React.FC<RecommendationDataLoaderProps> = ({
  userId: paramUserId,
  responseId,
}) => {
  const [userId, setUserId] = useState<number | null>(null);
  const [data, setData] = useState<RecommendationResultDto[] | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [bookmarkedPolicyIds, setBookmarkedPolicyIds] = useState<number[]>([]);
  const router = useRouter();

  // 1. URL userId와 로컬스토리지 userId 비교 (로그인 체크 및 불일치 처리)
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

  // 2. userId 생기면 localStorage에서 북마크 복원
  useEffect(() => {
    if (!userId) return;
    if (typeof window !== 'undefined') {
      try {
        const stored = localStorage.getItem(STORAGE_KEY);
        if (stored) {
          setBookmarkedPolicyIds(JSON.parse(stored));
        }
      } catch {
        console.log('북마크 복원 실패');
      }
    }
  }, [userId]);

  // 3. 5분마다 북마크 서버 동기화
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

  // 4. 추천 데이터 fetch
  useEffect(() => {
    if (!userId) return;

    const loadRecommendations = async () => {
      try {
        setLoading(true);
        setError(null);
        const res = await fetchRecommendations(userId, responseId);
        setData(res);
      } catch (err) {
        setError('추천 정보를 불러오는데 실패했습니다.');
        console.error('Failed to fetch recommendations:', err);
      } finally {
        setLoading(false);
      }
    };

    loadRecommendations();
  }, [userId, responseId]);

  if (userId === null) {
    return <div>로그인 확인 중...</div>;
  }

  if (loading) {
    return <SkeletonLoader />;
  }

  if (error) {
    return (
      <div className="error-container">
        <p className="error-text">{error}</p>
      </div>
    );
  }

  if (!data || data.length === 0) {
    return (
      <div className="error-container">
        <p className="error-text">추천 결과가 없습니다.</p>
      </div>
    );
  }

  const handlePolicyClick = (policyId: number) => {
    router.push(`/policy/policy_detail/${policyId}`);
  };

  const handleRegionClick = (regionCode: string) => {
    router.push(`/recommendation/${userId}/${responseId}/${regionCode}`);
  };

  const handleBookmarkToggle = (policyId: number) => {
    setBookmarkedPolicyIds((prev) => {
      const isBookmarked = prev.includes(policyId);
      const updated = isBookmarked ? prev.filter((id) => id !== policyId) : [...prev, policyId];

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

  const handleViewAllPolicies = () => {
    router.push('/policy/totalPolicies');
  };

  const handleRetakeSurvey = () => {
    router.push('/survey/');
  };

  return (
    <div>
      <RecommendationList
        data={data}
        onPolicyClick={handlePolicyClick}
        onRegionClick={handleRegionClick}
        onBookmarkToggle={handleBookmarkToggle}
        bookmarkedPolicyIds={bookmarkedPolicyIds}
      />
      <FooterActions
        onViewAllPolicies={handleViewAllPolicies}
        onRetakeSurvey={handleRetakeSurvey}
      />
    </div>
  );
};

export default RecommendationDataLoader;

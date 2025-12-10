"use client";

import { getSido, regionFullPath, regionLabel, toSigungu5 } from "@/components/constants/region-map";
import { getMyRegionScores, getRegionScore } from "@/libs/api/mypage.api";
import type { MyRegionScoreDto, RegionScoreDto, UserProfileDto } from "@/types/api/mypage.types";
import { useQuery } from "@tanstack/react-query";
import { AnimatePresence, motion } from "framer-motion";
import { useEffect, useMemo, useState } from "react";
import styles from "../MyPageLayout.module.css";

// 점수 프로그레스 바
function ScoreProgress({ score, max = 300 }: { score: number; max?: number }) {
  const pct = Math.min(Math.round((score / max) * 100), 100);
  return (
    <div className={styles.scoreProgressWrap} aria-label={`점수 달성률: ${pct}%`}>
      <div className={styles.scoreBarBg}>
        <motion.div
          className={styles.scoreBarFill}
          initial={{ width: 0 }}
          animate={{ width: `${pct}%` }}
          transition={{ duration: 0.85, type: "spring" }}
        />
      </div>
      <span className={styles.scorePercent}>{pct}%</span>
    </div>
  );
}

// 활동 내역을 분석해서 카테고리별 집계
function analyzeActivityHistory(scoreHistory: RegionScoreDto['scoreHistory']) {
  if (!scoreHistory || scoreHistory.length === 0) {
    return {
      postCount: 0,
      replyCount: 0,
      commentCount: 0,
      surveyCount: 0,
      postScore: 0,
      replyScore: 0,
      commentScore: 0,
      surveyScore: 0
    };
  }

  let postCount = 0;
  let replyCount = 0;
  let commentCount = 0;
  let surveyCount = 0;
  let postScore = 0;
  let replyScore = 0;
  let commentScore = 0;
  let surveyScore = 0;

  scoreHistory.forEach(history => {
    const reason = history.reason.toLowerCase();
    const delta = history.delta || 0;

    // 게시글 작성 (18점)
    if (reason === 'post' || reason.includes('post') || reason.includes('게시글')) {
      postCount++;
      postScore += delta;
    }
    // 답글 작성 (2점) - REPLY
    else if (reason === 'reply') {
      replyCount++;
      replyScore += delta;
    }
    // 댓글 작성 (3점) - COMMENT
    else if (reason === 'comment' || reason.includes('comment') || reason.includes('댓글')) {
      commentCount++;
      commentScore += delta;
    }
    // 설문 관련 (5점)
    else if (reason.includes('survey') || reason.includes('설문') || reason.includes('응답')) {
      surveyCount++;
      surveyScore += delta;
    }
    // 기타 활동은 댓글로 분류 (POLICY_LIKE 등)
    else {
      commentCount++;
      commentScore += delta;
    }
  });

  return {
    postCount,
    replyCount,
    commentCount,
    surveyCount,
    postScore,
    replyScore,
    commentScore,
    surveyScore
  };
}

// 활동 타입별 표시 정보
function getActivityDisplayInfo(reason: string) {
  const reasonLower = reason.toLowerCase();
  
  if (reasonLower === 'post' || reasonLower.includes('post') || reasonLower.includes('게시글')) {
    return { icon: '📄', description: '게시글 작성' };
  }
  if (reasonLower === 'reply') {
    return { icon: '💬', description: '답글 작성' };
  }
  if (reasonLower === 'comment' || reasonLower.includes('comment') || reasonLower.includes('댓글')) {
    return { icon: '💭', description: '댓글 작성' };
  }
  if (reasonLower.includes('survey') || reasonLower.includes('설문') || reasonLower.includes('응답')) {
    return { icon: '📋', description: '설문 응답' };
  }
  if (reasonLower === 'policy_like') {
    return { icon: '👍', description: '정책 좋아요' };
  }
  
  // 기타
  return { icon: '🔗', description: reason };
}

// 시도 → 항목 리스트로 그룹화(UX: optgroup)
function groupBySido(items: MyRegionScoreDto[]) {
  const map = new Map<string, MyRegionScoreDto[]>();
  for (const it of items) {
    const sd = getSido(it.regionId) || "기타";
    if (!map.has(sd)) map.set(sd, []);
    map.get(sd)!.push(it);
  }
  // 각 그룹 정렬: 라벨 기준
  for (const [k, arr] of map) {
    arr.sort((a, b) => {
      const la = regionLabel(a.regionId);
      const lb = regionLabel(b.regionId);
      return la.localeCompare(lb, "ko");
    });
    map.set(k, arr);
  }
  // 시도명 정렬
  return Array.from(map.entries()).sort((a, b) => a[0].localeCompare(b[0], "ko"));
}

// 상세 fetch - 백엔드 Long 타입 호환
function useDetailRegionScore(regionId?: number | string) {
  return useQuery<RegionScoreDto>({
    queryKey: ["region-score", regionId],
    // 백엔드에서 int regionId를 받으므로 number로 캐스팅
    queryFn: () => {
      if (regionId === undefined || regionId === null || regionId === "") {
        throw new Error("Invalid regionId");
      }
      const numericRegionId = typeof regionId === 'string' ? parseInt(regionId, 10) : regionId;
      return getRegionScore(numericRegionId);
    },
    enabled: regionId !== undefined && regionId !== null && regionId !== "",
    staleTime: 1000 * 60 * 5,
    retry: 2,
  });
}

export default function RegionScorePanel({ user }: { user: UserProfileDto }) {
  const { data: regionList = [], isLoading, isError, refetch } = useQuery<MyRegionScoreDto[]>({
    queryKey: ["my-region-scores"],
    queryFn: getMyRegionScores,
    staleTime: 1000 * 60 * 10,
    retry: 2,
  });

  const [regionId, setRegionId] = useState<number | string | undefined>(undefined);
  const [showActivityHistory, setShowActivityHistory] = useState(false);

  useEffect(() => {
    if (regionList.length && regionId === undefined) {
      // 첫 번째 지역의 regionId를 number로 설정
      setRegionId(regionList[0].regionId);
    }
  }, [regionList, regionId]);

  const groups = useMemo(() => groupBySido(regionList), [regionList]);

  const { data: detail, isLoading: detailLoading, error: detailError } = useDetailRegionScore(regionId);

  if (isLoading) {
    return (
      <section className={styles.panelWrap} aria-busy="true">
        <div className={styles.scoreSkeleton} />
      </section>
    );
  }

  if (isError || !regionList.length) {
    return (
      <section className={styles.panelWrap} role="alert">
        <div className={styles.errorMsg}>⚠️ 지역 점수 정보를 불러올 수 없습니다.</div>
        <button className={styles.retryBtn} onClick={() => refetch()}>
          🔄 다시 시도
        </button>
      </section>
    );
  }

  return (
    <section className={styles.panelWrap} aria-labelledby="regionScorePanelTitle">
      <h2 id="regionScorePanelTitle" className={styles.title}>
        내 지역별 점수 <span className={styles.titleEmoji} aria-hidden>🏅</span>
      </h2>

      {/* UX 향상: 시도(optgroup)별 그룹 + 라벨/툴팁 */}
      <div className={styles.regionSelectRow}>
        <label htmlFor="regionSelect" className={styles.regionSelectLabel}>
          지역 선택
        </label>

        <select
          id="regionSelect"
          className={styles.regionSelect}
          value={String(regionId ?? "")}
          onChange={(e) => {
            const picked = e.target.value;
            const found = regionList.find((r) => String(r.regionId) === picked);
            // number 타입으로 설정하여 백엔드 호환성 보장
            setRegionId(found ? Number(found.regionId) : Number(picked));
          }}
          aria-label="내 점수 지역 선택 (시도별 그룹)"
        >
          {groups.map(([sido, items]) => (
            <optgroup key={sido} label={sido}>
              {items.map((r) => {
                const label = regionLabel(r.regionId);
                const full = regionFullPath(r.regionId, " · ");
                return (
                  <option
                    key={String(r.regionId)}
                    value={String(r.regionId)}
                    title={full}
                  >
                    {label} ({r.score}점)
                  </option>
                );
              })}
            </optgroup>
          ))}
        </select>
      </div>

      {/* 상세 영역 */}
      <AnimatePresence mode="wait">
        {detailLoading ? (
          <motion.div
            key="loading"
            className={styles.scoreDetailSkeleton}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            aria-busy="true"
          />
        ) : detailError ? (
          <motion.div
            key="error"
            className={styles.errorInfo}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            role="alert"
          >
            <span>⚠️ 점수 상세 정보를 불러올 수 없습니다.</span>
            <button 
              className={styles.retryBtn} 
              onClick={() => refetch()}
              style={{ marginTop: '8px' }}
            >
              🔄 다시 시도
            </button>
          </motion.div>
        ) : detail ? (
          <motion.div
            key={String(detail.regionId)}
            className={styles.scoreSummary}
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            transition={{ duration: 0.45, type: "spring" }}
            tabIndex={0}
            aria-label={`${regionLabel(detail?.regionId, detail?.regionName)} 지역 점수 상세`}
          >
            <div className={styles.regionTitle}>
              <span className={styles.regionBadge}>
                #{toSigungu5(detail.regionId) || String(detail.regionId)}
              </span>
              <span className={styles.regionName}>
                {regionLabel(detail?.regionId, detail?.regionName)}
              </span>
            </div>

            <ScoreProgress score={detail.score} />

            {/* 활동 분석 및 점수 표시 */}
            {(() => {
              const analyzed = analyzeActivityHistory(detail.scoreHistory);
              
              return (
                <ul className={styles.scoreList} aria-live="polite">
                  <li>
                    <span>📄 게시글 작성</span>
                    <span>
                      <b>{analyzed.postCount}</b>건 × 18점 = <b>{analyzed.postScore}</b>점
                    </span>
                  </li>
                  <li>
                    <span>💬 답글 작성</span>
                    <span>
                      <b>{analyzed.replyCount}</b>건 × 2점 = <b>{analyzed.replyScore}</b>점
                    </span>
                  </li>
                  <li>
                    <span>💭 댓글 작성</span>
                    <span>
                      <b>{analyzed.commentCount}</b>건 × 3점 = <b>{analyzed.commentScore}</b>점
                    </span>
                  </li>
                  <li>
                    <span>📋 설문 응답</span>
                    <span>
                      <b>{analyzed.surveyCount}</b>건 × 5점 = <b>{analyzed.surveyScore}</b>점
                    </span>
                  </li>
                  <li className={styles.totalScoreRow}>
                    <span>총점</span>
                    <span className={styles.totalScore}>{detail.score}점</span>
                  </li>
                </ul>
              );
            })()}

            {/* 활동 내역 토글 버튼 - 깔끔한 노란색 스타일 */}
            <button
              className={styles.activityToggleBtn}
              onClick={() => setShowActivityHistory(!showActivityHistory)}
              aria-expanded={showActivityHistory}
              aria-controls="activityHistory"
              style={{
                background: '#ffc107',
                border: 'none',
                borderRadius: '8px',
                padding: '12px 20px',
                width: '100%',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: '14px',
                fontWeight: '600',
                color: '#ffffff',
                cursor: 'pointer',
                transition: 'all 0.2s ease',
                marginTop: '16px',
                letterSpacing: '0.5px'
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.background = '#ffb300';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.background = '#ffc107';
              }}
              onMouseDown={(e) => {
                e.currentTarget.style.transform = 'scale(0.98)';
              }}
              onMouseUp={(e) => {
                e.currentTarget.style.transform = 'scale(1)';
              }}
            >
              활동 내역 {showActivityHistory ? '숨기기' : '보기'}
            </button>

            {/* 활동 내역 목록 - 접힌 상태 */}
            <AnimatePresence>
              {showActivityHistory && (
                <motion.div
                  id="activityHistory"
                  className={styles.activityHistory}
                  initial={{ height: 0, opacity: 0 }}
                  animate={{ height: "auto", opacity: 1 }}
                  exit={{ height: 0, opacity: 0 }}
                  transition={{ duration: 0.3, ease: "easeInOut" }}
                  style={{ overflow: "hidden" }}
                >
                  <h3 className={styles.activityTitle}>최근 활동 내역</h3>
                  <ul className={styles.activityList}>
                    {detail.scoreHistory && detail.scoreHistory.length > 0 ? (
                      detail.scoreHistory.slice(0, 10).map((history, index) => {
                        const displayInfo = getActivityDisplayInfo(history.reason);
                        return (
                          <li key={`${history.date}-${index}`} className={styles.activityItem}>
                            <span className={styles.activityIcon}>
                              {displayInfo.icon}
                            </span>
                            <span className={styles.activityDescription}>
                              {displayInfo.description}
                            </span>
                            <span className={styles.activityDate}>{history.date}</span>
                            <span className={`${styles.activityScore} ${history.delta >= 0 ? styles.positive : styles.negative}`}>
                              {history.delta >= 0 ? '+' : ''}{history.delta}점
                            </span>
                          </li>
                        );
                      })
                    ) : (
                      <li className={styles.emptyActivity}>
                        <span className={styles.emptyIcon}>📭</span>
                        <span>아직 활동 내역이 없습니다.</span>
                      </li>
                    )}
                  </ul>
                </motion.div>
              )}
            </AnimatePresence>
          </motion.div>
        ) : (
          <motion.div
            key="empty"
            className={styles.emptyInfo}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            aria-live="polite"
          >
            <span>이 지역에 기록된 점수가 없습니다.</span>
          </motion.div>
        )}
      </AnimatePresence>
    </section>
  );
}
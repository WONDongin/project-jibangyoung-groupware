"use client";

import {
  getRecommendationCounts,
  getUserRecommendation,
  recommendPost,
} from "@/libs/api/community/community.api";
import { useAuthStore } from "@/store/authStore";
import React, { useCallback, useEffect, useState } from "react";
import styles from "../../../Community.module.css";

interface RecommendationButtonsProps {
  postId: number;
}

const RecommendationButtons: React.FC<RecommendationButtonsProps> = ({
  postId,
}) => {
  const [selectedType, setSelectedType] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [recommendationCounts, setRecommendationCounts] = useState<
    Record<string, number>
  >({
    쏠쏠정보: 0,
    흥미진진: 0,
    공감백배: 0,
    분석탁월: 0,
    후속강추: 0,
  });
  const user = useAuthStore((state) => state.user);

  const recommendationTypes = [
    "쏠쏠정보",
    "흥미진진",
    "공감백배",
    "분석탁월",
    "후속강추",
  ];

  const loadRecommendationData = useCallback(async () => {
    try {
      const counts = await getRecommendationCounts(postId);
      setRecommendationCounts(counts);
    } catch (error) {
      console.error("추천 개수 조회 실패:", error);
    }
  }, [postId]);

  const loadUserRecommendation = useCallback(async () => {
    try {
      const currentRecommendation = await getUserRecommendation(postId);
      setSelectedType(currentRecommendation);
    } catch (error: any) {
      // 401 에러 (인증 실패)는 조용히 처리하여 리다이렉트 방지
      if (error?.response?.status === 401 || error?.message?.includes('401')) {
        console.log("비로그인 사용자 - 추천 상태 조회 생략");
        return;
      }
      console.error("추천 상태 조회 실패:", error);
    }
  }, [postId]);

  useEffect(() => {
    loadRecommendationData();
  }, [loadRecommendationData]);

  useEffect(() => {
    if (user) {
      loadUserRecommendation();
    }
  }, [user, loadUserRecommendation]);

  const handleRecommend = async (type: string) => {
    if (!user) {
      alert("로그인이 필요합니다.");
      return;
    }

    setIsLoading(true);
    try {
      await recommendPost(postId, type);

      // 같은 타입 클릭 시 추천 취소, 다른 타입 클릭 시 변경
      const newSelectedType = selectedType === type ? null : type;
      setSelectedType(newSelectedType);

      // 추천 개수 다시 로드
      await loadRecommendationData();

      if (newSelectedType === null) {
        alert(`${type} 추천이 취소되었습니다.`);
      } else {
        alert(`${type} 추천이 완료되었습니다!`);
      }
    } catch (error) {
      console.error("추천 실패:", error);
      alert("추천에 실패했습니다.");
    } finally {
      setIsLoading(false);
    }
  };

  const handleLoginAlert = () => {
    alert("로그인한 사용자만 추천할 수 있습니다.");
  };

  if (!user) {
    return (
      <div style={{ textAlign: "center", marginTop: "20px" }}>
        <div
          style={{
            display: "flex",
            justifyContent: "center",
            gap: "10px",
            marginTop: "10px",
          }}
        >
          <div style={{ display: "inline-block", margin: "0 10px" }}>
            <button className={styles.recommendButton} onClick={handleLoginAlert}>
              쏠쏠정보 ({recommendationCounts["쏠쏠정보"]})
            </button>
          </div>
          <div style={{ display: "inline-block", margin: "0 10px" }}>
            <button className={styles.recommendButton} onClick={handleLoginAlert}>
              흥미진진 ({recommendationCounts["흥미진진"]})
            </button>
          </div>
          <div style={{ display: "inline-block", margin: "0 10px" }}>
            <button className={styles.recommendButton} onClick={handleLoginAlert}>
              공감백배 ({recommendationCounts["공감백배"]})
            </button>
          </div>
          <div style={{ display: "inline-block", margin: "0 10px" }}>
            <button className={styles.recommendButton} onClick={handleLoginAlert}>
              분석탁월 ({recommendationCounts["분석탁월"]})
            </button>
          </div>
          <div style={{ display: "inline-block", margin: "0 10px" }}>
            <button className={styles.recommendButton} onClick={handleLoginAlert}>
              후속강추 ({recommendationCounts["후속강추"]})
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div style={{ textAlign: "center", marginTop: "20px" }}>
      <div style={{ marginBottom: "10px" }}></div>
      <div
        style={{
          display: "flex",
          justifyContent: "center",
          gap: "10px",
          flexWrap: "wrap",
        }}
      >
        <div style={{ display: "inline-block", margin: "0 5px" }}>
          <button
            onClick={() => handleRecommend("쏠쏠정보")}
            disabled={isLoading}
            className={`${styles.recommendButton} ${
              selectedType === "쏠쏠정보" ? styles.recommendButtonActive : ""
            }`}
          >
            쏠쏠정보 ({recommendationCounts["쏠쏠정보"]}){" "}
            {selectedType === "쏠쏠정보" && "✓"}
          </button>
        </div>
        <div style={{ display: "inline-block", margin: "0 5px" }}>
          <button
            onClick={() => handleRecommend("흥미진진")}
            disabled={isLoading}
            className={`${styles.recommendButton} ${
              selectedType === "흥미진진" ? styles.recommendButtonActive : ""
            }`}
          >
            흥미진진 ({recommendationCounts["흥미진진"]}){" "}
            {selectedType === "흥미진진" && "✓"}
          </button>
        </div>
        <div style={{ display: "inline-block", margin: "0 5px" }}>
          <button
            onClick={() => handleRecommend("공감백배")}
            disabled={isLoading}
            className={`${styles.recommendButton} ${
              selectedType === "공감백배" ? styles.recommendButtonActive : ""
            }`}
          >
            공감백배 ({recommendationCounts["공감백배"]}){" "}
            {selectedType === "공감백배" && "✓"}
          </button>
        </div>
        <div style={{ display: "inline-block", margin: "0 5px" }}>
          <button
            onClick={() => handleRecommend("분석탁월")}
            disabled={isLoading}
            className={`${styles.recommendButton} ${
              selectedType === "분석탁월" ? styles.recommendButtonActive : ""
            }`}
          >
            분석탁월 ({recommendationCounts["분석탁월"]}){" "}
            {selectedType === "분석탁월" && "✓"}
          </button>
        </div>
        <div style={{ display: "inline-block", margin: "0 5px" }}>
          <button
            onClick={() => handleRecommend("후속강추")}
            disabled={isLoading}
            className={`${styles.recommendButton} ${
              selectedType === "후속강추" ? styles.recommendButtonActive : ""
            }`}
          >
            후속강추 ({recommendationCounts["후속강추"]}){" "}
            {selectedType === "후속강추" && "✓"}
          </button>
        </div>
      </div>
    </div>
  );
};

export default RecommendationButtons;

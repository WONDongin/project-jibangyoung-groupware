"use client";

import { patchMyProfile } from "@/libs/api/mypage.api";
import type { UserProfileDto } from "@/types/api/mypage.types";
import { useMutation } from "@tanstack/react-query";
import Image from "next/image";
import { useState } from "react";
import { REGION_INDEX, regionLabel } from "../../../components/constants/region-map"; // 관심지역 상수 import
import styles from "../MyPageLayout.module.css";

// status 매핑
export const STATUS_MAP: Record<UserProfileDto["status"], string> = {
  ACTIVE: "활동중",
  DEACTIVATED: "비활성",
  LOCKED: "잠김",
  PENDING: "대기",
};

// REGION_INDEX에서 지역 옵션 생성 (구군2 > 구군1 > 시도 순으로 표시)
const REGION_OPTIONS = Object.entries(REGION_INDEX)
  .filter(([code, data]) => code !== "99999") // 전국 제외
  .map(([code, data]) => ({
    code,
    label: regionLabel(code), // 구군2 > 구군1 > 시도 순으로 라벨 반환
  }))
  .filter(item => item.label.trim() !== "") // 빈 라벨 제외
  .sort((a, b) => a.label.localeCompare(b.label)); // 가나다순 정렬

const MAX_REGION_SELECT = 3;

interface ProfileEditPanelProps {
  user: UserProfileDto | null;
}

function ProfileSkeleton() {
  return (
    <section
      className={styles.mypageProfileCard + " animate-pulse"}
      aria-busy="true"
    >
      <div className={styles.mypageProfileInfo}>
        <div
          className={styles.mypageProfileNickname}
          style={{
            width: 92,
            background: "#ececec",
            borderRadius: 7,
            height: 27,
          }}
        />
        {[...Array(5)].map((_, i) => (
          <div
            key={i}
            className={styles.mypageProfileRow}
            style={{
              width: 160,
              background: "#eee",
              borderRadius: 7,
              height: 17,
              margin: "9px 0",
            }}
          />
        ))}
      </div>
      <div
        style={{
          width: 110,
          height: 110,
          background: "#f4f4f4",
          borderRadius: "50%",
        }}
      />
    </section>
  );
}

export default function ProfileEditPanel({ user }: ProfileEditPanelProps) {
  const [nickname, setNickname] = useState(user?.nickname ?? "");
  const [phone, setPhone] = useState(user?.phone ?? "");
  const [profileImageUrl, setProfileImageUrl] = useState(user?.profileImageUrl ?? "");
  const [imgError, setImgError] = useState(false);
  const [success, setSuccess] = useState(false);
  
  // 관심지역 관련 상태
  const [regionQuery, setRegionQuery] = useState("");
  const [regionMulti, setRegionMulti] = useState<string[]>(() => {
    // 기존 user.region 문자열을 파싱하여 코드 배열로 변환
    if (!user?.region) return [];
    const regionNames = user.region.split(",").map(name => name.trim());
    const codes: string[] = [];
    
    regionNames.forEach(name => {
      const found = REGION_OPTIONS.find(option => option.label === name);
      if (found) codes.push(found.code);
    });
    
    return codes;
  });

  const { mutate, isPending, error } = useMutation({
    mutationFn: async () => {
      if (!user?.id) throw new Error("로그인 정보가 올바르지 않습니다.");
      
      // 선택한 지역 코드 → 이름(라벨)로 변환
      const regionNames = regionMulti
        .map((code) => regionLabel(code))
        .filter((name) => !!name && name.trim() !== "")
        .join(",");
      
      await patchMyProfile(user.id, { 
        nickname, 
        phone, 
        region: regionNames, // 관심지역을 이름으로 API에 전달
        profileImageUrl 
      });
    },
    onSuccess: () => setSuccess(true),
    onError: () => setSuccess(false),
  });

  if (!user) return <ProfileSkeleton />;

  const statusLabel =
    user.status && STATUS_MAP[user.status as keyof typeof STATUS_MAP]
      ? STATUS_MAP[user.status as keyof typeof STATUS_MAP]
      : user.status ?? "-";

  // 지역 실시간 필터링
  const filteredRegionOptions = REGION_OPTIONS.filter(
    (region) =>
      region.label
        .toLowerCase()
        .replace(/ /g, "")
        .includes(regionQuery.trim().toLowerCase().replace(/ /g, "")) &&
      !regionMulti.includes(region.code)
  );

  return (
    <section className={styles.mypageProfileCard} aria-label="프로필 정보">
      <div className={styles.mypageProfileInfo}>
        <div className={styles.mypageProfileNickname} tabIndex={0}>
          <input
            className={styles.profileEditInput}
            value={nickname}
            onChange={(e) => setNickname(e.target.value)}
            maxLength={16}
            aria-label="닉네임"
            disabled={isPending}
          />
          {success && (
            <span className={styles.profileSaveToast}>✔ 저장됨</span>
          )}
        </div>
        <div className={styles.mypageProfileRow}>
          <strong>아이디</strong>
          <span>{user.username}</span>
        </div>
        <div className={styles.mypageProfileRow}>
          <strong>이메일</strong>
          <span>{user.email}</span>
        </div>
        <div className={styles.mypageProfileRow}>
          <strong>전화번호</strong>
          <input
            className={styles.profileEditInput}
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            maxLength={16}
            aria-label="전화번호"
            disabled={isPending}
          />
        </div>
        <div className={styles.mypageProfileRow}>
          <strong>상태</strong>
          <span
            className={
              styles.mypageProfileStatus +
              " " +
              styles[
                user.status
                  ? `status_${user.status.toLowerCase()}`
                  : "status_unknown"
              ]
            }
          >
            {statusLabel}
          </span>
        </div>
        
        {/* 관심지역 편집 섹션 */}
        <div className={styles.mypageProfileRow} style={{ flexDirection: "column", alignItems: "flex-start" }}>
          <strong style={{ marginBottom: "8px" }}>관심지역</strong>
          
          {/* 지역 검색 입력 */}
          <div style={{ width: "100%", marginBottom: "8px" }}>
            <div style={{ fontSize: "0.85em", marginBottom: "4px", color: "#666", fontWeight: 500 }}>
              지역명 검색 (최대 {MAX_REGION_SELECT}개)
            </div>
            <input
              type="text"
              value={regionQuery}
              onChange={(e) => setRegionQuery(e.target.value)}
              className={styles.profileEditInput}
              placeholder="예: 서울, 부산 등 입력"
              disabled={isPending || regionMulti.length >= MAX_REGION_SELECT}
              aria-label="지역명 검색"
              style={{ 
                width: "100%", 
                fontSize: "0.9em",
                padding: "6px 8px"
              }}
            />
          </div>

          {/* 검색 결과 체크박스 */}
          {regionQuery.trim() && (
            <div style={{ 
              width: "100%", 
              marginBottom: "8px",
              maxHeight: "120px",
              overflowY: "auto",
              border: "1px solid #e0e0e0",
              borderRadius: "4px",
              padding: "6px"
            }}>
              {filteredRegionOptions.length === 0 ? (
                <div style={{ color: "#999", fontSize: "0.85em", padding: "4px" }}>
                  검색 결과 없음
                </div>
              ) : (
                filteredRegionOptions.map((region) => {
                  const isChecked = regionMulti.includes(region.code);
                  const isDisabled = !isChecked && regionMulti.length >= MAX_REGION_SELECT;
                  return (
                    <label 
                      key={region.code} 
                      style={{ 
                        display: "flex", 
                        alignItems: "center", 
                        padding: "4px 2px",
                        cursor: isDisabled ? "not-allowed" : "pointer",
                        opacity: isDisabled ? 0.5 : 1,
                        fontSize: "0.85em"
                      }}
                    >
                      <input
                        type="checkbox"
                        value={region.code}
                        checked={isChecked}
                        disabled={isDisabled || isPending}
                        onChange={(e) => {
                          const selected = regionMulti;
                          const next = e.target.checked
                            ? [...selected, region.code]
                            : selected.filter((r) => r !== region.code);
                          setRegionMulti(next);
                        }}
                        style={{ display: "none" }}
                      />
                      <span
                        style={{
                          display: "inline-block",
                          width: 14,
                          height: 14,
                          border: "1.5px solid #ffe140",
                          borderRadius: 3,
                          background: isChecked ? "#ffe140" : "#fff",
                          marginRight: 6,
                          transition: "background 0.15s"
                        }}
                      >
                        {isChecked && (
                          <span style={{
                            display: "block",
                            width: 8,
                            height: 8,
                            margin: "2px auto",
                            borderRadius: 2,
                            background: "#20c37b"
                          }} />
                        )}
                      </span>
                      {region.label}
                    </label>
                  );
                })
              )}
            </div>
          )}

          {/* 선택된 지역 태그 */}
          <div style={{ 
            width: "100%", 
            display: "flex", 
            flexWrap: "wrap", 
            gap: "4px",
            marginBottom: "4px"
          }}>
            {regionMulti.map((code) => (
              <span 
                key={code} 
                style={{
                  display: "inline-flex",
                  alignItems: "center",
                  background: "#f0f8ff",
                  border: "1px solid #cde7ff",
                  borderRadius: "12px",
                  padding: "2px 8px",
                  fontSize: "0.8em",
                  color: "#2563eb"
                }}
              >
                {regionLabel(code)}
                <button
                  type="button"
                  onClick={() =>
                    setRegionMulti(prev => prev.filter((item) => item !== code))
                  }
                  style={{
                    marginLeft: "4px",
                    background: "none",
                    border: "none",
                    color: "#2563eb",
                    cursor: "pointer",
                    fontSize: "1.1em",
                    lineHeight: 1,
                    padding: 0
                  }}
                  aria-label={`${regionLabel(code)} 제거`}
                  tabIndex={0}
                >
                  ×
                </button>
              </span>
            ))}
          </div>
          
          {regionMulti.length >= MAX_REGION_SELECT && (
            <div style={{ color: "#d89105", fontSize: "0.8em" }}>
              최대 {MAX_REGION_SELECT}개까지 선택할 수 있습니다.
            </div>
          )}
        </div>

        <button
          className={styles.profileEditSaveBtn}
          onClick={() => mutate()}
          disabled={isPending}
        >
          저장
        </button>
        {error && (
          <div className={styles.profileEditError}>
            저장 실패! 다시 시도해 주세요.
          </div>
        )}
      </div>
      <Image
        src="/default-profile.webp"
        alt="프로필 이미지"
        width={110}
        height={110}
        className={styles.mypageProfileBear}
        priority
      />
    </section>
  );
}
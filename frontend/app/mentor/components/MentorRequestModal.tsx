import { CommonModal, ModalButton } from "@/app/admin/components/AdminModal";
import { RegionLookup } from "@/app/admin/hooks/useAdminRegion";
import { AdMentorRequest } from "@/types/api/adMentorRequest";
import { useMemo, useState } from "react";
import styles from "../../admin/AdminPage.module.css";

interface MentorRequestModalProps {
  data: AdMentorRequest;
  userRole?: string;
  onRequest?: () => void;
  onFirstApprove?: () => void;
  onSecondApprove?: () => void; // MENTOR_A (2차 승인)
  onFinalApprove?: () => void; // ADMIN (최종 승인 = FINAL_APPROVED)
  onReject: (reason: string) => void;
  onClose: () => void;
  regionOptions: { code: number; name: string }[];
  regionMap: Map<number, RegionLookup>;
}

export function MentorRequestModal({
  data,
  userRole,
  regionOptions,
  regionMap,
  onRequest,
  onFirstApprove,
  onSecondApprove,
  onFinalApprove,
  onReject,
  onClose,
}: MentorRequestModalProps) {
  // 반려 모드 (사유 입력)
  const [isRejecting, setIsRejecting] = useState(false);
  const [rejectReason, setRejectReason] = useState("");

  // 지역명
  const regionName = useMemo(() => {
    const id = Number(data.regionId);
    const exact = regionMap.get(id);
    if (exact) {
      const sido = (exact.sido ?? "").trim();
      const gu = (exact.guGun ?? "").trim();
      return gu && gu !== sido ? `${sido} ${gu}` : sido || String(id);
    }
    const parent = regionMap.get(Math.floor(id / 1000) * 1000);
    return parent?.sido ?? String(id);
  }, [regionMap, data.regionId]);

  const resetReject = () => {
    setIsRejecting(false);
    setRejectReason("");
  };

  const handleRejectConfirm = () => {
    const reason = rejectReason.trim();
    if (!reason) {
      alert("반려 사유를 입력해 주세요.");
      return;
    }
    onReject(reason);
    resetReject();
  };

  // 역할/상태별 기본 버튼
  const getBaseButtons = (): ModalButton[] => {
    // ADMIN: FIRST_APPROVED, REQUESTED, PENDING, SECOND_APPROVED → 최종 승인, 반려
    if (
      userRole === "ADMIN" &&
      ["FIRST_APPROVED", "REQUESTED", "PENDING", "SECOND_APPROVED"].includes(
        data.status
      )
    ) {
      return [
        onFinalApprove && {
          label: "최종 승인",
          onClick: () => onFinalApprove?.(),
          type: "primary",
        },
        {
          label: "반려",
          onClick: () => setIsRejecting(true),
          type: "danger",
        },
      ].filter(Boolean) as ModalButton[];
    }

    // MENTOR_A: FIRST_APPROVED, REQUESTED, PENDING → 2차 승인, 반려
    if (
      userRole === "MENTOR_A" &&
      ["FIRST_APPROVED", "REQUESTED", "PENDING"].includes(data.status)
    ) {
      return [
        onSecondApprove && {
          label: "2차 승인",
          onClick: () => onSecondApprove?.(),
          type: "secondary",
        },
        {
          label: "반려",
          onClick: () => setIsRejecting(true),
          type: "danger",
        },
      ].filter(Boolean) as ModalButton[];
    }

    // MENTOR_A: SECOND_APPROVED → 2차 승인 취소(= 1차 승인 API 호출)
    if (userRole === "MENTOR_A" && data.status === "SECOND_APPROVED") {
      return [
        {
          label: "2차 승인 취소",
          onClick: () => onFirstApprove?.(),
          type: "warning",
        },
        {
          label: "반려",
          onClick: () => setIsRejecting(true),
          type: "danger",
        },
      ];
    }

    // MENTOR_B: REQUESTED, PENDING → 1차 승인, 반려
    if (
      userRole === "MENTOR_B" &&
      ["REQUESTED", "PENDING"].includes(data.status)
    ) {
      return [
        onFirstApprove && {
          label: "1차 승인",
          onClick: () => onFirstApprove?.(),
          type: "info",
        },
        {
          label: "반려",
          onClick: () => setIsRejecting(true),
          type: "danger",
        },
      ].filter(Boolean) as ModalButton[];
    }

    // MENTOR_B: FIRST_APPROVED → 1차 승인 취소
    if (userRole === "MENTOR_B" && data.status === "FIRST_APPROVED") {
      return [
        {
          label: "1차 승인 취소",
          onClick: () => onRequest?.(),
          type: "warning",
        },
        {
          label: "반려",
          onClick: () => setIsRejecting(true),
          type: "danger",
        },
      ];
    }

    // MENTOR_C: PENDING → 승인요청
    if (userRole === "MENTOR_C" && data.status === "PENDING") {
      return [
        onRequest && {
          label: "승인 요청",
          onClick: () => onRequest?.(),
          type: "warning",
        },
      ].filter(Boolean) as ModalButton[];
    }

    return [];
  };

  // 반려 모드일 때 버튼
  const getButtons = (): ModalButton[] => {
    if (isRejecting) {
      return [
        {
          label: "반려 확정",
          onClick: () => handleRejectConfirm(),
          type: "danger",
        },
        { label: "취소", onClick: () => resetReject(), type: "secondary" },
      ];
    }
    return getBaseButtons();
  };

  // 이메일 마스킹 함수
  const maskEmail = (email?: string) => {
    if (!email) return "";
    const [local, domain] = email.split("@");
    if (!domain) return email;

    const maskedLocal =
      local.length <= 3
        ? local[0] + "*".repeat(local.length - 1)
        : local.slice(0, 3) + "*".repeat(local.length - 3);

    const [domainName, domainExt] = domain.split(".");
    const maskedDomain =
      domainName.length <= 3
        ? domainName[0] + "*".repeat(domainName.length - 1)
        : domainName.slice(0, 3) + "*".repeat(domainName.length - 3);

    return `${maskedLocal}@${maskedDomain}.${domainExt || ""}`;
  };

  return (
    <CommonModal
      title="멘토 신청 상세"
      content={
        <div className={styles.modalGrid}>
          <div className={styles.modalRow}>
            <span className={styles.modalLabel}>ID</span>
            <span className={styles.modalValue}>{data.userName}</span>
          </div>

          <div className={styles.modalRow}>
            <span className={styles.modalLabel}>이메일</span>
            <span className={styles.modalValue}>
              {maskEmail(data.userEmail)}
            </span>
          </div>

          <div className={styles.modalRow}>
            <span className={styles.modalLabel}>담당지역</span>
            <span className={styles.modalValue}>{regionName}</span>
          </div>

          <div className={styles.modalRow}>
            <span className={styles.modalLabel}>행정기관여부</span>
            <span className={styles.modalValue}>
              {data.governmentAgency ? "예" : "아니오"}
            </span>
          </div>

          <div className={styles.modalBlock}>
            <span className={styles.modalLabel}>신청사유</span>
            <div className={styles.modalTextareaWrap}>
              <textarea
                className={styles.modalTextarea}
                value={data.reason || ""}
                readOnly
              />
            </div>
          </div>

          {data.status === "REJECTED" && (
            <div className={styles.modalBlock}>
              <span className={styles.modalLabel}>반려사유</span>
              <div className={styles.modalTextareaWrap}>
                <textarea
                  className={styles.modalTextarea}
                  value={data.rejectionReason || ""}
                  readOnly
                />
              </div>
            </div>
          )}

          {isRejecting && (
            <div className={styles.modalBlock}>
              <span
                className={`${styles.modalLabel} ${styles.rejectSectionTitle}`}
              >
                반려 사유 입력 (필수)
              </span>
              <div className={styles.modalTextareaWrap}>
                <textarea
                  className={styles.modalTextarea}
                  value={rejectReason}
                  onChange={(e) => setRejectReason(e.target.value)}
                  placeholder="반려 사유를 입력하세요"
                />
              </div>
            </div>
          )}
        </div>
      }
      buttons={getButtons()}
      onClose={() => {
        resetReject();
        onClose();
      }}
    />
  );
}

import { DownloadButton } from "@/app/admin/components/AdminDownBtn";
import { RegionLookup } from "@/app/admin/hooks/useAdminRegion";
import {
  AdMentorRequest,
  MentorRequestStatus,
} from "@/types/api/adMentorRequest";
import styles from "../../admin/AdminPage.module.css";

const STATUS_LABELS: Record<MentorRequestStatus, { label: string }> = {
  FINAL_APPROVED: { label: "최종 승인" },
  SECOND_APPROVED: { label: "2차 승인" },
  FIRST_APPROVED: { label: "1차 승인" },
  REQUESTED: { label: "승인 요청" },
  PENDING: { label: "승인 대기" },
  REJECTED: { label: "반려" },
};

const STATUS_CLASS: Record<MentorRequestStatus, string> = {
  FINAL_APPROVED: styles.statusFinalApproved,
  SECOND_APPROVED: styles.statusSecondApproved,
  FIRST_APPROVED: styles.statusFirstApproved,
  REQUESTED: styles.statusRequested,
  PENDING: styles.statusPending,
  REJECTED: styles.statusRejected,
};

export interface MentorRequestRowProps {
  app: AdMentorRequest;
  index: number;
  total: number;
  onClick: () => void;
  regionOptions: { code: number; name: string }[];
  regionMap: Map<number, RegionLookup>;
}

export function MentorRequestRow({
  app,
  index,
  total,
  onClick,
  regionOptions,
  regionMap,
}: MentorRequestRowProps) {
  const handleDownloadClick = (e: React.MouseEvent) => e.stopPropagation();

  const regionName = (() => {
    const exact = regionMap.get(app.regionId);
    if (exact) {
      const sido = (exact.sido ?? "").trim();
      const gu = (exact.guGun ?? "").trim();
      return gu && gu !== sido ? `${sido} ${gu}` : sido || String(app.regionId);
    }
    const parent = regionMap.get(Math.floor(app.regionId / 1000) * 1000);
    return parent?.sido ?? String(app.regionId);
  })();

  const statusLabel =
    STATUS_LABELS[app.status as MentorRequestStatus]?.label ?? app.status;
  const statusClass = STATUS_CLASS[app.status as MentorRequestStatus] ?? "";

  const reviewedByLabel = app.nickname ? app.nickname : "미지정";

  function formatDate(dateStr: string | null | undefined) {
    if (!dateStr) return "-";
    const date = new Date(dateStr);
    if (isNaN(date.getTime())) return dateStr.replace("T", " ").slice(0, 10);
    return date.toLocaleDateString("ko-KR", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
    });
  }

  return (
    <tr onClick={onClick} className={styles.rowClickable}>
      <td>{total - index}</td>
      <td>{app.userName}</td>
      <td>
        {app.reason
          ? app.reason.trim().length > 15
            ? app.reason.trim().slice(0, 15) + "..."
            : app.reason.trim()
          : ""}
      </td>
      <td>{regionName}</td>
      <td>{formatDate(app.createdAt)}</td>
      <td>{reviewedByLabel}</td>
      <td>
        <span className={`${styles.statusBadge} ${statusClass}`}>
          {statusLabel}
        </span>
      </td>
      <td>
        {app.documentUrl ? (
          <DownloadButton
            fileUrl={app.documentUrl}
            onClick={handleDownloadClick}
          />
        ) : (
          <span className={styles.textMuted}>없음</span>
        )}
      </td>
    </tr>
  );
}

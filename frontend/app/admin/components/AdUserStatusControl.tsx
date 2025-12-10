import { ChangeUserStatusPayload } from "@/types/api/adminUser";
import styles from "../AdminPage.module.css";

interface UserStatusControlProps {
  value: ChangeUserStatusPayload["status"];
  onChange: (value: ChangeUserStatusPayload["status"]) => void;
}

const USER_STATUS_OPTIONS: {
  value: ChangeUserStatusPayload["status"];
  label: string;
}[] = [
  { value: "ACTIVE", label: "활성" },
  { value: "DEACTIVATED", label: "비활성" },
  { value: "SUSPENDED", label: "정지" },
  { value: "DELETED", label: "삭제" },
];

export function AdUserStatusControl({
  value,
  onChange,
}: UserStatusControlProps) {
  return (
    <div className={styles.modalRow}>
      <span className={styles.modalLabel}>유저 상태</span>
      <span className={styles.modalValue}>
        <select
          className={styles.selectControl}
          value={value}
          onChange={(e) =>
            onChange(e.target.value as ChangeUserStatusPayload["status"])
          }
        >
          {USER_STATUS_OPTIONS.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
      </span>
    </div>
  );
}

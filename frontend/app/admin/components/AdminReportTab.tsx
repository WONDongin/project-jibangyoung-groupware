import { ReportTabType } from "@/types/api/adMentorReport";
import styles from "../AdminPage.module.css";
interface AdminReportTabProps {
  selectedType: ReportTabType;
  onSelectType: (type: ReportTabType) => void;
  tabOptions: ReportTabType[];
}

export function AdminReportTab({
  selectedType,
  onSelectType,
  tabOptions,
}: AdminReportTabProps) {
  return (
    <div className={styles.reportTabs}>
      {tabOptions.map((tab) => (
        <button
          key={tab}
          onClick={() => onSelectType(tab)}
          className={`${styles.reportTabButton} ${
            selectedType === tab ? styles.activeReportTab : ""
          }`}
        >
          {tab}
        </button>
      ))}
    </div>
  );
}

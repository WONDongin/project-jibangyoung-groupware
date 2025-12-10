"use client";

import { useRouter } from "next/navigation";
import styles from "../MentorInfo.module.css";

export default function MentorApplyButton() {
  const router = useRouter();

  const handleApplyClick = () => {
    router.push("/mentor/apply");
  };

  return (
    <div className={styles.buttonContainer}>
      <button 
        onClick={handleApplyClick} 
        className={styles.applyButton}
      >
        멘토 신청
      </button>
    </div>
  );
}
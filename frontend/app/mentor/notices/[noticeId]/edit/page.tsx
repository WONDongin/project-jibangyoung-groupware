"use client";

import { Suspense } from "react";
import styles from "../../write/MentorNoticeWrite.module.css";
import MentorNoticeEdit from "./components/MentorNoticeEdit";

interface Props {
  params: Promise<{
    noticeId: string;
  }>;
}

export default async function MentorNoticeEditPage({ params }: Props) {
  const noticeId = parseInt((await params).noticeId, 10);

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <h1 className={styles.title}>멘토 공지</h1>
        <hr className={styles.divider} />
      </div>
      
      <div className={styles.writeHeader}>
        <h2 className={styles.writeTitle}>공지글 수정</h2>
      </div>
      
      <Suspense fallback={<div>로딩 중...</div>}>
        <MentorNoticeEdit noticeId={noticeId} />
      </Suspense>
    </div>
  );
}
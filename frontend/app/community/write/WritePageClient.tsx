"use client";

import { Suspense } from "react";
import { useSearchParams } from "next/navigation";
import PopularPosts from "../[regionCode]/components/PopularPosts";
import styles from "../[regionCode]/components/BoardList.module.css";
import WriteForm from "./WriteForm";

function WritePageContent() {
  const searchParams = useSearchParams();
  const regionCode = searchParams.get("regionCode");

  return (
    <main className={styles.main}>
      <div className={styles.content}>
        <Suspense fallback={<div>폼 로딩 중...</div>}>
          <WriteForm />
        </Suspense>
      </div>
      <aside className={styles.sidebar}>
        {regionCode && <PopularPosts regionCode={regionCode} />}
      </aside>
    </main>
  );
}

export default function WritePageClient() {
  return (
    <Suspense fallback={<div>로딩 중...</div>}>
      <WritePageContent />
    </Suspense>
  );
}
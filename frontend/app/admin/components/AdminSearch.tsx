"use client";

import { useCallback, useEffect, useState } from "react";
import styles from "../AdminPage.module.css";

interface SearchBarProps {
  onSearch: (keyword: string) => void;
  placeholder?: string;
}

export function AdminSearch({
  onSearch,
  placeholder = "검색어를 입력하세요",
}: SearchBarProps) {
  const [keyword, setKeyword] = useState("");
  const [debouncedKeyword, setDebouncedKeyword] = useState(keyword);

  // 300ms 디바운스
  useEffect(() => {
    const timer = setTimeout(() => setDebouncedKeyword(keyword), 300);
    return () => clearTimeout(timer);
  }, [keyword]);

  // 디바운스된 값으로 검색 호출
  useEffect(() => {
    onSearch(debouncedKeyword);
  }, [debouncedKeyword, onSearch]);

  // ESC로 입력창 초기화 (선택)
  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent<HTMLInputElement>) => {
      if (e.key === "Escape") setKeyword("");
    },
    []
  );

  return (
    <div className={styles.searchBar}>
      <svg
        xmlns="http://www.w3.org/2000/svg"
        className={styles.searchIcon}
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth={2}
      >
        <circle cx="11" cy="11" r="8" />
        <line x1="21" y1="21" x2="16.65" y2="16.65" />
      </svg>
      <input
        type="text"
        value={keyword}
        onChange={(e) => setKeyword(e.target.value)}
        onKeyDown={handleKeyDown}
        placeholder={placeholder}
        aria-label="검색어 입력"
      />
    </div>
  );
}

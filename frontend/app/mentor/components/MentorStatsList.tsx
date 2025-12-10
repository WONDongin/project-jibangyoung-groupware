import { AdminRegionTab } from "@/app/admin/components/AdminRegionTab";
import { AdminSearch } from "@/app/admin/components/AdminSearch";
import { Pagination } from "@/app/admin/components/Pagination";
import { useAdminRegion } from "@/app/admin/hooks/useAdminRegion";
import { fetchAdMentorLogList } from "@/libs/api/admin/adminMentor.api";
import { AdMentorLogList } from "@/types/api/adMentorLogList";
import { useCallback, useEffect, useMemo, useState } from "react";
import styles from "../MentorStats.module.css";
import { MentorStatsCard } from "./MentorStatsCard";

const MENTOR_ROLES = ["MENTOR_A", "MENTOR_B", "MENTOR_C"];

export function MentorStatsList() {
  const [logs, setLogs] = useState<AdMentorLogList[]>([]);
  const [searchResult, setSearchResult] = useState<AdMentorLogList[]>([]);
  const [searchKeyword, setSearchKeyword] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [selectedRegion, setSelectedRegion] = useState<number>(0);
  const ITEMS_PER_PAGE = 10;

  // 시/도 탭 옵션 + 정확코드→{sido, guGun} 맵
  const { regionOptions: allRegionOptions, regionMap } = useAdminRegion();

  // API 연동
  useEffect(() => {
    fetchAdMentorLogList()
      .then((data) => {
        setLogs(data);
        setSearchResult(data);
      })
      .catch((e) => {
        alert(
          e?.response?.data?.message || e?.message || "멘토 활동 통계 조회 실패"
        );
        setLogs([]);
        setSearchResult([]);
      });
  }, []);

  // logs에 실제로 등장한 "시/도(1000단위)"만 추출
  const regionPrefixesInStats = useMemo(
    () =>
      Array.from(
        new Set(logs.map((l) => Math.floor(l.regionId / 1000) * 1000))
      ),
    [logs]
  );

  // 담당 시/도만 탭에 노출 + '전체'
  const filteredRegionOptions = useMemo(
    () => [
      { code: 0, name: "전체" },
      ...allRegionOptions.filter((opt) =>
        regionPrefixesInStats.includes(opt.code)
      ),
    ],
    [allRegionOptions, regionPrefixesInStats]
  );

  // regionId(정확코드) → 표시명
  const regionIdToDisplay = useCallback(
    (code: number) => {
      const exact = regionMap.get(code);
      if (exact) {
        const sido = (exact.sido ?? "").trim();
        const gu = (exact.guGun ?? "").trim();
        return gu && gu !== sido ? `${sido} ${gu}` : sido || String(code);
      }
      const parent = regionMap.get(Math.floor(code / 1000) * 1000);
      return parent?.sido ?? String(code);
    },
    [regionMap]
  );

  // 시/도(1000단위) 코드 → 시/도명
  const regionPrefixToSido = useCallback(
    (prefix: number) => {
      const parent = regionMap.get(prefix);
      return parent?.sido ?? String(prefix);
    },
    [regionMap]
  );

  // 지역 탭 선택 (시/도 1000단위 기준 필터)
  const handleSelectRegion = useCallback(
    (_regionName: string, regionCode: number) => {
      setSelectedRegion(regionCode);
      setCurrentPage(1);

      const normalized =
        regionCode === 0 ? 0 : Math.floor(regionCode / 1000) * 1000;

      let filtered =
        normalized === 0
          ? logs
          : logs.filter(
              (l) => Math.floor(l.regionId / 1000) * 1000 === normalized
            );

      if (searchKeyword.trim()) {
        const kw = searchKeyword.trim().toLowerCase();
        filtered = filtered.filter(
          (log) =>
            (log.roleDescription ?? log.role).toLowerCase().includes(kw) ||
            regionIdToDisplay(log.regionId).toLowerCase().includes(kw)
        );
      }

      setSearchResult(filtered);
    },
    [logs, searchKeyword, regionIdToDisplay]
  );

  // 검색 (현재 선택된 시/도 유지)
  const handleSearch = useCallback(
    (keyword: string) => {
      setSearchKeyword(keyword);
      const kw = keyword.trim().toLowerCase();

      const normalized =
        selectedRegion === 0 ? 0 : Math.floor(selectedRegion / 1000) * 1000;

      let filtered =
        normalized === 0
          ? logs
          : logs.filter(
              (l) => Math.floor(l.regionId / 1000) * 1000 === normalized
            );

      if (kw) {
        filtered = filtered.filter(
          (log) =>
            (log.roleDescription ?? log.role).toLowerCase().includes(kw) ||
            regionIdToDisplay(log.regionId).toLowerCase().includes(kw)
        );
      }

      setSearchResult(filtered);
      setCurrentPage(1);
    },
    [logs, selectedRegion, regionIdToDisplay]
  );

  const summedByRegionRole = useMemo(() => {
    const m = new Map<string, AdMentorLogList>();
    searchResult.forEach((l) => {
      const prefix = Math.floor(l.regionId / 1000) * 1000;
      const key = `${prefix}-${l.role}`;
      const prev = m.get(key);

      if (!prev) {
        m.set(key, {
          ...l,
          regionId: prefix,
          noticeCount: l.noticeCount ?? 0,
          postCount: l.postCount ?? 0, // 공지와 별도 집계(요구사항 그대로)
          commentCount: l.commentCount ?? 0,
          approvedCount: l.approvedCount ?? 0,
          ignoredCount: l.ignoredCount ?? 0,
          invalidCount: l.invalidCount ?? 0,
          pendingCount: l.pendingCount ?? 0,
          rejectedCount: l.rejectedCount ?? 0,
          requestedCount: l.requestedCount ?? 0,
        });
      } else {
        prev.noticeCount += l.noticeCount ?? 0;
        prev.postCount += l.postCount ?? 0;
        prev.commentCount += l.commentCount ?? 0;
        prev.approvedCount += l.approvedCount ?? 0;
        prev.ignoredCount += l.ignoredCount ?? 0;
        prev.invalidCount += l.invalidCount ?? 0;
        prev.pendingCount += l.pendingCount ?? 0;
        prev.rejectedCount += l.rejectedCount ?? 0;
        prev.requestedCount += l.requestedCount ?? 0;
      }
    });
    return m;
  }, [searchResult]);

  // 시/도(1000단위) 단위로 그룹 (합산 맵 기준)
  const groupedRegionPrefixes = useMemo(() => {
    const prefixes = new Set<number>();
    summedByRegionRole.forEach((_v, k) => {
      const [prefix] = k.split("-");
      prefixes.add(Number(prefix));
    });
    return Array.from(prefixes);
  }, [summedByRegionRole]);

  const paginatedRegions = useMemo(
    () =>
      groupedRegionPrefixes.slice(
        (currentPage - 1) * ITEMS_PER_PAGE,
        currentPage * ITEMS_PER_PAGE
      ),
    [groupedRegionPrefixes, currentPage]
  );

  const totalPages = Math.ceil(groupedRegionPrefixes.length / ITEMS_PER_PAGE);

  return (
    <div>
      <h1 className={styles.title}>멘토 활동 통계</h1>

      <AdminRegionTab
        regionOptions={filteredRegionOptions} // 담당 시/도만
        selectedRegionCode={selectedRegion}
        onSelectRegion={handleSelectRegion}
      />

      <AdminSearch
        placeholder="멘토 등급/지역명 검색"
        onSearch={handleSearch}
      />

      <div className={styles.statsGrid}>
        {paginatedRegions.length === 0 ? (
          <div style={{ padding: "30px 0", textAlign: "center" }}>
            일치하는 정보가 없습니다.
          </div>
        ) : (
          paginatedRegions.flatMap((regionPrefix) =>
            MENTOR_ROLES.map((role) => {
              const summed = summedByRegionRole.get(`${regionPrefix}-${role}`);

              return (
                <MentorStatsCard
                  key={`${regionPrefix}-${role}`}
                  log={
                    summed ?? {
                      userId: 0,
                      nickname: "",
                      role,
                      roleDescription:
                        role === "MENTOR_A"
                          ? "멘토 A"
                          : role === "MENTOR_B"
                            ? "멘토 B"
                            : "멘토 C",
                      regionId: regionPrefix,
                      noticeCount: 0,
                      postCount: 0,
                      commentCount: 0,
                      approvedCount: 0,
                      ignoredCount: 0,
                      invalidCount: 0,
                      pendingCount: 0,
                      rejectedCount: 0,
                      requestedCount: 0,
                    }
                  }
                  regionName={regionPrefixToSido(regionPrefix)}
                />
              );
            })
          )
        )}
      </div>

      <Pagination
        currentPage={currentPage}
        totalPages={totalPages}
        onPageChange={setCurrentPage}
      />
    </div>
  );
}

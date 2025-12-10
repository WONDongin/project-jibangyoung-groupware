import { AdminRegionTab } from "@/app/admin/components/AdminRegionTab";
import { AdminSearch } from "@/app/admin/components/AdminSearch";
import { Pagination } from "@/app/admin/components/Pagination";
import { useAdminRegion } from "@/app/admin/hooks/useAdminRegion";
import { fetchAdMentorLogList } from "@/libs/api/admin/adminMentor.api";
import { AdMentorLogList } from "@/types/api/adMentorLogList";
import { useCallback, useEffect, useMemo, useState } from "react";
import styles from "../../admin/AdminPage.module.css";
import { MentorLogRow } from "./MentorLogRow";

export function MentorLogList() {
  const [logs, setLogs] = useState<AdMentorLogList[]>([]);
  const [searchResult, setSearchResult] = useState<AdMentorLogList[]>([]);
  const [searchKeyword, setSearchKeyword] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [selectedRegion, setSelectedRegion] = useState<number>(0);
  const ITEMS_PER_PAGE = 10;

  // ✅ 시/도 탭 옵션 + 정확코드→{sido, guGun} 맵
  const { regionOptions: allRegionOptions, regionMap } = useAdminRegion();

  useEffect(() => {
    fetchAdMentorLogList()
      .then((data) => {
        setLogs(data);
        setSearchResult(data);
      })
      .catch((e) => {
        alert(e?.response?.data?.message || e?.message || "활동로그 조회 실패");
        setLogs([]);
        setSearchResult([]);
      });
  }, []);

  // ✅ logs에 등장한 "시/도(1000단위)"만 추출
  const logRegionPrefixes = useMemo(
    () =>
      Array.from(
        new Set(logs.map((l) => Math.floor(l.regionId / 1000) * 1000))
      ),
    [logs]
  );

  // ✅ 담당 시/도만 탭에 노출 + '전체'
  const filteredRegionOptions = useMemo(
    () => [
      { code: 0, name: "전체" },
      ...allRegionOptions.filter((opt) => logRegionPrefixes.includes(opt.code)),
    ],
    [allRegionOptions, logRegionPrefixes]
  );

  // ✅ 지역 탭 선택(시/도 1000단위 기준)
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
            log.nickname.toLowerCase().includes(kw)
        );
      }

      setSearchResult(filtered);
    },
    [logs, searchKeyword]
  );

  // ✅ 검색(현재 선택된 시/도 유지)
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
            log.nickname.toLowerCase().includes(kw)
        );
      }

      setSearchResult(filtered);
      setCurrentPage(1);
    },
    [logs, selectedRegion]
  );

  const goToPage = (page: number) => setCurrentPage(page);

  const totalPages = Math.ceil(searchResult.length / ITEMS_PER_PAGE);
  const paginatedData = useMemo(
    () =>
      searchResult.slice(
        (currentPage - 1) * ITEMS_PER_PAGE,
        currentPage * ITEMS_PER_PAGE
      ),
    [searchResult, currentPage]
  );

  return (
    <div>
      <h1 className={styles.title}>멘토 활동로그</h1>

      {/* 담당 시/도만 지역탭에 노출 */}
      <AdminRegionTab
        regionOptions={filteredRegionOptions}
        selectedRegionCode={selectedRegion}
        onSelectRegion={handleSelectRegion}
      />

      <AdminSearch placeholder="닉네임/등급 검색" onSearch={handleSearch} />

      <div className={styles.tableWrapper}>
        <table className={styles.userTable}>
          <thead>
            <tr>
              <th>NO</th>
              <th>닉네임</th>
              <th>등급</th>
              <th>지역</th>
              <th>게시글 작성수</th>
              <th>댓글 작성수</th>
              <th>신고 처리완료 건수</th>
            </tr>
          </thead>
          <tbody>
            {paginatedData.length === 0 ? (
              <tr>
                <td
                  colSpan={7}
                  style={{ textAlign: "center", padding: "20px" }}
                >
                  일치하는 정보가 없습니다.
                </td>
              </tr>
            ) : (
              paginatedData.map((log, idx) => (
                <MentorLogRow
                  key={`${log.userId}-${log.regionId}`}
                  log={log}
                  index={idx}
                  ITEMS_PER_PAGE={ITEMS_PER_PAGE}
                  currentPage={currentPage}
                  totalCount={searchResult.length} // ✅ 추가: 전체(필터/검색 적용) 개수
                  regionMap={regionMap}
                />
              ))
            )}
          </tbody>
        </table>
        <Pagination
          currentPage={currentPage}
          totalPages={totalPages}
          onPageChange={goToPage}
        />
      </div>
    </div>
  );
}

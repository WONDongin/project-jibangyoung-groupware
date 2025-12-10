import { AdminRegionTab } from "@/app/admin/components/AdminRegionTab";
import { AdminSearch } from "@/app/admin/components/AdminSearch";
import { Pagination } from "@/app/admin/components/Pagination";
import { useAdminRegion } from "@/app/admin/hooks/useAdminRegion";
import { fetchMentorRegionUsers } from "@/libs/api/admin/adminMentor.api";
import { AdMentorUser } from "@/types/api/adMentorUser";
import { useCallback, useEffect, useMemo, useState } from "react";
import styles from "../../admin/AdminPage.module.css";
import { MentorLocalRow } from "./MentorLocalRow";

export function MentorLocalList() {
  const [users, setUsers] = useState<AdMentorUser[]>([]);
  const [searchResult, setSearchResult] = useState<AdMentorUser[]>([]);
  const [searchKeyword, setSearchKeyword] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [selectedRegionCode, setSelectedRegionCode] = useState(0);
  const ITEMS_PER_PAGE = 10;

  // 시/도 탭용 옵션 + 정확코드→{sido, guGun}
  const {
    regionOptions: allRegionOptions,
    regionMap,
    loading: regionLoading,
  } = useAdminRegion();

  // 멘토 목록 fetch
  useEffect(() => {
    fetchMentorRegionUsers()
      .then((data) => {
        setUsers(data);
        setSearchResult(data);
      })
      .catch((e) => {
        alert(
          e?.response?.data?.message || e?.message || "유저 목록 조회 실패"
        );
      });
  }, []);

  // 멘토 목록 "시/도(1000단위)"
  const userRegionPrefixes = useMemo(
    () =>
      Array.from(
        new Set(users.map((u) => Math.floor(u.region_id / 1000) * 1000))
      ),
    [users]
  );

  // 지역탭 옵션:시/도만 + '전체'
  const filteredRegionOptions = useMemo(
    () => [
      { code: 0, name: "전체" },
      ...allRegionOptions.filter((opt) =>
        userRegionPrefixes.includes(opt.code)
      ),
    ],
    [allRegionOptions, userRegionPrefixes]
  );

  // 지역 선택 시 필터 (시/도 1000단위 기준)
  const handleSelectRegion = useCallback(
    (_regionName: string, code: number) => {
      setSelectedRegionCode(code);
      setCurrentPage(1);

      const normalized = code === 0 ? 0 : Math.floor(code / 1000) * 1000;

      let filtered =
        normalized === 0
          ? users
          : users.filter(
              (u) => Math.floor(u.region_id / 1000) * 1000 === normalized
            );

      if (searchKeyword.trim()) {
        const kw = searchKeyword.trim().toLowerCase();
        filtered = filtered.filter(
          (user) =>
            (user.nickname && user.nickname.toLowerCase().includes(kw)) ||
            (user.role && user.role.toLowerCase().includes(kw))
        );
      }

      setSearchResult(filtered);
    },
    [users, searchKeyword]
  );

  // 검색 시 필터 (현재 선택된 시/도 유지)
  const handleSearch = useCallback(
    (keyword: string) => {
      setSearchKeyword(keyword);
      const kw = keyword.trim().toLowerCase();

      const normalized =
        selectedRegionCode === 0
          ? 0
          : Math.floor(selectedRegionCode / 1000) * 1000;

      let filtered =
        normalized === 0
          ? users
          : users.filter(
              (u) => Math.floor(u.region_id / 1000) * 1000 === normalized
            );

      if (kw) {
        filtered = filtered.filter(
          (user) =>
            (user.nickname && user.nickname.toLowerCase().includes(kw)) ||
            (user.role && user.role.toLowerCase().includes(kw))
        );
      }

      setSearchResult(filtered);
      setCurrentPage(1);
    },
    [users, selectedRegionCode]
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
      <h1 className={styles.title}>멘토목록</h1>

      <div className={styles.mentorInfoBox}>
        <h3>📢 멘토 활동점수 및 자동승격 안내</h3>
        <ul>
          <li>
            게시글 작성 시 : <b>+10점</b> (삭제 시 -10점)
          </li>
          <li>
            답글 작성 시 : <b>+5점</b> (삭제 시 -5점)
          </li>
          <li>
            댓글 작성 시 : <b>+2점</b> (삭제 시 -2점)
          </li>
          <li>
            설문 응답 시 : <b>+3점</b> (삭제 시 -3점)
          </li>
          <li>
            점수는 <b>5분마다</b> 업데이트되며, 기준 충족 시 <b>자동 승격</b>
          </li>
        </ul>
        <div className={styles.promotionRules}>
          <p>
            <b>200~400점</b> : <code>MENTOER_C</code> →{" "}
            <code>
              <b style={{ color: "#3b82f6 " }}>MENTOER_B</b>
            </code>{" "}
            자동승격
          </p>
          <p>
            <b>401~600점</b> : <code>MENTOER_B</code> →{" "}
            <code>
              <b style={{ color: "#3b82f6 " }}>MENTOER_A</b>
            </code>{" "}
            자동승격
          </p>
        </div>
      </div>

      <AdminRegionTab
        regionOptions={filteredRegionOptions}
        selectedRegionCode={selectedRegionCode}
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
              <th>담당지역</th>
              <th>경고</th>
              <th>활동점수</th>
            </tr>
          </thead>
          <tbody>
            {paginatedData.length === 0 ? (
              <tr>
                <td
                  colSpan={6}
                  style={{ textAlign: "center", padding: "20px" }}
                >
                  {regionLoading
                    ? "지역 정보 로딩중..."
                    : "일치하는 정보가 없습니다."}
                </td>
              </tr>
            ) : (
              paginatedData.map((user, idx) => (
                <MentorLocalRow
                  key={`${user.id}-${user.region_id}`}
                  user={user}
                  index={idx}
                  totalCount={searchResult.length}
                  ITEMS_PER_PAGE={ITEMS_PER_PAGE}
                  currentPage={currentPage}
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

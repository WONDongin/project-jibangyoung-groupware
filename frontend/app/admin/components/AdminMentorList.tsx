import { AdminRegionTab } from "@/app/admin/components/AdminRegionTab";
import { AdminSearch } from "@/app/admin/components/AdminSearch";
import { Pagination } from "@/app/admin/components/Pagination";
import { useAdminRegion } from "@/app/admin/hooks/useAdminRegion";
import { MentorRequestModal } from "@/app/mentor/components/MentorRequestModal";
import { MentorRequestRow } from "@/app/mentor/components/MentorRequestRow";

import {
  approveMentorRequestFinal,
  fetchAdminAllMentorRequestList,
} from "@/libs/api/admin/admin.api";
import { rejectMentorRequest } from "@/libs/api/admin/adminMentor.api";

import { AdMentorRequest } from "@/types/api/adMentorRequest";
import { useCallback, useEffect, useMemo, useState } from "react";
import styles from "../../admin/AdminPage.module.css";

export function AdminMentorList() {
  const [applications, setApplications] = useState<AdMentorRequest[]>([]);
  const [searchResult, setSearchResult] = useState<AdMentorRequest[]>([]);
  const [selectedMentor, setSelectedMentor] = useState<AdMentorRequest | null>(
    null
  );
  const [selectedRegionId, setSelectedRegionId] = useState<number>(0);
  const [searchKeyword, setSearchKeyword] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [userRole, setUserRole] = useState<string | undefined>(undefined);
  const [currentNickname, setCurrentNickname] = useState<string | undefined>();
  const ITEMS_PER_PAGE = 10;

  // 전체 시/도 탭 옵션 + 정확코드→{sido, guGun} 맵
  const { regionOptions: allRegionOptions, regionMap } = useAdminRegion();

  useEffect(() => {
    if (typeof window !== "undefined") {
      try {
        const raw = localStorage.getItem("auth-store-v3");
        if (raw) {
          const user = JSON.parse(raw)?.state?.user;
          setUserRole(user?.role);
          setCurrentNickname(user?.nickname);
        }
      } catch {
        setUserRole(undefined);
        setCurrentNickname(undefined);
      }
    }
  }, []);

  // 최초 데이터 로드: 관리자 전체 리스트
  useEffect(() => {
    fetchAdminAllMentorRequestList()
      .then((data) => {
        setApplications(data);
        setSearchResult(data);
      })
      .catch((e) => {
        alert(
          e?.response?.data?.message ||
            e?.message ||
            "멘토 신청 전체 목록 조회 실패"
        );
      });
  }, []);

  // 시·도(1000단위) + 키워드 필터
  const filterData = useCallback(
    (regionCode: number, keyword: string) => {
      const normalized =
        regionCode === 0 ? 0 : Math.floor(regionCode / 1000) * 1000;

      let filtered =
        normalized === 0
          ? applications
          : applications.filter(
              (app) => Math.floor(app.regionId / 1000) * 1000 === normalized
            );

      if (keyword.trim() !== "") {
        const lowered = keyword.trim().toLowerCase();
        filtered = filtered.filter(
          (app) =>
            app.userName.toLowerCase().includes(lowered) ||
            app.userEmail.toLowerCase().includes(lowered) ||
            (app.reason && app.reason.toLowerCase().includes(lowered)) ||
            (app.status && app.status.toLowerCase().includes(lowered)) ||
            (app.createdAt && app.createdAt.toLowerCase().includes(lowered))
        );
      }

      setSearchResult(filtered);
      setCurrentPage(1);
    },
    [applications]
  );

  // 탭 변경
  const handleRegionChange = useCallback(
    (_region: string, code: number) => {
      setSelectedRegionId(code);
      filterData(code, searchKeyword);
    },
    [filterData, searchKeyword]
  );

  // 검색
  const handleSearch = useCallback(
    (keyword: string) => {
      setSearchKeyword(keyword);
      filterData(selectedRegionId, keyword);
    },
    [filterData, selectedRegionId]
  );

  // 한 행만 부분 업데이트
  const patchRow = useCallback(
    (id: number, patch: Partial<AdMentorRequest>) => {
      setApplications((prev) =>
        prev.map((app) => (app.id === id ? { ...app, ...patch } : app))
      );
      setSearchResult((prev) =>
        prev.map((app) => (app.id === id ? { ...app, ...patch } : app))
      );
    },
    []
  );

  // 최종 승인
  const handleFinalApprove = useCallback(
    (id: number) => {
      approveMentorRequestFinal(id)
        .then(() => {
          patchRow(id, {
            status: "FINAL_APPROVED",
            nickname: currentNickname ?? null,
          });
          setSelectedMentor(null);
          alert("최종 승인 처리 완료되었습니다!");
        })
        .catch((e) => {
          alert(
            e?.response?.data?.message ||
              e?.message ||
              "처리 중 오류가 발생했습니다."
          );
        });
    },
    [patchRow, currentNickname]
  );

  // 반려
  const handleReject = useCallback(
    (id: number, reason: string) => {
      const trimmed = reason.trim();
      if (!trimmed) {
        alert("반려 사유를 입력해 주세요.");
        return;
      }
      rejectMentorRequest(id, trimmed)
        .then(() => {
          patchRow(id, {
            status: "REJECTED",
            nickname: currentNickname ?? null,
            rejectionReason: trimmed,
          });
          setSelectedMentor(null);
          alert("반려 처리 완료되었습니다!");
        })
        .catch((e) => {
          alert(
            e?.response?.data?.message ||
              e?.message ||
              "반려 처리 중 오류가 발생했습니다."
          );
        });
    },
    [patchRow, currentNickname]
  );

  const totalPages = useMemo(
    () => Math.ceil(searchResult.length / ITEMS_PER_PAGE),
    [searchResult.length]
  );
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
      <h1 className={styles.title}>멘토 신청 목록</h1>

      <AdminRegionTab
        regionOptions={allRegionOptions}
        selectedRegionCode={selectedRegionId}
        onSelectRegion={handleRegionChange}
      />

      <AdminSearch
        placeholder="이름, 사유, 상태, 신청일 검색"
        onSearch={handleSearch}
      />

      <div className={styles.tableWrapper}>
        <table className={styles.userTable}>
          <thead>
            <tr>
              <th>NO</th>
              <th>ID</th>
              <th>신청사유</th>
              <th>신청지역</th>
              <th>신청일자</th>
              <th>담당자</th>
              <th>승인여부</th>
              <th>첨부파일</th>
            </tr>
          </thead>
          <tbody>
            {paginatedData.length === 0 ? (
              <tr>
                <td
                  colSpan={8}
                  style={{ textAlign: "center", padding: "20px" }}
                >
                  일치하는 정보가 없습니다.
                </td>
              </tr>
            ) : (
              paginatedData.map((app, index) => (
                <MentorRequestRow
                  key={app.id}
                  app={app}
                  index={index + (currentPage - 1) * ITEMS_PER_PAGE}
                  total={searchResult.length}
                  onClick={() => setSelectedMentor(app)}
                  regionOptions={allRegionOptions}
                  regionMap={regionMap}
                />
              ))
            )}
          </tbody>
        </table>

        <Pagination
          currentPage={currentPage}
          totalPages={totalPages}
          onPageChange={setCurrentPage}
        />
      </div>

      {selectedMentor && (
        <MentorRequestModal
          data={selectedMentor}
          userRole={userRole}
          onFinalApprove={() => handleFinalApprove(selectedMentor.id)}
          onReject={(reason) => handleReject(selectedMentor.id, reason)}
          onClose={() => setSelectedMentor(null)}
          regionOptions={allRegionOptions}
          regionMap={regionMap}
        />
      )}
    </div>
  );
}

export default AdminMentorList;

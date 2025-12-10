import { RegionLookup } from "@/app/admin/hooks/useAdminRegion";
import { AdMentorLogList } from "@/types/api/adMentorLogList";

interface MentorLogRowProps {
  log: AdMentorLogList;
  index: number;
  ITEMS_PER_PAGE: number;
  currentPage: number;
  totalCount: number; // ✅ 추가: 전체(필터/검색 결과) 개수
  regionMap: Map<number, RegionLookup>;
}

export function MentorLogRow({
  log,
  index,
  ITEMS_PER_PAGE,
  currentPage,
  totalCount,
  regionMap,
}: MentorLogRowProps) {
  // ✅ 내림차순 번호: n → 1 (페이지 넘어가도 이어짐)
  const order = (totalCount - (ITEMS_PER_PAGE * (currentPage - 1) + index))
    .toString()
    .padStart(2, "0");

  // 표시: 정확코드면 "시도 구군", 없으면 부모(1000단위) 시도
  const regionName = (() => {
    const exact = regionMap.get(log.regionId);
    if (exact) {
      const sido = (exact.sido ?? "").trim();
      const gu = (exact.guGun ?? "").trim();
      return gu && gu !== sido ? `${sido} ${gu}` : sido || String(log.regionId);
    }
    const parent = regionMap.get(Math.floor(log.regionId / 1000) * 1000);
    return parent?.sido ?? String(log.regionId);
  })();

  return (
    <tr>
      <td>{order}</td>
      <td>{log.nickname}</td>
      <td>{log.roleDescription ?? log.role}</td>
      <td>{regionName}</td>
      <td>{log.postCount}</td>
      <td>{log.commentCount}</td>
      <td>{log.approvedCount}</td>
    </tr>
  );
}

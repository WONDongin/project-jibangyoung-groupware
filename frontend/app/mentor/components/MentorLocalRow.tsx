import { RegionLookup } from "@/app/admin/hooks/useAdminRegion";
import { AdMentorUser } from "@/types/api/adMentorUser";

interface Props {
  user: AdMentorUser;
  index: number;
  totalCount: number;
  ITEMS_PER_PAGE: number;
  currentPage: number;
  regionMap: Map<number, RegionLookup>;
}

export function MentorLocalRow({
  user,
  index,
  totalCount,
  ITEMS_PER_PAGE,
  currentPage,
  regionMap,
}: Props) {
  const order = totalCount - ((currentPage - 1) * ITEMS_PER_PAGE + index);

  const regionName = (() => {
    const exact = regionMap.get(user.region_id);
    if (exact) {
      const sido = (exact.sido ?? "").trim();
      const gu = (exact.guGun ?? "").trim();
      return gu && gu !== sido
        ? `${sido} ${gu}`
        : sido || String(user.region_id);
    }
    const parent = regionMap.get(Math.floor(user.region_id / 1000) * 1000);
    return parent?.sido ?? String(user.region_id);
  })();

  return (
    <tr>
      <td>{order}</td>
      <td>{user.nickname}</td>
      <td>{user.roleDescription}</td>
      <td>{regionName}</td>
      <td>{user.warning_count}</td>
      <td>{user.current_score}</td>
    </tr>
  );
}

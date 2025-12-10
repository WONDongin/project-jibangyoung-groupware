import { fetchAdminRegion } from "@/libs/api/admin/admin.api";
import { AdminRegion } from "@/types/api/adminRegion";
import { useEffect, useMemo, useState } from "react";

export interface RegionTabOption {
  code: number; // 1000단위 정규화 코드 (탭/필터용)
  name: string; // 시/도 표시명
}

export interface RegionLookup {
  code: number; // 원본 region_code (정확 매칭용)
  sido: string;
  guGun: string; // "서울특별시" | "수원시" | "수원시 장안구" 등
}

export function useAdminRegion() {
  const [regionOptions, setRegionOptions] = useState<RegionTabOption[]>([]);
  const [regionMap, setRegionMap] = useState<Map<number, RegionLookup>>(
    new Map()
  );
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      try {
        const data: AdminRegion[] = await fetchAdminRegion();
        // data 원본: [{ region_code, sido, guGun }, ...]

        // 1) 정확 코드 조회용 맵 (행 표시용)
        const map = new Map<number, RegionLookup>();
        data.forEach((r) => {
          map.set(r.region_code, {
            code: r.region_code,
            sido: r.sido,
            guGun: (r.guGun ?? "").trim(),
          });
        });
        setRegionMap(map);

        // 2) 탭/필터용 1000단위 정규화 + 시/도 중복 제거
        const normalized = data.map((r) => ({
          code: Math.floor(r.region_code / 1000) * 1000,
          name: r.sido,
        }));
        const deduped = Array.from(
          new Map(normalized.map((x) => [x.code, x])).values()
        ).sort((a, b) => a.name.localeCompare(b.name, "ko"));
        setRegionOptions([{ code: 0, name: "전체" }, ...deduped]);
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  // 부모(시/도) 코드 -> 시/도명 조회용 헬퍼 (없으면 undefined)
  const getSidoByNormalizedCode = useMemo(() => {
    const m = new Map(regionOptions.map((o) => [o.code, o.name]));
    return (code1000: number) => m.get(code1000);
  }, [regionOptions]);

  return { regionOptions, regionMap, getSidoByNormalizedCode, loading };
}

import { Region } from "@/types/api/region.d";
import { REGION_INDEX, regionFullPath } from "@/components/constants/region-map";

// 지역 코드로부터 시도+군구1+군구2를 표시하는 함수
export const getGuGunNameByCode = (regionCode: string): string => {
  return regionFullPath(regionCode) || regionCode;
};

// 지역 코드로부터 지역 정보를 찾는 함수
export const getRegionByCode = async (regionCode: string): Promise<Region | null> => {
  try {
    const { getRegionsBoard } = await import("@/libs/api/region.api");
    const regions: Region[] = await getRegionsBoard();
    return regions.find(r => r.regionCode.toString() === regionCode) || null;
  } catch (error) {
    console.error("Failed to get region:", error);
    return null;
  }
};
package com.jibangyoung.domain.admin.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.jibangyoung.domain.admin.dto.AdRegionDTO;
import com.jibangyoung.domain.policy.entity.Region;

@Repository
public interface AdRegionRepository extends JpaRepository<Region, Integer> {

    @Query(value = """
        SELECT *
        FROM (
            SELECT r.region_code AS regionCode,
                r.sido        AS sido,
                r.sido        AS guGun
            FROM region r
            WHERE NULLIF(TRIM(r.gu_gun_1), '') IS NULL

            UNION ALL

            SELECT r.region_code AS regionCode,
                r.sido        AS sido,
                TRIM(r.gu_gun_1) AS guGun
            FROM region r
            WHERE NULLIF(TRIM(r.gu_gun_1), '') IS NOT NULL
            AND NULLIF(TRIM(r.gu_gun_2), '') IS NULL

            UNION ALL

            SELECT r.region_code AS regionCode,
                r.sido        AS sido,
                CONCAT_WS(' ', TRIM(r.gu_gun_1), TRIM(r.gu_gun_2)) AS guGun
            FROM region r
            WHERE NULLIF(TRIM(r.gu_gun_2), '') IS NOT NULL
        ) t
        WHERE t.regionCode <> 99999
        AND TRIM(t.sido) <> '전국'
        ORDER BY t.sido ASC, t.guGun ASC
    """, nativeQuery = true)
    List<AdRegionDTO> findDistinctSidoList();
}

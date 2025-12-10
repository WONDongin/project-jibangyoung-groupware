package com.jibangyoung.domain.admin.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.jibangyoung.domain.admin.dto.AdRegionDTO;
import com.jibangyoung.domain.admin.repository.AdRegionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdRegionService {
    private final AdRegionRepository adRegionRepository;

    public List<AdRegionDTO> getSidoList() {
        return adRegionRepository.findDistinctSidoList().stream()
            .map(p -> new AdRegionDTO(
                p.getRegion_code(),   // 11000, 11110, 11140 ...
                p.getSido(),         // "서울특별시"
                p.getGuGun()         // 시도레벨: "서울특별시", 시/군: "수원시", 구/군: "종로구" 등
            ))
            .collect(Collectors.toList());
    }
}

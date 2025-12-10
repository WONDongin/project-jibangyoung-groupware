package com.jibangyoung.domain.policy.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jibangyoung.domain.policy.dto.PolicyCardDto;
import com.jibangyoung.domain.policy.dto.PolicyDetailDto;
import com.jibangyoung.domain.policy.service.PolicyDetailService;
import com.jibangyoung.domain.policy.service.PolicyService;
import com.jibangyoung.global.annotation.UserActivityLogging;

@RestController
@RequestMapping("/api/policy")
public class PolicyController {

    @Autowired
    private PolicyDetailService policyDetailService;

    @Autowired
    private PolicyService policyService;

    public PolicyController(PolicyService policyService, PolicyDetailService policyDetailService) {
        this.policyService = policyService;
        this.policyDetailService = policyDetailService;
    }

    @GetMapping("/policy.c")
    @UserActivityLogging(actionType = "POLICY_CARDS_VIEW", priority = UserActivityLogging.Priority.NORMAL, description = "정책 카드 목록 조회")
    public List<PolicyCardDto> getPolicyCards() {
        return policyService.getActivePolicyCards();
    }

    @GetMapping("/region.api")
    @UserActivityLogging(actionType = "POLICY_REGION_VIEW", priority = UserActivityLogging.Priority.NORMAL, description = "지역별 정책 조회")
    public List<PolicyCardDto> getPoliciesByRegion(
            @RequestParam(name = "region_code", required = false, defaultValue = "99999") int regionCode) {
        return policyService.getPoliciesByRegion(regionCode);
    }

    @GetMapping("/policyDetail/{NO}")
    @UserActivityLogging(actionType = "POLICY_DETAIL_VIEW", priority = UserActivityLogging.Priority.NORMAL, description = "정책 상세 조회")
    public List<PolicyDetailDto> getMethodName(@PathVariable Integer NO) {
        return policyDetailService.getPolicyDetail(NO);
    }
}

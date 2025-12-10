package com.jibangyoung.domain.survey.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jibangyoung.domain.survey.dto.AnswerJsonDto;
import com.jibangyoung.domain.survey.service.SurveyAnswerService;
import com.jibangyoung.global.annotation.UserActivityLogging;
import com.jibangyoung.global.security.CustomUserPrincipal;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/survey")
@RequiredArgsConstructor
public class SurveyAnswerController {

    private final SurveyAnswerService service;

    @PostMapping("/surveyAnswer")
    @UserActivityLogging(actionType = "SURVEY_SUBMIT", scoreDelta = 25, priority = UserActivityLogging.Priority.HIGH, description = "설문 응답 제출")
    public ResponseEntity<Map<String, Long>> saveAnswers(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestBody AnswerJsonDto request) {

        Long userId = principal.getId();
        var answers = request.getAnswers();

        Long responseId = service.saveSurveyAnswers(userId, answers);

        Map<String, Long> body = Map.of(
                "userId", userId,
                "responseId", responseId);

        return ResponseEntity.ok(body);
    }
}

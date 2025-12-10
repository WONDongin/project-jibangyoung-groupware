package com.jibangyoung.global.aspect;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.jibangyoung.global.annotation.UserActivityLogging;
import com.jibangyoung.global.common.UserActivityLog;
import com.jibangyoung.global.service.UserActivityLogService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 🎭 사용자 활동 로깅 AOP — 안전 캐스팅/기본값 보강 (수정: userId null 허용)
 */
@Aspect
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class UserActivityLoggingAspect {

    private final UserActivityLogService logService;

    @Around("@annotation(com.jibangyoung.global.annotation.UserActivityLogging)")
    public Object logUserActivity(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String logId = UUID.randomUUID().toString();
        UserActivityLogging annotation = getAnnotation(joinPoint);

        HttpServletRequest request = getCurrentRequest();
        Map<String, Object> contextInfo = extractContextInfo(joinPoint, request);

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            logSuccess(logId, annotation, joinPoint, contextInfo, executionTime, result);
            return result;
        } catch (Exception e) {
            if (annotation.logOnFailure()) {
                long executionTime = System.currentTimeMillis() - startTime;
                logFailure(logId, annotation, joinPoint, contextInfo, executionTime, e);
            }
            throw e;
        }
    }

    private void logSuccess(String logId, UserActivityLogging annotation,
            ProceedingJoinPoint joinPoint, Map<String, Object> contextInfo,
            long executionTime, Object result) {

        UserActivityLog.UserActivityLogBuilder builder = createBaseLogBuilder(
                logId, annotation, joinPoint, contextInfo, executionTime);

        Map<String, Object> resultInfo = extractResultInfo(result);

        if (resultInfo.containsKey("refId")) {
            builder.refId(toLong(resultInfo.get("refId")));
        }

        Long userId = null;
        if (resultInfo.containsKey("userId")) {
            userId = toLong(resultInfo.get("userId"));
        }
        if (resultInfo.containsKey("regionId")) {
            builder.regionId(toLong(resultInfo.get("regionId")));
        }

        if (userId == null) {
            Object paramUserId = ((Map<String, Object>) contextInfo.get("requestParams")).get("userId");
            if (paramUserId != null)
                userId = toLong(paramUserId);
        }

        if (userId == null) {
            try {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null) {
                    Object principal = auth.getPrincipal();
                    try {
                        Method m = principal.getClass().getMethod("getId");
                        Object val = m.invoke(principal);
                        userId = toLong(val);
                    } catch (NoSuchMethodException ignore) {
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // 수정: userId null 허용 (익명 사용자)
        builder.userId(userId); // null 그대로 설정

        UserActivityLog successLog = UserActivityLog.success(builder);

        log.debug("🎭 성공 로그 생성: logId={}, userId={}, actionType={}",
                logId, userId, annotation.actionType());

        logService.saveLogAsync(successLog);
    }

    private void logFailure(String logId, UserActivityLogging annotation,
            ProceedingJoinPoint joinPoint, Map<String, Object> contextInfo,
            long executionTime, Exception e) {

        UserActivityLog.UserActivityLogBuilder builder = createBaseLogBuilder(
                logId, annotation, joinPoint, contextInfo, executionTime);

        // 실패 로그에서도 userId는 null 허용
        builder.userId(null);

        UserActivityLog failureLog = UserActivityLog.failure(builder, e.getMessage());

        log.debug("🎭 실패 로그 생성: logId={}, actionType={}, error={}",
                logId, annotation.actionType(), e.getMessage());

        logService.saveLogAsync(failureLog);
    }

    private UserActivityLog.UserActivityLogBuilder createBaseLogBuilder(
            String logId, UserActivityLogging annotation, ProceedingJoinPoint joinPoint,
            Map<String, Object> contextInfo, long executionTime) {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        return UserActivityLog.builder()
                .logId(logId)
                .actionType(annotation.actionType())
                .scoreDelta(annotation.scoreDelta())
                .priority(annotation.priority().name())
                .memo(annotation.description())
                .methodName(signature.getMethod().getName())
                .className(signature.getDeclaringType().getSimpleName())
                .executionTime(executionTime)
                .ipAddr((String) contextInfo.get("ipAddr"))
                .userAgent((String) contextInfo.get("userAgent"))
                .platform((String) contextInfo.get("platform"))
                .lang((String) contextInfo.getOrDefault("lang", "ko"))
                .requestParams(getRequestParamsFromContext(contextInfo))
                .ttl(86400L);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getRequestParamsFromContext(Map<String, Object> contextInfo) {
        Object requestParams = contextInfo.get("requestParams");
        if (requestParams instanceof Map<?, ?> m) {
            try {
                return (Map<String, Object>) m;
            } catch (ClassCastException e) {
                return new HashMap<>();
            }
        }
        return new HashMap<>();
    }

    private Map<String, Object> extractContextInfo(ProceedingJoinPoint joinPoint,
            HttpServletRequest request) {
        Map<String, Object> context = new HashMap<>();
        if (request != null) {
            context.put("ipAddr", getClientIpAddr(request));
            context.put("userAgent", request.getHeader("User-Agent"));
            context.put("platform", determinePlatform(request));
            context.put("lang", request.getHeader("Accept-Language"));
        }
        context.put("requestParams", extractMethodParams(joinPoint));
        return context;
    }

    private Map<String, Object> extractMethodParams(ProceedingJoinPoint joinPoint) {
        Map<String, Object> params = new HashMap<>();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        if (paramNames != null && args != null) {
            for (int i = 0; i < Math.min(paramNames.length, args.length); i++) {
                if (args[i] != null && !isSensitiveParam(paramNames[i])) {
                    params.put(paramNames[i], args[i]);
                }
            }
        }
        return params;
    }

    private Map<String, Object> extractResultInfo(Object result) {
        Map<String, Object> info = new HashMap<>();
        if (result == null)
            return info;
        try {
            if (result.getClass().getSimpleName().equals("ApiResponse")) {
                Method getDataMethod = result.getClass().getMethod("getData");
                Object data = getDataMethod.invoke(result);
                if (data != null)
                    extractIdFields(data, info);
            } else {
                extractIdFields(result, info);
            }
        } catch (Exception e) {
            /* ignore */ }
        return info;
    }

    private void extractIdFields(Object data, Map<String, Object> info) {
        try {
            Class<?> c = data.getClass();
            String[] methods = { "getUserId", "getRegionId", "getId", "getRefId" };
            String[] keys = { "userId", "regionId", "refId", "refId" };
            for (int i = 0; i < methods.length; i++) {
                try {
                    Method m = c.getMethod(methods[i]);
                    Object val = m.invoke(data);
                    if (val != null) {
                        info.put(keys[i], val);
                    }
                } catch (NoSuchMethodException ignore) {
                }
            }
        } catch (Exception ignore) {
        }
    }

    private UserActivityLogging getAnnotation(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        return method.getAnnotation(UserActivityLogging.class);
    }

    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            return attrs.getRequest();
        } catch (Exception e) {
            return null;
        }
    }

    private String getClientIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private String determinePlatform(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        return ua != null && ua.toLowerCase().contains("mobile") ? "MOBILE" : "PC";
    }

    private boolean isSensitiveParam(String name) {
        String n = name.toLowerCase();
        return n.contains("password") || n.contains("token") || n.contains("secret") || n.contains("key");
    }

    private Long toLong(Object v) {
        if (v == null)
            return null;
        if (v instanceof Long l)
            return l;
        if (v instanceof Integer i)
            return i.longValue();
        if (v instanceof Number n)
            return n.longValue();
        try {
            return Long.parseLong(v.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
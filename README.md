# 지방청년

> 지방 청년들을 위한 종합 정보 플랫폼

## 프로젝트 소개

지방청년은 지방에 거주하는 청년들이 자신의 지역에서 활용할 수 있는 정책 정보를 쉽게 찾고, 지역별 커뮤니티를 통해 소통할 수 있도록 돕는 웹 플랫폼입니다. 수도권에 집중된 현재 상황에서 다양한 지방 정책들을 알려주어 지방 이주를 유도하고, 지방 정착을 지원하는 것을 목표로 합니다.

## 주요 기능

- **맞춤형 정책 추천**: 설문조사 기반 개인화 정책 추천 시스템
- **지역별 커뮤니티**: 지역 코드별 게시판을 통한 청년 간 소통 공간
- **정책 통합 관리**: 전국 청년 정책 정보 검색, 즐겨찾기, 상세 조회
- **멘토링 시스템**: 멘토 신청/승인, 공지사항 관리, 멘토 대시보드
- **대시보드**: 인기 게시글, 정책 랭킹, 지역별 통계 시각화
- **마이페이지**: 프로필 관리, 활동 내역, 지역 점수 시스템
- **관리자 시스템**: 사용자/게시글/신고 통합 관리, 멘토 승인

## ERD

<img width="3190" height="1612" alt="4 ERD_3조" src="https://github.com/user-attachments/assets/dca61e60-932c-4f1b-978f-cab57d9fb17f" />

## 시스템 아키텍처

<img width="721" height="462" alt="전체 아키텍처" src="https://github.com/user-attachments/assets/3c574bed-f086-448c-ac21-7d310b2f202b" />

## 기술 스택

### Backend
- **Framework**: Spring Boot 3.5.4
- **Language**: Java 17
- **Database**: MariaDB
- **Cache**: Redis 7
- **Authentication**: JWT, OAuth2 (Naver)
- **Security**: Spring Security
- **Build Tool**: Gradle
- **ORM**: JPA/Hibernate + QueryDSL

### Frontend
- **Framework**: Next.js 15.3.4
- **Language**: TypeScript
- **Styling**: CSS Modules
- **State Management**: Zustand
- **Data Fetching**: TanStack Query (React Query)
- **UI Components**: Custom Components
- **Charts**: Chart.js + react-chartjs-2
- **Editor**: CKEditor 5

### Infrastructure
- **Containerization**: Docker & Docker Compose
- **Cloud Storage**: AWS S3
- **CI/CD**: GitHub Actions (3개 워크플로우: CI, CD, Preview)
- **Monitoring**: Spring Boot Actuator
- **Scheduling**: Spring Scheduler (다중 스케줄러 운영)

## 프로젝트 구조

```
JIBANGYOUNG/
├── backend/                 # Spring Boot 백엔드
│   ├── src/main/java/
│   │   └── com/jibangyoung/
│   │       ├── domain/      # 도메인별 패키지
│   │       └── global/      # 공통 설정 및 유틸
├── frontend/                # Next.js 프론트엔드
│   ├── app/                # App Router 페이지
│   ├── components/         # 재사용 가능한 컴포넌트
│   ├── libs/              # API 및 유틸리티
│   └── types/             # TypeScript 타입 정의
└── infra/                  # 인프라 설정
    ├── docker/            # Docker 설정
    └── scripts/           # 배포 스크립트
```

## 주요 도메인 및 기능

### 인증 시스템 (Auth)
- **로그인/회원가입**: 이메일 인증, 중복 검사
- **소셜 로그인**: 네이버 OAuth2 연동
- **비밀번호 관리**: 재설정, 토큰 기반 인증
- **JWT 토큰**: Access/Refresh Token 관리
- **프로필**: 사용자 정보 수정, 프로필 이미지 업로드

### 정책 관리 (Policy)
- **정책 목록**: 활성 정책 카드 형태로 조회 (`/api/policy/policy.c`)
- **지역별 정책**: 지역 코드 기반 필터링 (`/api/policy/region.api`)
- **정책 상세**: 개별 정책 상세 정보 제공
- **즐겨찾기**: 사용자별 정책 저장 기능
- **맞춤 추천**: 설문 결과 기반 정책 추천

### 지역 커뮤니티 (Community)
- **지역별 게시판**: 동적 라우팅 (`/community/[regionCode]`)
- **게시글 CRUD**: 작성, 수정, 삭제, 조회
- **댓글 시스템**: 계층형 댓글 구조
- **추천/신고**: 게시글/댓글 추천 및 신고 기능
- **인기 게시글**: 일간/주간/월간 인기글 정렬
- **이미지 업로드**: AWS S3 Presigned URL 활용

### 대시보드 (Dashboard)
- **월간 인기글**: 지역별 월간 HOT 게시글 Top 10
- **정책 랭킹**: 인기 정책 실시간 집계
- **지역 랭킹**: 지역별 활동 점수 순위
- **리뷰 통계**: 상위 리뷰 게시글 대시보드

### 추천 시스템 (Recommendation)
- **설문조사**: 사용자 선호도 조사 (`/survey`)
- **정책 추천**: 설문 결과 기반 지역 추천
- **추천 결과**: 개인화된 추천 지역 및 이유 제공
- **상세 추천**: 지역별 맞춤 정책 추천

### 마이페이지 (MyPage)
- **프로필 관리**: 개인정보 수정
- **활동 내역**: 작성한 게시글/댓글 관리
- **지역 점수**: 사용자별 지역 활동 점수 시스템
- **알림 관리**: 사용자 알림 설정 및 조회
- **신고 내역**: 신고한 게시글/댓글 관리

### 멘토링 시스템 (Mentor)
- **멘토 신청**: 멘토 인증 요청 시스템
- **멘토 관리**: 신청 승인/거부, 등급 관리
- **공지사항**: 멘토 전용 공지사항 CRUD
- **통계 대시보드**: 멘토별 활동 통계
- **로그 관리**: 멘토 활동 로그 추적

### 관리자 시스템 (Admin)
- **사용자 관리**: 회원 정보, 권한, 상태 관리
- **게시글 관리**: 전체 게시글 모니터링 및 관리
- **신고 관리**: 신고된 컨텐츠 검토 및 처리
- **지역 관리**: 지역 코드 및 정보 관리
- **멘토 승인**: 멘토 신청 검토 및 승인 프로세스

### 설문 시스템 (Survey)
- **설문 응답**: JSON 형태 설문 응답 저장
- **응답 분석**: 설문 결과 기반 추천 로직
- **설문 관리**: 설문 문항 및 결과 관리

## 주요 페이지 구조

### 프론트엔드 (Next.js App Router)
```
/                           # 메인 대시보드
├── /auth                   # 인증 관련
│   ├── /login             # 로그인
│   ├── /register          # 회원가입
│   ├── /find-id           # 아이디 찾기
│   ├── /find-password     # 비밀번호 찾기
│   └── /reset-password    # 비밀번호 재설정
├── /community             # 커뮤니티
│   ├── /main             # 커뮤니티 메인
│   ├── /write            # 게시글 작성
│   └── /[regionCode]     # 지역별 게시판
│       └── /[postId]     # 게시글 상세
│           └── /edit     # 게시글 수정
├── /policy               # 정책 관련
│   ├── /totalPolicies   # 전체 정책 목록
│   ├── /rec_Policies    # 즐겨찾기 정책
│   ├── /recommendedList # 추천 정책 목록
│   └── /policy_detail/[id] # 정책 상세
├── /survey               # 설문조사
├── /recommendation       # 추천 결과
│   └── /[userId]/[responseId]
│       └── /[regionCode] # 지역별 추천 상세
├── /mypage              # 마이페이지
├── /mentor              # 멘토 시스템
│   ├── /info           # 멘토 소개
│   ├── /apply          # 멘토 신청
│   └── /notices        # 멘토 공지사항
│       └── /[noticeId] # 공지사항 상세
└── /admin              # 관리자 페이지
```

### 백엔드 API 엔드포인트
```
/api/auth/*             # 인증 관련 API
/api/community/*        # 커뮤니티 API
/api/policy/*           # 정책 관련 API
/api/recommendation/*   # 추천 시스템 API
/api/survey/*           # 설문조사 API
/api/mypage/*           # 마이페이지 API
/api/admin/*            # 관리자 API
/api/mentor/*           # 멘토 시스템 API
/api/dashboard/*        # 대시보드 API
```

## 주요 기술적 특징

### 보안
- **JWT 기반 인증**: Access/Refresh Token 분리
- **소셜 로그인**: 네이버 OAuth2 연동
- **권한 관리**: 사용자 역할별 접근 제어 (USER, ADMIN, MENTOR)
- **입력 검증**: Bean Validation 및 커스텀 검증 로직

### 성능 최적화
- **Redis 캐싱**: 자주 조회되는 데이터 캐싱
- **TanStack Query (React Query)**: 서버 상태 관리 및 캐싱

### 모니터링 및 로깅
- **사용자 활동 로깅**: AOP 기반 활동 추적 (`@UserActivityLogging`)
- **Spring Batch**: 대용량 로그 데이터 처리
- **ShedLock**: 분산 환경에서 스케줄러 중복 실행 방지
- **다중 스케줄러**: 토큰 정리, 캐시 갱신, 점수 집계 등 7개 스케줄러 운영

### 데이터베이스 설계
- **JPA + QueryDSL**: 타입 안전한 동적 쿼리
- **지역 점수 시스템**: 사용자별 지역 활동 점수 집계
- **계층형 댓글**: 대댓글 구조 지원

## 팀원 및 담당 업무

| 이름 | 담당 영역 | 주요 기능 |
|---|---|---|
| **[박찬희](https://github.com/DevchannyP)** | 인증/마이페이지 | • JWT 기반 인증 시스템<br>• OAuth2 소셜 로그인<br>• 마이페이지 프로필 관리<br>• 지역 점수 시스템 |
| **[김기흔](https://github.com/kheun-jav)** | 정책 관련 | • 정책 데이터 관리<br>• 정책 검색 및 필터링<br>• 정책 상세 정보 제공<br>• 정책 즐겨찾기 기능<br>• 정책 추천 시스템 연동 |
| **[오예록](https://github.com/ye-rok)** | 커뮤니티/대시보드 | • 지역별 커뮤니티 게시판<br>• 게시글/댓글 CRUD<br>• 멘토 신청 및 관리<br>• 멘토 대시보드|
| **[원동인](https://github.com/WONDongin)** | 관리자/멘토 시스템 | • 관리자 페이지 전체<br>• 사용자/게시글/신고 관리<br>• 멘토 승인 시스템<br>• 멘토 공지사항 관리|

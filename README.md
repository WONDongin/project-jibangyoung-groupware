### ⭐ Local Youth Collaboration Platform (청년 지역 정착 지원 통합 그룹웨어)

<br>

### 한 줄 소개 (Key Point)

> **멘토 활동 기반 자동 승급 시스템과 관리자 운영 자동화를 중심으로 설계한 백엔드 중심 플랫폼** <Br>
> 단순 CRUD 서비스가 아닌, "활동 → 점수 → 권한 변화 → 운영 제어" 흐름을 자동화한 프로젝트입니다.

<br>

### 소개 (Overview)

> 지역 청년의 정착을 위해 정책 추천 · 커뮤니티 · 설문 · 멘토링 기능을  
하나의 서비스로 통합한 웹 애플리케이션입니다. <Br>

> Spring Boot + Next.js 기반 **모듈형 모놀리식 아키텍처**로 설계되었으며,  
사용자 역할에 따라 기능과 데이터 접근이 달라지는 구조를 구현했습니다.

<Br>

### ⚙️ 사용자 기능 (Features)

- 사용자
  - 정책 추천 및 지역 정보 탐색
  - 지역 커뮤니티 활동
  - 멘토 신청 / 설문 참여

- 멘토
  - 승인 기반 멘토 활동 가능
  - 등급 승급(활동 점수 기반)
  - 멘토링 기여 관리

- 관리자(Admin)
  - 사용자 전체 관리 (권한/상태 변경)
  - 멘토 승인 / 경고 / 승급 제어
  - 정책 정보 및 커뮤니티 운영 관리
  - 신고/제재 처리

- 인증 & 권한
  - `OAuth2` + `JWT` 기반 로그인
  - Role 기반 접근 제어 (`ADMIN` / `MENTOR` / `USER`)
  - 비인가 사용자 접근 차단

<Br>

### ⚙️ 기술 스택 (Tech Stack)
| 구분                      | 사용 기술                                               |
| ----------------------- | --------------------------------------------------- |
| **Frontend**            | Next.js(App Router), TypeScript, TailwindCSS        |
| **Backend**             | Spring Boot, JPA, QueryDSL                          |
| **Auth**                | OAuth2 로그인, JWT 인증                                  |
| **Database**            | MySQL, Redis(캐싱 도입 준비)                              |
| **Server**              | AWS EC2, NGINX, Docker                              |
| **Architecture**        | Modular Monolithic, Domain → Application → UI Layer |
| **CI/CD**               | GitHub Actions 기반 자동화                               |
| **Collaboration Tools** | GitHub Projects, Pull Request Review                |

<Br>

### 📂 프로젝트 구조 (Project Structure)

```bash
/backend
 ├── domain
 │     ├── admin/       # 관리자 도메인 API
 │     ├── mentor/      # 멘토 신청·승급 로직
 │     ├── policy/      # 정책 추천/조회
 │     ├── community/   # 지역 커뮤니티
 │     └── survey/      # 지역 기반 설문
 ├── auth/              # OAuth2 + JWT 인증
 └── config/            # Security & Infra 설정

/frontend
 ├── app/               # Next.js App Router 기반 UI
 └── components/        # 공통 UI 요소


```

<br>

### 📂 전체 아키텍처 (Architecture)
- 핵심 도메인 분리 + 계층화된 구조로 서비스 확장성 확보

```bash
[Client - Next.js] 
   ↓ (REST API)
[Spring Boot Application]
   ├─ Admin Domain
   ├─ Mentor Domain
   ├─ Policy / Community / Survey
   └─ Authentication (OAuth2, JWT)
   ↓
[MySQL + Redis]
   ↓
[AWS EC2 Deployment & GitHub Actions CI]
```

<br>

### ⚙️ 담당 개발 파트 (Core Logic)

### 1. 멘토 자동 승급 시스템 (핵심)

**문제**
- 기존 구조: 관리자 수동 승인 및 관리 필요
- 운영 비용 증가 + 실시간 반영 어려움

**해결**
- 멘토 활동 로그 기반 점수 계산
- `@Scheduled` 기반 자동 승급 처리
- `ShedLock` 적용으로 스케줄러 중복 실행 방지

**결과**
- 멘토 신청 → 승인 → 활동 → 승급으로 이어지는 단계별 흐름 설계
- 멘토 활동 로그(게시글, 댓글 등)를 기반으로 점수 산정 구조 구현
- 스케줄러를 통해 주기적으로 활동 점수 갱신 → 관리자 개입 최소화
- 멘토 등급(A / B / C)에 따른 권한 분리 및 기능 제한 적용
- 핵심 소스: `domain/mentor/*.java`
```
멘토 신청

  ↓ (신청 정보 저장)

관리자 승인 (1차 / 최종)

  ↓ (Role 변경: USER → MENTOR)

멘토 활동 로그 누적

  ↓ (활동 점수 계산 및 갱신)

멘토 등급 유지 / 자동 승급
```

<br>

### 2. Admin 운영 및 제어 흐름

- 관리자 중심의 플랫폼 운영 로직 통합 관리
- 사용자 / 멘토 / 신고 / 정책 데이터를 하나의 흐름으로 제어
- `QueryDSL` 기반 조건 검색 + 페이징 처리
- 운영 효율을 고려한 UI 연동 완료
- 모든 주요 제어 행위는 로그(Audit) 기록 구조로 관리
- 핵심 소스: `domain/admin/*.java`
```
신고 또는 운영 이슈 발생
   ↓
관리자 검토
   ↓
권한 조정 / 콘텐츠 블라인드 처리 / 상태 변경
   ↓
처리 이력 로그 저장
```

<br>

### 3. 인증 & 권한 제어
- 각 화면 접근은 자동으로 역할에 따라 분기됩니다. 

| 구성              | 설명                     |
| --------------- | ---------------------- |
| OAuth2 로그인      | Google/Social 로그인 연동   |
| JWT 토큰 인증       | Stateless 방식           |
| Spring Security | URI 접근 제어, Role 기반 필터링 |

<br>

### 4. CI/CD & DevOps
| 항목                    |         상태        |
| --------------------- | :---------------: |
| Backend Unit Test     |       🟢 성공       |
| Frontend Lint + Build |       🟢 성공       |
| Required Check        |       🟢 성공       |
| AWS Deploy (CD)       | 🔒 Secret 제거로 비활성 |
<img src="./docs/screenshots/img_1.png"  width="500"/>
<img src="./docs/screenshots/img_2.png"  width="500"/>

<Br>

### ⚡ 트러블슈팅 & 개선 경험
1. 스케줄러 중복 실행 문제 <br>
- 문제: 서버 재시작 시 스케줄러 중복 실행 가능성
  - 해결: `ShedLock` 적용
  - 결과: 단일 실행 보장 (멀티 인스턴스 대응)

<br>

2. 승급 로직 중복 실행 문제 (멱등성)
- 문제: 반복 실행 시 중복 승급 위험
  - 해결: 현재 `Role` 기준 조건부 업데이트
  - 결과: 동일 작업 반복 실행에도 데이터 일관성 유지

<br>

3. 조회 성능 개선 설계
- 문제: 게시글 및 인기 데이터 조회 빈도 높음
  - 해결: Redis 캐싱 구조 설계
  - 결과: DB 부하 감소 및 응답 속도 개선 방향 확보

<br>

### ⚙️ 설계 의도 (Design Decision)
- 모듈형 모놀리식 구조 선택 → 빠른 개발 + 도메인 중심 설계에 적합
- `Scheduler` 도입 → 운영 자동화 및 관리자 의존도 감소
- `QueryDSL` 사용 → 동적 쿼리 및 유지보수성 확보
- `Redis` 캐싱 고려 → 조회 성능 개선 및 확장성 대응

<Br>

### 📈 개선 포인트
- N+1 문제 개선 (`fetch join` 활용)
- `QueryDSL` 기반 검색 구조 개선
- 인증 구조 확장 (`JWT` + `OAuth2`)

<Br>

### 🔍 화면 예시 (Screenshots)
<img src="./docs/screenshots/img_3.png" width="700"/>
<img src="./docs/screenshots/img_4.png" width="700"/>
<img src="./docs/screenshots/img_5.png" width="700"/>
<img src="./docs/screenshots/img_6.png" width="700"/>
<img src="./docs/screenshots/img_7.png" width="700"/>

<Br>

### 📄 배운 점 (What I Learned)

- 모듈형 모놀리식 설계를 실 서비스에 적용
- QueryDSL 기반 조건 검색 & 도메인 구분
- OAuth2 + JWT 인증체계 구축 경험
- GitHub Actions 기반 CI 구현
- 협업 환경에서의 코드 리뷰/분담 경험
- 서비스 백엔드 핵심 도메인 주도 개발

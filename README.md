### ⭐ Local Youth Collaboration Platform (청년 지역 정착 지원 통합 그룹웨어)

<br>

### 소개 (Overview)

> 지역 청년의 정착을 위해 필요한 정책 추천 · 커뮤니티 · 설문 · 멘토링 기능을
하나의 서비스 플랫폼으로 통합한 웹 애플리케이션입니다. <Br>
Spring Boot + Next.js 기반 모듈형 모놀리식 아키텍처로 설계되었으며,
사용자 권한에 따라 다양한 기능을 제공하는 협업 시스템입니다.

<Br>

### ⚙️ 주요 기능 (Features)

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

### 🗄 핵심 로직 (Core Logic)

<br>

### 1. 멘토 승급 및 권한 흐름

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
- QueryDSL 기반 조건 검색 + 페이징 처리
- 운영 효율을 고려한 UI 연동 완료
- 모든 주요 제어 행위는 로그(Audit) 기록 구조로 관리
- 핵심 소스: domain/admin/*.java
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

<Br>

### 🔍 화면 예시 (Screenshots)

#### 관리자 페이지
<img src="./docs/screenshots/mainLms.png" />

#### 멘토 페이지
<img src="./docs/screenshots/deptLMS.png" />

#### 메인 페이지
<img src="./docs/screenshots/classLMS.png" />

<Br>

### 📄 배운 점 (What I Learned)

- 모듈형 모놀리식 설계를 실 서비스에 적용
- QueryDSL 기반 조건 검색 & 도메인 구분
- OAuth2 + JWT 인증체계 구축 경험
- GitHub Actions 기반 CI 구현
- 협업 환경에서의 코드 리뷰/분담 경험
- 서비스 백엔드 핵심 도메인 주도 개발

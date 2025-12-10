import { NextRequest, NextResponse } from "next/server";

export async function GET(
  req: NextRequest,
  { params }: { params: { provider: string } }
) {
  const { provider } = params;

  // [1] OAuth 인증 코드 등 파싱
  const url = new URL(req.url);
  const code = url.searchParams.get("code");

  // [2] provider별 분기 (카카오/네이버/구글)
  switch (provider) {
    case "kakao":
      // 카카오 OAuth 토큰 교환 → 유저 정보 조회 → JWT 발급 요청(백엔드 API)
      break;
    case "google":
      // 구글 OAuth 로직
      break;
    case "naver":
      // 네이버 OAuth 로직
      break;
    default:
      return NextResponse.redirect("/login?error=unknown_provider");
  }

  // [3] JWT를 쿠키(set-cookie)로 저장
  // 예: Response 쿠키 설정(실무는 보안 설정 포함)
  // NextResponse.cookies.set("accessToken", ...);

  // [4] 최종 리다이렉트(로그인 성공 후 메인/대시보드 등)
  return NextResponse.redirect("/dashboard");
}
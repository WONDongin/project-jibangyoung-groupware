interface RecommendationHeaderProps {
  username: string;
  regionName: string;
  rankGroup: number;
}

export function RecommendationHeader({ username, regionName, rankGroup }: RecommendationHeaderProps) {
  const getRankText = (rank: number) => {
    switch (rank) {
      case 1: return '1순위는';
      case 2: return '2순위는';
      case 3: return '3순위는';
      default: return `${rank}순위는`;
    }
  };

  return (
    <div className="recommendation-header">
      <h1 className="header-title">
        <span className="user-name">{username}</span>님의
      </h1>
      <h2 className="header-title">
        <span className="rank-text">추천지역 {getRankText(rankGroup)}</span>{' '}
        <span className="region-name">{regionName}</span>입니다.
      </h2>
      <p className="subtitle">
        다음과 같은 이유로 추천드립니다.
      </p>
    </div>
  );
}
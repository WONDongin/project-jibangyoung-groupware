import {
  BarElement,
  CategoryScale,
  Chart as ChartJS,
  Legend,
  LinearScale,
  Tooltip,
} from "chart.js";
import { Bar } from "react-chartjs-2";

ChartJS.register(CategoryScale, LinearScale, BarElement, Tooltip, Legend);

interface Props {
  notice: number;
  post: number;
  comment: number;
}

export function PostCommentBarChart({ notice, post, comment }: Props) {
  const data = {
    labels: ["공지", "게시글", "댓글"],
    datasets: [
      {
        label: "작성 수",
        data: [notice, post, comment],
        backgroundColor: ["#a5c8ff", "#b6d6ff", "#d1e3ff"],
        categoryPercentage: 0.8, // 카테고리 영역 비율
        barPercentage: 1, // 막대 자체의 비율
      },
    ],
  };

  const options = {
    indexAxis: "y" as const,
    plugins: { legend: { display: false } },
    scales: {
      x: {
        beginAtZero: true,
        ticks: { stepSize: 1 },
        grid: { display: false },
      },
      y: {
        grid: { display: false },
        ticks: { autoSkip: false }, // ← 라벨 생략 금지
      },
    },
    maintainAspectRatio: false,
  };

  return (
    <div style={{ height: 140, padding: "8px 0" }}>
      {" "}
      {/* 높이 ↑ */}
      <Bar data={data} options={options} />
    </div>
  );
}

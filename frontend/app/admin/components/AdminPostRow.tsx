import { RegionLookup } from "@/app/admin/hooks/useAdminRegion";
import { AdminPost } from "@/types/api/adminPost";
import styles from "../AdminPage.module.css";
interface AdminPostRowProps {
  post: AdminPost;
  index: number;
  searchResultLength: number;
  currentPage: number;
  ITEMS_PER_PAGE: number;
  regionMap: Map<number, RegionLookup>;
  onDelete: (id: number) => void;
  onRestore: (id: number) => void;
  processing: boolean;
}

export function AdminPostRow({
  post,
  index,
  searchResultLength,
  currentPage,
  ITEMS_PER_PAGE,
  regionMap,
  onDelete,
  onRestore,
  processing,
}: AdminPostRowProps) {
  const codePrefix = Math.floor(post.region_id / 1000) * 1000;

  const regionName = (() => {
    const exact = regionMap.get(post.region_id);
    if (exact) {
      const sido = (exact.sido ?? "").trim();
      const gu = (exact.guGun ?? "").trim();
      if (gu && gu !== sido) return `${sido} ${gu}`;
      return sido || String(post.region_id);
    }

    const parent = regionMap.get(codePrefix);
    return parent?.sido ?? String(post.region_id);
  })();

  return (
    <tr key={post.id} style={post.deleted ? { opacity: 0.6 } : {}}>
      <td>
        {searchResultLength - ((currentPage - 1) * ITEMS_PER_PAGE + index)}
      </td>
      <td>
        {post.title
          ? post.title.trim().length > 25
            ? post.title.trim().slice(0, 25) + "..."
            : post.title.trim()
          : ""}
      </td>
      <td>{post.nickname}</td>
      <td>{String(post.created_at).substring(0, 10)}</td>
      <td>{post.views}</td>
      <td>{post.likes}</td>
      <td>{regionName}</td>
      <td className={styles.actionCell}>
        <a
          href={`/community/${Math.floor(post.region_id / 1000)}/${post.id}`}
          target="_blank"
          rel="noreferrer"
          className={styles.urlButton}
        >
          URL
        </a>
        {post.deleted ? (
          <button
            onClick={() => onRestore(post.id)}
            className={styles.restoreButton}
            disabled={processing}
          >
            {processing ? "복구중..." : "복구"}
          </button>
        ) : (
          <button
            onClick={() => onDelete(post.id)}
            className={styles.deleteButton}
            disabled={processing}
          >
            {processing ? "삭제중..." : "삭제"}
          </button>
        )}
      </td>
    </tr>
  );
}

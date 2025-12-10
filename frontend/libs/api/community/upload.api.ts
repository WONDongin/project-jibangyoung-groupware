export async function getPresignedUrl(file: File) {
  const ext = file.name.split(".").pop();
  const fileName = `${crypto.randomUUID()}.${ext}`;
  const BASE = process.env.NEXT_PUBLIC_API_BASE_URL!;

  const res = await fetch(`${BASE}/api/community/presign`, {
    method: "POST",
    headers: { 
      "Content-Type": "application/json",
      "Authorization": `Bearer ${localStorage.getItem("accessToken")}`,
    },
    body: JSON.stringify({
      fileName,
      contentType: file.type,
    }),
  });

  if (!res.ok) {
    const errorText = await res.text();
    console.error("Presigned URL 발급 실패:", {
      status: res.status,
      statusText: res.statusText,
      body: errorText,
      url: res.url
    });
    throw new Error("Presigned URL 발급 실패");
  }

  return await res.json(); // { url, publicUrl }
}

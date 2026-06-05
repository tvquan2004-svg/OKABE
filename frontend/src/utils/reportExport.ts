import { getBaseUrl } from './urlHelper';

function getToken(): string | null {
  return localStorage.getItem('okabe_access_token');
}

export async function downloadExport(
  url: string,
  filename: string,
): Promise<void> {
  const token = getToken();
  const response = await fetch(`${getBaseUrl()}${url}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `Export failed (${response.status})`);
  }

  const blob = await response.blob();
  const blobUrl = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = blobUrl;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(blobUrl);
}

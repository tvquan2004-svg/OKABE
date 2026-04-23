export const getBaseUrl = () => {
  let envUrl = import.meta.env.VITE_API_BASE_URL as string;
  if (!envUrl) return 'http://localhost:8080';
  
  if (envUrl.endsWith('/')) envUrl = envUrl.slice(0, -1);
  
  // Remove /api/v1 if it exists to get the root server URL
  if (envUrl.endsWith('/api/v1')) {
    envUrl = envUrl.slice(0, -7);
  } else if (envUrl.endsWith('/v1')) {
    envUrl = envUrl.slice(0, -3);
  } else if (envUrl.endsWith('/api')) {
    envUrl = envUrl.slice(0, -4);
  }
  
  return envUrl;
};

export const getFullFileUrl = (url?: string) => {
  if (!url) return '';
  if (url.startsWith('http')) return url;
  
  const baseUrl = getBaseUrl();
  return `${baseUrl}${url}`;
};

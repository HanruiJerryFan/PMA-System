function trimTrailingSlash(value = "") {
  return value.replace(/\/+$/, "");
}

export function resolveApiBaseUrl() {
  const envBaseUrl = trimTrailingSlash(process.env.REACT_APP_API_BASE_URL || "");
  if (envBaseUrl) {
    return envBaseUrl.endsWith("/api") ? envBaseUrl : `${envBaseUrl}/api`;
  }

  if (typeof window !== "undefined") {
    const { hostname, origin } = window.location;
    if (hostname === "localhost" || hostname === "127.0.0.1") {
      return "http://localhost:8081/api";
    }
    return `${trimTrailingSlash(origin)}/api`;
  }

  return "http://localhost:8081/api";
}


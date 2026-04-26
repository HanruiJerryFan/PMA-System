import axios from "axios";
import { resolveApiBaseUrl } from "./baseUrl";

function redirectToPermissionDenied(message = "You do not have permission to perform this action.") {
  if (typeof window === "undefined") {
    return;
  }

  const currentPath = `${window.location.pathname}${window.location.search}`;
  if (window.location.pathname === "/permission-denied") {
    return;
  }

  sessionStorage.setItem("permission_denied_reason", message);
  window.location.href = `/permission-denied?from=${encodeURIComponent(currentPath)}`;
}

function redirectToChangePassword() {
  if (typeof window === "undefined") {
    return;
  }
  if (window.location.pathname === "/change-password") {
    return;
  }
  window.location.href = "/change-password";
}

const api = axios.create({
  baseURL: resolveApiBaseUrl(),
  withCredentials: true,
  timeout: 10000,
});

function buildApiError(error) {
  if (error.response?.data && typeof error.response.data === "object" && "message" in error.response.data) {
    return new Error(error.response.data.message || "Request failed");
  }
  if (error.response?.status === 403) {
    return new Error("Permission denied");
  }
  if (error.response?.status === 401) {
    return new Error("Authentication required");
  }
  if (error.message === "Network Error") {
    return new Error("Network error or backend unavailable");
  }
  return error;
}

api.interceptors.request.use(
  (config) => {
    console.log("Request", config.method?.toUpperCase(), config.url);
    return config;
  },
  (error) => Promise.reject(error)
);

api.interceptors.response.use(
  (response) => {
    const responseData = response.data;
    const isApiResponse =
      responseData &&
      !Array.isArray(responseData) &&
      typeof responseData === "object" &&
      typeof responseData.code === "number" &&
      "message" in responseData &&
      "data" in responseData;

    if (isApiResponse) {
      if (response.data.code === 200) {
        return {
          ...response,
          data: response.data.data,
        };
      }
      const error = new Error(response.data.message || "Request failed");
      error.code = response.data.code;
      error.response = response;
      return Promise.reject(error);
    }
    return response;
  },
  (error) => {
    if (error.response?.status === 401) {
      window.location.href = "/login";
    }
    if (error.response?.status === 403 && error.response?.data === "Password change required") {
      redirectToChangePassword();
    } else if (error.response?.status === 403) {
      redirectToPermissionDenied("You do not have permission to perform this action.");
    }
    return Promise.reject(buildApiError(error));
  }
);

export default api;

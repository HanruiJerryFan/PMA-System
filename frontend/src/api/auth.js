import axios from "axios";
import { resolveApiBaseUrl } from "./baseUrl";

const BASE_URL = `${resolveApiBaseUrl()}/auth`;
const PASSWORD_CHANGE_REQUIRED = "Password change required";

function redirectToChangePassword() {
  if (typeof window === "undefined" || window.location.pathname === "/change-password") {
    return;
  }
  window.location.href = "/change-password";
}

export async function login(username, password) {
  const response = await axios.post(
    `${BASE_URL}/login`,
    { username, password },
    { withCredentials: true }
  );
  return response.data;
}

export async function logout() {
  const response = await axios.post(
    `${BASE_URL}/logout`,
    {},
    { withCredentials: true }
  );
  return response.data;
}

export async function heartbeat() {
  try {
    const response = await axios.post(
      `${BASE_URL}/heartbeat`,
      {},
      { withCredentials: true }
    );
    return response.data;
  } catch (error) {
    if (error.response?.status === 403 && error.response?.data === PASSWORD_CHANGE_REQUIRED) {
      redirectToChangePassword();
    }
    throw error;
  }
}

export function sendLogoutBeacon() {
  if (typeof navigator === "undefined") {
    return false;
  }

  try {
    return navigator.sendBeacon(
      `${BASE_URL}/logout-beacon`,
      new Blob([], { type: "application/json" })
    );
  } catch (error) {
    return false;
  }
}

export async function register(username, password, realName) {
  const response = await axios.post(`${BASE_URL}/register`, {
    username,
    password,
    realName,
  });
  return response.data;
}

export async function changePassword(oldPassword, newPassword) {
  const response = await axios.post(
    `${BASE_URL}/change-password`,
    { oldPassword, newPassword },
    { withCredentials: true }
  );
  return response.data;
}

export async function getCurrentUser() {
  try {
    const response = await axios.get(`${BASE_URL}/me`, { withCredentials: true });
    return response.data;
  } catch (error) {
    if (error.response && error.response.status === 401) {
      return null;
    }
    throw error;
  }
}

import React from "react";
import { act, render, screen, waitFor } from "@testing-library/react";
import { AuthProvider, useAuth } from "./AuthContext";
import { getCurrentUser } from "../api/auth";

jest.mock("../api/auth", () => ({
  getCurrentUser: jest.fn(),
}));

function AuthConsumer({ label }) {
  const { currentUser, loggedIn, loading } = useAuth();
  return (
    <div data-testid={label}>
      {loading ? "loading" : loggedIn ? currentUser?.username : "guest"}
    </div>
  );
}

function AuthController() {
  const { setAuthState, clearAuth, refreshCurrentUser } = useAuth();
  return (
    <div>
      <button type="button" onClick={() => setAuthState(true, { id: 2, username: "bob" })}>
        set-bob
      </button>
      <button type="button" onClick={() => clearAuth()}>
        clear
      </button>
      <button type="button" onClick={() => refreshCurrentUser()}>
        refresh
      </button>
    </div>
  );
}

describe("AuthContext", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test("fetches current user once across multiple consumers and propagates auth transitions", async () => {
    getCurrentUser
      .mockResolvedValueOnce({ id: 1, username: "alice", authorities: ["customer.access"] })
      .mockResolvedValueOnce({ id: 3, username: "carol", authorities: [] });

    render(
      <AuthProvider>
        <AuthConsumer label="consumer-a" />
        <AuthConsumer label="consumer-b" />
        <AuthController />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId("consumer-a").textContent).toBe("alice");
      expect(screen.getByTestId("consumer-b").textContent).toBe("alice");
    });
    expect(getCurrentUser).toHaveBeenCalledTimes(1);

    await act(async () => {
      screen.getByText("set-bob").click();
    });
    expect(screen.getByTestId("consumer-a").textContent).toBe("bob");
    expect(screen.getByTestId("consumer-b").textContent).toBe("bob");

    await act(async () => {
      screen.getByText("clear").click();
    });
    expect(screen.getByTestId("consumer-a").textContent).toBe("guest");
    expect(screen.getByTestId("consumer-b").textContent).toBe("guest");

    await act(async () => {
      screen.getByText("refresh").click();
    });
    await waitFor(() => {
      expect(screen.getByTestId("consumer-a").textContent).toBe("carol");
      expect(screen.getByTestId("consumer-b").textContent).toBe("carol");
    });
    expect(getCurrentUser).toHaveBeenCalledTimes(2);
  });

  test("provides safe default fallback when rendered outside AuthProvider", () => {
    render(<AuthConsumer label="isolated" />);
    expect(screen.getByTestId("isolated").textContent).toBe("guest");
    expect(getCurrentUser).not.toHaveBeenCalled();
  });
});

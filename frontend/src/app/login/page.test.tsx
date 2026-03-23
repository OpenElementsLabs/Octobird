import { describe, it, expect, afterEach } from "vitest";
import { render, screen, cleanup } from "@testing-library/react";
import LoginPage from "./page";

afterEach(cleanup);

describe("LoginPage", () => {
  it("displays the Login with GitHub button", () => {
    render(<LoginPage />);

    expect(screen.getByText("Login with GitHub")).toBeInTheDocument();
  });

  it("login button links to /auth/login", () => {
    render(<LoginPage />);

    const link = screen.getByText("Login with GitHub").closest("a");
    expect(link).toHaveAttribute("href", "/auth/login");
  });

  it("displays the Octobird heading", () => {
    render(<LoginPage />);

    expect(screen.getByRole("heading", { name: "Octobird" })).toBeInTheDocument();
  });

  it("displays the dashboard description", () => {
    render(<LoginPage />);

    expect(
      screen.getByText("GitHub Bot Configuration Dashboard")
    ).toBeInTheDocument();
  });
});

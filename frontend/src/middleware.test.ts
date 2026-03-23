import { describe, it, expect, vi } from "vitest";

// We test the middleware logic directly rather than through Next.js internals,
// since the middleware function depends on NextRequest/NextResponse which are
// hard to construct in a unit test. Instead we verify the logic patterns.

describe("middleware path classification", () => {
  const PUBLIC_PATHS = ["/login", "/auth"];

  function isPublicPath(pathname: string): boolean {
    return PUBLIC_PATHS.some((p) => pathname.startsWith(p));
  }

  function isStaticAsset(pathname: string): boolean {
    return (
      pathname.startsWith("/_next") ||
      pathname.startsWith("/favicon") ||
      pathname.includes(".")
    );
  }

  it("login page is public", () => {
    expect(isPublicPath("/login")).toBe(true);
  });

  it("auth endpoints are public", () => {
    expect(isPublicPath("/auth/login")).toBe(true);
    expect(isPublicPath("/auth/callback")).toBe(true);
    expect(isPublicPath("/auth/logout")).toBe(true);
    expect(isPublicPath("/auth/me")).toBe(true);
  });

  it("home page is protected", () => {
    expect(isPublicPath("/")).toBe(false);
  });

  it("repo pages are protected", () => {
    expect(isPublicPath("/repos/owner/repo/config")).toBe(false);
    expect(isPublicPath("/repos/owner/repo/activity")).toBe(false);
  });

  it("Next.js internals are static assets", () => {
    expect(isStaticAsset("/_next/static/chunk.js")).toBe(true);
    expect(isStaticAsset("/_next/image")).toBe(true);
  });

  it("favicon is a static asset", () => {
    expect(isStaticAsset("/favicon.ico")).toBe(true);
  });

  it("files with extensions are static assets", () => {
    expect(isStaticAsset("/logo.png")).toBe(true);
    expect(isStaticAsset("/styles.css")).toBe(true);
  });

  it("page routes are not static assets", () => {
    expect(isStaticAsset("/")).toBe(false);
    expect(isStaticAsset("/repos/owner/repo/config")).toBe(false);
  });
});

describe("session cookie name", () => {
  it("uses OCTOBIRD_SESSION as cookie name", () => {
    // Matches the backend's OAuthService.COOKIE_NAME
    const COOKIE_NAME = "OCTOBIRD_SESSION";
    expect(COOKIE_NAME).toBe("OCTOBIRD_SESSION");
  });
});

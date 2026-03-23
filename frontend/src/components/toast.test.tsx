import { describe, it, expect, vi, afterEach } from "vitest";
import { render, screen, cleanup, fireEvent } from "@testing-library/react";
import { ToastContainer } from "./toast";

afterEach(cleanup);

describe("ToastContainer", () => {
  it("renders success toast with green styling", () => {
    const toasts = [{ id: 1, message: "Saved!", type: "success" as const }];
    render(<ToastContainer toasts={toasts} onRemove={() => {}} />);

    const toast = screen.getByText("Saved!").closest("div.bg-oe-green, [class*='bg-oe-green']");
    expect(toast).toBeInTheDocument();
  });

  it("renders error toast with red styling", () => {
    const toasts = [{ id: 1, message: "Failed!", type: "error" as const }];
    render(<ToastContainer toasts={toasts} onRemove={() => {}} />);

    const toast = screen.getByText("Failed!").closest("div");
    expect(toast?.className).toContain("bg-oe-red");
  });

  it("calls onRemove when close button is clicked", () => {
    const onRemove = vi.fn();
    const toasts = [{ id: 42, message: "Test", type: "success" as const }];
    render(<ToastContainer toasts={toasts} onRemove={onRemove} />);

    // Use fireEvent instead of userEvent to avoid async issues
    const closeButton = screen.getByRole("button");
    fireEvent.click(closeButton);

    expect(onRemove).toHaveBeenCalledWith(42);
  });

  it("renders multiple toasts", () => {
    const toasts = [
      { id: 1, message: "First", type: "success" as const },
      { id: 2, message: "Second", type: "error" as const },
    ];
    render(<ToastContainer toasts={toasts} onRemove={() => {}} />);

    expect(screen.getByText("First")).toBeInTheDocument();
    expect(screen.getByText("Second")).toBeInTheDocument();
  });

  it("renders nothing when toasts array is empty", () => {
    const { container } = render(
      <ToastContainer toasts={[]} onRemove={() => {}} />
    );

    expect(container.querySelectorAll("button").length).toBe(0);
  });
});

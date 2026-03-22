"use client";

import { useState } from "react";

interface CollapsibleSectionProps {
  title: string;
  defaultOpen?: boolean;
  children: React.ReactNode;
}

export default function CollapsibleSection({
  title,
  defaultOpen = true,
  children,
}: CollapsibleSectionProps) {
  const [isOpen, setIsOpen] = useState(defaultOpen);

  return (
    <div className="rounded-lg border border-oe-light-gray">
      <button
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        className="flex w-full items-center justify-between px-5 py-3 text-left font-heading text-sm font-semibold text-oe-dark transition-colors hover:bg-oe-light-gray/50"
      >
        {title}
        <svg
          className={`h-4 w-4 text-oe-mid-gray transition-transform ${
            isOpen ? "rotate-180" : ""
          }`}
          fill="none"
          viewBox="0 0 24 24"
          strokeWidth={2}
          stroke="currentColor"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            d="M19.5 8.25l-7.5 7.5-7.5-7.5"
          />
        </svg>
      </button>
      {isOpen && <div className="border-t border-oe-light-gray px-5 py-4">{children}</div>}
    </div>
  );
}

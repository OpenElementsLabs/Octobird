"use client";

import { useState } from "react";

interface ListEditorProps {
  items: string[];
  onChange: (items: string[]) => void;
  placeholder?: string;
}

export default function ListEditor({
  items,
  onChange,
  placeholder = "Add item...",
}: ListEditorProps) {
  const [input, setInput] = useState("");

  const addItem = () => {
    const trimmed = input.trim();
    if (trimmed && !items.includes(trimmed)) {
      onChange([...items, trimmed]);
      setInput("");
    }
  };

  const removeItem = (index: number) => {
    onChange(items.filter((_, i) => i !== index));
  };

  return (
    <div className="space-y-2">
      {items.length > 0 && (
        <ul className="space-y-1">
          {items.map((item, i) => (
            <li
              key={i}
              className="flex items-center justify-between rounded bg-oe-light-gray/50 px-3 py-1.5 text-sm"
            >
              <span>{item}</span>
              <button
                type="button"
                onClick={() => removeItem(i)}
                className="text-oe-red text-xs font-medium hover:underline"
              >
                Remove
              </button>
            </li>
          ))}
        </ul>
      )}
      <div className="flex gap-2">
        <input
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && (e.preventDefault(), addItem())}
          placeholder={placeholder}
          className="flex-1 rounded border border-oe-light-gray px-3 py-1.5 text-sm focus:border-oe-green focus:outline-none focus:ring-1 focus:ring-oe-green"
        />
        <button
          type="button"
          onClick={addItem}
          className="rounded bg-oe-green px-3 py-1.5 text-xs font-medium text-white hover:bg-oe-green-dark"
        >
          Add
        </button>
      </div>
    </div>
  );
}

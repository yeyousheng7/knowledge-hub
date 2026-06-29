import { describe, expect, it } from "vitest";

import { parseKhSourceHref } from "@/shared/markdown/kh-source";

describe("parseKhSourceHref", () => {
  it("maps private and public note sources to internal routes", () => {
    expect(parseKhSourceHref("kh-source://note/123")).toEqual({
      kind: "note",
      noteId: 123,
      path: "/notes/123",
    });
    expect(parseKhSourceHref("kh-source://public-note/456")).toEqual({
      kind: "public-note",
      noteId: 456,
      path: "/public/notes/456",
    });
  });

  it.each([
    "kh-source://unknown/1",
    "kh-source://note/not-a-number",
    "kh-source://note/0",
    "kh-source://note/-1",
    "kh-source://note/1/extra",
    "kh-source://note/1?redirect=/login",
    "kh-source://note/1#fragment",
    "javascript:alert(1)",
    "https://example.com/note/1",
  ])("rejects unsafe or unsupported source %s", (href) => {
    expect(parseKhSourceHref(href)).toBeNull();
  });
});

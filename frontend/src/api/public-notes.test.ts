import {
  buildPublicNoteListPath,
  buildPublicUserNotesPath,
} from "@/api/public-notes";
import { describe, expect, it } from "vitest";

describe("public notes API", () => {
  it("builds public note list paths with validated pagination and keyword", () => {
    expect(
      buildPublicNoteListPath({
        page: 2,
        size: 20,
        keyword: "  RAG  ",
      }),
    ).toBe("/public/notes?page=2&size=20&keyword=RAG");
  });

  it("rejects unsupported public note query values", () => {
    expect(() => buildPublicNoteListPath({ page: 0, size: 20 })).toThrow(
      /page/,
    );
    expect(() => buildPublicNoteListPath({ page: 1, size: 101 })).toThrow(
      /size/,
    );
    expect(() =>
      buildPublicNoteListPath({ page: 1, size: 20, keyword: "x".repeat(101) }),
    ).toThrow(/keyword/);
  });

  it("builds public user note list paths without adding unsupported filters", () => {
    expect(
      buildPublicUserNotesPath({
        username: "alice_01",
        page: 1,
        size: 10,
      }),
    ).toBe("/public/users/alice_01/notes?page=1&size=10");
  });
});

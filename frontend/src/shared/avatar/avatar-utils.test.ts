import { describe, expect, it } from "vitest";

import { getAvatarIdentity } from "@/shared/avatar/avatar-utils";

describe("getAvatarIdentity", () => {
  it("uses and uppercases the first latin nickname character", () => {
    expect(getAvatarIdentity("  alice", "fallback")).toMatchObject({
      name: "alice",
      label: "A",
    });
  });

  it("keeps the first Chinese character", () => {
    expect(getAvatarIdentity("小明", "ming").label).toBe("小");
  });

  it("falls back to username when nickname is blank", () => {
    expect(getAvatarIdentity("   ", "backend_user")).toMatchObject({
      name: "backend_user",
      label: "B",
    });
  });

  it("maps the same complete name to a stable color", () => {
    const first = getAvatarIdentity("Knowledge", "user");
    const second = getAvatarIdentity("Knowledge", "another-user");

    expect(first.colorClassName).toBe(second.colorClassName);
  });
});

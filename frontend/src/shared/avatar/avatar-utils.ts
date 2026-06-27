const AVATAR_COLORS = [
  "bg-blue-600",
  "bg-violet-600",
  "bg-emerald-600",
  "bg-amber-600",
  "bg-rose-600",
  "bg-cyan-600",
] as const;

export interface AvatarIdentity {
  label: string;
  colorClassName: (typeof AVATAR_COLORS)[number];
  name: string;
}

function displayName(nickname: string, username: string): string {
  return nickname.trim() || username.trim() || "?";
}

function stableHash(value: string): number {
  let hash = 2_166_136_261;

  for (const character of value) {
    hash ^= character.codePointAt(0) ?? 0;
    hash = Math.imul(hash, 16_777_619);
  }

  return hash >>> 0;
}

export function getAvatarIdentity(
  nickname: string,
  username: string,
): AvatarIdentity {
  const name = displayName(nickname, username);
  const firstCharacter = Array.from(name)[0] ?? "?";
  const label = /^[a-z]$/i.test(firstCharacter)
    ? firstCharacter.toUpperCase()
    : firstCharacter;

  return {
    name,
    label,
    colorClassName: AVATAR_COLORS[stableHash(name) % AVATAR_COLORS.length],
  };
}

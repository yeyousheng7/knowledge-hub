export type KhSourceKind = "note" | "public-note";

export interface KhSourceTarget {
  kind: KhSourceKind;
  noteId: number;
  path: string;
}

export function parseKhSourceHref(href: string | undefined): KhSourceTarget | null {
  if (!href?.toLowerCase().startsWith("kh-source:")) {
    return null;
  }

  let url: URL;

  try {
    url = new URL(href);
  } catch {
    return null;
  }

  if (
    url.protocol !== "kh-source:" ||
    url.username !== "" ||
    url.password !== "" ||
    url.port !== "" ||
    url.search !== "" ||
    url.hash !== ""
  ) {
    return null;
  }

  const kind = url.hostname;
  const idText = url.pathname.slice(1);

  if (
    (kind !== "note" && kind !== "public-note") ||
    !/^[1-9]\d*$/.test(idText)
  ) {
    return null;
  }

  const noteId = Number(idText);

  if (!Number.isSafeInteger(noteId)) {
    return null;
  }

  return {
    kind,
    noteId,
    path: kind === "note" ? `/notes/${noteId}` : `/public/notes/${noteId}`,
  };
}

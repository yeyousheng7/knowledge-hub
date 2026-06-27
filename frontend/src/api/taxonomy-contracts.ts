import {
  isRecord,
  readArray,
  readSafeInteger,
  readString,
} from "@/api/contracts";

export interface TaxonomyListItemResponse {
  id: number;
  name: string;
  createdAt: string;
  updatedAt: string;
}

export type CategoryListItemResponse = TaxonomyListItemResponse;
export type TagListItemResponse = TaxonomyListItemResponse;

function parseTaxonomyItem(
  value: unknown,
  type: "category" | "tag",
): TaxonomyListItemResponse {
  if (!isRecord(value)) {
    throw new TypeError(`Expected ${type} data`);
  }

  return {
    id: readSafeInteger(value, "id"),
    name: readString(value, "name"),
    createdAt: readString(value, "createdAt"),
    updatedAt: readString(value, "updatedAt"),
  };
}

export function parseCategoryListResponse(
  value: unknown,
): CategoryListItemResponse[] {
  if (!isRecord(value)) {
    throw new TypeError("Expected category list data");
  }

  return readArray(value, "items").map(
    (item) => parseTaxonomyItem(item, "category"),
  );
}

export function parseTagListResponse(value: unknown): TagListItemResponse[] {
  if (!isRecord(value)) {
    throw new TypeError("Expected tag list data");
  }

  return readArray(value, "items").map(
    (item) => parseTaxonomyItem(item, "tag"),
  );
}

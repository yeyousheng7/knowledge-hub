import { apiClient } from "@/api/client";
import {
  parseCategoryListResponse,
  parseCategoryResponse,
  parseTagListResponse,
  parseTagResponse,
  type CategoryListItemResponse,
  type TagListItemResponse,
} from "@/api/taxonomy-contracts";

function normalizeTaxonomyName(name: string): string {
  const normalized = name.trim();

  if (!normalized) {
    throw new RangeError("name must not be blank");
  }
  if (normalized.length > 30) {
    throw new RangeError("name must not exceed 30 characters");
  }

  return normalized;
}

export function getCategories(
  signal?: AbortSignal,
): Promise<CategoryListItemResponse[]> {
  return apiClient.request("/categories", {
    method: "GET",
    signal,
    parseData: parseCategoryListResponse,
  });
}

export function getTags(signal?: AbortSignal): Promise<TagListItemResponse[]> {
  return apiClient.request("/tags", {
    method: "GET",
    signal,
    parseData: parseTagListResponse,
  });
}

export function createCategory(
  name: string,
): Promise<CategoryListItemResponse> {
  return apiClient.request("/categories", {
    method: "POST",
    body: JSON.stringify({ name: normalizeTaxonomyName(name) }),
    parseData: parseCategoryResponse,
  });
}

export function createTag(name: string): Promise<TagListItemResponse> {
  return apiClient.request("/tags", {
    method: "POST",
    body: JSON.stringify({ name: normalizeTaxonomyName(name) }),
    parseData: parseTagResponse,
  });
}

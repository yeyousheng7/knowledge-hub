import { apiClient } from "@/api/client";
import {
  parseCategoryListResponse,
  parseTagListResponse,
  type CategoryListItemResponse,
  type TagListItemResponse,
} from "@/api/taxonomy-contracts";

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

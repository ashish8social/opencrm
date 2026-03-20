export interface CrmRecord {
  id: string;
  entityDefId: string;
  name: string;
  data: Record<string, unknown>;
  ownerId?: string;
  createdBy?: string;
  createdAt: string;
  updatedAt: string;
}

export interface PagedResult<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: string;
}

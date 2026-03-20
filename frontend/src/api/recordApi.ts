import client from './client';
import { CrmRecord, PagedResult, ApiResponse } from '../types/record';

export interface ListParams {
  page?: number;
  size?: number;
  sort?: string;
  filter?: string;
  q?: string;
}

export const recordApi = {
  list: (entityApiName: string, params: ListParams = {}) =>
    client.get<ApiResponse<PagedResult<CrmRecord>>>(`/data/${entityApiName}`, { params })
      .then(r => r.data.data),

  get: (entityApiName: string, id: string) =>
    client.get<ApiResponse<CrmRecord>>(`/data/${entityApiName}/${id}`).then(r => r.data.data),

  create: (entityApiName: string, data: Record<string, unknown>) =>
    client.post<ApiResponse<CrmRecord>>(`/data/${entityApiName}`, data).then(r => r.data.data),

  update: (entityApiName: string, id: string, data: Record<string, unknown>) =>
    client.put<ApiResponse<CrmRecord>>(`/data/${entityApiName}/${id}`, data).then(r => r.data.data),

  delete: (entityApiName: string, id: string) =>
    client.delete(`/data/${entityApiName}/${id}`),

  related: (entityApiName: string, id: string, relatedEntityApiName: string) =>
    client.get<ApiResponse<CrmRecord[]>>(`/data/${entityApiName}/${id}/related/${relatedEntityApiName}`)
      .then(r => r.data.data),

  search: (q: string, entities?: string) =>
    client.get<ApiResponse<CrmRecord[]>>('/search', { params: { q, entities } })
      .then(r => r.data.data),
};

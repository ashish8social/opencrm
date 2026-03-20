import client from './client';
import { EntityDef, FieldDef } from '../types/metadata';
import { ApiResponse } from '../types/record';

export const metadataApi = {
  listEntities: () =>
    client.get<ApiResponse<EntityDef[]>>('/metadata/entities').then(r => r.data.data),

  getEntity: (apiName: string) =>
    client.get<ApiResponse<EntityDef>>(`/metadata/entities/${apiName}`).then(r => r.data.data),

  createEntity: (data: Partial<EntityDef>) =>
    client.post<ApiResponse<EntityDef>>('/metadata/entities', data).then(r => r.data.data),

  updateEntity: (apiName: string, data: Partial<EntityDef>) =>
    client.put<ApiResponse<EntityDef>>(`/metadata/entities/${apiName}`, data).then(r => r.data.data),

  deleteEntity: (apiName: string) =>
    client.delete(`/metadata/entities/${apiName}`),

  listFields: (entityApiName: string) =>
    client.get<ApiResponse<FieldDef[]>>(`/metadata/entities/${entityApiName}/fields`).then(r => r.data.data),

  createField: (entityApiName: string, data: Partial<FieldDef>) =>
    client.post<ApiResponse<FieldDef>>(`/metadata/entities/${entityApiName}/fields`, data).then(r => r.data.data),

  updateField: (entityApiName: string, fieldApiName: string, data: Partial<FieldDef>) =>
    client.put<ApiResponse<FieldDef>>(`/metadata/entities/${entityApiName}/fields/${fieldApiName}`, data).then(r => r.data.data),

  deleteField: (entityApiName: string, fieldApiName: string) =>
    client.delete(`/metadata/entities/${entityApiName}/fields/${fieldApiName}`),
};

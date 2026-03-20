import client from './client';
import { ApiResponse, CrmRecord } from '../types/record';

export const salesApi = {
  generateQuotePdf: (quoteId: string) =>
    client.get(`/sales/quotes/${quoteId}/pdf`, { responseType: 'blob' }).then(r => r.data),

  convertQuoteToOrder: (quoteId: string) =>
    client.post<ApiResponse<CrmRecord>>(`/sales/quotes/${quoteId}/convert-to-order`).then(r => r.data.data),
};

import client from './client';
import { ApiResponse } from '../types/record';

export interface PipelineSummary {
  stage: string;
  count: number;
  totalAmount: number;
}

export interface RevenueByMonth {
  month: string;
  amount: number;
}

export interface TopAccount {
  id: string;
  name: string;
  totalAmount: number;
  dealCount: number;
}

export const reportApi = {
  pipelineSummary: () =>
    client.get<ApiResponse<PipelineSummary[]>>('/reports/pipeline-summary').then(r => r.data.data),

  revenueByMonth: () =>
    client.get<ApiResponse<RevenueByMonth[]>>('/reports/revenue-by-month').then(r => r.data.data),

  topAccounts: () =>
    client.get<ApiResponse<TopAccount[]>>('/reports/top-accounts').then(r => r.data.data),

  recordCounts: () =>
    client.get<ApiResponse<Record<string, number>>>('/reports/record-counts').then(r => r.data.data),
};

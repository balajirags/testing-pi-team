import { z } from 'zod';

export const CampaignStatusSchema = z.enum([
  'DRAFT',
  'ACTIVE',
  'PAUSED',
  'COMPLETED',
  'ARCHIVED',
]);

export type CampaignStatus = z.infer<typeof CampaignStatusSchema>;

export const CampaignSchema = z.object({
  id: z.string(),
  brandId: z.string(),
  adAccountId: z.string(),
  name: z.string(),
  budget: z.number().nullable().optional(),
  currency: z.string(),
  channel: z.string(),
  externalCampaignId: z.string(),
  status: CampaignStatusSchema,
  startDate: z.string().nullable().optional(),
  endDate: z.string().nullable().optional(),
  createdAt: z.string(),
  updatedAt: z.string(),
});

export type Campaign = z.infer<typeof CampaignSchema>;

export const PageCampaignSchema = z.object({
  content: z.array(CampaignSchema),
  totalElements: z.number().optional(),
  totalPages: z.number().optional(),
  size: z.number().optional(),
  number: z.number().optional(),
});

export type PageCampaign = z.infer<typeof PageCampaignSchema>;

export interface CreateCampaignInput {
  brandId: string;
  adAccountId: string;
  name: string;
  budget?: number;
  currency?: string;
  channel: string;
  externalCampaignId: string;
}

export interface UpdateCampaignInput {
  name?: string;
  budget?: number;
  currency?: string;
  status?: CampaignStatus;
  startDate?: string;
  endDate?: string;
}

export const CsvRowErrorSchema = z.object({
  rowNumber: z.number(),
  message: z.string(),
});

export type CsvRowError = z.infer<typeof CsvRowErrorSchema>;

export const CsvImportSummarySchema = z.object({
  total: z.number(),
  created: z.number(),
  failed: z.number(),
  errors: z.array(CsvRowErrorSchema),
});

export type CsvImportSummary = z.infer<typeof CsvImportSummarySchema>;

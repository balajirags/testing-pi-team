import {
  CampaignSchema,
  PageCampaignSchema,
  CsvImportSummarySchema,
  Campaign,
  PageCampaign,
  CsvImportSummary,
  CreateCampaignInput,
  UpdateCampaignInput,
} from '@/types/campaign';

const API_BASE = '/api/v1/campaigns';

export async function fetchCampaigns(params?: {
  brandId?: string;
  channel?: string;
  status?: string;
  page?: number;
  size?: number;
}): Promise<PageCampaign> {
  const query = new URLSearchParams();
  if (params?.brandId) query.append('brandId', params.brandId);
  if (params?.channel) query.append('channel', params.channel);
  if (params?.status) query.append('status', params.status);
  if (params?.page !== undefined) query.append('page', params.page.toString());
  if (params?.size !== undefined) query.append('size', params.size.toString());

  const url = `${API_BASE}?${query.toString()}`;
  const response = await fetch(url);

  if (!response.ok) {
    throw new Error(`Failed to fetch campaigns: ${response.statusText}`);
  }

  const rawData: unknown = await response.json();
  return PageCampaignSchema.parse(rawData);
}

export async function createCampaign(
  data: CreateCampaignInput
): Promise<Campaign> {
  const response = await fetch(API_BASE, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const errorData = (await response.json().catch(() => ({}))) as {
      detail?: string;
      invalidParams?: Record<string, string>;
    };
    const detailMsg =
      errorData.detail ||
      (errorData.invalidParams
        ? Object.values(errorData.invalidParams).join(', ')
        : 'Failed to create campaign');
    throw new Error(detailMsg);
  }

  const rawData: unknown = await response.json();
  return CampaignSchema.parse(rawData);
}

export async function updateCampaign(
  id: string,
  data: UpdateCampaignInput
): Promise<Campaign> {
  const response = await fetch(`${API_BASE}/${id}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const errorData = (await response.json().catch(() => ({}))) as {
      detail?: string;
      invalidParams?: Record<string, string>;
    };
    const detailMsg =
      errorData.detail ||
      (errorData.invalidParams
        ? Object.values(errorData.invalidParams).join(', ')
        : 'Failed to update campaign');
    throw new Error(detailMsg);
  }

  const rawData: unknown = await response.json();
  return CampaignSchema.parse(rawData);
}

export async function deleteCampaign(id: string): Promise<void> {
  const response = await fetch(`${API_BASE}/${id}`, {
    method: 'DELETE',
  });

  if (!response.ok) {
    throw new Error(`Failed to delete campaign: ${response.statusText}`);
  }
}

export async function importCampaignsCsv(file: File): Promise<CsvImportSummary> {
  const formData = new FormData();
  formData.append('file', file);

  const response = await fetch(`${API_BASE}/import`, {
    method: 'POST',
    body: formData,
  });

  if (!response.ok) {
    const errorData = (await response.json().catch(() => ({}))) as {
      detail?: string;
    };
    throw new Error(errorData.detail || `CSV import failed: ${response.statusText}`);
  }

  const rawData: unknown = await response.json();
  return CsvImportSummarySchema.parse(rawData);
}

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { fetchCampaigns, createCampaign, updateCampaign, deleteCampaign } from '../api/campaigns';

global.fetch = vi.fn();

describe('Campaigns API Client', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should fetch and parse campaigns list successfully', async () => {
    const mockResponse = {
      content: [
        {
          id: 'cmp-1',
          brandId: 'brand-1',
          adAccountId: 'ad-1',
          name: 'Summer Promo',
          budget: 1000,
          currency: 'USD',
          channel: 'META',
          externalCampaignId: 'ext-1',
          status: 'DRAFT',
          createdAt: '2026-08-21T00:00:00Z',
          updatedAt: '2026-08-21T00:00:00Z',
        },
      ],
      totalElements: 1,
      totalPages: 1,
    };

    (fetch as unknown as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      ok: true,
      json: async () => mockResponse,
    });

    const result = await fetchCampaigns({ channel: 'META' });

    expect(result.content).toHaveLength(1);
    expect(result.content[0].name).toBe('Summer Promo');
    expect(fetch).toHaveBeenCalledWith('/api/v1/campaigns?channel=META');
  });

  it('should post and parse new campaign creation', async () => {
    const mockCreated = {
      id: 'cmp-2',
      brandId: 'b-1',
      adAccountId: 'a-1',
      name: 'New Meta Campaign',
      budget: 5000,
      currency: 'USD',
      channel: 'META',
      externalCampaignId: 'meta-ext-2',
      status: 'DRAFT',
      createdAt: '2026-08-21T00:00:00Z',
      updatedAt: '2026-08-21T00:00:00Z',
    };

    (fetch as unknown as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      ok: true,
      json: async () => mockCreated,
    });

    const input = {
      brandId: 'b-1',
      adAccountId: 'a-1',
      name: 'New Meta Campaign',
      budget: 5000,
      currency: 'USD',
      channel: 'META',
      externalCampaignId: 'meta-ext-2',
    };

    const result = await createCampaign(input);

    expect(result.id).toBe('cmp-2');
    expect(result.status).toBe('DRAFT');
  });

  it('should update campaign and parse response', async () => {
    const mockUpdated = {
      id: 'cmp-1',
      brandId: 'b-1',
      adAccountId: 'a-1',
      name: 'Updated Name',
      budget: 8000,
      currency: 'USD',
      channel: 'META',
      externalCampaignId: 'ext-1',
      status: 'ACTIVE',
      createdAt: '2026-08-21T00:00:00Z',
      updatedAt: '2026-08-21T00:00:00Z',
    };

    (fetch as unknown as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      ok: true,
      json: async () => mockUpdated,
    });

    const result = await updateCampaign('cmp-1', { name: 'Updated Name', status: 'ACTIVE' });

    expect(result.name).toBe('Updated Name');
    expect(result.status).toBe('ACTIVE');
  });

  it('should delete campaign successfully', async () => {
    (fetch as unknown as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      ok: true,
    });

    await expect(deleteCampaign('cmp-1')).resolves.not.toThrow();
  });
});

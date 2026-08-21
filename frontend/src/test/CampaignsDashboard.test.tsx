import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import App from '../App';

global.fetch = vi.fn();

describe('CampaignsDashboard Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders dashboard title and loads campaigns table', async () => {
    const mockResponse = {
      content: [
        {
          id: 'cmp-100',
          brandId: 'brand-uuid-1',
          adAccountId: 'adacc-uuid-1',
          name: 'Q1 Retargeting Campaign',
          budget: 5000,
          currency: 'USD',
          channel: 'META',
          externalCampaignId: 'meta_ext_100',
          status: 'ACTIVE',
          createdAt: '2026-08-21T00:00:00Z',
          updatedAt: '2026-08-21T00:00:00Z',
        },
      ],
    };

    (fetch as unknown as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      ok: true,
      json: async () => mockResponse,
    });

    render(<App />);

    expect(screen.getByText('Campaigns Dashboard')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('Q1 Retargeting Campaign')).toBeInTheDocument();
      expect(screen.getAllByText('META').length).toBeGreaterThan(0);
      expect(screen.getByText('ACTIVE')).toBeInTheDocument();
    });
  });

  it('opens create modal when clicking "+ New Campaign"', async () => {
    (fetch as unknown as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      ok: true,
      json: async () => ({ content: [] }),
    });

    render(<App />);

    await waitFor(() => {
      expect(screen.getByText('New Campaign')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('New Campaign'));

    expect(screen.getByRole('heading', { name: 'Create Campaign' })).toBeInTheDocument();
  });
});

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { CampaignCsvImportModal } from '../components/CampaignCsvImportModal';
import * as api from '../api/campaigns';

vi.mock('../api/campaigns', () => ({
  importCampaignsCsv: vi.fn(),
}));

describe('CampaignCsvImportModal Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders modal title and file dropzone when open', () => {
    render(
      <CampaignCsvImportModal
        isOpen={true}
        onClose={vi.fn()}
        onSuccess={vi.fn()}
      />
    );

    expect(screen.getByText('Batch Campaign CSV Import')).toBeInTheDocument();
    expect(screen.getByText('Template')).toBeInTheDocument();
    expect(screen.getByText('Upload & Process')).toBeInTheDocument();
  });

  it('handles CSV upload and displays summary and error report', async () => {
    const mockSummary = {
      total: 5,
      created: 3,
      failed: 2,
      errors: [
        { rowNumber: 2, message: 'Brand not found with ID: bad-id' },
        { rowNumber: 5, message: 'external_campaign_id is required' },
      ],
    };

    (api.importCampaignsCsv as unknown as ReturnType<typeof vi.fn>).mockResolvedValueOnce(mockSummary);

    const onSuccess = vi.fn();
    const onClose = vi.fn();

    render(
      <CampaignCsvImportModal
        isOpen={true}
        onClose={onClose}
        onSuccess={onSuccess}
      />
    );

    const file = new File(['name,brand_id\nTest,b1'], 'import.csv', { type: 'text/csv' });
    const fileInput = screen.getByLabelText('Browse File');

    fireEvent.change(fileInput, { target: { files: [file] } });

    fireEvent.click(screen.getByText('Upload & Process'));

    await waitFor(() => {
      expect(screen.getByText('Total Processed')).toBeInTheDocument();
      expect(screen.getByText('Successfully Created')).toBeInTheDocument();
      expect(screen.getByText('Row Validation Errors (2)')).toBeInTheDocument();
      expect(screen.getByText('Brand not found with ID: bad-id')).toBeInTheDocument();
    });

    expect(onSuccess).toHaveBeenCalled();
  });
});

import React, { useState, useEffect } from 'react';
import { Campaign, CampaignStatus, CreateCampaignInput, UpdateCampaignInput } from '@/types/campaign';
import { X } from 'lucide-react';

interface CampaignModalProps {
  isOpen: boolean;
  campaign?: Campaign | null; // If present, edit mode; otherwise create mode
  onClose: () => void;
  onSubmitCreate: (data: CreateCampaignInput) => Promise<void>;
  onSubmitUpdate: (id: string, data: UpdateCampaignInput) => Promise<void>;
}

export const CampaignModal: React.FC<CampaignModalProps> = ({
  isOpen,
  campaign,
  onClose,
  onSubmitCreate,
  onSubmitUpdate,
}) => {
  const isEdit = Boolean(campaign);

  const [name, setName] = useState('');
  const [brandId, setBrandId] = useState('');
  const [adAccountId, setAdAccountId] = useState('');
  const [channel, setChannel] = useState('META');
  const [budget, setBudget] = useState('');
  const [currency, setCurrency] = useState('USD');
  const [externalCampaignId, setExternalCampaignId] = useState('');
  const [status, setStatus] = useState<CampaignStatus>('DRAFT');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (campaign) {
      setName(campaign.name);
      setBrandId(campaign.brandId);
      setAdAccountId(campaign.adAccountId);
      setChannel(campaign.channel);
      setBudget(campaign.budget !== null && campaign.budget !== undefined ? campaign.budget.toString() : '');
      setCurrency(campaign.currency);
      setExternalCampaignId(campaign.externalCampaignId);
      setStatus(campaign.status);
    } else {
      setName('');
      setBrandId('');
      setAdAccountId('');
      setChannel('META');
      setBudget('');
      setCurrency('USD');
      setExternalCampaignId('');
      setStatus('DRAFT');
    }
    setError(null);
  }, [campaign, isOpen]);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSubmitting(true);

    try {
      if (isEdit && campaign) {
        const updatePayload: UpdateCampaignInput = {
          name,
          budget: budget ? parseFloat(budget) : undefined,
          currency,
          status,
        };
        await onSubmitUpdate(campaign.id, updatePayload);
      } else {
        const createPayload: CreateCampaignInput = {
          name,
          brandId,
          adAccountId,
          channel,
          budget: budget ? parseFloat(budget) : undefined,
          currency,
          externalCampaignId,
        };
        await onSubmitCreate(createPayload);
      }
      onClose();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'An unexpected error occurred');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="modal-backdrop">
      <div className="modal">
        <div className="modal-header">
          <h2 className="modal-title">{isEdit ? 'Edit Campaign' : 'Create Campaign'}</h2>
          <button className="btn btn-secondary" onClick={onClose} style={{ padding: '0.25rem 0.5rem' }}>
            <X size={18} />
          </button>
        </div>

        {error && (
          <div style={{ color: 'var(--danger)', fontSize: '0.875rem', marginBottom: '1rem' }}>
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Name</label>
            <input
              type="text"
              className="input"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. Q1 Retargeting"
            />
          </div>

          {!isEdit && (
            <>
              <div className="form-group">
                <label>Brand ID (UUID)</label>
                <input
                  type="text"
                  className="input"
                  required
                  value={brandId}
                  onChange={(e) => setBrandId(e.target.value)}
                  placeholder="e.g. 123e4567-e89b-12d3-a456-426614174000"
                />
              </div>

              <div className="form-group">
                <label>Ad Account ID (UUID)</label>
                <input
                  type="text"
                  className="input"
                  required
                  value={adAccountId}
                  onChange={(e) => setAdAccountId(e.target.value)}
                  placeholder="e.g. 123e4567-e89b-12d3-a456-426614174001"
                />
              </div>
            </>
          )}

          <div className="form-group">
            <label>Channel</label>

            {isEdit ? (
              <input type="text" className="input" disabled value={channel} />
            ) : (
              <select className="select" value={channel} onChange={(e) => setChannel(e.target.value)}>
                <option value="META">META</option>
                <option value="GOOGLE">GOOGLE</option>
                <option value="TIKTOK">TIKTOK</option>
                <option value="LINKEDIN">LINKEDIN</option>
              </select>
            )}
          </div>

          {!isEdit && (
            <div className="form-group">
              <label>External Campaign ID</label>
              <input
                type="text"
                className="input"
                required
                value={externalCampaignId}
                onChange={(e) => setExternalCampaignId(e.target.value)}
                placeholder="e.g. meta_12345"
              />
            </div>
          )}

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
            <div className="form-group">
              <label>Budget</label>
              <input
                type="number"
                step="0.01"
                className="input"
                value={budget}
                onChange={(e) => setBudget(e.target.value)}
                placeholder="5000.00"
              />
            </div>

            <div className="form-group">
              <label>Currency</label>
              <input
                type="text"
                className="input"
                value={currency}
                onChange={(e) => setCurrency(e.target.value)}
              />
            </div>
          </div>

          {isEdit && (
            <div className="form-group">
              <label>Status</label>
              <select
                className="select"
                value={status}
                onChange={(e) => setStatus(e.target.value as CampaignStatus)}
              >
                <option value="DRAFT">DRAFT</option>
                <option value="ACTIVE">ACTIVE</option>
                <option value="PAUSED">PAUSED</option>
                <option value="COMPLETED">COMPLETED</option>
                <option value="ARCHIVED">ARCHIVED</option>
              </select>
            </div>
          )}

          <div className="form-actions">
            <button type="button" className="btn btn-secondary" onClick={onClose}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? 'Saving...' : 'Save Campaign'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

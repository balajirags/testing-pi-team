import React from 'react';
import { Campaign } from '@/types/campaign';
import { Edit2, Trash2, Plus, Filter } from 'lucide-react';

interface CampaignsTableProps {
  campaigns: Campaign[];
  isLoading: boolean;
  selectedChannel: string;
  onChannelChange: (channel: string) => void;
  onOpenCreateModal: () => void;
  onEditCampaign: (campaign: Campaign) => void;
  onDeleteCampaign: (id: string) => void;
}

export const CampaignsTable: React.FC<CampaignsTableProps> = ({
  campaigns,
  isLoading,
  selectedChannel,
  onChannelChange,
  onOpenCreateModal,
  onEditCampaign,
  onDeleteCampaign,
}) => {
  const getBadgeClass = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return 'badge badge-active';
      case 'PAUSED':
        return 'badge badge-paused';
      case 'COMPLETED':
        return 'badge badge-completed';
      case 'ARCHIVED':
        return 'badge badge-archived';
      default:
        return 'badge badge-draft';
    }
  };

  return (
    <div className="card">
      <div className="toolbar">
        <div className="filters">
          <Filter size={18} className="text-secondary" />
          <select
            className="select"
            value={selectedChannel}
            onChange={(e) => onChannelChange(e.target.value)}
          >
            <option value="">All Channels</option>
            <option value="META">META</option>
            <option value="GOOGLE">GOOGLE</option>
            <option value="TIKTOK">TIKTOK</option>
            <option value="LINKEDIN">LINKEDIN</option>
          </select>
        </div>

        <button className="btn btn-primary" onClick={onOpenCreateModal}>
          <Plus size={18} />
          <span>New Campaign</span>
        </button>
      </div>

      <div className="table-wrapper">
        <table className="table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Brand ID</th>
              <th>Ad Account ID</th>
              <th>Channel</th>
              <th>Budget</th>
              <th>Status</th>
              <th>External ID</th>
              <th style={{ textAlign: 'right' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr>
                <td colSpan={8} style={{ textAlign: 'center', padding: '2rem' }}>
                  Loading campaigns...
                </td>
              </tr>
            ) : campaigns.length === 0 ? (
              <tr>
                <td colSpan={8} style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-secondary)' }}>
                  No campaigns found.
                </td>
              </tr>
            ) : (
              campaigns.map((cmp) => (
                <tr key={cmp.id}>
                  <td style={{ fontWeight: 600 }}>{cmp.name}</td>
                  <td style={{ fontSize: '0.75rem', fontFamily: 'monospace' }}>{cmp.brandId}</td>
                  <td style={{ fontSize: '0.75rem', fontFamily: 'monospace' }}>{cmp.adAccountId}</td>
                  <td>{cmp.channel}</td>
                  <td>
                    {cmp.budget !== null && cmp.budget !== undefined
                      ? `${cmp.currency} ${cmp.budget.toLocaleString(undefined, { minimumFractionDigits: 2 })}`
                      : '-'}
                  </td>
                  <td>
                    <span className={getBadgeClass(cmp.status)}>{cmp.status}</span>
                  </td>
                  <td style={{ fontSize: '0.75rem', fontFamily: 'monospace' }}>{cmp.externalCampaignId}</td>
                  <td style={{ textAlign: 'right' }}>
                    <div style={{ display: 'inline-flex', gap: '0.5rem' }}>
                      <button
                        className="btn btn-secondary"
                        onClick={() => onEditCampaign(cmp)}
                        style={{ padding: '0.25rem 0.5rem' }}
                        title="Edit Campaign"
                      >
                        <Edit2 size={14} />
                      </button>
                      <button
                        className="btn btn-danger"
                        onClick={() => onDeleteCampaign(cmp.id)}
                        style={{ padding: '0.25rem 0.5rem' }}
                        title="Delete Campaign"
                      >
                        <Trash2 size={14} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};

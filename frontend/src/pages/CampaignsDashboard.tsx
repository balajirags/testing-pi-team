import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { fetchCampaigns, createCampaign, updateCampaign, deleteCampaign } from '@/api/campaigns';
import { Campaign, CreateCampaignInput, UpdateCampaignInput } from '@/types/campaign';
import { Header } from '@/components/Header';
import { CampaignsTable } from '@/components/CampaignsTable';
import { CampaignModal } from '@/components/CampaignModal';
import { Toast } from '@/components/Toast';

export const CampaignsDashboard: React.FC = () => {
  const queryClient = useQueryClient();
  const [selectedChannel, setSelectedChannel] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingCampaign, setEditingCampaign] = useState<Campaign | null>(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['campaigns', { channel: selectedChannel }],
    queryFn: () => fetchCampaigns({ channel: selectedChannel || undefined }),
  });

  const createMutation = useMutation({
    mutationFn: (newCampaign: CreateCampaignInput) => createCampaign(newCampaign),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['campaigns'] });
      setToastMessage('Campaign created successfully!');
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateCampaignInput }) => updateCampaign(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['campaigns'] });
      setToastMessage('Campaign updated successfully!');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteCampaign(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['campaigns'] });
      setToastMessage('Campaign archived successfully!');
    },
  });

  const handleOpenCreateModal = () => {
    setEditingCampaign(null);
    setIsModalOpen(true);
  };

  const handleEditCampaign = (campaign: Campaign) => {
    setEditingCampaign(campaign);
    setIsModalOpen(true);
  };

  const handleDeleteCampaign = async (id: string) => {
    if (window.confirm('Are you sure you want to archive this campaign?')) {
      try {
        await deleteMutation.mutateAsync(id);
      } catch (err: unknown) {
        setToastMessage(err instanceof Error ? err.message : 'Failed to delete campaign');
      }
    }
  };

  const handleCreateSubmit = async (payload: CreateCampaignInput) => {
    await createMutation.mutateAsync(payload);
  };

  const handleUpdateSubmit = async (id: string, payload: UpdateCampaignInput) => {
    await updateMutation.mutateAsync({ id, data: payload });
  };

  return (
    <div>
      <Header />
      <main className="container">
        <div style={{ marginBottom: '1.5rem' }}>
          <h1 style={{ fontSize: '1.5rem', fontWeight: 700 }}>Campaigns Dashboard</h1>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
            Manage and monitor multi-channel marketing campaigns.
          </p>
        </div>

        {isError && (
          <div
            className="card"
            style={{
              borderColor: 'var(--danger)',
              backgroundColor: '#fef2f2',
              marginBottom: '1.5rem',
              color: 'var(--danger)',
            }}
          >
            Failed to load campaigns: {error instanceof Error ? error.message : 'Unknown error'}
          </div>
        )}

        <CampaignsTable
          campaigns={data?.content ?? []}
          isLoading={isLoading}
          selectedChannel={selectedChannel}
          onChannelChange={setSelectedChannel}
          onOpenCreateModal={handleOpenCreateModal}
          onEditCampaign={handleEditCampaign}
          onDeleteCampaign={handleDeleteCampaign}
        />

        <CampaignModal
          isOpen={isModalOpen}
          campaign={editingCampaign}
          onClose={() => setIsModalOpen(false)}
          onSubmitCreate={handleCreateSubmit}
          onSubmitUpdate={handleUpdateSubmit}
        />

        <Toast message={toastMessage} onClose={() => setToastMessage(null)} />
      </main>
    </div>
  );
};

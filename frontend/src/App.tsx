import React from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { CampaignsDashboard } from '@/pages/CampaignsDashboard';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

export const App: React.FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <CampaignsDashboard />
    </QueryClientProvider>
  );
};

export default App;

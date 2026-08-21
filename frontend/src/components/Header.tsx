import React from 'react';
import { Layers } from 'lucide-react';

export const Header: React.FC = () => {
  return (
    <header className="header">
      <div className="brand">
        <Layers size={24} />
        <span>Campaigns Manager</span>
      </div>
    </header>
  );
};

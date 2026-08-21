import React, { useState } from 'react';
import { CsvImportSummary } from '@/types/campaign';
import { importCampaignsCsv } from '@/api/campaigns';
import { X, Upload, Download, FileText, CheckCircle2, AlertTriangle, Loader2 } from 'lucide-react';

interface CampaignCsvImportModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export const CampaignCsvImportModal: React.FC<CampaignCsvImportModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
}) => {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [summary, setSummary] = useState<CsvImportSummary | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      setSelectedFile(e.target.files[0]);
      setErrorMsg(null);
      setSummary(null);
    }
  };

  const handleDownloadTemplate = () => {
    const header = 'name,brand_id,ad_account_id,budget,currency,channel,external_campaign_id\n';
    const sampleRow = 'Sample Q1 Campaign,123e4567-e89b-12d3-a456-426614174000,123e4567-e89b-12d3-a456-426614174001,5000.00,USD,META,meta_sample_101\n';
    const blob = new Blob([header + sampleRow], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'campaign_import_template.csv';
    a.click();
    URL.revokeObjectURL(url);
  };

  const handleDownloadErrorLog = () => {
    if (!summary || summary.errors.length === 0) return;
    const content = summary.errors
      .map((e) => `Row ${e.rowNumber}: ${e.message}`)
      .join('\n');
    const blob = new Blob([content], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'csv_import_errors.txt';
    a.click();
    URL.revokeObjectURL(url);
  };

  const handleUpload = async () => {
    if (!selectedFile) {
      setErrorMsg('Please select a CSV file to upload.');
      return;
    }

    setIsUploading(true);
    setErrorMsg(null);
    setSummary(null);

    try {
      const result = await importCampaignsCsv(selectedFile);
      setSummary(result);
      if (result.created > 0) {
        onSuccess();
      }
    } catch (err: unknown) {
      setErrorMsg(err instanceof Error ? err.message : 'CSV import failed.');
    } finally {
      setIsUploading(false);
    }
  };

  const handleClose = () => {
    setSelectedFile(null);
    setSummary(null);
    setErrorMsg(null);
    onClose();
  };

  return (
    <div className="modal-backdrop">
      <div className="modal" style={{ maxWidth: '600px' }}>
        <div className="modal-header">
          <h2 className="modal-title">Batch Campaign CSV Import</h2>
          <button className="btn btn-secondary" onClick={handleClose} style={{ padding: '0.25rem 0.5rem' }}>
            <X size={18} />
          </button>
        </div>

        {!summary ? (
          <div>
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                marginBottom: '1rem',
              }}
            >
              <span style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
                Upload a `.csv` file containing campaign records.
              </span>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={handleDownloadTemplate}
                style={{ fontSize: '0.75rem', padding: '0.375rem 0.75rem' }}
              >
                <Download size={14} />
                <span>Template</span>
              </button>
            </div>

            <div
              style={{
                border: '2px dashed var(--border-color)',
                borderRadius: '0.5rem',
                padding: '2rem 1rem',
                textAlign: 'center',
                backgroundColor: '#f8fafc',
                marginBottom: '1.5rem',
              }}
            >
              <FileText size={36} style={{ color: 'var(--primary)', marginBottom: '0.5rem' }} />
              <p style={{ fontSize: '0.875rem', fontWeight: 500, marginBottom: '0.5rem' }}>
                {selectedFile ? selectedFile.name : 'Select or drag a CSV file here'}
              </p>
              <input
                type="file"
                accept=".csv"
                id="csv-file-input"
                onChange={handleFileChange}
                style={{ display: 'none' }}
              />
              <label htmlFor="csv-file-input" className="btn btn-secondary" style={{ cursor: 'pointer' }}>
                <Upload size={16} />
                <span>Browse File</span>
              </label>
            </div>

            {errorMsg && (
              <div
                style={{
                  color: 'var(--danger)',
                  fontSize: '0.875rem',
                  marginBottom: '1rem',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.5rem',
                }}
              >
                <AlertTriangle size={16} />
                <span>{errorMsg}</span>
              </div>
            )}

            <div className="form-actions">
              <button className="btn btn-secondary" onClick={handleClose}>
                Cancel
              </button>
              <button
                className="btn btn-primary"
                onClick={handleUpload}
                disabled={!selectedFile || isUploading}
              >
                {isUploading ? (
                  <>
                    <Loader2 size={16} className="spin" />
                    <span>Processing...</span>
                  </>
                ) : (
                  <>
                    <Upload size={16} />
                    <span>Upload & Process</span>
                  </>
                )}
              </button>
            </div>
          </div>
        ) : (
          <div>
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(3, 1fr)',
                gap: '1rem',
                marginBottom: '1.5rem',
                textAlign: 'center',
              }}
            >
              <div style={{ background: '#f1f5f9', padding: '1rem', borderRadius: '0.375rem' }}>
                <div style={{ fontSize: '1.25rem', fontWeight: 700 }}>{summary.total}</div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>Total Processed</div>
              </div>
              <div style={{ background: '#dcfce7', color: '#166534', padding: '1rem', borderRadius: '0.375rem' }}>
                <div style={{ fontSize: '1.25rem', fontWeight: 700 }}>{summary.created}</div>
                <div style={{ fontSize: '0.75rem' }}>Successfully Created</div>
              </div>
              <div style={{ background: '#fee2e2', color: '#991b1b', padding: '1rem', borderRadius: '0.375rem' }}>
                <div style={{ fontSize: '1.25rem', fontWeight: 700 }}>{summary.failed}</div>
                <div style={{ fontSize: '0.75rem' }}>Failed</div>
              </div>
            </div>

            {summary.errors.length > 0 && (
              <div>
                <div
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    marginBottom: '0.5rem',
                  }}
                >
                  <h3 style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--danger)' }}>
                    Row Validation Errors ({summary.errors.length})
                  </h3>
                  <button
                    className="btn btn-secondary"
                    onClick={handleDownloadErrorLog}
                    style={{ fontSize: '0.75rem', padding: '0.25rem 0.5rem' }}
                  >
                    <Download size={12} />
                    <span>Download Log</span>
                  </button>
                </div>

                <div style={{ maxHeight: '200px', overflowY: 'auto', border: '1px solid var(--border-color)', borderRadius: '0.375rem' }}>
                  <table className="table" style={{ fontSize: '0.75rem' }}>
                    <thead>
                      <tr>
                        <th style={{ width: '80px' }}>Row</th>
                        <th>Error Detail</th>
                      </tr>
                    </thead>
                    <tbody>
                      {summary.errors.map((err, idx) => (
                        <tr key={idx}>
                          <td style={{ fontWeight: 600 }}>Row {err.rowNumber}</td>
                          <td style={{ color: 'var(--danger)' }}>{err.message}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            {summary.failed === 0 && (
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.5rem',
                  color: 'var(--success)',
                  margin: '1rem 0',
                }}
              >
                <CheckCircle2 size={18} />
                <span style={{ fontSize: '0.875rem', fontWeight: 500 }}>All records imported successfully!</span>
              </div>
            )}

            <div className="form-actions">
              <button className="btn btn-primary" onClick={handleClose}>
                Done
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

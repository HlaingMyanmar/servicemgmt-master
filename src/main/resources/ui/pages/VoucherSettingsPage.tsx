import React, { useCallback, useEffect, useState } from 'react';
import Swal from 'sweetalert2';
import {
  DocumentType,
  VoucherSettingDto,
  voucherSettingService,
} from '../services/voucherSettingService';

// ── Constants ─────────────────────────────────────────────────────────────────

const FONT_FAMILY_OPTIONS = [
  { label: 'Pyidaungsu',     value: 'Pyidaungsu' },
  { label: '— Default —',   value: '' },
  { label: 'Segoe UI',      value: 'Segoe UI' },
  { label: 'Arial',         value: 'Arial' },
  { label: 'Tahoma',        value: 'Tahoma' },
  { label: 'Verdana',       value: 'Verdana' },
  { label: 'Calibri',       value: 'Calibri' },
  { label: 'Georgia',       value: 'Georgia' },
  { label: 'Times New Roman', value: 'Times New Roman' },
  { label: 'Courier New',   value: 'Courier New' },
];

const TABS: { type: DocumentType; label: string }[] = [
  { type: 'SALE',         label: 'Sales Invoice' },
  { type: 'SERVICE_JOB',  label: 'Service Voucher' },
  { type: 'SERVICE_DONE', label: 'Service Done' },
  { type: 'BOOKING',      label: 'Booking Receipt' },
  { type: 'PURCHASE',     label: 'Purchase Order' },
];

const PAPER_SIZES = ['A4', 'A5', 'POS_80MM', 'POS_58MM'];

// ── Small helper components ───────────────────────────────────────────────────

const Field: React.FC<{
  label: string;
  value: string;
  onChange: (v: string) => void;
  placeholder?: string;
  textarea?: boolean;
  rows?: number;
}> = ({ label, value, onChange, placeholder, textarea, rows = 3 }) => (
  <div>
    <label className="block text-xs font-medium text-slate-600 mb-1">{label}</label>
    {textarea ? (
      <textarea
        value={value}
        onChange={e => onChange(e.target.value)}
        placeholder={placeholder}
        rows={rows}
        className="w-full border rounded-lg px-3 py-1.5 text-sm focus:ring-2 focus:ring-indigo-500 resize-none"
      />
    ) : (
      <input
        value={value}
        onChange={e => onChange(e.target.value)}
        placeholder={placeholder}
        className="w-full border rounded-lg px-3 py-1.5 text-sm focus:ring-2 focus:ring-indigo-500"
      />
    )}
  </div>
);

const NumField: React.FC<{
  label: string;
  value: number | null;
  onChange: (v: number | null) => void;
  hint?: string;
}> = ({ label, value, onChange, hint }) => (
  <div>
    <label className="block text-xs font-medium text-slate-600 mb-1">{label}</label>
    <input
      type="number"
      value={value ?? ''}
      onChange={e => onChange(e.target.value === '' ? null : Number(e.target.value))}
      className="w-full border rounded-lg px-3 py-1.5 text-sm focus:ring-2 focus:ring-indigo-500"
    />
    {hint && <p className="text-[10px] text-slate-400 mt-0.5">{hint}</p>}
  </div>
);

const Toggle: React.FC<{
  label: string;
  checked: boolean;
  onChange: (v: boolean) => void;
}> = ({ label, checked, onChange }) => (
  <label className="flex items-center gap-3 cursor-pointer select-none">
    <div
      onClick={() => onChange(!checked)}
      className={`relative w-10 h-5 rounded-full transition-colors ${checked ? 'bg-indigo-600' : 'bg-slate-300'}`}
    >
      <span
        className={`absolute top-0.5 left-0.5 w-4 h-4 bg-white rounded-full shadow transition-transform ${checked ? 'translate-x-5' : ''}`}
      />
    </div>
    <span className="text-sm text-slate-700">{label}</span>
  </label>
);

// ── Section wrapper ───────────────────────────────────────────────────────────

const Section: React.FC<{ title: string; children: React.ReactNode }> = ({ title, children }) => (
  <div className="border rounded-xl p-4 bg-white">
    <h3 className="text-sm font-semibold text-slate-700 mb-3 pb-2 border-b">{title}</h3>
    <div className="space-y-3">{children}</div>
  </div>
);

const FontRow: React.FC<{
  label: string;
  family: string | null;
  sizePx: number | null;
  onFamily: (v: string | null) => void;
  onSize: (v: number | null) => void;
}> = ({ label, family, sizePx, onFamily, onSize }) => (
  <div className="flex items-center gap-3">
    <span className="text-xs text-slate-600 w-28 shrink-0">{label}</span>
    <select
      value={family ?? ''}
      onChange={e => onFamily(e.target.value === '' ? null : e.target.value)}
      className="flex-1 border rounded-lg px-2 py-1 text-sm focus:ring-2 focus:ring-indigo-500"
    >
      {FONT_FAMILY_OPTIONS.map(f => (
        <option key={f.value} value={f.value}>{f.label}</option>
      ))}
    </select>
    <input
      type="number"
      value={sizePx ?? ''}
      onChange={e => onSize(e.target.value === '' ? null : Number(e.target.value))}
      placeholder="px"
      className="w-16 border rounded-lg px-2 py-1 text-sm focus:ring-2 focus:ring-indigo-500"
    />
  </div>
);

// ── Main page ─────────────────────────────────────────────────────────────────

const VoucherSettingsPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<DocumentType>('SALE');
  const [settings, setSettings] = useState<Record<DocumentType, VoucherSettingDto | null>>({
    SALE: null, SERVICE_JOB: null, SERVICE_DONE: null, BOOKING: null, PURCHASE: null,
  });
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const all = await voucherSettingService.getAll();
      const map: Record<DocumentType, VoucherSettingDto | null> = {
        SALE: null, SERVICE_JOB: null, SERVICE_DONE: null, BOOKING: null, PURCHASE: null,
      };
      all.forEach(s => { map[s.documentType as DocumentType] = s; });
      setSettings(map);
    } catch {
      Swal.fire('Error', 'Failed to load voucher settings', 'error');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const current = settings[activeTab];

  const update = (patch: Partial<VoucherSettingDto>) => {
    if (!current) return;
    setSettings(prev => ({
      ...prev,
      [activeTab]: { ...prev[activeTab]!, ...patch },
    }));
  };

  const handleSave = async () => {
    if (!current) return;
    setSaving(true);
    try {
      const saved = await voucherSettingService.save(activeTab, current);
      setSettings(prev => ({ ...prev, [activeTab]: saved }));
      Swal.fire({ icon: 'success', title: 'Saved', timer: 1200, showConfirmButton: false });
    } catch {
      Swal.fire('Error', 'Failed to save settings', 'error');
    } finally {
      setSaving(false);
    }
  };

  const handleReset = async () => {
    const { isConfirmed } = await Swal.fire({
      title: 'Reset to defaults?',
      text: 'This will restore all settings for this document type to system defaults.',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Reset',
      confirmButtonColor: '#dc2626',
    });
    if (!isConfirmed) return;
    setSaving(true);
    try {
      const fresh = await voucherSettingService.reset(activeTab);
      setSettings(prev => ({ ...prev, [activeTab]: fresh }));
      Swal.fire({ icon: 'success', title: 'Reset', timer: 1200, showConfirmButton: false });
    } catch {
      Swal.fire('Error', 'Failed to reset settings', 'error');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="p-4 w-full max-w-none space-y-4 text-left">
      {/* Header */}
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-bold text-slate-800">Voucher Print Settings</h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Configure paper size, layout, and content for each document type.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button
            onClick={handleReset}
            disabled={saving || loading || !current}
            className="px-3 py-1.5 text-sm border border-red-300 text-red-600 rounded-lg hover:bg-red-50 disabled:opacity-40"
          >
            Reset to Defaults
          </button>
          <button
            onClick={handleSave}
            disabled={saving || loading || !current}
            className="px-4 py-1.5 text-sm bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-40"
          >
            {saving ? 'Saving…' : 'Save'}
          </button>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 border-b">
        {TABS.map(t => (
          <button
            key={t.type}
            onClick={() => setActiveTab(t.type)}
            className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
              activeTab === t.type
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-slate-500 hover:text-slate-700'
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {loading && (
        <div className="text-center py-12 text-slate-400 text-sm">Loading…</div>
      )}

      {!loading && current && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {/* Paper & Title */}
          <Section title="Paper & Title">
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">Paper Size</label>
              <select
                value={current.paperSize}
                onChange={e => update({ paperSize: e.target.value })}
                className="w-full border rounded-lg px-3 py-1.5 text-sm focus:ring-2 focus:ring-indigo-500"
              >
                {PAPER_SIZES.map(p => (
                  <option key={p} value={p}>{p}</option>
                ))}
              </select>
            </div>
            <Field
              label="Voucher Title"
              value={current.voucherTitle}
              onChange={v => update({ voucherTitle: v })}
              placeholder="e.g. SALES INVOICE"
            />
          </Section>

          {/* Display Toggles */}
          <Section title="Display Options">
            <Toggle label="Show Logo"           checked={current.showLogo}           onChange={v => update({ showLogo: v })} />
            <Toggle label="Show QR Code"        checked={current.showQrCode}        onChange={v => update({ showQrCode: v })} />
            <Toggle label="Show Signatures"     checked={current.showSignatures}    onChange={v => update({ showSignatures: v })} />
            <Toggle label="Show Payment History" checked={current.showPaymentHistory} onChange={v => update({ showPaymentHistory: v })} />
            <Toggle label="Show Serial Numbers" checked={current.showSerial}        onChange={v => update({ showSerial: v })} />
          </Section>

          {/* Signatures */}
          <Section title="Signature Labels">
            <Field label="Signature 1 Label" value={current.sign1Label} onChange={v => update({ sign1Label: v })} placeholder="Prepared By" />
            <Field label="Signature 2 Label" value={current.sign2Label} onChange={v => update({ sign2Label: v })} placeholder="Received By" />
          </Section>

          {/* Content */}
          <Section title="Content">
            <Field label="Footer Note"     value={current.footerNote}     onChange={v => update({ footerNote: v })}     textarea rows={2} placeholder="Thank you for your business" />
            <Field label="Customer Notice" value={current.customerNotice} onChange={v => update({ customerNotice: v })} textarea rows={2} placeholder="Warranty / return policy notice…" />
          </Section>

          {/* Typography */}
          <div className="md:col-span-2">
            <Section title="Typography (Font Family · Size px — leave blank to use defaults)">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-2">
                <FontRow
                  label="Header / Company"
                  family={current.headerFontFamily ?? null}
                  sizePx={current.headerFontSizePx ?? null}
                  onFamily={v => update({ headerFontFamily: v })}
                  onSize={v => update({ headerFontSizePx: v })}
                />
                <FontRow
                  label="Bill To / Info"
                  family={current.infoFontFamily ?? null}
                  sizePx={current.infoFontSizePx ?? null}
                  onFamily={v => update({ infoFontFamily: v })}
                  onSize={v => update({ infoFontSizePx: v })}
                />
                <FontRow
                  label="Table Header"
                  family={current.tableHeaderFontFamily ?? null}
                  sizePx={current.tableHeaderFontSizePx ?? null}
                  onFamily={v => update({ tableHeaderFontFamily: v })}
                  onSize={v => update({ tableHeaderFontSizePx: v })}
                />
                <FontRow
                  label="Table Data"
                  family={current.tableDataFontFamily ?? null}
                  sizePx={current.tableDataFontSizePx ?? null}
                  onFamily={v => update({ tableDataFontFamily: v })}
                  onSize={v => update({ tableDataFontSizePx: v })}
                />
                <FontRow
                  label="Footer"
                  family={current.footerFontFamily ?? null}
                  sizePx={current.footerFontSizePx ?? null}
                  onFamily={v => update({ footerFontFamily: v })}
                  onSize={v => update({ footerFontSizePx: v })}
                />
                <FontRow
                  label="Notice / Remark"
                  family={current.noticeFontFamily ?? null}
                  sizePx={current.noticeFontSizePx ?? null}
                  onFamily={v => update({ noticeFontFamily: v })}
                  onSize={v => update({ noticeFontSizePx: v })}
                />
              </div>
            </Section>
          </div>

          {/* Margins */}
          <Section title="Margins (mm)">
            <div className="grid grid-cols-2 gap-3">
              <NumField label="Top"    value={current.marginTopMm}    onChange={v => update({ marginTopMm: v })}    />
              <NumField label="Bottom" value={current.marginBottomMm} onChange={v => update({ marginBottomMm: v })} />
              <NumField label="Left"   value={current.marginLeftMm}   onChange={v => update({ marginLeftMm: v })}   />
              <NumField label="Right"  value={current.marginRightMm}  onChange={v => update({ marginRightMm: v })}  />
            </div>
          </Section>

          {/* Component Heights */}
          <Section title="Component Heights (px)">
            <div className="grid grid-cols-2 gap-3">
              <NumField label="Header"            value={current.headerHeightPx}       onChange={v => update({ headerHeightPx: v })}       />
              <NumField label="Cont. Header"      value={current.contHeaderHeightPx}   onChange={v => update({ contHeaderHeightPx: v })}   />
              <NumField label="Info Blocks"       value={current.infoBlocksHeightPx}   onChange={v => update({ infoBlocksHeightPx: v })}   />
              <NumField label="Table Header"      value={current.tableHeaderHeightPx}  onChange={v => update({ tableHeaderHeightPx: v })}  />
              <NumField label="Row Height"        value={current.rowHeightPx}          onChange={v => update({ rowHeightPx: v })}          hint="Affects rows per page" />
              <NumField label="Totals Area"       value={current.totalsAreaHeightPx}   onChange={v => update({ totalsAreaHeightPx: v })}   />
              <NumField label="Footer"            value={current.footerHeightPx}       onChange={v => update({ footerHeightPx: v })}       />
              <NumField label="Safety Margin"     value={current.safetyMarginPx}       onChange={v => update({ safetyMarginPx: v })}       />
            </div>
          </Section>

          {/* Derived rows per page */}
          {(current.rowsOnFirstPage != null || current.rowsOnContinuationPage != null) && (
            <div className="md:col-span-2 rounded-xl border border-indigo-100 bg-indigo-50 px-4 py-3 flex gap-8 text-sm">
              <span className="text-slate-600">
                Rows on first page: <strong className="text-indigo-700">{current.rowsOnFirstPage ?? '—'}</strong>
              </span>
              <span className="text-slate-600">
                Rows on continuation pages: <strong className="text-indigo-700">{current.rowsOnContinuationPage ?? '—'}</strong>
              </span>
              {current.updatedBy && (
                <span className="ml-auto text-slate-400 text-xs self-center">
                  Last saved by {current.updatedBy}
                  {current.updatedAt ? ` · ${new Date(current.updatedAt).toLocaleDateString()}` : ''}
                </span>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default VoucherSettingsPage;

import React, { useEffect, useState } from 'react';
import { backupService } from '../services/api';
import Swal from 'sweetalert2';

const DAYS_OF_WEEK = ['Monday','Tuesday','Wednesday','Thursday','Friday','Saturday','Sunday'];
const MONTHS = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];

const defaultSettings = {
  frequency: 'DAILY', dayValue: 1, monthValue: 1,
  backupTime: '02:00', backupDir: './backups', enabled: true, keepDays: 30,
  mysqldumpPath: '',
};

const BackupSettings: React.FC = () => {
  const [settings, setSettings] = useState<any>(defaultSettings);
  const [backups, setBackups]   = useState<string[]>([]);
  const [loading, setLoading]   = useState(false);
  const [running, setRunning]   = useState(false);
  const [importing, setImporting] = useState(false);
  const [importFile, setImportFile] = useState<File | null>(null);

  useEffect(() => {
    loadSettings();
    loadBackups();
  }, []);

  const loadSettings = async () => {
    try {
      const res = await backupService.getSettings();
      if (res.success) setSettings(res.data);
    } catch {}
  };

  const loadBackups = async () => {
    try {
      const res = await backupService.listBackups();
      if (res.success) setBackups(res.data ?? []);
    } catch {}
  };

  const handleSave = async () => {
    setLoading(true);
    try {
      const res = await backupService.saveSettings(settings);
      if (res.success) Swal.fire('Saved', 'Backup schedule saved', 'success');
      else Swal.fire('Error', res.message, 'error');
    } catch { Swal.fire('Error', 'Failed to save', 'error'); }
    finally { setLoading(false); }
  };

  const handleRunNow = async () => {
    setRunning(true);
    try {
      const res = await backupService.runNow();
      Swal.fire(res.success ? 'Success' : 'Failed', res.message, res.success ? 'success' : 'error');
      if (res.success) loadBackups();
    } catch { Swal.fire('Error', 'Backup failed', 'error'); }
    finally { setRunning(false); }
  };

  const handleImport = async () => {
    if (!importFile) {
      Swal.fire('Error', 'Please select a .sql file', 'error');
      return;
    }
    const confirmed = await Swal.fire({
      title: 'Import Backup?',
      text: 'This will restore database from the selected SQL file.',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Import',
    });
    if (!confirmed.isConfirmed) return;

    setImporting(true);
    try {
      const res = await backupService.importBackup(importFile);
      Swal.fire(res.success ? 'Success' : 'Failed', res.message, res.success ? 'success' : 'error');
      if (res.success) {
        setImportFile(null);
      }
    } catch {
      Swal.fire('Error', 'Import failed', 'error');
    } finally {
      setImporting(false);
    }
  };

  const set = (key: string, val: any) => setSettings((p: any) => ({ ...p, [key]: val }));

  return (
    <div className="p-6 space-y-6">
      <h1 className="text-2xl font-bold text-slate-800">Auto Backup Settings</h1>

      <div className="bg-white rounded-xl shadow p-6 space-y-5">
        {/* Enable toggle */}
        <div className="flex items-center gap-3">
          <input type="checkbox" id="enabled" checked={settings.enabled}
            onChange={e => set('enabled', e.target.checked)}
            className="w-4 h-4 accent-indigo-600" />
          <label htmlFor="enabled" className="font-medium text-slate-700">Enable Auto Backup</label>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {/* Frequency */}
          <div>
            <label className="block text-sm font-medium text-slate-600 mb-1">Frequency</label>
            <select value={settings.frequency} onChange={e => set('frequency', e.target.value)}
              className="w-full border rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-indigo-500">
              <option value="DAILY">Daily</option>
              <option value="WEEKLY">Weekly</option>
              <option value="MONTHLY">Monthly</option>
              <option value="YEARLY">Yearly</option>
            </select>
          </div>

          {/* Time */}
          <div>
            <label className="block text-sm font-medium text-slate-600 mb-1">Backup Time</label>
            <input type="time" value={settings.backupTime}
              onChange={e => set('backupTime', e.target.value)}
              className="w-full border rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-indigo-500" />
          </div>

          {/* Day of week (WEEKLY) */}
          {settings.frequency === 'WEEKLY' && (
            <div>
              <label className="block text-sm font-medium text-slate-600 mb-1">Day of Week</label>
              <select value={settings.dayValue} onChange={e => set('dayValue', Number(e.target.value))}
                className="w-full border rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-indigo-500">
                {DAYS_OF_WEEK.map((d, i) => <option key={i} value={i + 1}>{d}</option>)}
              </select>
            </div>
          )}

          {/* Day of month (MONTHLY / YEARLY) */}
          {(settings.frequency === 'MONTHLY' || settings.frequency === 'YEARLY') && (
            <div>
              <label className="block text-sm font-medium text-slate-600 mb-1">Day of Month</label>
              <select value={settings.dayValue} onChange={e => set('dayValue', Number(e.target.value))}
                className="w-full border rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-indigo-500">
                {Array.from({ length: 28 }, (_, i) => i + 1).map(d =>
                  <option key={d} value={d}>{d}</option>)}
              </select>
            </div>
          )}

          {/* Month (YEARLY) */}
          {settings.frequency === 'YEARLY' && (
            <div>
              <label className="block text-sm font-medium text-slate-600 mb-1">Month</label>
              <select value={settings.monthValue} onChange={e => set('monthValue', Number(e.target.value))}
                className="w-full border rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-indigo-500">
                {MONTHS.map((m, i) => <option key={i} value={i + 1}>{m}</option>)}
              </select>
            </div>
          )}

          {/* Backup dir */}
          <div>
            <label className="block text-sm font-medium text-slate-600 mb-1">Backup Directory</label>
            <input value={settings.backupDir} onChange={e => set('backupDir', e.target.value)}
              className="w-full border rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-indigo-500"
              placeholder="./backups" />
          </div>

          {/* mysqldump path */}
          <div className="md:col-span-2">
            <label className="block text-sm font-medium text-slate-600 mb-1">
              mysqldump Path <span className="text-slate-400 font-normal">(leave blank to auto-detect)</span>
            </label>
            <input value={settings.mysqldumpPath ?? ''} onChange={e => set('mysqldumpPath', e.target.value)}
              className="w-full border rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-indigo-500"
              placeholder="e.g. C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe" />
          </div>

          {/* Keep days */}
          <div>
            <label className="block text-sm font-medium text-slate-600 mb-1">Keep Backups (days)</label>
            <input type="number" min={1} value={settings.keepDays}
              onChange={e => set('keepDays', Number(e.target.value))}
              className="w-full border rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-indigo-500" />
          </div>
        </div>

        <div className="flex gap-3 pt-2">
          <button onClick={handleSave} disabled={loading}
            className="px-5 py-2 bg-indigo-600 text-white rounded-lg text-sm font-medium hover:bg-indigo-700 disabled:opacity-50">
            {loading ? 'Saving...' : 'Save Schedule'}
          </button>
          <button onClick={handleRunNow} disabled={running}
            className="px-5 py-2 bg-emerald-600 text-white rounded-lg text-sm font-medium hover:bg-emerald-700 disabled:opacity-50">
            {running ? 'Running...' : 'Backup Now'}
          </button>
        </div>

        <div className="border-t pt-4 space-y-2">
          <p className="text-sm font-medium text-slate-700">Import Backup (.sql)</p>
          <div className="flex flex-wrap items-center gap-3">
            <input
              type="file"
              accept=".sql"
              onChange={e => setImportFile(e.target.files?.[0] ?? null)}
              className="text-sm"
            />
            <button
              onClick={handleImport}
              disabled={importing || !importFile}
              className="px-5 py-2 bg-amber-600 text-white rounded-lg text-sm font-medium hover:bg-amber-700 disabled:opacity-50"
            >
              {importing ? 'Importing...' : 'Import SQL'}
            </button>
            {importFile && <span className="text-xs text-slate-500">{importFile.name}</span>}
          </div>
        </div>
      </div>

      {/* Backup file list */}
      <div className="bg-white rounded-xl shadow p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="font-semibold text-slate-700">Backup Files</h2>
          <button onClick={loadBackups} className="text-sm text-indigo-600 hover:underline">Refresh</button>
        </div>
        {backups.length === 0
          ? <p className="text-sm text-slate-400">No backup files found.</p>
          : <ul className="divide-y text-sm text-slate-600">
              {backups.map(f => (
                <li key={f} className="py-2 flex items-center gap-2">
                  <span className="text-slate-400">📄</span> {f}
                </li>
              ))}
            </ul>}
      </div>
    </div>
  );
};

export default BackupSettings;

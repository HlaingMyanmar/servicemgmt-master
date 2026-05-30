import React, { useEffect, useMemo, useState } from 'react';
import { useDataEvents } from '../hooks/useDataEvents';
import { CalendarRange, ChevronDown, Filter, RefreshCw, Search } from 'lucide-react';
import Swal from 'sweetalert2';
import { accountingApiService } from '../services/accountingapiservice';
import { journalApiService } from '../services/journalapiservice';
import { staffService } from '../services/staffapiservice';
import { AccountBalanceDTO, JournalEntryDTO, StaffDTO } from '../types';

const money = (value: number, empty = '0 MMK') => {
  if (!value) return empty;
  return `${new Intl.NumberFormat('en-US').format(value)} MMK`;
};

const fmtDate = (value?: string) => {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
};

const fmtTime = (value?: string) => {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '-';
  return date.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' });
};

type BalanceFilter = 'ALL' | 'BALANCED' | 'UNBALANCED';

const JournalEntryManagement: React.FC = () => {
  const [entries, setEntries] = useState<JournalEntryDTO[]>([]);
  const [staffs, setStaffs] = useState<StaffDTO[]>([]);
  const [balances, setBalances] = useState<AccountBalanceDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [search, setSearch] = useState('');
  const [staffFilter, setStaffFilter] = useState('ALL');
  const [balanceFilter, setBalanceFilter] = useState<BalanceFilter>('ALL');
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');

  const loadData = async () => {
    setLoading(true);
    try {
      const [entriesRes, staffRes, balancesRes] = await Promise.all([
        journalApiService.getAll(),
        staffService.getAll(),
        accountingApiService.getAllBalances()
      ]);
      setEntries(entriesRes || []);
      setStaffs(staffRes || []);
      setBalances(balancesRes || []);
    } catch (error) {
      console.error('Error fetching journal data:', error);
      Swal.fire('Error', 'Failed to load journal report.', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadData();
  }, []);
  useDataEvents(['Sale', 'Purchase', 'Expense', 'Income', 'Journal'], loadData);

  const getStaffName = (id?: number) => staffs.find((staff) => staff.id === id)?.name || '-';

  const getTotals = (entry: JournalEntryDTO) => {
    const details = entry.details || [];
    return {
      debit: details.reduce((sum, detail) => sum + (Number(detail.debit) || 0), 0),
      credit: details.reduce((sum, detail) => sum + (Number(detail.credit) || 0), 0)
    };
  };

  const latestBalanceByAccountId = useMemo(() => {
    const map = new Map<number, AccountBalanceDTO>();
    balances.forEach((row) => {
      const current = map.get(row.accountId);
      if (!current) {
        map.set(row.accountId, row);
        return;
      }
      const currentTime = new Date(current.lastUpdated || 0).getTime();
      const nextTime = new Date(row.lastUpdated || 0).getTime();
      if (Number.isNaN(currentTime) || nextTime >= currentTime) {
        map.set(row.accountId, row);
      }
    });
    return map;
  }, [balances]);

  const getCurrentBalance = (accountId?: number) => {
    if (!accountId) return 0;
    return latestBalanceByAccountId.get(accountId)?.currentBalance || 0;
  };

  const filteredEntries = useMemo(() => {
    const keyword = search.trim().toLowerCase();
    const from = dateFrom ? new Date(dateFrom) : null;
    const to = dateTo ? new Date(dateTo) : null;
    if (to) to.setHours(23, 59, 59, 999);

    return entries.filter((entry) => {
      const staffName = entry.staffName || getStaffName(entry.staffId);
      const totals = getTotals(entry);
      const balanced = totals.debit === totals.credit;
      const text = [
        entry.referenceNo,
        entry.description,
        staffName,
        entry.id ? `#${entry.id}` : ''
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();

      if (keyword && !text.includes(keyword)) return false;
      if (staffFilter !== 'ALL' && String(entry.staffId || '') !== staffFilter) return false;
      if (balanceFilter === 'BALANCED' && !balanced) return false;
      if (balanceFilter === 'UNBALANCED' && balanced) return false;

      if (!from && !to) return true;
      if (!entry.entryDate) return false;
      const date = new Date(entry.entryDate);
      if (Number.isNaN(date.getTime())) return false;
      if (from && date < from) return false;
      if (to && date > to) return false;
      return true;
    });
  }, [balanceFilter, dateFrom, dateTo, entries, search, staffFilter, staffs]);

  const summary = useMemo(() => {
    return filteredEntries.reduce(
      (acc, entry) => {
        const totals = getTotals(entry);
        const balanced = totals.debit === totals.credit;
        acc.count += 1;
        acc.totalDebit += totals.debit;
        acc.totalCredit += totals.credit;
        if (balanced) {
          acc.balanced += 1;
        } else {
          acc.unbalanced += 1;
        }
        return acc;
      },
      { count: 0, totalDebit: 0, totalCredit: 0, balanced: 0, unbalanced: 0 }
    );
  }, [filteredEntries]);

  const reportPeriod = useMemo(() => {
    if (dateFrom || dateTo) return `${dateFrom || 'Beginning'} to ${dateTo || 'Today'}`;
    return 'All posted periods';
  }, [dateFrom, dateTo]);

  return (
    <div className="w-full max-w-none space-y-6">
      <div className="flex flex-col lg:flex-row lg:items-start lg:justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-800">Journal Entries</h2>
          <p className="text-sm text-slate-500 mt-1">General ledger register with filtered totals and expandable entry lines.</p>
        </div>
        <div className="flex flex-wrap gap-3">
          <div className="bg-white rounded-xl border border-slate-200 shadow-sm px-4 py-3 min-w-[150px]">
            <p className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider">Entries</p>
            <p className="text-2xl font-bold text-slate-800 mt-1">{summary.count}</p>
          </div>
          <div className="bg-white rounded-xl border border-emerald-100 shadow-sm px-4 py-3 min-w-[170px]">
            <p className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider">Total Debit</p>
            <p className="text-2xl font-bold text-emerald-700 mt-1">{money(summary.totalDebit)}</p>
          </div>
          <div className="bg-white rounded-xl border border-rose-100 shadow-sm px-4 py-3 min-w-[170px]">
            <p className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider">Total Credit</p>
            <p className="text-2xl font-bold text-rose-700 mt-1">{money(summary.totalCredit)}</p>
          </div>
          <div className="bg-white rounded-xl border border-indigo-100 shadow-sm px-4 py-3 min-w-[150px]">
            <p className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider">Balanced</p>
            <p className="text-2xl font-bold text-indigo-700 mt-1">{summary.balanced}</p>
          </div>
        </div>
      </div>

      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="p-4 border-b border-slate-100 bg-slate-50/50 flex flex-col xl:flex-row xl:items-center xl:justify-between gap-4">
          <div>
            <h3 className="font-bold text-slate-800 text-sm">Journal Register</h3>
            <p className="text-xs text-slate-500 mt-1">Report Period: {reportPeriod}</p>
          </div>
          <div className="flex flex-wrap gap-2">
            <button
              onClick={() => void loadData()}
              className="flex items-center gap-2 px-3 py-1.5 bg-white border border-slate-200 rounded-lg text-xs font-medium text-slate-600 hover:bg-slate-50"
            >
              <RefreshCw size={14} className={loading ? 'animate-spin' : ''} /> Refresh
            </button>
            <button
              onClick={() => {
                setSearch('');
                setStaffFilter('ALL');
                setBalanceFilter('ALL');
                setDateFrom('');
                setDateTo('');
              }}
              className="px-3 py-1.5 bg-white border border-slate-200 rounded-lg text-xs font-medium text-slate-600 hover:bg-slate-50"
            >
              Clear Filters
            </button>
          </div>
        </div>

        <div className="p-4 border-b border-slate-100">
          <div className="grid grid-cols-1 xl:grid-cols-[minmax(0,2fr)_220px_180px_auto] gap-3">
            <div className="relative">
              <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
              <input
                type="text"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Search reference no, description, staff..."
                className="w-full pl-9 pr-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
              />
            </div>

            <div className="relative">
              <Filter size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
              <select
                value={staffFilter}
                onChange={(e) => setStaffFilter(e.target.value)}
                className="w-full pl-9 pr-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm text-slate-600 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
              >
                <option value="ALL">All Staff</option>
                {staffs.map((staff) => (
                  <option key={staff.id} value={String(staff.id)}>
                    {staff.name}
                  </option>
                ))}
              </select>
            </div>

            <select
              value={balanceFilter}
              onChange={(e) => setBalanceFilter(e.target.value as BalanceFilter)}
              className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm text-slate-600 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
            >
              <option value="ALL">All Status</option>
              <option value="BALANCED">Balanced Only</option>
              <option value="UNBALANCED">Check Needed</option>
            </select>

            <div className="flex flex-wrap items-center gap-2">
              <div className="inline-flex items-center gap-2 px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-xs font-medium text-slate-600">
                <CalendarRange size={14} className="text-slate-400" />
                <input
                  type="date"
                  value={dateFrom}
                  onChange={(e) => setDateFrom(e.target.value)}
                  className="bg-transparent outline-none"
                />
                <span className="text-slate-400">-</span>
                <input
                  type="date"
                  value={dateTo}
                  onChange={(e) => setDateTo(e.target.value)}
                  className="bg-transparent outline-none"
                />
              </div>
            </div>
          </div>
        </div>

        <div className="p-4 border-b border-slate-100 bg-slate-50/50 flex flex-wrap gap-3 text-xs">
          <span className="px-3 py-1 rounded-full bg-white border border-slate-200 text-slate-600">Filtered Entries: {summary.count}</span>
          <span className="px-3 py-1 rounded-full bg-white border border-slate-200 text-indigo-600">Balanced: {summary.balanced}</span>
          <span className="px-3 py-1 rounded-full bg-white border border-slate-200 text-amber-600">Need Check: {summary.unbalanced}</span>
        </div>

        <div className="overflow-auto">
          <table className="min-w-full text-sm">
            <thead className="sticky top-0 bg-white z-10 shadow-sm">
              <tr className="bg-slate-50 text-slate-500 uppercase text-[10px] font-bold tracking-wider">
                <th className="px-4 py-3 text-left">Reference No</th>
                <th className="px-4 py-3 text-left">Date</th>
                <th className="px-4 py-3 text-left">Description</th>
                <th className="px-4 py-3 text-left">Staff</th>
                <th className="px-4 py-3 text-right">Debit</th>
                <th className="px-4 py-3 text-right">Credit</th>
                <th className="px-4 py-3 text-center">Status</th>
                <th className="px-4 py-3 text-center">Action</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={8} className="px-4 py-10 text-center text-slate-500">
                    Loading journal entries...
                  </td>
                </tr>
              ) : filteredEntries.length === 0 ? (
                <tr>
                  <td colSpan={8} className="px-4 py-10 text-center text-slate-500">
                    No journal entries found.
                  </td>
                </tr>
              ) : (
                filteredEntries.map((entry, index) => {
                  const totals = getTotals(entry);
                  const balanced = totals.debit === totals.credit;
                  const isOpen = expandedId === entry.id;

                  return (
                    <React.Fragment key={entry.id || `${entry.referenceNo}-${index}`}>
                      <tr className="hover:bg-slate-50 text-xs">
                        <td className="px-4 py-3 align-top">
                          <div className="font-medium text-slate-800">{entry.referenceNo || `#${entry.id}`}</div>
                          <div className="text-[11px] text-slate-400 mt-1">#{entry.id || '-'}</div>
                        </td>
                        <td className="px-4 py-3 align-top">
                          <div className="text-slate-700">{fmtDate(entry.entryDate)}</div>
                          <div className="text-[11px] text-slate-400 mt-1">{fmtTime(entry.entryDate)}</div>
                        </td>
                        <td className="px-4 py-3 align-top text-slate-600 max-w-[420px]">
                          <div className="truncate">{entry.description || '-'}</div>
                        </td>
                        <td className="px-4 py-3 align-top">
                          <span className="inline-flex px-2.5 py-1 rounded-full bg-indigo-50 text-indigo-700 text-[11px] font-medium">
                            {entry.staffName || getStaffName(entry.staffId)}
                          </span>
                        </td>
                        <td className="px-4 py-3 align-top text-right font-mono text-emerald-700">
                          {money(totals.debit)}
                        </td>
                        <td className="px-4 py-3 align-top text-right font-mono text-rose-700">
                          {money(totals.credit)}
                        </td>
                        <td className="px-4 py-3 align-top text-center">
                          <span
                            className={`inline-flex px-2.5 py-1 rounded-full text-[10px] font-bold uppercase tracking-wide ${
                              balanced ? 'bg-emerald-50 text-emerald-700' : 'bg-amber-50 text-amber-700'
                            }`}
                          >
                            {balanced ? 'Balanced' : 'Check'}
                          </span>
                        </td>
                        <td className="px-4 py-3 align-top text-center">
                          <button
                            type="button"
                            onClick={() => setExpandedId(isOpen ? null : entry.id || null)}
                            className="inline-flex items-center justify-center w-8 h-8 rounded-lg border border-slate-200 text-slate-500 hover:bg-slate-50"
                          >
                            <ChevronDown size={15} className={`transition-transform ${isOpen ? 'rotate-180' : ''}`} />
                          </button>
                        </td>
                      </tr>

                      {isOpen && (
                        <tr className="bg-slate-50/50">
                          <td colSpan={8} className="px-4 py-4">
                            <div className="rounded-xl border border-slate-200 bg-white overflow-hidden">
                              <div className="px-4 py-3 border-b border-slate-100 flex flex-col md:flex-row md:items-center md:justify-between gap-2">
                                <div>
                                  <h4 className="font-bold text-slate-800 text-sm">Journal Detail Lines</h4>
                                  <p className="text-xs text-slate-500 mt-1">{entry.referenceNo}</p>
                                </div>
                                <div className={`text-xs font-semibold ${balanced ? 'text-emerald-700' : 'text-amber-700'}`}>
                                  {balanced ? 'Balanced entry' : 'Debit and credit need review'}
                                </div>
                              </div>

                              <div className="overflow-auto">
                                <table className="min-w-full text-xs">
                                  <thead>
                                    <tr className="bg-slate-50 text-slate-500 uppercase text-[10px] font-bold tracking-wider">
                                      <th className="px-4 py-3 text-left">Account</th>
                                      <th className="px-4 py-3 text-left">Name</th>
                                      <th className="px-4 py-3 text-right">Debit</th>
                                      <th className="px-4 py-3 text-right">Credit</th>
                                      <th className="px-4 py-3 text-right">Current Balance</th>
                                    </tr>
                                  </thead>
                                  <tbody>
                                    {(entry.details || []).map((detail, detailIndex) => (
                                      <tr key={detailIndex} className="border-t border-slate-100">
                                        <td className="px-4 py-3 font-mono text-slate-500">{detail.accountId || '-'}</td>
                                        <td className="px-4 py-3 text-slate-700">{detail.accountName || `Account #${detail.accountId}`}</td>
                                        <td className="px-4 py-3 text-right font-mono text-emerald-700">{money(detail.debit, '-')}</td>
                                        <td className="px-4 py-3 text-right font-mono text-rose-700">{money(detail.credit, '-')}</td>
                                        <td className={`px-4 py-3 text-right font-mono ${getCurrentBalance(detail.accountId) >= 0 ? 'text-slate-600' : 'text-rose-700'}`}>
                                          {money(getCurrentBalance(detail.accountId))}
                                        </td>
                                      </tr>
                                    ))}
                                  </tbody>
                                </table>
                              </div>

                              <div className="px-4 py-3 border-t border-slate-100 bg-slate-50/50 flex flex-col md:flex-row md:items-center md:justify-between gap-2">
                                <div className="text-xs text-slate-500">Prepared by: {entry.staffName || getStaffName(entry.staffId)}</div>
                                <div className="flex flex-wrap items-center gap-4 text-xs">
                                  <span className="font-semibold text-emerald-700">DR: {money(totals.debit)}</span>
                                  <span className="font-semibold text-rose-700">CR: {money(totals.credit)}</span>
                                </div>
                              </div>
                            </div>
                          </td>
                        </tr>
                      )}
                    </React.Fragment>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        <div className="px-4 py-4 border-t border-slate-100 bg-slate-50/50 flex flex-col md:flex-row md:items-center md:justify-between gap-3">
          <div>
            <p className="text-sm font-semibold text-slate-800">Grand Total</p>
            <p className="text-xs text-slate-500 mt-1">Real-time totals based on current filters</p>
          </div>
          <div className="flex flex-wrap items-center gap-5 text-sm">
            <span className="font-mono font-bold text-emerald-700">DR: {money(summary.totalDebit)}</span>
            <span className="font-mono font-bold text-rose-700">CR: {money(summary.totalCredit)}</span>
            <span className={`font-semibold ${summary.totalDebit === summary.totalCredit ? 'text-emerald-700' : 'text-amber-700'}`}>
              {summary.totalDebit === summary.totalCredit ? 'Balanced' : 'Check Entries'}
            </span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default JournalEntryManagement;

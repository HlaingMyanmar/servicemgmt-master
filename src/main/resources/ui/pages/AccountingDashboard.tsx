import React, { useState, useEffect, useMemo } from 'react';
import { useDataEvents } from '../hooks/useDataEvents';
import { accountingApiService } from '../services/accountingapiservice';
import { coaService } from '../services/coaapiservice';
import { staffService } from '../services/staffapiservice';
import { paymentMethodService } from '../services/paymentmethodapiservice';
import { profitLossService } from '../services/profitlossapiservice';
import { AccountBalanceDTO, PaymentTransactionDTO, ChartOfAccountDTO, StaffDTO, PaymentMethodDTO, AccountType, ProfitLossDTO } from '../types';
import {
  Wallet, Landmark, PieChart, RefreshCw, Banknote, Building2,
  TrendingUp, TrendingDown, ArrowDownLeft, ArrowUpRight, Minus
} from 'lucide-react';
import Swal from 'sweetalert2';

const money = (v: number | undefined) =>
  new Intl.NumberFormat('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 0 }).format(v || 0);

const money2 = (v: number | undefined) =>
  new Intl.NumberFormat('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(v || 0);

const fmtDate = (d?: string | null) => {
  if (!d) return '-';
  try { return new Date(d).toLocaleDateString('en-GB', { day: '2-digit', month: 'short' }); }
  catch { return '-'; }
};

type Direction = 'IN' | 'OUT' | 'NEUTRAL';
const directionOf = (refType?: string): Direction => {
  switch ((refType || '').toLowerCase().replace(/[^a-z_]/g, '')) {
    case 'sale':            return 'IN';
    case 'purchase_return': return 'IN';
    case 'service':         return 'IN';
    case 'purchase':        return 'OUT';
    case 'debt_payment':    return 'OUT';
    case 'sale_return':     return 'OUT';
    default:                return 'NEUTRAL';
  }
};
const TYPE_LABELS: Record<string, string> = {
  Sale: 'Sale', Purchase: 'Purchase', Sale_Return: 'Sale Return',
  Purchase_Return: 'Purchase Return', Debt_Payment: 'Debt Payment',
  Opening_Balance: 'Opening Balance', Transfer: 'Transfer',
};

const firstOfMonth = () => {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-01`;
};
const today = () => new Date().toISOString().slice(0, 10);

const AccountingDashboard: React.FC = () => {
  const [balances, setBalances]       = useState<AccountBalanceDTO[]>([]);
  const [transactions, setTransactions] = useState<PaymentTransactionDTO[]>([]);
  const [loading, setLoading]         = useState(true);
  const [activeTab, setActiveTab]     = useState<'overview' | 'cashflow' | 'opening'>('overview');
  const [accounts, setAccounts]       = useState<ChartOfAccountDTO[]>([]);
  const [staffs, setStaffs]           = useState<StaffDTO[]>([]);
  const [paymentMethods, setPaymentMethods] = useState<PaymentMethodDTO[]>([]);
  const [pl, setPl]                   = useState<ProfitLossDTO | null>(null);
  const [plLoading, setPlLoading]     = useState(false);
  const [plFrom, setPlFrom]           = useState(firstOfMonth());
  const [plTo, setPlTo]               = useState(today());

  const [openingAccountId, setOpeningAccountId]         = useState(0);
  const [openingAmount, setOpeningAmount]               = useState('');
  const [openingStaffId, setOpeningStaffId]             = useState(0);
  const [openingPaymentMethodId, setOpeningPaymentMethodId] = useState(0);
  const [openingSaving, setOpeningSaving]               = useState(false);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [balanceRes, transRes, coaRes, staffRes, payRes] = await Promise.all([
        accountingApiService.getAllBalances(),
        accountingApiService.getDetailedReport(),
        coaService.getAll(),
        staffService.getAllActive(),
        paymentMethodService.getAllActive()
      ]);
      setBalances(balanceRes);
      setTransactions(Array.isArray(transRes) ? transRes : []);
      setAccounts(coaRes);
      setStaffs(staffRes);
      setPaymentMethods(payRes);
    } catch (error) {
      console.error('Error fetching accounting data:', error);
    } finally {
      setLoading(false);
    }
  };

  const fetchPL = async (from = plFrom, to = plTo) => {
    setPlLoading(true);
    try {
      const res = await profitLossService.getReport(from, to);
      setPl(res);
    } catch { /* silent */ } finally { setPlLoading(false); }
  };

  useEffect(() => {
    fetchData();
    fetchPL();
  }, []);
  useDataEvents(['Sale', 'Purchase', 'Expense', 'Income', 'Payment', 'Journal'], () => { fetchData(); fetchPL(); });

  const accountById = useMemo(() => new Map(accounts.map(a => [a.id, a])), [accounts]);
  const balanceByAccountId = useMemo(() => new Map(balances.map(b => [b.accountId, b])), [balances]);

  const totalByAccountType = (type: AccountType) => balances.reduce((sum, b) => {
    const acc = accountById.get(b.accountId);
    return acc?.accountType === type ? sum + (Number(b.currentBalance) || 0) : sum;
  }, 0);

  const totalAssets      = totalByAccountType(AccountType.Asset);
  const totalLiabilities = totalByAccountType(AccountType.Liability);
  const totalEquity      = totalByAccountType(AccountType.Equity);

  const cashAndBankMethods = useMemo(() => {
    const seen = new Set<number>();
    return paymentMethods
      .filter(m => {
        const txt = `${m.methodName || ''} ${m.accountName || ''}`.toLowerCase();
        return m.accountId && ['cash', 'bank', 'kpay', 'wave', 'pay'].some(t => txt.includes(t));
      })
      .filter(m => {
        if (!m.accountId || seen.has(m.accountId)) return false;
        seen.add(m.accountId);
        return true;
      })
      .map(m => {
        const b = balanceByAccountId.get(m.accountId!);
        return { ...m, currentBalance: Number(b?.currentBalance) || 0, openingBalance: Number(b?.openingBalance) || 0, lastUpdated: b?.lastUpdated };
      });
  }, [balanceByAccountId, paymentMethods]);

  const cashTotal = cashAndBankMethods
    .filter(m => `${m.methodName} ${m.accountName || ''}`.toLowerCase().includes('cash'))
    .reduce((s, m) => s + m.currentBalance, 0);
  const bankTotal = cashAndBankMethods
    .filter(m => !`${m.methodName} ${m.accountName || ''}`.toLowerCase().includes('cash'))
    .reduce((s, m) => s + m.currentBalance, 0);

  const recentTxns = useMemo(() =>
    [...transactions]
      .sort((a, b) => new Date(b.paymentDate ?? 0).getTime() - new Date(a.paymentDate ?? 0).getTime())
      .slice(0, 15)
  , [transactions]);

  const totalIn  = useMemo(() => transactions.filter(t => directionOf(t.referenceType) === 'IN').reduce((s, t) => s + (t.amount || 0), 0), [transactions]);
  const totalOut = useMemo(() => transactions.filter(t => directionOf(t.referenceType) === 'OUT').reduce((s, t) => s + (t.amount || 0), 0), [transactions]);

  const handleSetOpening = async (e: React.FormEvent) => {
    e.preventDefault();
    const amount = parseFloat(openingAmount);
    if (!openingAccountId || !openingStaffId || !openingPaymentMethodId || Number.isNaN(amount)) {
      Swal.fire('Validation', 'Account, Staff, Payment Method, Amount တွေ ဖြည့်ပေးပါ။', 'warning');
      return;
    }
    setOpeningSaving(true);
    try {
      await accountingApiService.setOpeningBalance(openingAccountId, amount, openingStaffId, openingPaymentMethodId);
      setOpeningAccountId(0); setOpeningAmount(''); setOpeningStaffId(0); setOpeningPaymentMethodId(0);
      fetchData();
      Swal.fire({ icon: 'success', title: 'Opening balance set', toast: true, position: 'top-end', showConfirmButton: false, timer: 1500 });
    } catch (err: any) {
      Swal.fire('Error', err.message || 'Failed', 'error');
    } finally { setOpeningSaving(false); }
  };

  const tabs = [
    { key: 'overview',  label: 'Financial Overview' },
    { key: 'cashflow',  label: 'Cash Flow & Transactions' },
    { key: 'opening',   label: 'Opening Balance' },
  ] as const;

  return (
    <div className="space-y-5">
      {/* Header + Tabs */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h2 className="text-xl font-bold text-slate-800">Accounting Dashboard</h2>
          <div className="flex flex-wrap items-center gap-2 mt-2">
            {tabs.map(t => (
              <button key={t.key} onClick={() => setActiveTab(t.key)}
                className={`px-3 py-1.5 rounded-lg text-xs font-bold tracking-wide transition-all ${
                  activeTab === t.key ? 'bg-indigo-600 text-white shadow' : 'bg-white text-slate-500 border border-slate-200 hover:bg-slate-50'
                }`}>
                {t.label}
              </button>
            ))}
          </div>
        </div>
        <button onClick={() => { fetchData(); fetchPL(); }}
          className="flex items-center gap-2 px-3 py-1.5 bg-white border border-slate-200 rounded-lg text-xs font-medium text-slate-600 hover:bg-slate-50">
          <RefreshCw size={14} className={loading ? 'animate-spin' : ''} /> Refresh
        </button>
      </div>

      {/* ══ OVERVIEW TAB ══════════════════════════════════════════════════ */}
      {activeTab === 'overview' && (
        <>
          {/* P&L with date range */}
          <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
            <div className="px-5 py-3 border-b border-slate-100 bg-slate-800 flex flex-wrap items-center justify-between gap-3">
              <span className="font-bold text-white text-sm">Profit &amp; Loss</span>
              <div className="flex flex-wrap items-center gap-2">
                <input type="date" value={plFrom} onChange={e => setPlFrom(e.target.value)}
                  className="px-2 py-1 bg-slate-700 border border-slate-600 rounded text-xs text-slate-200 focus:outline-none focus:ring-1 focus:ring-indigo-400" />
                <span className="text-slate-400 text-xs">→</span>
                <input type="date" value={plTo} onChange={e => setPlTo(e.target.value)}
                  className="px-2 py-1 bg-slate-700 border border-slate-600 rounded text-xs text-slate-200 focus:outline-none focus:ring-1 focus:ring-indigo-400" />
                <button onClick={() => fetchPL(plFrom, plTo)} disabled={plLoading}
                  className="px-3 py-1 bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-bold rounded disabled:opacity-60">
                  {plLoading ? 'Loading…' : 'Apply'}
                </button>
                <button onClick={() => { const f = firstOfMonth(); const t = today(); setPlFrom(f); setPlTo(t); fetchPL(f, t); }}
                  className="px-2 py-1 bg-slate-700 hover:bg-slate-600 text-slate-300 text-xs rounded">
                  This Month
                </button>
              </div>
            </div>
            {pl ? (
              <div className="grid grid-cols-2 sm:grid-cols-4 divide-x divide-slate-100">
                {[
                  { label: 'Net Revenue (ဝင်ငွေ)', value: pl.netRevenue, color: 'text-indigo-700', bg: 'bg-indigo-50' },
                  { label: 'Net Purchases (ဝယ်ငွေ)', value: pl.netPurchases, color: 'text-amber-700', bg: 'bg-amber-50' },
                  { label: 'Total Expenses (ကုန်ကျစရိတ်)', value: pl.totalExpenses, color: 'text-rose-700', bg: 'bg-rose-50' },
                  { label: pl.netProfit >= 0 ? 'Net Profit (အမြတ်)' : 'Net Loss (အရှုံး)', value: pl.netProfit, color: pl.netProfit >= 0 ? 'text-emerald-700' : 'text-rose-700', bg: pl.netProfit >= 0 ? 'bg-emerald-50' : 'bg-rose-50' },
                ].map(c => (
                  <div key={c.label} className={`p-4 ${c.bg}`}>
                    <p className="text-[10px] font-bold text-slate-500 uppercase tracking-wider">{c.label}</p>
                    <p className={`text-xl font-bold mt-1 ${c.color} tabular-nums`}>{money(c.value)} Ks</p>
                  </div>
                ))}
              </div>
            ) : (
              <div className="p-6 text-center text-slate-400 text-xs">
                {plLoading ? 'Loading P&L...' : 'Unable to load P&L data'}
              </div>
            )}
          </div>

          {/* Gross Profit detail */}
          {pl && (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
              <div className="bg-white rounded-xl border border-slate-200 shadow-sm p-4">
                <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Gross Sales</p>
                <p className="text-lg font-bold text-slate-800 mt-1">{money(pl.grossSales)} Ks</p>
                {pl.salesReturns > 0 && <p className="text-[10px] text-rose-500 mt-0.5">(-) Returns: {money(pl.salesReturns)} Ks</p>}
              </div>
              <div className="bg-white rounded-xl border border-slate-200 shadow-sm p-4">
                <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Gross Profit</p>
                <p className={`text-lg font-bold mt-1 ${pl.grossProfit >= 0 ? 'text-emerald-700' : 'text-rose-700'}`}>{money(pl.grossProfit)} Ks</p>
                <p className="text-[10px] text-slate-400 mt-0.5">Revenue - Purchases</p>
              </div>
              <div className="bg-white rounded-xl border border-slate-200 shadow-sm p-4">
                <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Other Income</p>
                <p className="text-lg font-bold text-emerald-600 mt-1">{money(pl.totalOtherIncome)} Ks</p>
                <p className="text-[10px] text-slate-400 mt-0.5">{pl.otherIncomeItems.length} account(s)</p>
              </div>
              <div className="bg-white rounded-xl border border-slate-200 shadow-sm p-4">
                <div className="flex items-center gap-2">
                  {pl.netProfit >= 0
                    ? <TrendingUp size={16} className="text-emerald-500" />
                    : <TrendingDown size={16} className="text-rose-500" />}
                  <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Net {pl.netProfit >= 0 ? 'Profit' : 'Loss'}</p>
                </div>
                <p className={`text-lg font-bold mt-1 ${pl.netProfit >= 0 ? 'text-emerald-700' : 'text-rose-700'}`}>{money(Math.abs(pl.netProfit))} Ks</p>
              </div>
            </div>
          )}

          {/* Cash & Bank */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="bg-white p-4 rounded-2xl border border-emerald-200 border-l-4 border-l-emerald-500 shadow-sm">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-emerald-50 text-emerald-600 rounded-xl flex items-center justify-center"><Wallet size={20} /></div>
                <div>
                  <p className="text-[10px] font-bold text-slate-500 uppercase tracking-wider">Cash + Bank</p>
                  <p className="text-lg font-bold text-slate-800">{money(cashTotal + bankTotal)} Ks</p>
                </div>
              </div>
            </div>
            <div className="bg-white p-4 rounded-2xl border border-amber-200 border-l-4 border-l-amber-500 shadow-sm">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-amber-50 text-amber-600 rounded-xl flex items-center justify-center"><Banknote size={20} /></div>
                <div>
                  <p className="text-[10px] font-bold text-slate-500 uppercase tracking-wider">Cash in Hand</p>
                  <p className="text-lg font-bold text-slate-800">{money(cashTotal)} Ks</p>
                </div>
              </div>
            </div>
            <div className="bg-white p-4 rounded-2xl border border-sky-200 border-l-4 border-l-sky-500 shadow-sm">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-sky-50 text-sky-700 rounded-xl flex items-center justify-center"><Building2 size={20} /></div>
                <div>
                  <p className="text-[10px] font-bold text-slate-500 uppercase tracking-wider">Bank / Digital</p>
                  <p className="text-lg font-bold text-slate-800">{money(bankTotal)} Ks</p>
                </div>
              </div>
            </div>
          </div>

          {/* Cash & Bank detail table */}
          {cashAndBankMethods.length > 0 && (
            <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
              <div className="p-4 border-b border-slate-100 bg-indigo-50">
                <h3 className="font-bold text-slate-800 text-sm">Cash &amp; Bank Detail</h3>
              </div>
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse min-w-[600px]">
                  <thead>
                    <tr className="bg-slate-50 text-slate-500 uppercase text-[10px] font-bold tracking-wider">
                      <th className="px-4 py-3 border-b border-slate-100">Method</th>
                      <th className="px-4 py-3 border-b border-slate-100">Linked Account</th>
                      <th className="px-4 py-3 border-b border-slate-100 text-right">Opening</th>
                      <th className="px-4 py-3 border-b border-slate-100 text-right">Current Balance</th>
                      <th className="px-4 py-3 border-b border-slate-100 text-right">Updated</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {cashAndBankMethods.map(m => (
                      <tr key={m.id} className="hover:bg-slate-50 text-xs">
                        <td className="px-4 py-3 font-semibold text-slate-800">{m.methodName}</td>
                        <td className="px-4 py-3 text-slate-600">{m.accountName || '-'}</td>
                        <td className="px-4 py-3 text-right text-slate-500">{money2(m.openingBalance)}</td>
                        <td className={`px-4 py-3 text-right font-bold ${m.currentBalance >= 0 ? 'text-emerald-700' : 'text-rose-700'}`}>
                          {money2(m.currentBalance)} Ks
                        </td>
                        <td className="px-4 py-3 text-right text-slate-400">
                          {m.lastUpdated ? new Date(m.lastUpdated).toLocaleString() : '-'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* Balance Sheet summary */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            {[
              { label: 'Total Assets', value: totalAssets, Icon: Wallet, color: 'text-emerald-600', bg: 'bg-emerald-50' },
              { label: 'Total Liabilities', value: totalLiabilities, Icon: Landmark, color: 'text-rose-600', bg: 'bg-rose-50' },
              { label: 'Total Equity', value: totalEquity, Icon: PieChart, color: 'text-indigo-600', bg: 'bg-indigo-50' },
            ].map(c => (
              <div key={c.label} className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm">
                <div className="flex items-center gap-3">
                  <div className={`w-10 h-10 ${c.bg} ${c.color} rounded-xl flex items-center justify-center`}><c.Icon size={20} /></div>
                  <div>
                    <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">{c.label}</p>
                    <p className={`text-lg font-bold ${c.color} mt-0.5`}>{money2(c.value)} Ks</p>
                  </div>
                </div>
              </div>
            ))}
          </div>

          {/* Account Balances Table */}
          <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
            <div className="p-4 border-b border-slate-100 bg-slate-50/50">
              <h3 className="font-bold text-slate-800 text-sm">Account Balances (Trial Balance)</h3>
            </div>
            <div className="overflow-auto max-h-80 custom-scrollbar">
              <table className="w-full text-left border-collapse">
                <thead className="sticky top-0 bg-white z-10 shadow-sm">
                  <tr className="bg-slate-50 text-slate-500 uppercase text-[10px] font-bold tracking-wider">
                    <th className="px-4 py-3 border-b border-slate-100">Account</th>
                    <th className="px-4 py-3 border-b border-slate-100">Type</th>
                    <th className="px-4 py-3 border-b border-slate-100">Fiscal Year</th>
                    <th className="px-4 py-3 border-b border-slate-100 text-right">Current Balance</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {balances.length > 0 ? balances.map(b => {
                    const acc = accountById.get(b.accountId);
                    return (
                      <tr key={b.id} className="hover:bg-slate-50 text-xs">
                        <td className="px-4 py-2.5 font-medium text-slate-700">{b.accountName}</td>
                        <td className="px-4 py-2.5 text-slate-400 text-[10px]">{acc?.accountType ?? '-'}</td>
                        <td className="px-4 py-2.5 text-slate-500">{b.fiscalYear}</td>
                        <td className={`px-4 py-2.5 text-right font-bold ${b.currentBalance >= 0 ? 'text-emerald-600' : 'text-rose-600'}`}>
                          {money2(b.currentBalance)}
                        </td>
                      </tr>
                    );
                  }) : (
                    <tr><td colSpan={4} className="px-4 py-8 text-center text-slate-400 italic">No balance records</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}

      {/* ══ CASH FLOW TAB ═══════════════════════════════════════════════ */}
      {activeTab === 'cashflow' && (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="bg-white p-4 rounded-2xl border border-emerald-200 border-l-4 border-l-emerald-500 shadow-sm flex items-center gap-3">
              <div className="w-10 h-10 bg-emerald-50 rounded-xl flex items-center justify-center flex-shrink-0"><ArrowDownLeft size={20} className="text-emerald-600" /></div>
              <div>
                <p className="text-[10px] font-bold text-slate-500 uppercase tracking-wider">Total IN (ဝင်ငွေ)</p>
                <p className="text-xl font-bold text-emerald-700">{money(totalIn)} Ks</p>
              </div>
            </div>
            <div className="bg-white p-4 rounded-2xl border border-rose-200 border-l-4 border-l-rose-500 shadow-sm flex items-center gap-3">
              <div className="w-10 h-10 bg-rose-50 rounded-xl flex items-center justify-center flex-shrink-0"><ArrowUpRight size={20} className="text-rose-600" /></div>
              <div>
                <p className="text-[10px] font-bold text-slate-500 uppercase tracking-wider">Total OUT (ထွက်ငွေ)</p>
                <p className="text-xl font-bold text-rose-700">{money(totalOut)} Ks</p>
              </div>
            </div>
            <div className={`bg-white p-4 rounded-2xl shadow-sm flex items-center gap-3 border-l-4 ${(totalIn - totalOut) >= 0 ? 'border border-indigo-200 border-l-indigo-500' : 'border border-orange-200 border-l-orange-500'}`}>
              <div className={`w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0 ${(totalIn - totalOut) >= 0 ? 'bg-indigo-50' : 'bg-orange-50'}`}>
                <Minus size={20} className={(totalIn - totalOut) >= 0 ? 'text-indigo-600' : 'text-orange-600'} />
              </div>
              <div>
                <p className="text-[10px] font-bold text-slate-500 uppercase tracking-wider">Net Flow</p>
                <p className={`text-xl font-bold ${(totalIn - totalOut) >= 0 ? 'text-indigo-700' : 'text-orange-700'}`}>
                  {(totalIn - totalOut) >= 0 ? '+' : ''}{money(totalIn - totalOut)} Ks
                </p>
              </div>
            </div>
          </div>

          <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
            <div className="p-4 border-b border-slate-100 bg-slate-50/50 flex items-center justify-between">
              <h3 className="font-bold text-slate-800 text-sm">Recent Transactions (Latest 15)</h3>
              <span className="text-[11px] text-slate-400">{transactions.length} total records</span>
            </div>
            <div className="overflow-auto max-h-[520px] custom-scrollbar">
              <table className="w-full text-left border-collapse min-w-[720px]">
                <thead className="sticky top-0 bg-white z-10 shadow-sm">
                  <tr className="bg-slate-50 text-slate-500 uppercase text-[10px] font-bold tracking-wider">
                    <th className="px-4 py-3 border-b border-slate-100">Date</th>
                    <th className="px-4 py-3 border-b border-slate-100">Direction</th>
                    <th className="px-4 py-3 border-b border-slate-100">Type</th>
                    <th className="px-4 py-3 border-b border-slate-100">Reference</th>
                    <th className="px-4 py-3 border-b border-slate-100">Entity</th>
                    <th className="px-4 py-3 border-b border-slate-100">Method</th>
                    <th className="px-4 py-3 border-b border-slate-100 text-right">Amount (Ks)</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {recentTxns.length > 0 ? recentTxns.map(t => {
                    const dir = directionOf(t.referenceType);
                    return (
                      <tr key={t.id} className="hover:bg-slate-50 text-xs">
                        <td className="px-4 py-3 text-slate-500">{fmtDate(t.paymentDate as any)}</td>
                        <td className="px-4 py-3">
                          {dir === 'IN' && <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200"><ArrowDownLeft size={10}/> IN</span>}
                          {dir === 'OUT' && <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-rose-50 text-rose-700 border border-rose-200"><ArrowUpRight size={10}/> OUT</span>}
                          {dir === 'NEUTRAL' && <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-slate-100 text-slate-500 border border-slate-200">—</span>}
                        </td>
                        <td className="px-4 py-3">
                          <span className="px-2 py-0.5 bg-slate-100 text-slate-600 rounded text-[10px] font-bold">
                            {TYPE_LABELS[t.referenceType ?? ''] ?? t.referenceType ?? '-'}
                          </span>
                        </td>
                        <td className="px-4 py-3 font-mono text-indigo-600 font-medium">{t.referenceCode ?? `#${t.referenceId}`}</td>
                        <td className="px-4 py-3 text-slate-700">{t.entityName ?? '-'}</td>
                        <td className="px-4 py-3 text-slate-500">{t.paymentMethodName ?? '-'}</td>
                        <td className={`px-4 py-3 text-right font-bold tabular-nums ${dir === 'IN' ? 'text-emerald-700' : dir === 'OUT' ? 'text-rose-700' : 'text-slate-700'}`}>
                          {dir === 'IN' ? '+' : dir === 'OUT' ? '-' : ''}{money(t.amount)}
                        </td>
                      </tr>
                    );
                  }) : (
                    <tr><td colSpan={7} className="px-4 py-8 text-center text-slate-400 italic">No transactions found</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}

      {/* ══ OPENING BALANCE TAB ══════════════════════════════════════════ */}
      {activeTab === 'opening' && (
        <div className="space-y-6">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-6">
            <h3 className="text-sm font-bold text-slate-800 mb-4">Set Opening Balance</h3>
            <form onSubmit={handleSetOpening} className="flex flex-wrap items-end gap-3">
              <div className="min-w-[220px] flex-1">
                <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">Account</label>
                <select value={openingAccountId} onChange={e => setOpeningAccountId(Number(e.target.value))}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/20">
                  <option value={0}>Select account</option>
                  {accounts.map(a => <option key={a.id} value={a.id}>{a.accountName} ({a.code})</option>)}
                </select>
              </div>
              <div className="min-w-[140px]">
                <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">Amount</label>
                <input type="number" step="0.01" value={openingAmount} onChange={e => setOpeningAmount(e.target.value)}
                  placeholder="0.00" className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/20" />
              </div>
              <div className="min-w-[180px] flex-1">
                <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">Staff</label>
                <select value={openingStaffId} onChange={e => setOpeningStaffId(Number(e.target.value))}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/20">
                  <option value={0}>Select staff</option>
                  {staffs.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
                </select>
              </div>
              <div className="min-w-[180px] flex-1">
                <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">Payment Method</label>
                <select value={openingPaymentMethodId} onChange={e => setOpeningPaymentMethodId(Number(e.target.value))}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/20">
                  <option value={0}>Select method</option>
                  {paymentMethods.map(m => <option key={m.id} value={m.id}>{m.methodName}</option>)}
                </select>
              </div>
              <button type="submit" disabled={openingSaving}
                className="px-4 py-2 bg-indigo-600 text-white rounded-lg text-sm font-bold hover:bg-indigo-700 disabled:opacity-50">
                {openingSaving ? 'Saving...' : 'Set Opening'}
              </button>
            </form>
          </div>

          <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
            <div className="p-4 border-b border-slate-100 bg-slate-50/50">
              <h3 className="font-bold text-slate-800 text-sm">Opening Balances</h3>
            </div>
            <div className="overflow-auto max-h-96 custom-scrollbar">
              <table className="w-full text-left border-collapse">
                <thead className="sticky top-0 bg-white z-10 shadow-sm">
                  <tr className="bg-slate-50 text-slate-500 uppercase text-[10px] font-bold tracking-wider">
                    <th className="px-4 py-3 border-b border-slate-100">Account</th>
                    <th className="px-4 py-3 border-b border-slate-100">Fiscal Year</th>
                    <th className="px-4 py-3 border-b border-slate-100 text-right">Opening</th>
                    <th className="px-4 py-3 border-b border-slate-100 text-right">Current</th>
                    <th className="px-4 py-3 border-b border-slate-100 text-right">Last Updated</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {balances.length > 0 ? balances.map(b => (
                    <tr key={b.id} className="hover:bg-slate-50 text-xs">
                      <td className="px-4 py-3 font-medium text-slate-700">{b.accountName}</td>
                      <td className="px-4 py-3 text-slate-500">{b.fiscalYear}</td>
                      <td className="px-4 py-3 text-right text-slate-600">{money2(b.openingBalance)}</td>
                      <td className={`px-4 py-3 text-right font-bold ${b.currentBalance >= 0 ? 'text-emerald-600' : 'text-rose-600'}`}>
                        {money2(b.currentBalance)}
                      </td>
                      <td className="px-4 py-3 text-right text-slate-400">{new Date(b.lastUpdated).toLocaleString()}</td>
                    </tr>
                  )) : (
                    <tr><td colSpan={5} className="px-4 py-8 text-center text-slate-400 italic">No balance records</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AccountingDashboard;

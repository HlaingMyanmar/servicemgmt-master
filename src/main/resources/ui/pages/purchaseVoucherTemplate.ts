import { PurchaseDTO } from '../types';
import { buildCompanyContact, CompanySettings, getCachedCompanySettings } from '../utils/companySettings';
import { DEFAULT_PURCHASE_CONFIG, parseVoucherConfig, VoucherPurchaseConfig } from '../utils/voucherTemplateConfig';

const escapeHtml = (v?: string | number | null) =>
  String(v ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');

const money = (v: number) =>
  new Intl.NumberFormat('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(v || 0);

const fmtDate = (v?: string) => {
  if (!v) return '-';
  const d = new Date(v);
  if (Number.isNaN(d.getTime())) return v;
  return d.toLocaleString('en-GB', { year: 'numeric', month: 'short', day: '2-digit', hour: '2-digit', minute: '2-digit' });
};

type BuildPurchaseVoucherHtmlInput = {
  purchase: PurchaseDTO;
  settings?: CompanySettings;
  cfg?: Partial<VoucherPurchaseConfig>;
  preview?: boolean;
};

export const buildPurchaseVoucherHtml = ({
  purchase,
  settings,
  cfg: cfgOverride,
  preview = false,
}: BuildPurchaseVoucherHtmlInput): { html: string; popupSize: string } => {
  const cs = settings ?? getCachedCompanySettings();
  const baseCfg = parseVoucherConfig(cs.voucherConfigJson).purchase;
  const cfg: VoucherPurchaseConfig = { ...baseCfg, ...cfgOverride };

  const companyName = cs.companyName || 'Company';
  const companyContact = buildCompanyContact(cs);
  const footerNote = cs.footerNote || 'Thank you';
  const logoSrc = cs.logoBase64 || '/img/logo.png';
  const headerColor = cfg.headerColor || DEFAULT_PURCHASE_CONFIG.headerColor;

  const itemRows = (purchase.details || []).map((d, idx) => {
    const name = escapeHtml(d.productName || `Product #${d.productId}`);
    const qty = Number(d.qty) || 0;
    const cost = money(Number(d.unitCost) || 0);
    const sub = money(Number(d.subtotal) || 0);
    const serials = d.serialNumbers?.length ? d.serialNumbers.join(', ') : '-';
    return `
      <tr>
        <td class="center">${idx + 1}</td>
        <td>
          <div>${name}</div>
          ${d.serialNumbers?.length ? `<div class="item-sn">SN: ${escapeHtml(serials)}</div>` : ''}
          ${d.warrantyMonths ? `<div class="item-sn">Warranty: ${d.warrantyMonths} month(s)</div>` : ''}
        </td>
        <td class="num">${qty}</td>
        <td class="num">${cost}</td>
        <td class="num">${sub}</td>
      </tr>
    `;
  }).join('');

  const total = Number(purchase.totalAmount) || 0;
  const paid = Number(purchase.paidAmount) || 0;
  const due = Number(purchase.dueAmount) || (total - paid);

  const signaturesHtml = cfg.showSignatures ? `
    <div class="signatures">
      <div class="sign">${escapeHtml(cfg.sign1)}</div>
      <div class="sign">${escapeHtml(cfg.sign2)}</div>
    </div>
  ` : '';

  const html = `
<!doctype html>
<html>
<head>
  <meta charset="utf-8" />
  <title>Purchase Voucher ${escapeHtml(purchase.purchaseCode || purchase.id)}</title>
  <style>
    @page { size: A4 portrait; margin: 10mm; }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body { font-family: 'Segoe UI', Arial, sans-serif; color: #1e293b; font-size: 12px; line-height: 1.5; background: #fff; }

    .header {
      display: flex; justify-content: space-between; align-items: flex-start; gap: 16px;
      padding: 18px 20px 14px; background: ${headerColor}; border-radius: 10px 10px 0 0;
    }
    .brand-name { font-size: 22px; font-weight: 800; color: #fff; }
    .brand-sub { margin-top: 3px; font-size: 10px; color: #94a3b8; max-width: 340px; line-height: 1.4; }
    .inv-box { text-align: right; flex-shrink: 0; }
    .inv-label { font-size: 9px; text-transform: uppercase; letter-spacing: 1px; color: #64748b; }
    .inv-code { font-size: 18px; font-weight: 800; color: #fff; margin-top: 2px; }
    .inv-date { font-size: 10px; color: #94a3b8; margin-top: 2px; }

    .body-wrap { border: 1px solid #e2e8f0; border-top: none; border-radius: 0 0 10px 10px; padding: 14px 20px; }

    .blocks { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-bottom: 14px; padding-bottom: 14px; border-bottom: 1px dashed #e2e8f0; }
    .block { padding: 10px 12px; background: #f8fafc; border-radius: 7px; border: 1px solid #e2e8f0; }
    .block-title { font-size: 9px; text-transform: uppercase; letter-spacing: 0.8px; color: #64748b; margin-bottom: 6px; padding-bottom: 4px; border-bottom: 1px solid #e2e8f0; }
    .block-row { display: flex; justify-content: space-between; gap: 8px; margin-top: 4px; }
    .block-row:first-of-type { margin-top: 0; }
    .bl { color: #64748b; font-size: 10px; }
    .bv { font-weight: 600; font-size: 11px; text-align: right; }

    .badge { display: inline-block; padding: 1px 7px; border-radius: 20px; font-size: 10px; font-weight: 600; background: #dcfce7; color: #166534; }
    .badge.partial { background: #fef9c3; color: #854d0e; }
    .badge.pending { background: #f1f5f9; color: #475569; }

    .table-wrap { border: 1px solid #e2e8f0; border-radius: 7px; overflow: hidden; margin-bottom: 12px; }
    table { width: 100%; border-collapse: collapse; }
    th { background: #f1f5f9; color: #475569; font-size: 9.5px; text-transform: uppercase; letter-spacing: 0.6px; padding: 6px 8px; border-bottom: 1px solid #e2e8f0; font-weight: 700; }
    td { padding: 6px 8px; border-bottom: 1px solid #f1f5f9; vertical-align: top; }
    tr:last-child td { border-bottom: none; }
    tr:nth-child(even) td { background: #fafbfd; }
    .num { text-align: right; white-space: nowrap; }
    .center { text-align: center; }
    .item-sn { font-size: 9.5px; color: #64748b; margin-top: 1px; }

    .bottom-area { display: flex; justify-content: flex-end; align-items: flex-start; gap: 16px; }
    .summary-box { width: 250px; flex-shrink: 0; border: 1px solid #e2e8f0; border-radius: 7px; overflow: hidden; }
    .s-row { display: flex; justify-content: space-between; align-items: center; padding: 6px 12px; border-bottom: 1px solid #f1f5f9; font-size: 12px; }
    .s-row:last-child { border-bottom: none; }
    .s-row.highlight { background: ${headerColor}; color: #fff; font-weight: 700; }
    .s-row.sub-highlight { background: #f1f5f9; font-weight: 600; }
    .s-val { font-weight: 600; }

    .remark-box { margin-top: 12px; border: 1px solid #e2e8f0; border-radius: 7px; padding: 8px 10px; min-height: 34px; font-size: 11px; color: #475569; }
    .section-label { font-size: 9.5px; text-transform: uppercase; letter-spacing: 0.7px; color: #64748b; font-weight: 700; margin-bottom: 6px; }
    .signatures { margin-top: 24px; display: grid; grid-template-columns: 1fr 1fr; gap: 24px; }
    .sign { padding-top: 30px; border-top: 1px solid #cbd5e1; text-align: center; font-size: 10px; color: #64748b; }
    .footer-bar { margin-top: 12px; padding-top: 8px; border-top: 1px dashed #e2e8f0; text-align: center; font-size: 9.5px; color: #94a3b8; }
  </style>
</head>
<body>
  <div class="header">
    <div style="display:flex;align-items:center;gap:12px;">
      ${logoSrc ? `<img src="${logoSrc}" alt="logo" style="max-height:50px;max-width:80px;background:#fff;border-radius:6px;padding:4px;" />` : ''}
      <div>
        <div class="brand-name">${escapeHtml(companyName)}</div>
        <div class="brand-sub">${escapeHtml(companyContact)}</div>
      </div>
    </div>
    <div class="inv-box">
      <div class="inv-label">${escapeHtml(cfg.title)}</div>
      <div class="inv-code">${escapeHtml(purchase.purchaseCode || `#${purchase.id}`)}</div>
      <div class="inv-date">${escapeHtml(fmtDate(purchase.purchaseDate))}</div>
    </div>
  </div>

  <div class="body-wrap">
    <div class="blocks">
      <div class="block">
        <div class="block-title">${escapeHtml(cfg.secSupplier)}</div>
        <div class="block-row">
          <span class="bl">Supplier</span>
          <span class="bv">${escapeHtml(purchase.supplierName || '-')}</span>
        </div>
        <div class="block-row">
          <span class="bl">Payment</span>
          <span class="bv">
            <span class="badge${purchase.paymentStatus === 'Partial' ? ' partial' : purchase.paymentStatus === 'Pending' ? ' pending' : ''}">${escapeHtml(purchase.paymentStatus || '-')}</span>
          </span>
        </div>
      </div>
      <div class="block">
        <div class="block-title">${escapeHtml(cfg.secPreparedBy)}</div>
        <div class="block-row">
          <span class="bl">Staff</span>
          <span class="bv">${escapeHtml(purchase.staffName || '-')}</span>
        </div>
        <div class="block-row">
          <span class="bl">Date</span>
          <span class="bv">${escapeHtml(fmtDate(purchase.purchaseDate))}</span>
        </div>
      </div>
    </div>

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th style="width:5%" class="center">#</th>
            <th style="width:45%">${escapeHtml(cfg.colItem)}</th>
            <th style="width:10%" class="num">${escapeHtml(cfg.colQty)}</th>
            <th style="width:20%" class="num">${escapeHtml(cfg.colUnitCost)}</th>
            <th style="width:20%" class="num">${escapeHtml(cfg.colAmount)}</th>
          </tr>
        </thead>
        <tbody>
          ${itemRows || '<tr><td colspan="5" class="center" style="padding:16px;color:#94a3b8;">No items</td></tr>'}
        </tbody>
      </table>
    </div>

    <div class="bottom-area">
      <div class="summary-box">
        <div class="s-row"><span>Total</span><span class="s-val">${money(total)}</span></div>
        <div class="s-row sub-highlight"><span>Paid</span><span class="s-val">${money(paid)}</span></div>
        <div class="s-row highlight"><span>Balance Due</span><span class="s-val">${money(due)}</span></div>
      </div>
    </div>

    ${purchase.remark ? `
      <div class="section-label" style="margin-top:12px;">${escapeHtml(cfg.secRemark)}</div>
      <div class="remark-box">${escapeHtml(purchase.remark)}</div>
    ` : ''}

    ${signaturesHtml}

    <div class="footer-bar">
      ${escapeHtml(companyName)} &nbsp;·&nbsp; ${escapeHtml(footerNote)} &nbsp;·&nbsp; ${escapeHtml(fmtDate(purchase.purchaseDate))}
    </div>
  </div>

  ${preview ? '' : `
  <script>
    window.onload = function() {
      setTimeout(function() { window.print(); window.close(); }, 120);
    };
  </script>
  `}
</body>
</html>`;

  return { html, popupSize: 'width=980,height=860' };
};

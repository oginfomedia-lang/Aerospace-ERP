import React, { useEffect, useState } from 'react';
import procurementApi from '../api/procurementAxios';
import { OrganizationProfile } from '../types/procurement';
import { CertificateOfConformance } from '../types/qa';

interface Props {
  coc: CertificateOfConformance;
}

export const CertificateOfConformanceDocument: React.FC<Props> = ({ coc }) => {
  const [org, setOrg] = useState<OrganizationProfile | null>(null);

  useEffect(() => {
    procurementApi.get('/organization-profile').then(res => setOrg(res.data)).catch(() => {});
  }, []);

  return (
    <div className="text-black font-sans">
      <div className="text-center mb-2">
        <h1 className="text-xl font-bold">{org?.companyName}</h1>
        <p className="text-xs">{[org?.addressLine, org?.city].filter(Boolean).join(', ')}</p>
      </div>
      <hr className="border-t border-black" />
      <h2 className="text-center text-lg font-bold my-3">Certificate of Conformance</h2>

      <div className="grid grid-cols-2 gap-4 border-b border-black pb-2 mb-3 text-[11px]">
        <div>
          <p>Certificate No: <b>{coc.cocNumber}</b></p>
          <p>Part: <b>{coc.partName}</b> ({coc.skuCode})</p>
          <p>Batch: {coc.batchNumber}</p>
        </div>
        <div>
          <p>PO Reference: {coc.poNumber || '—'}</p>
          <p>Quantity Certified: <b>{coc.quantityCertified}</b></p>
          <p>Standard: {coc.certifyingStandard || '—'}</p>
        </div>
      </div>

      <p className="text-[11px] leading-relaxed mb-6">
        We hereby certify that the above-referenced material/parts were manufactured, inspected and
        tested in accordance with the applicable specifications and drawings, and conform to all
        requirements of the purchase order{coc.poNumber ? ` ${coc.poNumber}` : ''}
        {coc.certifyingStandard ? ` under ${coc.certifyingStandard}` : ''}.
      </p>

      <div className="flex justify-between text-[10px] mt-10">
        <div>
          <p>Issued By: {coc.issuedBy}</p>
          <p>Issued At: {new Date(coc.issuedAt).toLocaleString()}</p>
        </div>
        <div className="text-right">
          <p>Signature Hash:</p>
          <p className="font-mono break-all max-w-[200px]">{coc.signatureHash}</p>
        </div>
      </div>
    </div>
  );
};

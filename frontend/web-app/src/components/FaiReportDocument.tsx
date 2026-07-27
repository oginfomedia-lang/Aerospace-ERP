import React, { useEffect, useState } from 'react';
import procurementApi from '../api/procurementAxios';
import { OrganizationProfile } from '../types/procurement';
import { FaiRecord } from '../types/qa';

interface Props {
  record: FaiRecord;
}

export const FaiReportDocument: React.FC<Props> = ({ record }) => {
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
      <h2 className="text-center text-lg font-bold my-3">First Article Inspection Report</h2>

      <div className="grid grid-cols-2 gap-4 border-b border-black pb-2 mb-3 text-[11px]">
        <div>
          <p>FAI No: <b>{record.faiNumber}</b></p>
          <p>Part: <b>{record.partName}</b> ({record.skuCode})</p>
          <p>Revision: {record.partRevision || '—'}</p>
        </div>
        <div>
          <p>Batch: {record.batchNumber || '—'}</p>
          <p>Material Cert Ref: {record.materialCertRef || '—'}</p>
          <p>Overall Result: <b>{record.overallResult}</b></p>
        </div>
      </div>

      <table className="w-full text-[10px] border-collapse mb-3">
        <thead>
          <tr className="border-b border-black">
            <th className="text-left py-1">#</th>
            <th className="text-left py-1">Description</th>
            <th className="text-left py-1">Requirement</th>
            <th className="text-left py-1">Measured</th>
            <th className="text-left py-1">Result</th>
          </tr>
        </thead>
        <tbody>
          {record.characteristics.map((c, i) => (
            <tr key={i} className="border-b border-slate-300">
              <td className="py-1">{c.charNo}</td>
              <td className="py-1">{c.description}</td>
              <td className="py-1">{c.requirement}</td>
              <td className="py-1">{c.measuredResult}</td>
              <td className="py-1">{c.result}</td>
            </tr>
          ))}
        </tbody>
      </table>

      <p className="text-[11px] mb-4">Remarks: {record.remarks}</p>

      <div className="flex justify-between text-[10px] mt-8">
        <div>
          <p>Inspector: {record.inspectorName}</p>
          <p>Performed: {record.performedAt ? new Date(record.performedAt).toLocaleString() : '—'}</p>
        </div>
        <div className="text-right">
          <p>Signature Hash:</p>
          <p className="font-mono break-all max-w-[200px]">{record.signatureHash}</p>
        </div>
      </div>
    </div>
  );
};

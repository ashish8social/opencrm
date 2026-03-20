import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { reportApi } from '../api/reportApi';
import { metadataApi } from '../api/metadataApi';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  Cell, LineChart, Line,
} from 'recharts';

const COLORS = ['#3b82f6', '#6366f1', '#8b5cf6', '#a855f7', '#d946ef',
                '#ec4899', '#f43f5e', '#ef4444', '#22c55e', '#64748b'];

function formatCurrency(val: number) {
  if (val >= 1000000) return `$${(val / 1000000).toFixed(1)}M`;
  if (val >= 1000) return `$${(val / 1000).toFixed(0)}K`;
  return `$${val.toFixed(0)}`;
}

export function DashboardPage() {
  const { data: entities } = useQuery({
    queryKey: ['entities'],
    queryFn: metadataApi.listEntities,
  });

  const { data: counts } = useQuery({
    queryKey: ['record-counts'],
    queryFn: reportApi.recordCounts,
  });

  const { data: pipeline } = useQuery({
    queryKey: ['pipeline-summary'],
    queryFn: reportApi.pipelineSummary,
  });

  const { data: revenue } = useQuery({
    queryKey: ['revenue-by-month'],
    queryFn: reportApi.revenueByMonth,
  });

  const { data: topAccounts } = useQuery({
    queryKey: ['top-accounts'],
    queryFn: reportApi.topAccounts,
  });

  const keyEntities = ['Account', 'Contact', 'Opportunity', 'Quote', 'Order', 'Product'];

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Sales Dashboard</h1>

      {/* Record count cards */}
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4 mb-8">
        {keyEntities.map(apiName => {
          const entity = entities?.find(e => e.apiName === apiName);
          if (!entity) return null;
          return (
            <Link
              key={apiName}
              to={`/o/${apiName}`}
              className="bg-white rounded-lg shadow p-4 hover:shadow-md transition-shadow"
            >
              <p className="text-xs font-medium text-gray-500 uppercase">{entity.pluralLabel}</p>
              <p className="text-2xl font-bold text-gray-900 mt-1">
                {counts?.[apiName] ?? 0}
              </p>
            </Link>
          );
        })}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
        {/* Pipeline Funnel */}
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-base font-semibold text-gray-900 mb-4">Sales Pipeline</h2>
          {pipeline && pipeline.length > 0 ? (
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={pipeline} layout="vertical" margin={{ left: 80 }}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis type="number" tickFormatter={formatCurrency} />
                <YAxis type="category" dataKey="stage" width={100} tick={{ fontSize: 11 }} />
                <Tooltip formatter={(val: number) => formatCurrency(val)} />
                <Bar dataKey="totalAmount" name="Amount" radius={[0, 4, 4, 0]}>
                  {pipeline.map((_, i) => (
                    <Cell key={i} fill={COLORS[i % COLORS.length]} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          ) : (
            <p className="text-sm text-gray-500 py-8 text-center">
              No opportunity data yet. Create some Opportunities to see pipeline metrics.
            </p>
          )}
        </div>

        {/* Revenue by Month */}
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-base font-semibold text-gray-900 mb-4">Closed Won Revenue by Month</h2>
          {revenue && revenue.length > 0 ? (
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={revenue} margin={{ left: 20 }}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="month" tick={{ fontSize: 11 }} />
                <YAxis tickFormatter={formatCurrency} />
                <Tooltip formatter={(val: number) => formatCurrency(val)} />
                <Line type="monotone" dataKey="amount" stroke="#3b82f6" strokeWidth={2}
                       dot={{ fill: '#3b82f6', r: 4 }} name="Revenue" />
              </LineChart>
            </ResponsiveContainer>
          ) : (
            <p className="text-sm text-gray-500 py-8 text-center">
              No closed won deals yet. Close some Opportunities to see revenue trends.
            </p>
          )}
        </div>
      </div>

      {/* Top Accounts */}
      <div className="bg-white rounded-lg shadow p-6 mb-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-base font-semibold text-gray-900">Top Accounts by Deal Value</h2>
          <Link to="/o/Account" className="text-sm text-primary-600 hover:text-primary-800">
            View all &rarr;
          </Link>
        </div>
        {topAccounts && topAccounts.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead>
                <tr>
                  <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Account</th>
                  <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Total Deal Value</th>
                  <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Deals</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {topAccounts.map((acct, i) => (
                  <tr key={acct.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3">
                      <Link
                        to={`/o/Account/${acct.id}`}
                        className="text-sm text-primary-600 hover:text-primary-800 font-medium"
                      >
                        {acct.name}
                      </Link>
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-900 font-medium">
                      {formatCurrency(acct.totalAmount)}
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-600">{acct.dealCount}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <p className="text-sm text-gray-500 py-4 text-center">
            No account data yet.
          </p>
        )}
      </div>

      {/* Quick Links */}
      <div className="bg-white rounded-lg shadow p-6">
        <h2 className="text-base font-semibold text-gray-900 mb-3">Quick Actions</h2>
        <div className="flex flex-wrap gap-3">
          <Link to="/o/Account/new" className="px-4 py-2 bg-primary-50 text-primary-700 text-sm rounded-md hover:bg-primary-100">
            New Account
          </Link>
          <Link to="/o/Contact/new" className="px-4 py-2 bg-primary-50 text-primary-700 text-sm rounded-md hover:bg-primary-100">
            New Contact
          </Link>
          <Link to="/o/Opportunity/new" className="px-4 py-2 bg-primary-50 text-primary-700 text-sm rounded-md hover:bg-primary-100">
            New Opportunity
          </Link>
          <Link to="/o/Quote/new" className="px-4 py-2 bg-primary-50 text-primary-700 text-sm rounded-md hover:bg-primary-100">
            New Quote
          </Link>
          <Link to="/pipeline" className="px-4 py-2 bg-indigo-50 text-indigo-700 text-sm rounded-md hover:bg-indigo-100">
            Pipeline Board
          </Link>
          <Link to="/setup/entities" className="px-4 py-2 bg-gray-50 text-gray-700 text-sm rounded-md hover:bg-gray-100">
            Entity Builder
          </Link>
        </div>
      </div>
    </div>
  );
}

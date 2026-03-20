import { useParams, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { metadataApi } from '../api/metadataApi';
import { recordApi } from '../api/recordApi';
import { useState } from 'react';

export function ListView() {
  const { entityApiName } = useParams<{ entityApiName: string }>();
  const [page, setPage] = useState(0);

  const { data: entity } = useQuery({
    queryKey: ['entity', entityApiName],
    queryFn: () => metadataApi.getEntity(entityApiName!),
    enabled: !!entityApiName,
  });

  const { data: records, isLoading } = useQuery({
    queryKey: ['records', entityApiName, page],
    queryFn: () => recordApi.list(entityApiName!, { page, size: 25 }),
    enabled: !!entityApiName,
  });

  const displayFields = entity?.fields?.slice(0, 6) ?? [];

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900">
          {entity?.pluralLabel || entityApiName}
        </h1>
        <Link
          to={`/o/${entityApiName}/new`}
          className="px-4 py-2 bg-primary-600 text-white text-sm rounded-md hover:bg-primary-700"
        >
          New {entity?.label}
        </Link>
      </div>

      {isLoading ? (
        <div className="text-center py-12 text-gray-500">Loading...</div>
      ) : (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Name
                </th>
                {displayFields
                  .filter(f => f.apiName !== 'Name')
                  .slice(0, 5)
                  .map(field => (
                    <th key={field.apiName} className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      {field.label}
                    </th>
                  ))}
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {records?.content?.length === 0 && (
                <tr>
                  <td colSpan={6} className="px-6 py-12 text-center text-gray-500">
                    No records found. Create your first {entity?.label?.toLowerCase()}.
                  </td>
                </tr>
              )}
              {records?.content?.map(record => (
                <tr key={record.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4 whitespace-nowrap">
                    <Link
                      to={`/o/${entityApiName}/${record.id}`}
                      className="text-primary-600 hover:text-primary-800 font-medium"
                    >
                      {record.name || '(No Name)'}
                    </Link>
                  </td>
                  {displayFields
                    .filter(f => f.apiName !== 'Name')
                    .slice(0, 5)
                    .map(field => (
                      <td key={field.apiName} className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                        {String(record.data[field.apiName] ?? '')}
                      </td>
                    ))}
                </tr>
              ))}
            </tbody>
          </table>

          {records && records.totalPages > 1 && (
            <div className="px-6 py-3 bg-gray-50 flex items-center justify-between text-sm">
              <span className="text-gray-600">
                Page {records.page + 1} of {records.totalPages} ({records.totalElements} total)
              </span>
              <div className="flex gap-2">
                <button
                  onClick={() => setPage(p => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className="px-3 py-1 border rounded disabled:opacity-50"
                >
                  Previous
                </button>
                <button
                  onClick={() => setPage(p => p + 1)}
                  disabled={page >= records.totalPages - 1}
                  className="px-3 py-1 border rounded disabled:opacity-50"
                >
                  Next
                </button>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

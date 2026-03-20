import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { recordApi } from '../api/recordApi';
import { EntityDef } from '../types/metadata';

interface RelatedListProps {
  parentEntityApiName: string;
  parentRecordId: string;
  relatedEntity: EntityDef;
}

export function RelatedList({ parentEntityApiName, parentRecordId, relatedEntity }: RelatedListProps) {
  const { data: records, isLoading } = useQuery({
    queryKey: ['related', parentEntityApiName, parentRecordId, relatedEntity.apiName],
    queryFn: () => recordApi.related(parentEntityApiName, parentRecordId, relatedEntity.apiName),
  });

  if (isLoading) return null;
  if (!records || records.length === 0) return null;

  const displayFields = relatedEntity.fields?.filter(f =>
    f.apiName !== 'Name' && f.fieldType !== 'TEXTAREA' && f.fieldType !== 'RICH_TEXT'
  ).slice(0, 4) ?? [];

  return (
    <div className="bg-white rounded-lg shadow mt-6">
      <div className="px-6 py-4 border-b flex items-center justify-between">
        <h3 className="text-base font-semibold text-gray-900">
          {relatedEntity.pluralLabel} ({records.length})
        </h3>
        <Link
          to={`/o/${relatedEntity.apiName}/new`}
          className="text-sm text-primary-600 hover:text-primary-800"
        >
          New {relatedEntity.label}
        </Link>
      </div>
      <table className="min-w-full divide-y divide-gray-200">
        <thead className="bg-gray-50">
          <tr>
            <th className="px-6 py-2 text-left text-xs font-medium text-gray-500 uppercase">Name</th>
            {displayFields.map(f => (
              <th key={f.apiName} className="px-6 py-2 text-left text-xs font-medium text-gray-500 uppercase">
                {f.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-200">
          {records.map(record => (
            <tr key={record.id} className="hover:bg-gray-50">
              <td className="px-6 py-3 whitespace-nowrap">
                <Link
                  to={`/o/${relatedEntity.apiName}/${record.id}`}
                  className="text-primary-600 hover:text-primary-800 text-sm font-medium"
                >
                  {record.name || '(No Name)'}
                </Link>
              </td>
              {displayFields.map(f => (
                <td key={f.apiName} className="px-6 py-3 whitespace-nowrap text-sm text-gray-600">
                  {String(record.data[f.apiName] ?? '')}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

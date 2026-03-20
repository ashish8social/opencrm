import { useParams, Link, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { metadataApi } from '../api/metadataApi';
import { recordApi } from '../api/recordApi';
import { salesApi } from '../api/salesApi';
import { DynamicField } from './DynamicField';
import { RelatedList } from './RelatedList';
import { EntityDef } from '../types/metadata';

export function DetailView() {
  const { entityApiName, id } = useParams<{ entityApiName: string; id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const { data: entity } = useQuery({
    queryKey: ['entity', entityApiName],
    queryFn: () => metadataApi.getEntity(entityApiName!),
    enabled: !!entityApiName,
  });

  const { data: record, isLoading } = useQuery({
    queryKey: ['record', entityApiName, id],
    queryFn: () => recordApi.get(entityApiName!, id!),
    enabled: !!entityApiName && !!id,
  });

  // Fetch all entities to find related ones (those with lookup/master-detail pointing to this entity)
  const { data: allEntities } = useQuery({
    queryKey: ['entities-with-fields'],
    queryFn: async () => {
      const entities = await metadataApi.listEntities();
      const withFields = await Promise.all(
        entities.map(e => metadataApi.getEntity(e.apiName))
      );
      return withFields;
    },
  });

  const deleteMutation = useMutation({
    mutationFn: () => recordApi.delete(entityApiName!, id!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['records', entityApiName] });
      navigate(`/o/${entityApiName}`);
    },
  });

  const pdfMutation = useMutation({
    mutationFn: () => salesApi.generateQuotePdf(id!),
    onSuccess: (blob) => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `quote-${record?.name || id}.pdf`;
      a.click();
      window.URL.revokeObjectURL(url);
    },
  });

  const convertMutation = useMutation({
    mutationFn: () => salesApi.convertQuoteToOrder(id!),
    onSuccess: (order) => {
      queryClient.invalidateQueries({ queryKey: ['records'] });
      navigate(`/o/Order/${order.id}`);
    },
  });

  // Find related entities (those with lookup/master-detail fields pointing to this entity)
  const relatedEntities: EntityDef[] = [];
  if (allEntities && entity) {
    for (const e of allEntities) {
      if (e.apiName === entityApiName) continue;
      const hasRelation = e.fields?.some(f =>
        (f.fieldType === 'LOOKUP' || f.fieldType === 'MASTER_DETAIL') &&
        f.refEntityId === entity.id
      );
      if (hasRelation) relatedEntities.push(e);
    }
  }

  if (isLoading) {
    return <div className="text-center py-12 text-gray-500">Loading...</div>;
  }

  if (!record) {
    return <div className="text-center py-12 text-gray-500">Record not found</div>;
  }

  const isQuote = entityApiName === 'Quote';
  const quoteStatus = isQuote ? String(record.data['Status'] ?? '') : '';

  return (
    <div className="max-w-4xl">
      <div className="flex items-center justify-between mb-6">
        <div>
          <p className="text-sm text-gray-500">{entity?.label}</p>
          <h1 className="text-2xl font-bold text-gray-900">{record.name || '(No Name)'}</h1>
        </div>
        <div className="flex gap-2">
          {isQuote && (
            <>
              <button
                onClick={() => pdfMutation.mutate()}
                disabled={pdfMutation.isPending}
                className="px-4 py-2 border border-gray-300 text-gray-700 text-sm rounded-md hover:bg-gray-50 disabled:opacity-50"
              >
                {pdfMutation.isPending ? 'Generating...' : 'Download PDF'}
              </button>
              {quoteStatus === 'Approved' && (
                <button
                  onClick={() => {
                    if (window.confirm('Convert this Quote to an Order? The quote status will be changed to "Converted".')) {
                      convertMutation.mutate();
                    }
                  }}
                  disabled={convertMutation.isPending}
                  className="px-4 py-2 bg-green-600 text-white text-sm rounded-md hover:bg-green-700 disabled:opacity-50"
                >
                  {convertMutation.isPending ? 'Converting...' : 'Convert to Order'}
                </button>
              )}
            </>
          )}
          <Link
            to={`/o/${entityApiName}/${id}/edit`}
            className="px-4 py-2 bg-primary-600 text-white text-sm rounded-md hover:bg-primary-700"
          >
            Edit
          </Link>
          <button
            onClick={() => {
              if (window.confirm('Are you sure you want to delete this record?')) {
                deleteMutation.mutate();
              }
            }}
            className="px-4 py-2 border border-red-300 text-red-700 text-sm rounded-md hover:bg-red-50"
          >
            Delete
          </button>
        </div>
      </div>

      {convertMutation.isError && (
        <div className="mb-4 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded text-sm">
          {(convertMutation.error as Error).message || 'Conversion failed'}
        </div>
      )}

      <div className="bg-white rounded-lg shadow p-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Details</h2>
        <div className="grid grid-cols-2 gap-4">
          {entity?.fields?.map(field => (
            <div key={field.apiName} className={field.fieldType === 'TEXTAREA' || field.fieldType === 'RICH_TEXT' ? 'col-span-2' : ''}>
              <DynamicField
                field={field}
                value={record.data[field.apiName]}
                onChange={() => {}}
                readOnly
              />
            </div>
          ))}
        </div>

        <div className="mt-6 pt-4 border-t text-xs text-gray-400">
          Created: {new Date(record.createdAt).toLocaleString()} |
          Updated: {new Date(record.updatedAt).toLocaleString()}
        </div>
      </div>

      {relatedEntities.map(re => (
        <RelatedList
          key={re.apiName}
          parentEntityApiName={entityApiName!}
          parentRecordId={id!}
          relatedEntity={re}
        />
      ))}
    </div>
  );
}

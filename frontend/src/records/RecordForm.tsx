import { useState, FormEvent } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { metadataApi } from '../api/metadataApi';
import { recordApi } from '../api/recordApi';
import { DynamicField } from './DynamicField';

export function RecordForm() {
  const { entityApiName, id } = useParams<{ entityApiName: string; id?: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const isEdit = !!id;

  const { data: entity } = useQuery({
    queryKey: ['entity', entityApiName],
    queryFn: () => metadataApi.getEntity(entityApiName!),
    enabled: !!entityApiName,
  });

  const { data: existingRecord } = useQuery({
    queryKey: ['record', entityApiName, id],
    queryFn: () => recordApi.get(entityApiName!, id!),
    enabled: isEdit,
  });

  const [formData, setFormData] = useState<Record<string, unknown>>({});
  const [initialized, setInitialized] = useState(false);

  if (isEdit && existingRecord && !initialized) {
    setFormData(existingRecord.data);
    setInitialized(true);
  }

  const mutation = useMutation({
    mutationFn: (data: Record<string, unknown>) =>
      isEdit
        ? recordApi.update(entityApiName!, id!, data)
        : recordApi.create(entityApiName!, data),
    onSuccess: (result) => {
      queryClient.invalidateQueries({ queryKey: ['records', entityApiName] });
      navigate(`/o/${entityApiName}/${result.id}`);
    },
  });

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    mutation.mutate(formData);
  };

  const editableFields = entity?.fields?.filter(
    f => f.fieldType !== 'FORMULA' && f.fieldType !== 'AUTO_NUMBER'
  ) ?? [];

  return (
    <div className="max-w-3xl">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">
        {isEdit ? `Edit ${entity?.label}` : `New ${entity?.label}`}
      </h1>

      <form onSubmit={handleSubmit} className="bg-white rounded-lg shadow p-6 space-y-4">
        {mutation.isError && (
          <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded text-sm">
            {(mutation.error as Error).message || 'An error occurred'}
          </div>
        )}

        <div className="grid grid-cols-2 gap-4">
          {editableFields.map(field => (
            <div key={field.apiName} className={field.fieldType === 'TEXTAREA' || field.fieldType === 'RICH_TEXT' ? 'col-span-2' : ''}>
              <DynamicField
                field={field}
                value={formData[field.apiName]}
                onChange={val => setFormData(prev => ({ ...prev, [field.apiName]: val }))}
              />
            </div>
          ))}
        </div>

        <div className="flex gap-3 pt-4 border-t">
          <button
            type="submit"
            disabled={mutation.isPending}
            className="px-4 py-2 bg-primary-600 text-white text-sm rounded-md hover:bg-primary-700 disabled:opacity-50"
          >
            {mutation.isPending ? 'Saving...' : 'Save'}
          </button>
          <button
            type="button"
            onClick={() => navigate(-1)}
            className="px-4 py-2 border border-gray-300 text-gray-700 text-sm rounded-md hover:bg-gray-50"
          >
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
}

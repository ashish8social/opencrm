import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { metadataApi } from '../api/metadataApi';
import { FieldType } from '../types/metadata';

const FIELD_TYPES: FieldType[] = [
  'TEXT', 'NUMBER', 'DECIMAL', 'DATE', 'DATETIME',
  'BOOLEAN', 'PICKLIST', 'MULTI_PICKLIST',
  'EMAIL', 'PHONE', 'URL', 'CURRENCY',
  'TEXTAREA', 'LOOKUP', 'MASTER_DETAIL',
];

export function EntityBuilderPage() {
  const queryClient = useQueryClient();
  const { data: entities, isLoading } = useQuery({
    queryKey: ['entities'],
    queryFn: metadataApi.listEntities,
  });

  const [showForm, setShowForm] = useState(false);
  const [entityForm, setEntityForm] = useState({
    apiName: '',
    label: '',
    pluralLabel: '',
    description: '',
  });

  const createMutation = useMutation({
    mutationFn: metadataApi.createEntity,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['entities'] });
      setShowForm(false);
      setEntityForm({ apiName: '', label: '', pluralLabel: '', description: '' });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: metadataApi.deleteEntity,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['entities'] }),
  });

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Entity Builder</h1>
        <button
          onClick={() => setShowForm(!showForm)}
          className="px-4 py-2 bg-primary-600 text-white text-sm rounded-md hover:bg-primary-700"
        >
          {showForm ? 'Cancel' : 'New Entity'}
        </button>
      </div>

      {showForm && (
        <div className="bg-white rounded-lg shadow p-6 mb-6">
          <h2 className="text-lg font-semibold mb-4">Create Custom Entity</h2>
          <form
            onSubmit={e => {
              e.preventDefault();
              createMutation.mutate(entityForm);
            }}
            className="grid grid-cols-2 gap-4"
          >
            <div>
              <label className="block text-sm font-medium text-gray-700">API Name</label>
              <input
                type="text"
                required
                value={entityForm.apiName}
                onChange={e => setEntityForm(f => ({ ...f, apiName: e.target.value }))}
                placeholder="e.g. Support_Case__c"
                className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700">Label</label>
              <input
                type="text"
                required
                value={entityForm.label}
                onChange={e => setEntityForm(f => ({ ...f, label: e.target.value }))}
                className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700">Plural Label</label>
              <input
                type="text"
                required
                value={entityForm.pluralLabel}
                onChange={e => setEntityForm(f => ({ ...f, pluralLabel: e.target.value }))}
                className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700">Description</label>
              <input
                type="text"
                value={entityForm.description}
                onChange={e => setEntityForm(f => ({ ...f, description: e.target.value }))}
                className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
              />
            </div>
            <div className="col-span-2">
              <button
                type="submit"
                disabled={createMutation.isPending}
                className="px-4 py-2 bg-primary-600 text-white text-sm rounded-md hover:bg-primary-700 disabled:opacity-50"
              >
                {createMutation.isPending ? 'Creating...' : 'Create Entity'}
              </button>
            </div>
          </form>
        </div>
      )}

      {isLoading ? (
        <div className="text-center py-12 text-gray-500">Loading...</div>
      ) : (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">API Name</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Label</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Type</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {entities?.map(entity => (
                <tr key={entity.apiName} className="hover:bg-gray-50">
                  <td className="px-6 py-4 text-sm font-medium">
                    <Link to={`/setup/entities/${entity.apiName}`} className="text-primary-600 hover:text-primary-800">
                      {entity.apiName}
                    </Link>
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-600">{entity.label}</td>
                  <td className="px-6 py-4 text-sm">
                    <span className={`px-2 py-1 rounded-full text-xs ${
                      entity.isCustom ? 'bg-green-100 text-green-800' : 'bg-blue-100 text-blue-800'
                    }`}>
                      {entity.isCustom ? 'Custom' : 'Standard'}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-sm">
                    {entity.isCustom && (
                      <button
                        onClick={() => {
                          if (window.confirm(`Delete ${entity.label}?`)) {
                            deleteMutation.mutate(entity.apiName);
                          }
                        }}
                        className="text-red-600 hover:text-red-800"
                      >
                        Delete
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

import { useState, useRef, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { recordApi } from '../api/recordApi';
import { metadataApi } from '../api/metadataApi';
import { FieldDef } from '../types/metadata';

interface LookupFieldProps {
  field: FieldDef;
  value: unknown;
  onChange: (value: unknown) => void;
  readOnly?: boolean;
}

export function LookupField({ field, value, onChange, readOnly }: LookupFieldProps) {
  const [search, setSearch] = useState('');
  const [isOpen, setIsOpen] = useState(false);
  const [selectedName, setSelectedName] = useState<string | null>(null);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // Resolve the target entity's apiName from refEntityId
  const { data: entities } = useQuery({
    queryKey: ['entities'],
    queryFn: metadataApi.listEntities,
  });

  const targetEntity = entities?.find(e => e.id === field.refEntityId);
  const targetApiName = targetEntity?.apiName;

  // Resolve the current value's name
  const { data: currentRecord } = useQuery({
    queryKey: ['record-lookup', value],
    queryFn: async () => {
      if (!value || !targetApiName) return null;
      try {
        return await recordApi.get(targetApiName, String(value));
      } catch {
        return null;
      }
    },
    enabled: !!value && !!targetApiName,
  });

  useEffect(() => {
    if (currentRecord) {
      setSelectedName(currentRecord.name);
    }
  }, [currentRecord]);

  // Search results
  const { data: searchResults } = useQuery({
    queryKey: ['lookup-search', targetApiName, search],
    queryFn: () => recordApi.list(targetApiName!, { q: search, size: 10 }),
    enabled: !!targetApiName && search.length > 0 && isOpen,
  });

  // Close dropdown on outside click
  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  if (readOnly) {
    return (
      <div>
        <label className="block text-sm font-medium text-gray-700">{field.label}</label>
        <p className="mt-1 text-sm text-gray-900 py-2">
          {selectedName || currentRecord?.name || String(value ?? '-')}
        </p>
      </div>
    );
  }

  return (
    <div ref={dropdownRef} className="relative">
      <label className="block text-sm font-medium text-gray-700">
        {field.label}
        {field.required && <span className="text-red-500 ml-1">*</span>}
      </label>
      <div className="relative mt-1">
        <input
          type="text"
          value={isOpen ? search : (selectedName || '')}
          onChange={e => {
            setSearch(e.target.value);
            if (!isOpen) setIsOpen(true);
          }}
          onFocus={() => {
            setIsOpen(true);
            setSearch('');
          }}
          placeholder={`Search ${targetEntity?.pluralLabel || field.label}...`}
          className="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500 text-sm"
        />
        {!!value && (
          <button
            type="button"
            onClick={() => {
              onChange(null);
              setSelectedName(null);
              setSearch('');
            }}
            className="absolute right-2 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 text-sm"
          >
            &times;
          </button>
        )}
      </div>
      {isOpen && (
        <div className="absolute z-10 mt-1 w-full bg-white border border-gray-200 rounded-md shadow-lg max-h-48 overflow-y-auto">
          {searchResults?.content?.length === 0 && search.length > 0 && (
            <div className="px-3 py-2 text-sm text-gray-500">No results found</div>
          )}
          {search.length === 0 && (
            <div className="px-3 py-2 text-sm text-gray-400">Type to search...</div>
          )}
          {searchResults?.content?.map(record => (
            <button
              key={record.id}
              type="button"
              onClick={() => {
                onChange(record.id);
                setSelectedName(record.name);
                setIsOpen(false);
                setSearch('');
              }}
              className="w-full text-left px-3 py-2 text-sm hover:bg-primary-50 hover:text-primary-700 border-b border-gray-100 last:border-0"
            >
              {record.name || '(No Name)'}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

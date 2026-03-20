import { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '../auth/AuthContext';
import { recordApi } from '../api/recordApi';
import { metadataApi } from '../api/metadataApi';

export function TopBar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [search, setSearch] = useState('');
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const { data: entities } = useQuery({
    queryKey: ['entities'],
    queryFn: metadataApi.listEntities,
  });

  const entityMap = new Map(entities?.map(e => [e.id, e]) ?? []);

  const { data: results } = useQuery({
    queryKey: ['global-search', search],
    queryFn: () => recordApi.search(search),
    enabled: search.length >= 2,
  });

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const handleSelect = (entityDefId: string, recordId: string) => {
    const entity = entityMap.get(entityDefId);
    if (entity) {
      navigate(`/o/${entity.apiName}/${recordId}`);
      setSearch('');
      setIsOpen(false);
    }
  };

  return (
    <header className="h-14 bg-white border-b border-gray-200 flex items-center justify-between px-6">
      <div ref={dropdownRef} className="relative flex items-center gap-4">
        <input
          type="search"
          placeholder="Search across all records..."
          value={search}
          onChange={e => {
            setSearch(e.target.value);
            if (e.target.value.length >= 2) setIsOpen(true);
          }}
          onFocus={() => {
            if (search.length >= 2) setIsOpen(true);
          }}
          className="w-80 px-3 py-1.5 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-1 focus:ring-primary-500"
        />
        {isOpen && results && results.length > 0 && (
          <div className="absolute top-full left-0 mt-1 w-96 bg-white border border-gray-200 rounded-md shadow-lg max-h-80 overflow-y-auto z-50">
            {results.map(record => {
              const entity = entityMap.get(record.entityDefId);
              return (
                <button
                  key={record.id}
                  onClick={() => handleSelect(record.entityDefId, record.id)}
                  className="w-full text-left px-4 py-3 hover:bg-gray-50 border-b border-gray-100 last:border-0"
                >
                  <div className="text-sm font-medium text-gray-900">
                    {record.name || '(No Name)'}
                  </div>
                  <div className="text-xs text-gray-500 mt-0.5">
                    {entity?.label || 'Unknown Entity'}
                  </div>
                </button>
              );
            })}
          </div>
        )}
        {isOpen && results && results.length === 0 && search.length >= 2 && (
          <div className="absolute top-full left-0 mt-1 w-96 bg-white border border-gray-200 rounded-md shadow-lg z-50">
            <div className="px-4 py-3 text-sm text-gray-500">No results found</div>
          </div>
        )}
      </div>
      <div className="flex items-center gap-4">
        <span className="text-sm text-gray-600">{user?.fullName || user?.username}</span>
        <button
          onClick={logout}
          className="text-sm text-gray-500 hover:text-gray-700"
        >
          Sign out
        </button>
      </div>
    </header>
  );
}

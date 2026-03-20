import { Link, useLocation } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { metadataApi } from '../api/metadataApi';

const iconMap: Record<string, string> = {
  Account: 'building-2',
  Contact: 'users',
  Opportunity: 'trophy',
  Product: 'package',
  Quote: 'file-text',
  Order: 'shopping-cart',
};

export function Sidebar() {
  const location = useLocation();
  const { data: entities } = useQuery({
    queryKey: ['entities'],
    queryFn: metadataApi.listEntities,
  });

  const salesEntities = entities?.filter(e => !e.isCustom) ?? [];
  const customEntities = entities?.filter(e => e.isCustom) ?? [];

  return (
    <aside className="w-60 bg-gray-900 text-white flex flex-col h-full">
      <div className="p-4 border-b border-gray-700">
        <Link to="/" className="text-xl font-bold text-white hover:text-primary-300">
          OpenCRM
        </Link>
      </div>

      <nav className="flex-1 overflow-y-auto py-4">
        <div className="px-4 mb-2 space-y-1">
          <Link
            to="/"
            className={`block px-3 py-2 rounded text-sm ${
              location.pathname === '/' ? 'bg-primary-600' : 'hover:bg-gray-800'
            }`}
          >
            Dashboard
          </Link>
          <Link
            to="/pipeline"
            className={`block px-3 py-2 rounded text-sm ${
              location.pathname === '/pipeline' ? 'bg-primary-600' : 'hover:bg-gray-800'
            }`}
          >
            Pipeline Board
          </Link>
        </div>

        {salesEntities.length > 0 && (
          <div className="px-4 mt-4">
            <h3 className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-2">
              Sales
            </h3>
            {salesEntities.map(entity => (
              <Link
                key={entity.apiName}
                to={`/o/${entity.apiName}`}
                className={`block px-3 py-2 rounded text-sm ${
                  location.pathname.startsWith(`/o/${entity.apiName}`)
                    ? 'bg-primary-600'
                    : 'hover:bg-gray-800'
                }`}
              >
                {entity.pluralLabel}
              </Link>
            ))}
          </div>
        )}

        {customEntities.length > 0 && (
          <div className="px-4 mt-4">
            <h3 className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-2">
              Custom
            </h3>
            {customEntities.map(entity => (
              <Link
                key={entity.apiName}
                to={`/o/${entity.apiName}`}
                className={`block px-3 py-2 rounded text-sm ${
                  location.pathname.startsWith(`/o/${entity.apiName}`)
                    ? 'bg-primary-600'
                    : 'hover:bg-gray-800'
                }`}
              >
                {entity.pluralLabel}
              </Link>
            ))}
          </div>
        )}

        <div className="px-4 mt-4">
          <h3 className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-2">
            Setup
          </h3>
          <Link
            to="/setup/entities"
            className={`block px-3 py-2 rounded text-sm ${
              location.pathname.startsWith('/setup') ? 'bg-primary-600' : 'hover:bg-gray-800'
            }`}
          >
            Entity Builder
          </Link>
        </div>
      </nav>
    </aside>
  );
}

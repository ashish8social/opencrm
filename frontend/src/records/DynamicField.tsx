import { FieldDef } from '../types/metadata';
import { LookupField } from './LookupField';

interface DynamicFieldProps {
  field: FieldDef;
  value: unknown;
  onChange: (value: unknown) => void;
  readOnly?: boolean;
}

export function DynamicField({ field, value, onChange, readOnly }: DynamicFieldProps) {
  const baseClass = "mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500 text-sm";

  if (readOnly || field.fieldType === 'FORMULA' || field.fieldType === 'AUTO_NUMBER') {
    if ((field.fieldType === 'LOOKUP' || field.fieldType === 'MASTER_DETAIL') && readOnly) {
      return <LookupField field={field} value={value} onChange={() => {}} readOnly />;
    }
    return (
      <div>
        <label className="block text-sm font-medium text-gray-700">{field.label}</label>
        <p className="mt-1 text-sm text-gray-900 py-2">{String(value ?? '-')}</p>
      </div>
    );
  }

  const renderInput = () => {
    switch (field.fieldType) {
      case 'TEXT':
      case 'EMAIL':
      case 'PHONE':
      case 'URL':
        return (
          <input
            type={field.fieldType === 'EMAIL' ? 'email' : field.fieldType === 'URL' ? 'url' : 'text'}
            value={String(value ?? '')}
            onChange={e => onChange(e.target.value)}
            required={field.required}
            maxLength={field.maxLength ?? undefined}
            className={baseClass}
          />
        );

      case 'NUMBER':
      case 'DECIMAL':
      case 'CURRENCY':
        return (
          <input
            type="number"
            value={String(value ?? '')}
            onChange={e => onChange(e.target.value ? Number(e.target.value) : null)}
            required={field.required}
            step={field.fieldType === 'NUMBER' ? '1' : '0.01'}
            className={baseClass}
          />
        );

      case 'DATE':
        return (
          <input
            type="date"
            value={String(value ?? '')}
            onChange={e => onChange(e.target.value)}
            required={field.required}
            className={baseClass}
          />
        );

      case 'DATETIME':
        return (
          <input
            type="datetime-local"
            value={String(value ?? '')}
            onChange={e => onChange(e.target.value)}
            required={field.required}
            className={baseClass}
          />
        );

      case 'BOOLEAN':
        return (
          <label className="flex items-center gap-2 mt-2">
            <input
              type="checkbox"
              checked={Boolean(value)}
              onChange={e => onChange(e.target.checked)}
              className="rounded border-gray-300 text-primary-600 focus:ring-primary-500"
            />
            <span className="text-sm text-gray-700">{field.label}</span>
          </label>
        );

      case 'TEXTAREA':
      case 'RICH_TEXT':
        return (
          <textarea
            value={String(value ?? '')}
            onChange={e => onChange(e.target.value)}
            required={field.required}
            rows={4}
            className={baseClass}
          />
        );

      case 'PICKLIST': {
        const options = parsePicklist(field.picklistValues);
        return (
          <select
            value={String(value ?? '')}
            onChange={e => onChange(e.target.value)}
            required={field.required}
            className={baseClass}
          >
            <option value="">-- Select --</option>
            {options.map(opt => (
              <option key={opt.value} value={opt.value}>{opt.label}</option>
            ))}
          </select>
        );
      }

      case 'MULTI_PICKLIST': {
        const options = parsePicklist(field.picklistValues);
        const selected = Array.isArray(value) ? value : [];
        return (
          <select
            multiple
            value={selected.map(String)}
            onChange={e => {
              const vals = Array.from(e.target.selectedOptions, o => o.value);
              onChange(vals);
            }}
            className={baseClass + ' h-32'}
          >
            {options.map(opt => (
              <option key={opt.value} value={opt.value}>{opt.label}</option>
            ))}
          </select>
        );
      }

      case 'LOOKUP':
      case 'MASTER_DETAIL':
        return <LookupField field={field} value={value} onChange={onChange} />;

      default:
        return (
          <input
            type="text"
            value={String(value ?? '')}
            onChange={e => onChange(e.target.value)}
            className={baseClass}
          />
        );
    }
  };

  return (
    <div>
      {field.fieldType !== 'BOOLEAN' && (
        <label className="block text-sm font-medium text-gray-700">
          {field.label}
          {field.required && <span className="text-red-500 ml-1">*</span>}
        </label>
      )}
      {renderInput()}
    </div>
  );
}

function parsePicklist(json?: string): Array<{ value: string; label: string }> {
  if (!json) return [];
  try {
    return JSON.parse(json);
  } catch {
    return [];
  }
}

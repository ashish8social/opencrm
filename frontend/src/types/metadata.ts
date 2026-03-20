export interface EntityDef {
  id: string;
  apiName: string;
  label: string;
  pluralLabel: string;
  description?: string;
  isCustom: boolean;
  icon: string;
  nameField?: string;
  fields?: FieldDef[];
}

export interface FieldDef {
  id: string;
  apiName: string;
  label: string;
  fieldType: FieldType;
  required: boolean;
  uniqueField?: boolean;
  sortOrder: number;
  defaultValue?: string;
  refEntityId?: string;
  relationType?: 'LOOKUP' | 'MASTER_DETAIL';
  picklistValues?: string;
  formula?: string;
  autoNumberFmt?: string;
  maxLength?: number;
}

export type FieldType =
  | 'TEXT' | 'NUMBER' | 'DECIMAL' | 'DATE' | 'DATETIME'
  | 'BOOLEAN' | 'PICKLIST' | 'MULTI_PICKLIST'
  | 'EMAIL' | 'PHONE' | 'URL' | 'CURRENCY'
  | 'TEXTAREA' | 'RICH_TEXT'
  | 'FORMULA' | 'AUTO_NUMBER'
  | 'LOOKUP' | 'MASTER_DETAIL';

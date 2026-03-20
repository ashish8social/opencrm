-- =============================================
-- SALES CLOUD: Seed Entity Definitions
-- =============================================

-- Account
INSERT INTO entity_defs (id, api_name, label, plural_label, description, is_custom, icon, name_field)
VALUES ('a0000000-0000-0000-0000-000000000001', 'Account', 'Account', 'Accounts', 'Companies and organizations', false, 'building-2', 'Name');

-- Contact
INSERT INTO entity_defs (id, api_name, label, plural_label, description, is_custom, icon, name_field)
VALUES ('a0000000-0000-0000-0000-000000000002', 'Contact', 'Contact', 'Contacts', 'People associated with accounts', false, 'users', 'Name');

-- Opportunity
INSERT INTO entity_defs (id, api_name, label, plural_label, description, is_custom, icon, name_field)
VALUES ('a0000000-0000-0000-0000-000000000003', 'Opportunity', 'Opportunity', 'Opportunities', 'Sales deals and pipeline', false, 'trophy', 'Name');

-- Product
INSERT INTO entity_defs (id, api_name, label, plural_label, description, is_custom, icon, name_field)
VALUES ('a0000000-0000-0000-0000-000000000004', 'Product', 'Product', 'Products', 'Product catalog', false, 'package', 'Name');

-- Price Book
INSERT INTO entity_defs (id, api_name, label, plural_label, description, is_custom, icon, name_field)
VALUES ('a0000000-0000-0000-0000-000000000005', 'PriceBook', 'Price Book', 'Price Books', 'Price lists for products', false, 'book-open', 'Name');

-- Price Book Entry
INSERT INTO entity_defs (id, api_name, label, plural_label, description, is_custom, icon, name_field)
VALUES ('a0000000-0000-0000-0000-000000000006', 'PriceBookEntry', 'Price Book Entry', 'Price Book Entries', 'Product pricing in price books', false, 'tag', 'Name');

-- Opportunity Line Item
INSERT INTO entity_defs (id, api_name, label, plural_label, description, is_custom, icon, name_field)
VALUES ('a0000000-0000-0000-0000-000000000007', 'OpportunityLineItem', 'Opportunity Product', 'Opportunity Products', 'Products on opportunities', false, 'list', 'Name');

-- Quote
INSERT INTO entity_defs (id, api_name, label, plural_label, description, is_custom, icon, name_field)
VALUES ('a0000000-0000-0000-0000-000000000008', 'Quote', 'Quote', 'Quotes', 'Sales quotes and proposals', false, 'file-text', 'Name');

-- Quote Line Item
INSERT INTO entity_defs (id, api_name, label, plural_label, description, is_custom, icon, name_field)
VALUES ('a0000000-0000-0000-0000-000000000009', 'QuoteLineItem', 'Quote Line Item', 'Quote Line Items', 'Line items on quotes', false, 'list', 'Name');

-- Order
INSERT INTO entity_defs (id, api_name, label, plural_label, description, is_custom, icon, name_field)
VALUES ('a0000000-0000-0000-0000-00000000000a', 'Order', 'Order', 'Orders', 'Sales orders', false, 'shopping-cart', 'Name');

-- Order Item
INSERT INTO entity_defs (id, api_name, label, plural_label, description, is_custom, icon, name_field)
VALUES ('a0000000-0000-0000-0000-00000000000b', 'OrderItem', 'Order Item', 'Order Items', 'Line items on orders', false, 'list', 'Name');


-- =============================================
-- ACCOUNT FIELDS
-- =============================================
INSERT INTO field_defs (entity_def_id, api_name, label, field_type, required, sort_order) VALUES
('a0000000-0000-0000-0000-000000000001', 'Name', 'Account Name', 'TEXT', true, 1);
INSERT INTO field_defs (entity_def_id, api_name, label, field_type, required, sort_order, picklist_values) VALUES
('a0000000-0000-0000-0000-000000000001', 'Industry', 'Industry', 'PICKLIST', false, 2,
 '[{"value":"Technology","label":"Technology"},{"value":"Finance","label":"Finance"},{"value":"Healthcare","label":"Healthcare"},{"value":"Manufacturing","label":"Manufacturing"},{"value":"Retail","label":"Retail"},{"value":"Education","label":"Education"},{"value":"Other","label":"Other"}]');
INSERT INTO field_defs (entity_def_id, api_name, label, field_type, required, sort_order, picklist_values) VALUES
('a0000000-0000-0000-0000-000000000001', 'Type', 'Type', 'PICKLIST', false, 3,
 '[{"value":"Prospect","label":"Prospect"},{"value":"Customer","label":"Customer"},{"value":"Partner","label":"Partner"},{"value":"Vendor","label":"Vendor"}]');
INSERT INTO field_defs (entity_def_id, api_name, label, field_type, required, sort_order) VALUES
('a0000000-0000-0000-0000-000000000001', 'Phone', 'Phone', 'PHONE', false, 4),
('a0000000-0000-0000-0000-000000000001', 'Website', 'Website', 'URL', false, 5),
('a0000000-0000-0000-0000-000000000001', 'AnnualRevenue', 'Annual Revenue', 'CURRENCY', false, 6),
('a0000000-0000-0000-0000-000000000001', 'Employees', 'Employees', 'NUMBER', false, 7),
('a0000000-0000-0000-0000-000000000001', 'BillingAddress', 'Billing Address', 'TEXTAREA', false, 8),
('a0000000-0000-0000-0000-000000000001', 'Description', 'Description', 'TEXTAREA', false, 9);


-- =============================================
-- CONTACT FIELDS
-- =============================================
INSERT INTO field_defs (entity_def_id, api_name, label, field_type, required, sort_order) VALUES
('a0000000-0000-0000-0000-000000000002', 'Name', 'Full Name', 'TEXT', true, 1),
('a0000000-0000-0000-0000-000000000002', 'FirstName', 'First Name', 'TEXT', false, 2),
('a0000000-0000-0000-0000-000000000002', 'LastName', 'Last Name', 'TEXT', true, 3),
('a0000000-0000-0000-0000-000000000002', 'Email', 'Email', 'EMAIL', false, 4),
('a0000000-0000-0000-0000-000000000002', 'Phone', 'Phone', 'PHONE', false, 5),
('a0000000-0000-0000-0000-000000000002', 'Title', 'Title', 'TEXT', false, 6),
('a0000000-0000-0000-0000-000000000002', 'Department', 'Department', 'TEXT', false, 7);
INSERT INTO field_defs (entity_def_id, api_name, label, field_type, required, sort_order, ref_entity_id, relation_type) VALUES
('a0000000-0000-0000-0000-000000000002', 'Account', 'Account', 'LOOKUP', false, 8, 'a0000000-0000-0000-0000-000000000001', 'LOOKUP');
INSERT INTO field_defs (entity_def_id, api_name, label, field_type, required, sort_order) VALUES
('a0000000-0000-0000-0000-000000000002', 'MailingAddress', 'Mailing Address', 'TEXTAREA', false, 9);


-- =============================================
-- OPPORTUNITY FIELDS
-- =============================================
INSERT INTO field_defs (entity_def_id, api_name, label, field_type, required, sort_order) VALUES
('a0000000-0000-0000-0000-000000000003', 'Name', 'Opportunity Name', 'TEXT', true, 1);
INSERT INTO field_defs (entity_def_id, api_name, label, field_type, required, sort_order, ref_entity_id, relation_type) VALUES
('a0000000-0000-0000-0000-000000000003', 'Account', 'Account', 'LOOKUP', false, 2, 'a0000000-0000-0000-0000-000000000001', 'LOOKUP');
INSERT INTO field_defs (entity_def_id, api_name, label, field_type, required, sort_order) VALUES
('a0000000-0000-0000-0000-000000000003', 'Amount', 'Amount', 'CURRENCY', false, 3),
('a0000000-0000-0000-0000-000000000003', 'CloseDate', 'Close Date', 'DATE', true, 4);
INSERT INTO field_defs (entity_def_id, api_name, label, field_type, required, sort_order, picklist_values) VALUES
('a0000000-0000-0000-0000-000000000003', 'Stage', 'Stage', 'PICKLIST', true, 5,
 '[{"value":"Prospecting","label":"Prospecting"},{"value":"Qualification","label":"Qualification"},{"value":"Needs Analysis","label":"Needs Analysis"},{"value":"Value Proposition","label":"Value Proposition"},{"value":"Proposal","label":"Proposal"},{"value":"Negotiation","label":"Negotiation"},{"value":"Closed Won","label":"Closed Won"},{"value":"Closed Lost","label":"Closed Lost"}]');
INSERT INTO field_defs (entity_def_id, api_name, label, field_type, required, sort_order) VALUES
('a0000000-0000-0000-0000-000000000003', 'Probability', 'Probability (%)', 'NUMBER', false, 6);
INSERT INTO field_defs (entity_def_id, api_name, label, field_type, required, sort_order, picklist_values) VALUES
('a0000000-0000-0000-0000-000000000003', 'Type', 'Type', 'PICKLIST', false, 7,
 '[{"value":"New Business","label":"New Business"},{"value":"Existing Business","label":"Existing Business"},{"value":"Renewal","label":"Renewal"}]'),
('a0000000-0000-0000-0000-000000000003', 'LeadSource', 'Lead Source', 'PICKLIST', false, 8,
 '[{"value":"Web","label":"Web"},{"value":"Phone","label":"Phone"},{"value":"Referral","label":"Referral"},{"value":"Partner","label":"Partner"},{"value":"Trade Show","label":"Trade Show"},{"value":"Other","label":"Other"}]');
INSERT INTO field_defs (entity_def_id, api_name, label, field_type, required, sort_order) VALUES
('a0000000-0000-0000-0000-000000000003', 'Description', 'Description', 'TEXTAREA', false, 9);


-- =============================================
-- PRODUCT FIELDS
-- =============================================
INSERT INTO field_defs (entity_def_id, api_name, label, field_type, required, sort_order) VALUES
('a0000000-0000-0000-0000-000000000004', 'Name', 'Product Name', 'TEXT', true, 1),
('a0000000-0000-0000-0000-000000000004', 'ProductCode', 'Product Code', 'TEXT', false, 2),
('a0000000-0000-0000-0000-000000000004', 'Description', 'Description', 'TEXTAREA', false, 3),
('a0000000-0000-0000-0000-000000000004', 'Active', 'Active', 'BOOLEAN', false, 4);
INSERT INTO field_defs (entity_def_id, api_name, label, field_type, required, sort_order, picklist_values) VALUES
('a0000000-0000-0000-0000-000000000004', 'Family', 'Product Family', 'PICKLIST', false, 5,
 '[{"value":"Software","label":"Software"},{"value":"Hardware","label":"Hardware"},{"value":"Services","label":"Services"},{"value":"Support","label":"Support"}]');


-- =============================================
-- PRICE BOOK FIELDS
-- =============================================
INSERT INTO field_defs (entity_def_id, api_name, label, field_type, required, sort_order) VALUES
('a0000000-0000-0000-0000-000000000005', 'Name', 'Price Book Name', 'TEXT', true, 1),
('a0000000-0000-0000-0000-000000000005', 'Active', 'Active', 'BOOLEAN', false, 2),
('a0000000-0000-0000-0000-000000000005', 'IsStandard', 'Is Standard', 'BOOLEAN', false, 3),
('a0000000-0000-0000-0000-000000000005', 'Description', 'Description', 'TEXTAREA', false, 4);


-- =============================================
-- PRICE BOOK ENTRY FIELDS
-- =============================================
INSERT INTO field_defs (entity_def_id, api_name, label, field_type, required, sort_order) VALUES
('a0000000-0000-0000-0000-000000000006', 'Name', 'Entry Name', 'TEXT', false, 1);
INSERT INTO field_defs (entity_def_id, api_name, label, field_type, required, sort_order, ref_entity_id, relation_type) VALUES
('a0000000-0000-0000-0000-000000000006', 'Product', 'Product', 'LOOKUP', true, 2, 'a0000000-0000-0000-0000-000000000004', 'LOOKUP'),
('a0000000-0000-0000-0000-000000000006', 'PriceBook', 'Price Book', 'LOOKUP', true, 3, 'a0000000-0000-0000-0000-000000000005', 'LOOKUP');
INSERT INTO field_defs (entity_def_id, api_name, label, field_type, required, sort_order) VALUES
('a0000000-0000-0000-0000-000000000006', 'UnitPrice', 'Unit Price', 'CURRENCY', true, 4),
('a0000000-0000-0000-0000-000000000006', 'Active', 'Active', 'BOOLEAN', false, 5);


-- =============================================
-- OPPORTUNITY LINE ITEM FIELDS
-- =============================================
INSERT INTO field_defs (entity_def_id, api_name, label, field_type, required, sort_order) VALUES
('a0000000-0000-0000-0000-000000000007', 'Name', 'Line Item Name', 'TEXT', false, 1);
INSERT INTO field_defs (entity_def_id, api_name, label, field_type, required, sort_order, ref_entity_id, relation_type, cascade_delete) VALUES
('a0000000-0000-0000-0000-000000000007', 'Opportunity', 'Opportunity', 'MASTER_DETAIL', true, 2, 'a0000000-0000-0000-0000-000000000003', 'MASTER_DETAIL', true);
INSERT INTO field_defs (entity_def_id, api_name, label, field_type, required, sort_order, ref_entity_id, relation_type) VALUES
('a0000000-0000-0000-0000-000000000007', 'Product', 'Product', 'LOOKUP', true, 3, 'a0000000-0000-0000-0000-000000000004', 'LOOKUP');
INSERT INTO field_defs (entity_def_id, api_name, label, field_type, required, sort_order) VALUES
('a0000000-0000-0000-0000-000000000007', 'Quantity', 'Quantity', 'NUMBER', true, 4),
('a0000000-0000-0000-0000-000000000007', 'UnitPrice', 'Unit Price', 'CURRENCY', true, 5),
('a0000000-0000-0000-0000-000000000007', 'TotalPrice', 'Total Price', 'CURRENCY', false, 6),
('a0000000-0000-0000-0000-000000000007', 'Discount', 'Discount (%)', 'DECIMAL', false, 7);


-- =============================================
-- QUOTE FIELDS
-- =============================================
INSERT INTO field_defs (entity_def_id, api_name, label, field_type, required, sort_order) VALUES
('a0000000-0000-0000-0000-000000000008', 'Name', 'Quote Name', 'TEXT', true, 1);
INSERT INTO field_defs (entity_def_id, api_name, label, field_type, required, sort_order, ref_entity_id, relation_type, cascade_delete) VALUES
('a0000000-0000-0000-0000-000000000008', 'Opportunity', 'Opportunity', 'MASTER_DETAIL', true, 2, 'a0000000-0000-0000-0000-000000000003', 'MASTER_DETAIL', true);
INSERT INTO field_defs (entity_def_id, api_name, label, field_type, required, sort_order, ref_entity_id, relation_type) VALUES
('a0000000-0000-0000-0000-000000000008', 'Account', 'Account', 'LOOKUP', false, 3, 'a0000000-0000-0000-0000-000000000001', 'LOOKUP'),
('a0000000-0000-0000-0000-000000000008', 'Contact', 'Contact', 'LOOKUP', false, 4, 'a0000000-0000-0000-0000-000000000002', 'LOOKUP');
INSERT INTO field_defs (entity_def_id, api_name, label, field_type, required, sort_order, picklist_values) VALUES
('a0000000-0000-0000-0000-000000000008', 'Status', 'Status', 'PICKLIST', true, 5,
 '[{"value":"Draft","label":"Draft"},{"value":"Needs Review","label":"Needs Review"},{"value":"Approved","label":"Approved"},{"value":"Rejected","label":"Rejected"},{"value":"Accepted","label":"Accepted"}]');
INSERT INTO field_defs (entity_def_id, api_name, label, field_type, required, sort_order) VALUES
('a0000000-0000-0000-0000-000000000008', 'ExpirationDate', 'Expiration Date', 'DATE', false, 6),
('a0000000-0000-0000-0000-000000000008', 'Subtotal', 'Subtotal', 'CURRENCY', false, 7),
('a0000000-0000-0000-0000-000000000008', 'Discount', 'Discount', 'CURRENCY', false, 8),
('a0000000-0000-0000-0000-000000000008', 'TotalPrice', 'Total Price', 'CURRENCY', false, 9),
('a0000000-0000-0000-0000-000000000008', 'Description', 'Description', 'TEXTAREA', false, 10);


-- =============================================
-- QUOTE LINE ITEM FIELDS
-- =============================================
INSERT INTO field_defs (entity_def_id, api_name, label, field_type, required, sort_order) VALUES
('a0000000-0000-0000-0000-000000000009', 'Name', 'Line Item Name', 'TEXT', false, 1);
INSERT INTO field_defs (entity_def_id, api_name, label, field_type, required, sort_order, ref_entity_id, relation_type, cascade_delete) VALUES
('a0000000-0000-0000-0000-000000000009', 'Quote', 'Quote', 'MASTER_DETAIL', true, 2, 'a0000000-0000-0000-0000-000000000008', 'MASTER_DETAIL', true);
INSERT INTO field_defs (entity_def_id, api_name, label, field_type, required, sort_order, ref_entity_id, relation_type) VALUES
('a0000000-0000-0000-0000-000000000009', 'Product', 'Product', 'LOOKUP', true, 3, 'a0000000-0000-0000-0000-000000000004', 'LOOKUP');
INSERT INTO field_defs (entity_def_id, api_name, label, field_type, required, sort_order) VALUES
('a0000000-0000-0000-0000-000000000009', 'Quantity', 'Quantity', 'NUMBER', true, 4),
('a0000000-0000-0000-0000-000000000009', 'UnitPrice', 'Unit Price', 'CURRENCY', true, 5),
('a0000000-0000-0000-0000-000000000009', 'Discount', 'Discount (%)', 'DECIMAL', false, 6),
('a0000000-0000-0000-0000-000000000009', 'TotalPrice', 'Total Price', 'CURRENCY', false, 7);


-- =============================================
-- ORDER FIELDS
-- =============================================
INSERT INTO field_defs (entity_def_id, api_name, label, field_type, required, sort_order) VALUES
('a0000000-0000-0000-0000-00000000000a', 'Name', 'Order Name', 'TEXT', true, 1);
INSERT INTO field_defs (entity_def_id, api_name, label, field_type, required, sort_order, ref_entity_id, relation_type) VALUES
('a0000000-0000-0000-0000-00000000000a', 'Account', 'Account', 'LOOKUP', true, 2, 'a0000000-0000-0000-0000-000000000001', 'LOOKUP'),
('a0000000-0000-0000-0000-00000000000a', 'Opportunity', 'Opportunity', 'LOOKUP', false, 3, 'a0000000-0000-0000-0000-000000000003', 'LOOKUP'),
('a0000000-0000-0000-0000-00000000000a', 'Quote', 'Quote', 'LOOKUP', false, 4, 'a0000000-0000-0000-0000-000000000008', 'LOOKUP');
INSERT INTO field_defs (entity_def_id, api_name, label, field_type, required, sort_order, picklist_values) VALUES
('a0000000-0000-0000-0000-00000000000a', 'Status', 'Status', 'PICKLIST', true, 5,
 '[{"value":"Draft","label":"Draft"},{"value":"Activated","label":"Activated"},{"value":"Fulfilled","label":"Fulfilled"},{"value":"Cancelled","label":"Cancelled"}]');
INSERT INTO field_defs (entity_def_id, api_name, label, field_type, required, sort_order) VALUES
('a0000000-0000-0000-0000-00000000000a', 'OrderDate', 'Order Date', 'DATE', false, 6),
('a0000000-0000-0000-0000-00000000000a', 'TotalAmount', 'Total Amount', 'CURRENCY', false, 7),
('a0000000-0000-0000-0000-00000000000a', 'ContractNumber', 'Contract Number', 'TEXT', false, 8);


-- =============================================
-- ORDER ITEM FIELDS
-- =============================================
INSERT INTO field_defs (entity_def_id, api_name, label, field_type, required, sort_order) VALUES
('a0000000-0000-0000-0000-00000000000b', 'Name', 'Item Name', 'TEXT', false, 1);
INSERT INTO field_defs (entity_def_id, api_name, label, field_type, required, sort_order, ref_entity_id, relation_type, cascade_delete) VALUES
('a0000000-0000-0000-0000-00000000000b', 'Order', 'Order', 'MASTER_DETAIL', true, 2, 'a0000000-0000-0000-0000-00000000000a', 'MASTER_DETAIL', true);
INSERT INTO field_defs (entity_def_id, api_name, label, field_type, required, sort_order, ref_entity_id, relation_type) VALUES
('a0000000-0000-0000-0000-00000000000b', 'Product', 'Product', 'LOOKUP', true, 3, 'a0000000-0000-0000-0000-000000000004', 'LOOKUP');
INSERT INTO field_defs (entity_def_id, api_name, label, field_type, required, sort_order) VALUES
('a0000000-0000-0000-0000-00000000000b', 'Quantity', 'Quantity', 'NUMBER', true, 4),
('a0000000-0000-0000-0000-00000000000b', 'UnitPrice', 'Unit Price', 'CURRENCY', true, 5),
('a0000000-0000-0000-0000-00000000000b', 'TotalPrice', 'Total Price', 'CURRENCY', false, 6);

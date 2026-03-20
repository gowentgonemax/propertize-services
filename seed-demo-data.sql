-- ============================================================
-- Propertize Demo Seed Data
-- Run: psql -h localhost -U ravishah -d propertize_db -f seed-demo-data.sql
-- Idempotent: Uses ON CONFLICT DO NOTHING / safe deletes
-- ============================================================

BEGIN;

-- ============================================================
-- 1. ORGANIZATIONS
-- ============================================================
INSERT INTO organizations (
    id, organization_name, organization_code, organization_type,
    operational_status, status, subscription_tier,
    contact_email, contact_phone, website, description,
    property_count, user_count, storage_used_gb,
    max_properties, max_users, max_storage_gb,
    address_street, address_city, address_state, address_postal_code, address_country,
    created_at, updated_at, version
) VALUES
(
    'org-blueoak-001', 'BlueOak Residential Partners', 'BLUEOAK', 'PROPERTY_MANAGEMENT_COMPANY',
    'ACTIVE', 'ACTIVE', 'PROFESSIONAL',
    'info@blueoakresidential.com', '+1-415-555-0101', 'https://blueoakresidential.com',
    'Premier property management firm specializing in residential complexes across the Bay Area.',
    12, 4, 1.2,
    50, 25, 100.0,
    '500 California St', 'San Francisco', 5, '94104', 'US',
    NOW() - INTERVAL '180 days', NOW() - INTERVAL '5 days', 1
),
(
    'org-sunrise-002', 'Sunrise Property Group', 'SUNRISE', 'REAL_ESTATE_INVESTMENT_TRUST',
    'ACTIVE', 'ACTIVE', 'ENTERPRISE',
    'contact@sunrisepropertygroup.com', '+1-310-555-0202', 'https://sunrisepropertygroup.com',
    'Full-service real estate investment and property management organization with over 200 units.',
    8, 6, 0.8,
    100, 50, 500.0,
    '1888 Century Park East', 'Los Angeles', 5, '90067', 'US',
    NOW() - INTERVAL '365 days', NOW() - INTERVAL '10 days', 1
),
(
    'org-urban-003', 'Urban Living Properties', 'URBAN', 'INDIVIDUAL_LANDLORD',
    'ACTIVE', 'ACTIVE', 'STARTER',
    'hello@urbanlivingproperties.com', '+1-312-555-0303', NULL,
    'Independent landlord managing a portfolio of urban apartments in Chicago.',
    4, 2, 0.3,
    20, 10, 50.0,
    '875 N Michigan Ave', 'Chicago', 14, '60611', 'US',
    NOW() - INTERVAL '90 days', NOW() - INTERVAL '2 days', 1
)
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 2. ORGANIZATION MEMBERSHIP (link admin user to all orgs)
-- ============================================================
INSERT INTO organization_membership (
    id, organization_id, user_id, status, created_at, joined_at
) VALUES
    ('mem-admin-blueoak', 'org-blueoak-001', '1', 'ACTIVE', NOW() - INTERVAL '180 days', NOW() - INTERVAL '180 days'),
    ('mem-admin-sunrise', 'org-sunrise-002', '1', 'ACTIVE', NOW() - INTERVAL '365 days', NOW() - INTERVAL '365 days'),
    ('mem-admin-urban',   'org-urban-003',   '1', 'ACTIVE', NOW() - INTERVAL '90 days',  NOW() - INTERVAL '90 days')
ON CONFLICT (id) DO NOTHING;

-- Update admin user's primary organization
UPDATE users SET organization_id = 'org-blueoak-001' WHERE id = 1;

-- ============================================================
-- 3. PROPERTIES — BlueOak (4 properties)
-- ============================================================
INSERT INTO property (
    id, organization_id, property_name,
    address_street, address_city, address_state, address_postal_code, address_country,
    property_type, bedrooms, bathrooms, square_footage, year_built,
    status, monthly_rent,
    created_at, updated_at, version
) VALUES
(
    'prop-blueoak-001', 'org-blueoak-001', 'Sunset Terrace Apartments',
    '1200 Market St', 'San Francisco', 5, '94102', 'US',
    'APARTMENT', NULL, NULL, NULL, 1998,
    'OCCUPIED', 3200.00,
    NOW() - INTERVAL '160 days', NOW(), 1
),
(
    'prop-blueoak-002', 'org-blueoak-001', 'Bay View Complex',
    '450 Embarcadero', 'San Francisco', 5, '94105', 'US',
    'APARTMENT', NULL, NULL, NULL, 2005,
    'OCCUPIED', 4500.00,
    NOW() - INTERVAL '140 days', NOW(), 1
),
(
    'prop-blueoak-003', 'org-blueoak-001', 'Oak Street Townhomes',
    '220 Oak St', 'Oakland', 5, '94607', 'US',
    'HOUSE', 3, 2, 1800, 1985,
    'OCCUPIED', 2800.00,
    NOW() - INTERVAL '120 days', NOW(), 1
),
(
    'prop-blueoak-004', 'org-blueoak-001', 'Mission Heights Studio',
    '3400 16th St', 'San Francisco', 5, '94114', 'US',
    'APARTMENT', 1, 1, 550, 1972,
    'AVAILABLE', 2100.00,
    NOW() - INTERVAL '100 days', NOW(), 1
),

-- ============================================================
-- PROPERTIES — Sunrise (4 properties)
-- ============================================================
(
    'prop-sunrise-001', 'org-sunrise-002', 'Beverly Hills Residences',
    '9500 Wilshire Blvd', 'Beverly Hills', 5, '90212', 'US',
    'APARTMENT', 2, 2, 1200, 2010,
    'OCCUPIED', 5800.00,
    NOW() - INTERVAL '340 days', NOW(), 1
),
(
    'prop-sunrise-002', 'org-sunrise-002', 'Santa Monica Ocean View',
    '2401 Ocean Ave', 'Santa Monica', 5, '90405', 'US',
    'APARTMENT', 1, 1, 750, 2015,
    'OCCUPIED', 3600.00,
    NOW() - INTERVAL '300 days', NOW(), 1
),
(
    'prop-sunrise-003', 'org-sunrise-002', 'Silver Lake Bungalows',
    '1850 Silver Lake Blvd', 'Los Angeles', 5, '90026', 'US',
    'HOUSE', 2, 1, 1100, 1960,
    'OCCUPIED', 2900.00,
    NOW() - INTERVAL '280 days', NOW(), 1
),
(
    'prop-sunrise-004', 'org-sunrise-002', 'Downtown LA Lofts',
    '600 S Spring St', 'Los Angeles', 5, '90014', 'US',
    'APARTMENT', 1, 1, 900, 2018,
    'AVAILABLE', 2500.00,
    NOW() - INTERVAL '260 days', NOW(), 1
),

-- ============================================================
-- PROPERTIES — Urban Living (4 properties)
-- ============================================================
(
    'prop-urban-001', 'org-urban-003', 'Lincoln Park Studios',
    '2400 N Cannon Dr', 'Chicago', 14, '60614', 'US',
    'APARTMENT', 1, 1, 600, 1995,
    'OCCUPIED', 1800.00,
    NOW() - INTERVAL '80 days', NOW(), 1
),
(
    'prop-urban-002', 'org-urban-003', 'Wicker Park Flat',
    '1500 N Damen Ave', 'Chicago', 14, '60622', 'US',
    'APARTMENT', 2, 1, 950, 2003,
    'OCCUPIED', 2200.00,
    NOW() - INTERVAL '75 days', NOW(), 1
),
(
    'prop-urban-003', 'org-urban-003', 'Logan Square 2BR',
    '2700 N Milwaukee Ave', 'Chicago', 14, '60647', 'US',
    'APARTMENT', 2, 2, 1050, 2001,
    'OCCUPIED', 2400.00,
    NOW() - INTERVAL '60 days', NOW(), 1
),
(
    'prop-urban-004', 'org-urban-003', 'River North Penthouse',
    '430 N Wabash Ave', 'Chicago', 14, '60611', 'US',
    'APARTMENT', 3, 2, 1600, 2020,
    'AVAILABLE', 4200.00,
    NOW() - INTERVAL '30 days', NOW(), 1
)
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 4. TENANTS
-- ============================================================
INSERT INTO tenant (
    id, organization_id,
    first_name, last_name, email, phone,
    date_of_birth, status,
    emergency_contact_name, emergency_contact_phone, emergency_contact_relationship,
    created_at, updated_at, version
) VALUES
-- BlueOak tenants
('ten-001', 'org-blueoak-001', 'James', 'Mitchell', 'james.mitchell@email.com', '+1-415-555-1001',
 '1988-04-15', 'ACTIVE', 'Sarah Mitchell', '+1-415-555-1002', 'SPOUSE', NOW() - INTERVAL '155 days', NOW(), 1),
('ten-002', 'org-blueoak-001', 'Emily', 'Chen', 'emily.chen@email.com', '+1-415-555-1003',
 '1993-09-22', 'ACTIVE', 'David Chen', '+1-415-555-1004', 'PARENT', NOW() - INTERVAL '135 days', NOW(), 1),
('ten-003', 'org-blueoak-001', 'Marcus', 'Johnson', 'marcus.johnson@email.com', '+1-510-555-1005',
 '1985-12-03', 'ACTIVE', 'Linda Johnson', '+1-510-555-1006', 'PARENT', NOW() - INTERVAL '115 days', NOW(), 1),
('ten-004', 'org-blueoak-001', 'Sofia', 'Rodriguez', 'sofia.rodriguez@email.com', '+1-415-555-1007',
 '1996-07-30', 'ACTIVE', 'Carlos Rodriguez', '+1-415-555-1008', 'SIBLING', NOW() - INTERVAL '95 days', NOW(), 1),
('ten-005', 'org-blueoak-001', 'Tyler', 'Brooks', 'tyler.brooks@email.com', '+1-415-555-1009',
 '1990-01-18', 'ACTIVE', 'Kim Brooks', '+1-415-555-1010', 'SPOUSE', NOW() - INTERVAL '75 days', NOW(), 1),

-- Sunrise tenants
('ten-006', 'org-sunrise-002', 'Alexandra', 'Kim', 'alexandra.kim@email.com', '+1-310-555-2001',
 '1991-03-10', 'ACTIVE', 'Robert Kim', '+1-310-555-2002', 'PARENT', NOW() - INTERVAL '330 days', NOW(), 1),
('ten-007', 'org-sunrise-002', 'Nathan', 'Williams', 'nathan.williams@email.com', '+1-310-555-2003',
 '1987-11-25', 'ACTIVE', 'Grace Williams', '+1-310-555-2004', 'SIBLING', NOW() - INTERVAL '290 days', NOW(), 1),
('ten-008', 'org-sunrise-002', 'Priya', 'Patel', 'priya.patel@email.com', '+1-310-555-2005',
 '1994-05-08', 'ACTIVE', 'Raj Patel', '+1-310-555-2006', 'PARENT', NOW() - INTERVAL '270 days', NOW(), 1),
('ten-009', 'org-sunrise-002', 'Connor', 'Murphy', 'connor.murphy@email.com', '+1-323-555-2007',
 '1989-08-14', 'ACTIVE', 'Brigid Murphy', '+1-323-555-2008', 'SPOUSE', NOW() - INTERVAL '250 days', NOW(), 1),
('ten-010', 'org-sunrise-002', 'Jasmine', 'Taylor', 'jasmine.taylor@email.com', '+1-310-555-2009',
 '1995-02-28', 'ACTIVE', 'Mark Taylor', '+1-310-555-2010', 'PARENT', NOW() - INTERVAL '230 days', NOW(), 1),

-- Urban Living tenants
('ten-011', 'org-urban-003', 'David', 'Park', 'david.park@email.com', '+1-312-555-3001',
 '1992-06-17', 'ACTIVE', 'Jin Park', '+1-312-555-3002', 'PARENT', NOW() - INTERVAL '78 days', NOW(), 1),
('ten-012', 'org-urban-003', 'Rachel', 'Green', 'rachel.green@email.com', '+1-312-555-3003',
 '1988-10-05', 'ACTIVE', 'Tom Green', '+1-312-555-3004', 'SPOUSE', NOW() - INTERVAL '72 days', NOW(), 1),
('ten-013', 'org-urban-003', 'Michael', 'Torres', 'michael.torres@email.com', '+1-312-555-3005',
 '1997-03-22', 'ACTIVE', 'Rosa Torres', '+1-312-555-3006', 'PARENT', NOW() - INTERVAL '58 days', NOW(), 1),
('ten-014', 'org-urban-003', 'Aisha', 'Washington', 'aisha.washington@email.com', '+1-312-555-3007',
 '1990-12-09', 'ACTIVE', 'Howard Washington', '+1-312-555-3008', 'PARENT', NOW() - INTERVAL '42 days', NOW(), 1),
('ten-015', 'org-urban-003', 'Kevin', 'Zhang', 'kevin.zhang@email.com', '+1-312-555-3009',
 '1986-07-11', 'ACTIVE', 'Mei Zhang', '+1-312-555-3010', 'SIBLING', NOW() - INTERVAL '28 days', NOW(), 1)
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 5. LEASES
-- ============================================================
INSERT INTO lease (
    id, organization_id, property_id, tenant_id,
    lease_number, lease_type, status,
    start_date, end_date, monthly_rent,
    security_deposit,
    created_at, updated_at, version
) VALUES
-- BlueOak leases
('lease-001', 'org-blueoak-001', 'prop-blueoak-001', 'ten-001',
 'LSE-2025-001', 'FIXED_TERM', 'ACTIVE',
 '2025-03-01', '2026-02-28', 3200.00, 6400.00,
 NOW() - INTERVAL '155 days', NOW(), 1),
('lease-002', 'org-blueoak-001', 'prop-blueoak-001', 'ten-002',
 'LSE-2025-002', 'FIXED_TERM', 'ACTIVE',
 '2025-04-01', '2026-03-31', 3400.00, 6800.00,
 NOW() - INTERVAL '135 days', NOW(), 1),
('lease-003', 'org-blueoak-001', 'prop-blueoak-002', 'ten-003',
 'LSE-2025-003', 'FIXED_TERM', 'ACTIVE',
 '2025-05-01', '2026-04-30', 2800.00, 5600.00,
 NOW() - INTERVAL '115 days', NOW(), 1),
('lease-004', 'org-blueoak-001', 'prop-blueoak-003', 'ten-004',
 'LSE-2025-004', 'FIXED_TERM', 'ACTIVE',
 '2025-06-01', '2026-05-31', 4500.00, 9000.00,
 NOW() - INTERVAL '95 days', NOW(), 1),
('lease-005', 'org-blueoak-001', 'prop-blueoak-001', 'ten-005',
 'LSE-2025-005', 'MONTH_TO_MONTH', 'ACTIVE',
 '2025-08-01', '2026-07-31', 3600.00, 7200.00,
 NOW() - INTERVAL '75 days', NOW(), 1),

-- Sunrise leases
('lease-006', 'org-sunrise-002', 'prop-sunrise-001', 'ten-006',
 'LSE-2024-006', 'FIXED_TERM', 'ACTIVE',
 '2024-06-01', '2026-05-31', 5800.00, 11600.00,
 NOW() - INTERVAL '330 days', NOW(), 1),
('lease-007', 'org-sunrise-002', 'prop-sunrise-002', 'ten-007',
 'LSE-2024-007', 'FIXED_TERM', 'ACTIVE',
 '2024-10-01', '2025-09-30', 3600.00, 7200.00,
 NOW() - INTERVAL '290 days', NOW(), 1),
('lease-008', 'org-sunrise-002', 'prop-sunrise-002', 'ten-008',
 'LSE-2024-008', 'FIXED_TERM', 'ACTIVE',
 '2024-12-01', '2025-11-30', 3800.00, 7600.00,
 NOW() - INTERVAL '270 days', NOW(), 1),
('lease-009', 'org-sunrise-002', 'prop-sunrise-003', 'ten-009',
 'LSE-2025-009', 'FIXED_TERM', 'ACTIVE',
 '2025-01-01', '2025-12-31', 2900.00, 5800.00,
 NOW() - INTERVAL '250 days', NOW(), 1),
('lease-010', 'org-sunrise-002', 'prop-sunrise-001', 'ten-010',
 'LSE-2025-010', 'MONTH_TO_MONTH', 'ACTIVE',
 '2025-03-01', '2026-02-28', 6200.00, 12400.00,
 NOW() - INTERVAL '230 days', NOW(), 1),

-- Urban Living leases
('lease-011', 'org-urban-003', 'prop-urban-001', 'ten-011',
 'LSE-2025-011', 'FIXED_TERM', 'ACTIVE',
 '2025-10-01', '2026-09-30', 1800.00, 3600.00,
 NOW() - INTERVAL '78 days', NOW(), 1),
('lease-012', 'org-urban-003', 'prop-urban-002', 'ten-012',
 'LSE-2025-012', 'FIXED_TERM', 'ACTIVE',
 '2025-10-15', '2026-10-14', 2200.00, 4400.00,
 NOW() - INTERVAL '72 days', NOW(), 1),
('lease-013', 'org-urban-003', 'prop-urban-003', 'ten-013',
 'LSE-2025-013', 'FIXED_TERM', 'ACTIVE',
 '2025-11-01', '2026-10-31', 2400.00, 4800.00,
 NOW() - INTERVAL '58 days', NOW(), 1),
('lease-014', 'org-urban-003', 'prop-urban-001', 'ten-014',
 'LSE-2025-014', 'MONTH_TO_MONTH', 'ACTIVE',
 '2025-12-01', '2026-11-30', 1900.00, 3800.00,
 NOW() - INTERVAL '42 days', NOW(), 1),
('lease-015', 'org-urban-003', 'prop-urban-002', 'ten-015',
 'LSE-2025-015', 'FIXED_TERM', 'ACTIVE',
 '2025-12-15', '2026-12-14', 2300.00, 4600.00,
 NOW() - INTERVAL '28 days', NOW(), 1)
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 6. MAINTENANCE REQUESTS
-- ============================================================
INSERT INTO maintenance_request (
    id, organization_id, property_id, title,
    description, category, priority, status,
    reported_by_tenant_id,
    created_at, updated_at, version
) VALUES
-- BlueOak maintenance
('maint-001', 'org-blueoak-001', 'prop-blueoak-001', 'HVAC Unit Making Noise',
 'The heating unit in unit 3B makes a loud grinding noise at startup', 'HVAC', 'HIGH', 'IN_PROGRESS',
 'ten-001', NOW() - INTERVAL '14 days', NOW(), 1),
('maint-002', 'org-blueoak-001', 'prop-blueoak-001', 'Bathroom Faucet Leaking',
 'Hot water faucet in master bath drips continuously', 'PLUMBING', 'MEDIUM', 'OPEN',
 'ten-002', NOW() - INTERVAL '7 days', NOW(), 1),
('maint-003', 'org-blueoak-001', 'prop-blueoak-002', 'Broken Window Lock',
 'Window lock on bedroom window is broken, security concern', 'SECURITY', 'HIGH', 'OPEN',
 'ten-003', NOW() - INTERVAL '3 days', NOW(), 1),
('maint-004', 'org-blueoak-001', 'prop-blueoak-003', 'Garage Door Malfunction',
 'Garage door remote stopped working, manual override also failing', 'ELECTRICAL', 'MEDIUM', 'COMPLETED',
 'ten-004', NOW() - INTERVAL '21 days', NOW() - INTERVAL '10 days', 1),
('maint-005', 'org-blueoak-001', 'prop-blueoak-001', 'Pest Control Needed',
 'Noticed cockroaches in kitchen area near sink', 'PEST_CONTROL', 'HIGH', 'IN_PROGRESS',
 'ten-005', NOW() - INTERVAL '5 days', NOW(), 1),

-- Sunrise maintenance
('maint-006', 'org-sunrise-002', 'prop-sunrise-001', 'Pool Pump Not Working',
 'Community pool pump has stopped circulating water', 'APPLIANCE', 'HIGH', 'IN_PROGRESS',
 'ten-006', NOW() - INTERVAL '10 days', NOW(), 1),
('maint-007', 'org-sunrise-002', 'prop-sunrise-002', 'Elevator Out of Service',
 'Building elevator is stuck on floor 3, needs urgent repair', 'GENERAL', 'URGENT', 'OPEN',
 'ten-007', NOW() - INTERVAL '1 day', NOW(), 1),
('maint-008', 'org-sunrise-002', 'prop-sunrise-003', 'Paint Peeling in Living Room',
 'Wall paint in living room is peeling near window', 'PAINTING', 'LOW', 'OPEN',
 'ten-008', NOW() - INTERVAL '12 days', NOW(), 1),
('maint-009', 'org-sunrise-002', 'prop-sunrise-001', 'Dishwasher Not Draining',
 'Dishwasher leaves standing water after cycle completes', 'APPLIANCE', 'MEDIUM', 'COMPLETED',
 'ten-009', NOW() - INTERVAL '30 days', NOW() - INTERVAL '15 days', 1),
('maint-010', 'org-sunrise-002', 'prop-sunrise-002', 'Hallway Light Fixture Out',
 'Common area hallway light on 2nd floor is not working', 'ELECTRICAL', 'LOW', 'COMPLETED',
 'ten-010', NOW() - INTERVAL '25 days', NOW() - INTERVAL '20 days', 1),

-- Urban Living maintenance
('maint-011', 'org-urban-003', 'prop-urban-001', 'Radiator Not Heating',
 'Unit radiator is not producing heat despite thermostat being set to 72F', 'HVAC', 'HIGH', 'IN_PROGRESS',
 'ten-011', NOW() - INTERVAL '6 days', NOW(), 1),
('maint-012', 'org-urban-003', 'prop-urban-002', 'Garbage Disposal Broken',
 'Kitchen garbage disposal makes humming noise but does not spin', 'APPLIANCE', 'MEDIUM', 'OPEN',
 'ten-012', NOW() - INTERVAL '4 days', NOW(), 1),
('maint-013', 'org-urban-003', 'prop-urban-003', 'Front Door Lock Sticking',
 'Front door deadbolt is very stiff and hard to turn', 'GENERAL', 'MEDIUM', 'OPEN',
 'ten-013', NOW() - INTERVAL '8 days', NOW(), 1),
('maint-014', 'org-urban-003', 'prop-urban-001', 'Balcony Drain Clogged',
 'Balcony drain is backed up causing water to pool during rain', 'PLUMBING', 'HIGH', 'COMPLETED',
 'ten-014', NOW() - INTERVAL '20 days', NOW() - INTERVAL '12 days', 1),
('maint-015', 'org-urban-003', 'prop-urban-002', 'Bathroom Tile Grout Cracking',
 'Grout between shower tiles is cracking and needs resealing', 'GENERAL', 'LOW', 'OPEN',
 'ten-015', NOW() - INTERVAL '9 days', NOW(), 1)
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 7. PAYMENTS (last 3 months of rent)
-- ============================================================
INSERT INTO payment (
    id, organization_id,
    amount, payment_date, status,
    payment_category, payment_context,
    lease_id, tenant_id, property_id,
    description,
    created_at, updated_at, version
) VALUES
-- BlueOak payments (3 months for 5 leases)
('pay-001', 'org-blueoak-001', 3200.00, CURRENT_DATE - INTERVAL '90 days', 'COMPLETED', 'RENT', 'LEASE_PAYMENT', 'lease-001', 'ten-001', 'prop-blueoak-001', 'Monthly rent - Jan 2026', NOW() - INTERVAL '90 days', NOW(), 1),
('pay-002', 'org-blueoak-001', 3200.00, CURRENT_DATE - INTERVAL '60 days', 'COMPLETED', 'RENT', 'LEASE_PAYMENT', 'lease-001', 'ten-001', 'prop-blueoak-001', 'Monthly rent - Feb 2026', NOW() - INTERVAL '60 days', NOW(), 1),
('pay-003', 'org-blueoak-001', 3200.00, CURRENT_DATE - INTERVAL '30 days', 'COMPLETED', 'RENT', 'LEASE_PAYMENT', 'lease-001', 'ten-001', 'prop-blueoak-001', 'Monthly rent - Mar 2026', NOW() - INTERVAL '30 days', NOW(), 1),
('pay-004', 'org-blueoak-001', 3400.00, CURRENT_DATE - INTERVAL '90 days', 'COMPLETED', 'RENT', 'LEASE_PAYMENT', 'lease-002', 'ten-002', 'prop-blueoak-001', 'Monthly rent - Jan 2026', NOW() - INTERVAL '90 days', NOW(), 1),
('pay-005', 'org-blueoak-001', 3400.00, CURRENT_DATE - INTERVAL '60 days', 'COMPLETED', 'RENT', 'LEASE_PAYMENT', 'lease-002', 'ten-002', 'prop-blueoak-001', 'Monthly rent - Feb 2026', NOW() - INTERVAL '60 days', NOW(), 1),
('pay-006', 'org-blueoak-001', 3400.00, CURRENT_DATE - INTERVAL '30 days', 'COMPLETED', 'RENT', 'LEASE_PAYMENT', 'lease-002', 'ten-002', 'prop-blueoak-001', 'Monthly rent - Mar 2026', NOW() - INTERVAL '30 days', NOW(), 1),
('pay-007', 'org-blueoak-001', 2800.00, CURRENT_DATE - INTERVAL '90 days', 'COMPLETED', 'RENT', 'LEASE_PAYMENT', 'lease-003', 'ten-003', 'prop-blueoak-002', 'Monthly rent - Jan 2026', NOW() - INTERVAL '90 days', NOW(), 1),
('pay-008', 'org-blueoak-001', 2800.00, CURRENT_DATE - INTERVAL '60 days', 'COMPLETED', 'RENT', 'LEASE_PAYMENT', 'lease-003', 'ten-003', 'prop-blueoak-002', 'Monthly rent - Feb 2026', NOW() - INTERVAL '60 days', NOW(), 1),
('pay-009', 'org-blueoak-001', 2800.00, CURRENT_DATE - INTERVAL '30 days', 'PENDING',    'RENT', 'LEASE_PAYMENT', 'lease-003', 'ten-003', 'prop-blueoak-002', 'Monthly rent - Mar 2026', NOW() - INTERVAL '30 days', NOW(), 1),
('pay-010', 'org-blueoak-001', 4500.00, CURRENT_DATE - INTERVAL '90 days', 'COMPLETED', 'RENT', 'LEASE_PAYMENT', 'lease-004', 'ten-004', 'prop-blueoak-003', 'Monthly rent - Jan 2026', NOW() - INTERVAL '90 days', NOW(), 1),
('pay-011', 'org-blueoak-001', 4500.00, CURRENT_DATE - INTERVAL '60 days', 'COMPLETED', 'RENT', 'LEASE_PAYMENT', 'lease-004', 'ten-004', 'prop-blueoak-003', 'Monthly rent - Feb 2026', NOW() - INTERVAL '60 days', NOW(), 1),
('pay-012', 'org-blueoak-001', 4500.00, CURRENT_DATE - INTERVAL '30 days', 'COMPLETED', 'RENT', 'LEASE_PAYMENT', 'lease-004', 'ten-004', 'prop-blueoak-003', 'Monthly rent - Mar 2026', NOW() - INTERVAL '30 days', NOW(), 1),

-- Sunrise payments
('pay-013', 'org-sunrise-002', 5800.00, CURRENT_DATE - INTERVAL '90 days', 'COMPLETED', 'RENT', 'LEASE_PAYMENT', 'lease-006', 'ten-006', 'prop-sunrise-001', 'Monthly rent - Jan 2026', NOW() - INTERVAL '90 days', NOW(), 1),
('pay-014', 'org-sunrise-002', 5800.00, CURRENT_DATE - INTERVAL '60 days', 'COMPLETED', 'RENT', 'LEASE_PAYMENT', 'lease-006', 'ten-006', 'prop-sunrise-001', 'Monthly rent - Feb 2026', NOW() - INTERVAL '60 days', NOW(), 1),
('pay-015', 'org-sunrise-002', 5800.00, CURRENT_DATE - INTERVAL '30 days', 'COMPLETED', 'RENT', 'LEASE_PAYMENT', 'lease-006', 'ten-006', 'prop-sunrise-001', 'Monthly rent - Mar 2026', NOW() - INTERVAL '30 days', NOW(), 1),
('pay-016', 'org-sunrise-002', 3600.00, CURRENT_DATE - INTERVAL '90 days', 'COMPLETED', 'RENT', 'LEASE_PAYMENT', 'lease-007', 'ten-007', 'prop-sunrise-002', 'Monthly rent - Jan 2026', NOW() - INTERVAL '90 days', NOW(), 1),
('pay-017', 'org-sunrise-002', 3600.00, CURRENT_DATE - INTERVAL '60 days', 'COMPLETED', 'RENT', 'LEASE_PAYMENT', 'lease-007', 'ten-007', 'prop-sunrise-002', 'Monthly rent - Feb 2026', NOW() - INTERVAL '60 days', NOW(), 1),
('pay-018', 'org-sunrise-002', 3600.00, CURRENT_DATE - INTERVAL '30 days', 'PENDING',    'RENT', 'LEASE_PAYMENT', 'lease-007', 'ten-007', 'prop-sunrise-002', 'Monthly rent - Mar 2026', NOW() - INTERVAL '30 days', NOW(), 1),
('pay-019', 'org-sunrise-002', 6200.00, CURRENT_DATE - INTERVAL '90 days', 'COMPLETED', 'RENT', 'LEASE_PAYMENT', 'lease-010', 'ten-010', 'prop-sunrise-001', 'Monthly rent - Jan 2026', NOW() - INTERVAL '90 days', NOW(), 1),
('pay-020', 'org-sunrise-002', 6200.00, CURRENT_DATE - INTERVAL '60 days', 'COMPLETED', 'RENT', 'LEASE_PAYMENT', 'lease-010', 'ten-010', 'prop-sunrise-001', 'Monthly rent - Feb 2026', NOW() - INTERVAL '60 days', NOW(), 1),
('pay-021', 'org-sunrise-002', 6200.00, CURRENT_DATE - INTERVAL '30 days', 'COMPLETED', 'RENT', 'LEASE_PAYMENT', 'lease-010', 'ten-010', 'prop-sunrise-001', 'Monthly rent - Mar 2026', NOW() - INTERVAL '30 days', NOW(), 1),

-- Urban Living payments
('pay-022', 'org-urban-003', 1800.00, CURRENT_DATE - INTERVAL '60 days', 'COMPLETED', 'RENT', 'LEASE_PAYMENT', 'lease-011', 'ten-011', 'prop-urban-001', 'Monthly rent - Feb 2026', NOW() - INTERVAL '60 days', NOW(), 1),
('pay-023', 'org-urban-003', 1800.00, CURRENT_DATE - INTERVAL '30 days', 'COMPLETED', 'RENT', 'LEASE_PAYMENT', 'lease-011', 'ten-011', 'prop-urban-001', 'Monthly rent - Mar 2026', NOW() - INTERVAL '30 days', NOW(), 1),
('pay-024', 'org-urban-003', 2200.00, CURRENT_DATE - INTERVAL '60 days', 'COMPLETED', 'RENT', 'LEASE_PAYMENT', 'lease-012', 'ten-012', 'prop-urban-002', 'Monthly rent - Feb 2026', NOW() - INTERVAL '60 days', NOW(), 1),
('pay-025', 'org-urban-003', 2200.00, CURRENT_DATE - INTERVAL '30 days', 'COMPLETED', 'RENT', 'LEASE_PAYMENT', 'lease-012', 'ten-012', 'prop-urban-002', 'Monthly rent - Mar 2026', NOW() - INTERVAL '30 days', NOW(), 1),
('pay-026', 'org-urban-003', 2400.00, CURRENT_DATE - INTERVAL '30 days', 'PENDING',    'RENT', 'LEASE_PAYMENT', 'lease-013', 'ten-013', 'prop-urban-003', 'Monthly rent - Mar 2026', NOW() - INTERVAL '30 days', NOW(), 1),
('pay-027', 'org-urban-003', 4200.00, CURRENT_DATE - INTERVAL '5 days',  'PENDING',    'SECURITY_DEPOSIT', 'DEPOSIT', 'lease-015', 'ten-015', 'prop-urban-004', 'Security deposit', NOW() - INTERVAL '5 days', NOW(), 1)
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 8. UPDATE PROPERTY COUNTS ON ORGANIZATIONS
-- ============================================================
UPDATE organizations SET property_count = 4 WHERE id IN ('org-blueoak-001', 'org-sunrise-002', 'org-urban-003');
UPDATE organizations SET user_count = 4 WHERE id = 'org-blueoak-001';
UPDATE organizations SET user_count = 6 WHERE id = 'org-sunrise-002';
UPDATE organizations SET user_count = 2 WHERE id = 'org-urban-003';

-- ============================================================
-- 9. AUDIT LOG ENTRIES
-- ============================================================
INSERT INTO audit_log (
    id, organization_id, actor_id, actor_username,
    action, entity_type, entity_id,
    details, created_at, updated_at, version
) VALUES
('audit-001', 'org-blueoak-001', 'admin', 'admin', 'ORGANIZATION_CREATED', 'ORGANIZATION', 'org-blueoak-001',
 '{"action": "Organization BlueOak Residential Partners was created"}', NOW() - INTERVAL '180 days', NOW(), 1),
('audit-002', 'org-blueoak-001', 'admin', 'admin', 'PROPERTY_CREATED', 'PROPERTY', 'prop-blueoak-001',
 '{"action": "Property Sunset Terrace Apartments was added"}', NOW() - INTERVAL '160 days', NOW(), 1),
('audit-003', 'org-sunrise-002', 'admin', 'admin', 'ORGANIZATION_CREATED', 'ORGANIZATION', 'org-sunrise-002',
 '{"action": "Organization Sunrise Property Group was created"}', NOW() - INTERVAL '365 days', NOW(), 1),
('audit-004', 'org-urban-003', 'admin', 'admin', 'ORGANIZATION_CREATED', 'ORGANIZATION', 'org-urban-003',
 '{"action": "Organization Urban Living Properties was created"}', NOW() - INTERVAL '90 days', NOW(), 1),
('audit-005', 'org-blueoak-001', 'admin', 'admin', 'LEASE_CREATED', 'LEASE', 'lease-001',
 '{"action": "Lease LSE-2025-001 created for tenant James Mitchell"}', NOW() - INTERVAL '155 days', NOW(), 1)
ON CONFLICT (id) DO NOTHING;

COMMIT;

-- Verification
SELECT 'organizations' as entity, COUNT(*) FROM organizations
UNION ALL SELECT 'properties', COUNT(*) FROM property
UNION ALL SELECT 'tenants', COUNT(*) FROM tenant
UNION ALL SELECT 'leases', COUNT(*) FROM lease
UNION ALL SELECT 'maintenance_requests', COUNT(*) FROM maintenance_request
UNION ALL SELECT 'payments', COUNT(*) FROM payment
UNION ALL SELECT 'memberships', COUNT(*) FROM organization_membership;


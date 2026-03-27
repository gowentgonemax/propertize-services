# WageCraft UI/UX Design Specifications

## Color Palette

### Primary Colors
```css
--primary-main: #1976d2;
--primary-light: #42a5f5;
--primary-dark: #1565c0;
--primary-contrast: #ffffff;
```

### Secondary Colors
```css
--secondary-main: #dc004e;
--secondary-light: #f73378;
--secondary-dark: #9a0036;
--secondary-contrast: #ffffff;
```

### Status Colors
```css
--success: #2e7d32;
--warning: #ed6c02;
--error: #d32f2f;
--info: #0288d1;
```

### Neutral Colors
```css
--grey-50: #fafafa;
--grey-100: #f5f5f5;
--grey-200: #eeeeee;
--grey-300: #e0e0e0;
--grey-400: #bdbdbd;
--grey-500: #9e9e9e;
--grey-600: #757575;
--grey-700: #616161;
--grey-800: #424242;
--grey-900: #212121;
```

---

## Typography

### Font Family
```css
font-family: 'Inter', 'Roboto', 'Helvetica Neue', Arial, sans-serif;
```

### Font Sizes
- **h1**: 2.5rem (40px) - Page titles
- **h2**: 2rem (32px) - Section headers
- **h3**: 1.75rem (28px) - Subsection headers
- **h4**: 1.5rem (24px) - Card titles
- **h5**: 1.25rem (20px) - List headers
- **h6**: 1rem (16px) - Small headers
- **body1**: 1rem (16px) - Main content
- **body2**: 0.875rem (14px) - Secondary content
- **caption**: 0.75rem (12px) - Helper text
- **button**: 0.875rem (14px) - Button text

---

## Spacing System

Use 8px base unit:
- **xs**: 4px (0.5 unit)
- **sm**: 8px (1 unit)
- **md**: 16px (2 units)
- **lg**: 24px (3 units)
- **xl**: 32px (4 units)
- **xxl**: 48px (6 units)

---

## Layout Specifications

### Sidebar Navigation
- **Width**: 260px (expanded), 64px (collapsed)
- **Background**: #ffffff or #f5f5f5
- **Border**: 1px solid #e0e0e0

#### Menu Items
- **Height**: 48px
- **Padding**: 12px 16px
- **Icon Size**: 24px
- **Hover Background**: rgba(25, 118, 210, 0.08)
- **Active Background**: rgba(25, 118, 210, 0.12)

### Top Header
- **Height**: 64px
- **Background**: #ffffff
- **Shadow**: 0 2px 4px rgba(0,0,0,0.1)
- **Padding**: 0 24px

### Content Area
- **Max Width**: 1440px
- **Padding**: 24px
- **Background**: #fafafa

---

## Component Specifications

### Buttons

#### Primary Button
```css
background: #1976d2;
color: #ffffff;
padding: 8px 22px;
border-radius: 4px;
font-size: 14px;
font-weight: 500;
text-transform: uppercase;
box-shadow: 0 2px 4px rgba(0,0,0,0.2);
```

**States:**
- Hover: background: #1565c0
- Active: background: #0d47a1
- Disabled: background: #e0e0e0, color: #9e9e9e

#### Secondary Button
```css
background: transparent;
color: #1976d2;
border: 1px solid #1976d2;
padding: 8px 22px;
```

#### Icon Button
```css
width: 40px;
height: 40px;
border-radius: 50%;
padding: 8px;
```

### Cards

```css
background: #ffffff;
border-radius: 8px;
box-shadow: 0 2px 8px rgba(0,0,0,0.08);
padding: 24px;
margin-bottom: 24px;
```

**Card Header:**
- Font size: 20px
- Font weight: 600
- Margin bottom: 16px

### Tables

#### Table Header
```css
background: #f5f5f5;
font-weight: 600;
font-size: 14px;
color: #424242;
padding: 16px;
border-bottom: 2px solid #e0e0e0;
```

#### Table Row
```css
padding: 12px 16px;
border-bottom: 1px solid #e0e0e0;
transition: background 0.2s;
```

**Hover State:**
```css
background: #fafafa;
cursor: pointer;
```

#### Pagination
- Position: Bottom right
- Size: Small (32px height)
- Max visible pages: 7

### Forms

#### Input Fields
```css
width: 100%;
padding: 10px 14px;
border: 1px solid #bdbdbd;
border-radius: 4px;
font-size: 14px;
```

**States:**
- Focus: border-color: #1976d2, box-shadow: 0 0 0 2px rgba(25,118,210,0.2)
- Error: border-color: #d32f2f
- Disabled: background: #f5f5f5, color: #9e9e9e

#### Labels
```css
font-size: 14px;
font-weight: 500;
color: #424242;
margin-bottom: 8px;
```

#### Helper Text
```css
font-size: 12px;
color: #757575;
margin-top: 4px;
```

**Error Text:**
```css
color: #d32f2f;
```

### Status Badges

```css
display: inline-flex;
align-items: center;
padding: 4px 12px;
border-radius: 16px;
font-size: 12px;
font-weight: 600;
text-transform: uppercase;
```

**Active:**
- Background: #e8f5e9
- Color: #2e7d32

**Inactive:**
- Background: #f5f5f5
- Color: #757575

**Pending:**
- Background: #fff3e0
- Color: #e65100

**Approved:**
- Background: #e3f2fd
- Color: #1565c0

**Rejected/Error:**
- Background: #ffebee
- Color: #c62828

### Modals/Dialogs

```css
background: #ffffff;
border-radius: 8px;
box-shadow: 0 8px 32px rgba(0,0,0,0.24);
max-width: 600px;
padding: 24px;
```

**Overlay:**
```css
background: rgba(0,0,0,0.5);
backdrop-filter: blur(2px);
```

---

## Page Layouts

### Dashboard
```
+------------------------------------------------------------------+
|  Header (Logo, Search, Notifications, Profile)                  |
+------------------------------------------------------------------+
|        |                                                         |
|  Side  |  +------------------+  +------------------+            |
|  bar   |  | Total Clients    |  | Active Employees |            |
|        |  | 145              |  | 2,847            |            |
|        |  +------------------+  +------------------+            |
|        |                                                         |
|  Nav   |  +----------------------------------------+            |
|        |  |                                        |            |
|  Menu  |  |      Payroll Trend Chart               |            |
|        |  |                                        |            |
|        |  +----------------------------------------+            |
|        |                                                         |
|        |  +----------------------------------------+            |
|        |  |  Recent Activity                       |            |
|        |  |  • Payroll approved for Acme Corp      |            |
|        |  |  • New employee added                  |            |
|        |  +----------------------------------------+            |
+------------------------------------------------------------------+
```

### Client List
```
+------------------------------------------------------------------+
|  Header                                                          |
+------------------------------------------------------------------+
|        |                                                         |
|  Side  |  Clients                            [+ Add Client]     |
|  bar   |                                                         |
|        |  [Search] [Filter: Status▾] [Filter: Schedule▾]        |
|        |                                                         |
|        |  +--------------------------------------------------+  |
|        |  | Company Name    | Contact    | Status  | Actions |  |
|        |  |--------------------------------------------------|  |
|        |  | Acme Corp       | Jane Smith | Active  | [...] |  |
|        |  | TechStart Inc   | John Doe   | Active  | [...] |  |
|        |  | ...             | ...        | ...     | [...] |  |
|        |  +--------------------------------------------------+  |
|        |                                                         |
|        |  [< Previous]  1 2 3 ... 10  [Next >]                  |
+------------------------------------------------------------------+
```

### Employee Details
```
+------------------------------------------------------------------+
|  Header                                                          |
+------------------------------------------------------------------+
|        |                                                         |
|  Side  |  ← Back to Employees                                    |
|  bar   |                                                         |
|        |  +--------------------------------------------------+  |
|        |  | [Photo]  John Doe                              |  |
|        |  |          Software Engineer                      |  |
|        |  |          [Active]                               |  |
|        |  |                                                  |  |
|        |  |  [Edit] [Terminate] [View Paystubs]              |  |
|        |  +--------------------------------------------------+  |
|        |                                                         |
|        |  [Personal Info] [Compensation] [Benefits] [Time Off]  |
|        |                                                         |
|        |  +--------------------------------------------------+  |
|        |  | Personal Information                             |  |
|        |  |                                                  |  |
|        |  | Email: john.doe@acme.com                         |  |
|        |  | Phone: 555-1234                                  |  |
|        |  | Hire Date: Jan 10, 2023                          |  |
|        |  | Department: Engineering                          |  |
|        |  +--------------------------------------------------+  |
+------------------------------------------------------------------+
```

### Payroll Processing
```
+------------------------------------------------------------------+
|  Header                                                          |
+------------------------------------------------------------------+
|        |                                                         |
|  Side  |  Process Payroll Run                                    |
|  bar   |                                                         |
|        |  +--------------------------------------------------+  |
|        |  | Step 1: Select Period          [●]━━━○━━━○       |  |
|        |  |                                                  |  |
|        |  | Pay Period: Jan 1 - Jan 15                       |  |
|        |  | Pay Date: Jan 20                                 |  |
|        |  | Client: Acme Corporation                         |  |
|        |  |                                                  |  |
|        |  |                                [Cancel] [Next >]  |  |
|        |  +--------------------------------------------------+  |
|        |                                                         |
|        |  (Flows through: Select Period → Review → Process →     |
|        |   Approve → Complete)                                   |
+------------------------------------------------------------------+
```

---

## Iconography

### Recommended Icon Library
- **Material Icons** (Google)
- **Font Awesome**
- **Heroicons**

### Icon Usage

| Context              | Icon Name            | Size |
|---------------------|---------------------|------|
| Dashboard           | dashboard           | 24px |
| Clients             | business            | 24px |
| Employees           | people              | 24px |
| Payroll             | payment             | 24px |
| Time Tracking       | access_time         | 24px |
| Benefits            | health_and_safety   | 24px |
| Reports             | assessment          | 24px |
| Settings            | settings            | 24px |
| Add                 | add_circle          | 20px |
| Edit                | edit                | 20px |
| Delete              | delete              | 20px |
| View                | visibility          | 20px |
| Download            | download            | 20px |
| Search              | search              | 24px |
| Filter              | filter_list         | 20px |
| Notification        | notifications       | 24px |
| Profile             | account_circle      | 32px |

---

## Responsive Breakpoints

```css
/* Mobile */
@media (max-width: 599px) {
  /* Sidebar collapses to bottom nav */
  /* Tables become cards */
  /* Single column layout */
}

/* Tablet */
@media (min-width: 600px) and (max-width: 959px) {
  /* Collapsible sidebar */
  /* 2 column grid for cards */
}

/* Desktop */
@media (min-width: 960px) {
  /* Full sidebar visible */
  /* Multi-column layouts */
}

/* Large Desktop */
@media (min-width: 1440px) {
  /* Max container width applied */
}
```

---

## Animations & Transitions

### Default Transition
```css
transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
```

### Page Transitions
```css
/* Enter */
opacity: 0 → 1;
transform: translateY(20px) → translateY(0);
duration: 300ms;

/* Exit */
opacity: 1 → 0;
duration: 200ms;
```

### Loading States
- **Skeleton Loader**: Animated gradient
- **Spinner**: Circular progress, 40px diameter
- **Progress Bar**: Linear, 4px height

---

## Accessibility

### Focus States
```css
outline: 2px solid #1976d2;
outline-offset: 2px;
```

### Keyboard Navigation
- Tab order follows visual flow
- All interactive elements keyboard accessible
- Escape closes modals/dropdowns
- Enter/Space activates buttons

### ARIA Labels
- Use descriptive labels for icons
- Announce loading states
- Label form inputs properly
- Use role attributes appropriately

---

## Sample Component Wireframes

### Data Card
```
+--------------------------------+
| Title                    [...]  |
+--------------------------------+
| Subtitle                       |
|                                |
| Primary Metric: 1,234          |
| Change: +5.2% ↑                |
|                                |
| [View Details]                 |
+--------------------------------+
```

### Employee Card (Mobile)
```
+--------------------------------+
| [Avatar] John Doe        [●]   |
|          Software Engineer     |
+--------------------------------+
| Email: john@example.com        |
| Dept: Engineering              |
| Hire Date: Jan 10, 2023        |
+--------------------------------+
| [View] [Edit]                  |
+--------------------------------+
```

### Filter Panel
```
+--------------------------------+
| Filters                    [×]  |
+--------------------------------+
| Status                         |
| □ Active                       |
| □ Inactive                     |
| □ Terminated                   |
|                                |
| Department                     |
| [Select Department ▾]          |
|                                |
| Hire Date Range                |
| From: [Date Picker]            |
| To:   [Date Picker]            |
|                                |
| [Clear] [Apply Filters]        |
+--------------------------------+
```

---

## Dark Mode Support

### Dark Theme Colors
```css
--dark-bg-primary: #121212;
--dark-bg-secondary: #1e1e1e;
--dark-bg-paper: #2c2c2c;
--dark-text-primary: #ffffff;
--dark-text-secondary: #b0b0b0;
--dark-divider: #3f3f3f;
```

### Implementation
- Toggle in user settings
- Store preference in localStorage
- Apply via CSS classes or theme context
- Maintain WCAG AA contrast ratios

---

## File Upload UI

```
+--------------------------------+
| Drop files here or             |
|                                |
|      [📁 Choose File]          |
|                                |
| Supported: PDF, CSV, XLS       |
| Max size: 10MB                 |
+--------------------------------+
```

**Uploading State:**
```
+--------------------------------+
| document.pdf                   |
| [████████░░] 80%              |
+--------------------------------+
```

---

## Notification Toast

```
+--------------------------------+
| [✓] Employee added successfully|
|                            [×]  |
+--------------------------------+
```

**Variants:**
- Success: Green (#2e7d32)
- Error: Red (#d32f2f)
- Warning: Orange (#ed6c02)
- Info: Blue (#0288d1)

**Position:** Top-right
**Duration:** 5 seconds (dismissible)


# WageCraft Frontend Development Guide

## Overview
WageCraft is a comprehensive payroll management system that handles client management, employee management, payroll processing, time tracking, benefits administration, and more.

---

## Table of Contents
1. [Technology Stack Recommendations](#technology-stack-recommendations)
2. [Project Structure](#project-structure)
3. [Key Features to Implement](#key-features-to-implement)
4. [State Management](#state-management)
5. [Authentication Flow](#authentication-flow)
6. [Page Components](#page-components)
7. [Reusable Components](#reusable-components)
8. [API Integration](#api-integration)
9. [Best Practices](#best-practices)

---

## Technology Stack Recommendations

### Core Framework
- **React** (18+) with TypeScript
- **Next.js** (13+) for SSR/SSG capabilities (optional but recommended)
- **Vue.js 3** or **Angular** as alternatives

### UI Component Libraries
- **Material-UI (MUI)** - Comprehensive, enterprise-ready
- **Ant Design** - Great for data-heavy applications
- **Chakra UI** - Modern, accessible
- **Tailwind CSS** - Utility-first styling

### State Management
- **Redux Toolkit** - For complex state
- **Zustand** - Lightweight alternative
- **React Query / TanStack Query** - For server state (highly recommended)
- **Recoil** - For React applications

### Form Management
- **React Hook Form** - Performant, minimal re-renders
- **Formik** - Popular alternative

### Data Grid/Table
- **AG-Grid** - Feature-rich, enterprise-grade
- **TanStack Table** - Headless, flexible
- **MUI DataGrid** - If using Material-UI

### Date Handling
- **date-fns** - Lightweight, modern
- **Day.js** - Moment.js alternative

### HTTP Client
- **Axios** - Feature-rich
- **Fetch API** with wrappers

### Charts & Visualization
- **Recharts** - React-specific
- **Chart.js** with react-chartjs-2
- **Apache ECharts**

---

## Project Structure

```
src/
├── api/
│   ├── client.ts
│   ├── auth.ts
│   ├── employee.ts
│   ├── payroll.ts
│   └── interceptors.ts
├── components/
│   ├── common/
│   │   ├── Button/
│   │   ├── Input/
│   │   ├── Table/
│   │   ├── Modal/
│   │   ├── Pagination/
│   │   └── ErrorBoundary/
│   ├── layout/
│   │   ├── Header/
│   │   ├── Sidebar/
│   │   ├── Footer/
│   │   └── Layout.tsx
│   ├── auth/
│   │   ├── LoginForm.tsx
│   │   └── RegisterForm.tsx
│   ├── clients/
│   │   ├── ClientList.tsx
│   │   ├── ClientForm.tsx
│   │   └── ClientCard.tsx
│   ├── employees/
│   │   ├── EmployeeList.tsx
│   │   ├── EmployeeForm.tsx
│   │   ├── EmployeeCard.tsx
│   │   └── EmployeeDetails.tsx
│   └── payroll/
│       ├── PayrollList.tsx
│       ├── PayrollForm.tsx
│       ├── PayrollDetails.tsx
│       └── PaystubView.tsx
├── pages/ (or routes/)
│   ├── auth/
│   │   ├── login.tsx
│   │   └── register.tsx
│   ├── dashboard/
│   │   └── index.tsx
│   ├── clients/
│   │   ├── index.tsx
│   │   ├── [id].tsx
│   │   └── new.tsx
│   ├── employees/
│   │   ├── index.tsx
│   │   ├── [id].tsx
│   │   └── new.tsx
│   └── payroll/
│       ├── index.tsx
│       ├── [id].tsx
│       └── new.tsx
├── hooks/
│   ├── useAuth.ts
│   ├── useClients.ts
│   ├── useEmployees.ts
│   ├── usePayroll.ts
│   └── usePagination.ts
├── store/
│   ├── authSlice.ts
│   ├── clientSlice.ts
│   ├── employeeSlice.ts
│   └── store.ts
├── types/
│   ├── models.ts
│   ├── api.ts
│   └── common.ts
├── utils/
│   ├── formatters.ts
│   ├── validators.ts
│   ├── constants.ts
│   └── helpers.ts
├── styles/
│   ├── globals.css
│   └── theme.ts
└── config/
    ├── api.config.ts
    └── app.config.ts
```

---

## Key Features to Implement

### 1. Dashboard
- **KPIs & Metrics**: Total clients, active employees, upcoming payrolls
- **Quick Actions**: Create client, add employee, run payroll
- **Recent Activity**: Latest payroll runs, new employees
- **Charts**: Payroll trends, employee distribution by department

### 2. Client Management
- **Client List**: Paginated table with search and filters
- **Client Details**: View/edit client information
- **Client Creation**: Multi-step form with validation
- **Client Status**: Toggle active/inactive status

### 3. Employee Management
- **Employee List**: Filterable by status, department, client
- **Employee Profile**: Personal info, compensation, benefits
- **Employee Onboarding**: Step-by-step employee creation
- **Employee Termination**: Handle employee termination process

### 4. Payroll Processing
- **Payroll Calendar**: Visual calendar of pay periods
- **Create Payroll Run**: Select period and employees
- **Process Payroll**: Calculate wages, taxes, deductions
- **Approve Payroll**: Review and approve for payment
- **Paystub Generation**: View and download paystubs

### 5. Time & Attendance
- **Time Entry**: Clock in/out interface
- **Timesheet View**: Weekly/monthly timesheets
- **Leave Management**: Request and approve time off
- **Overtime Tracking**: Monitor and approve overtime

### 6. Benefits Administration
- **Benefit Plans**: Manage health, dental, retirement plans
- **Enrollment**: Employee benefit enrollment
- **Deduction Management**: Track benefit deductions

### 7. Reports & Analytics
- **Payroll Reports**: Summary, detailed, tax reports
- **Employee Reports**: Headcount, turnover, demographics
- **Compliance Reports**: Tax filings, labor law compliance
- **Export Capabilities**: PDF, Excel, CSV

---

## State Management

### Authentication State
```typescript
interface AuthState {
  user: User | null;
  token: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
}
```

### Client State
```typescript
interface ClientState {
  clients: Client[];
  selectedClient: Client | null;
  isLoading: boolean;
  error: string | null;
  pagination: {
    page: number;
    limit: number;
    totalPages: number;
    totalElements: number;
  };
}
```

### Employee State
```typescript
interface EmployeeState {
  employees: Employee[];
  selectedEmployee: Employee | null;
  isLoading: boolean;
  error: string | null;
  filters: {
    clientId?: string;
    status?: EmployeeStatus;
    department?: string;
  };
  pagination: PaginationState;
}
```

---

## Authentication Flow

### 1. Login Process
```typescript
// Login flow
const login = async (credentials: LoginRequest) => {
  try {
    const response = await authApi.login(credentials);
    // Store token
    localStorage.setItem('token', response.token);
    localStorage.setItem('refreshToken', response.refreshToken);
    // Set axios header
    axios.defaults.headers.common['Authorization'] = `Bearer ${response.token}`;
    // Navigate to dashboard
    router.push('/dashboard');
  } catch (error) {
    // Handle error
  }
};
```

### 2. Token Refresh
```typescript
// Axios interceptor for token refresh
axios.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      try {
        const refreshToken = localStorage.getItem('refreshToken');
        const response = await authApi.refreshToken(refreshToken);
        localStorage.setItem('token', response.token);
        axios.defaults.headers.common['Authorization'] = `Bearer ${response.token}`;
        return axios(originalRequest);
      } catch (refreshError) {
        // Redirect to login
        window.location.href = '/auth/login';
      }
    }
    return Promise.reject(error);
  }
);
```

### 3. Protected Routes
```typescript
// Route guard component
const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated, isLoading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.push('/auth/login');
    }
  }, [isAuthenticated, isLoading, router]);

  if (isLoading) {
    return <LoadingSpinner />;
  }

  return isAuthenticated ? <>{children}</> : null;
};
```

---

## Page Components

### Dashboard Page
```typescript
const Dashboard: React.FC = () => {
  // Fetch dashboard data
  const { data: stats } = useQuery('dashboardStats', fetchDashboardStats);
  
  return (
    <Layout>
      <Grid container spacing={3}>
        <Grid item xs={12} md={3}>
          <StatCard title="Total Clients" value={stats?.totalClients} />
        </Grid>
        <Grid item xs={12} md={3}>
          <StatCard title="Active Employees" value={stats?.activeEmployees} />
        </Grid>
        <Grid item xs={12} md={3}>
          <StatCard title="Upcoming Payrolls" value={stats?.upcomingPayrolls} />
        </Grid>
        <Grid item xs={12} md={3}>
          <StatCard title="This Month's Payroll" value={formatCurrency(stats?.monthlyPayroll)} />
        </Grid>
        <Grid item xs={12} md={8}>
          <PayrollTrendChart data={stats?.payrollTrend} />
        </Grid>
        <Grid item xs={12} md={4}>
          <RecentActivity activities={stats?.recentActivity} />
        </Grid>
      </Grid>
    </Layout>
  );
};
```

### Client List Page
```typescript
const ClientList: React.FC = () => {
  const [page, setPage] = useState(0);
  const [filters, setFilters] = useState<ClientFilters>({});
  
  const { data, isLoading } = useQuery(
    ['clients', page, filters],
    () => fetchClients({ page, limit: 20, ...filters })
  );

  return (
    <Layout>
      <PageHeader
        title="Clients"
        action={<Button onClick={() => router.push('/clients/new')}>Add Client</Button>}
      />
      <FilterBar filters={filters} onFilterChange={setFilters} />
      <DataTable
        columns={clientColumns}
        data={data?.content}
        loading={isLoading}
        onRowClick={(client) => router.push(`/clients/${client.id}`)}
      />
      <Pagination
        page={page}
        totalPages={data?.totalPages}
        onPageChange={setPage}
      />
    </Layout>
  );
};
```

### Employee Form
```typescript
const EmployeeForm: React.FC<{ clientId: string; employeeId?: string }> = ({
  clientId,
  employeeId,
}) => {
  const { control, handleSubmit, formState: { errors } } = useForm<EmployeeFormData>();
  const mutation = useMutation(employeeId ? updateEmployee : createEmployee);

  const onSubmit = async (data: EmployeeFormData) => {
    try {
      await mutation.mutateAsync({ clientId, employeeId, data });
      showSuccess('Employee saved successfully');
      router.push(`/clients/${clientId}/employees`);
    } catch (error) {
      showError('Failed to save employee');
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <Grid container spacing={2}>
        <Grid item xs={12} md={6}>
          <Controller
            name="firstName"
            control={control}
            rules={{ required: 'First name is required' }}
            render={({ field }) => (
              <TextField
                {...field}
                label="First Name"
                error={!!errors.firstName}
                helperText={errors.firstName?.message}
                fullWidth
              />
            )}
          />
        </Grid>
        {/* More fields... */}
        <Grid item xs={12}>
          <Button type="submit" variant="contained" disabled={mutation.isLoading}>
            {mutation.isLoading ? 'Saving...' : 'Save Employee'}
          </Button>
        </Grid>
      </Grid>
    </form>
  );
};
```

---

## Reusable Components

### DataTable Component
```typescript
interface DataTableProps<T> {
  columns: Column<T>[];
  data: T[];
  loading?: boolean;
  onRowClick?: (row: T) => void;
  selectable?: boolean;
  onSelectionChange?: (selected: T[]) => void;
}

const DataTable = <T extends { id: string }>({
  columns,
  data,
  loading,
  onRowClick,
}: DataTableProps<T>) => {
  // Implementation
};
```

### StatusBadge Component
```typescript
interface StatusBadgeProps {
  status: string;
  colorMap?: Record<string, string>;
}

const StatusBadge: React.FC<StatusBadgeProps> = ({ status, colorMap }) => {
  const defaultColors = {
    ACTIVE: 'success',
    INACTIVE: 'default',
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'error',
  };
  
  const color = colorMap?.[status] || defaultColors[status] || 'default';
  
  return <Chip label={status} color={color} size="small" />;
};
```

---

## API Integration

### API Client Setup
```typescript
// api/client.ts
import axios from 'axios';

const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    // Handle errors globally
    if (error.response?.status === 401) {
      // Handle unauthorized
    }
    return Promise.reject(error);
  }
);

export default apiClient;
```

### API Service Functions
```typescript
// api/employee.ts
import apiClient from './client';
import { Employee, PaginatedResponse, EmployeeFormData } from '@/types/models';

export const employeeApi = {
  getEmployees: (clientId: string, params: FilterParams) =>
    apiClient.get<PaginatedResponse<Employee>>(
      `/clients/${clientId}/employees`,
      { params }
    ),

  getEmployeeById: (id: string) =>
    apiClient.get<Employee>(`/employees/${id}`),

  createEmployee: (clientId: string, data: EmployeeFormData) =>
    apiClient.post<Employee>(`/clients/${clientId}/employees`, data),

  updateEmployee: (id: string, data: EmployeeFormData) =>
    apiClient.put<Employee>(`/employees/${id}`, data),

  terminateEmployee: (id: string) =>
    apiClient.post(`/employees/${id}/terminate`),
};
```

---

## Best Practices

### 1. Error Handling
- Implement global error boundary
- Show user-friendly error messages
- Log errors for debugging
- Provide retry mechanisms

### 2. Loading States
- Show skeleton loaders during data fetch
- Disable buttons during mutations
- Use optimistic updates where appropriate

### 3. Form Validation
- Client-side validation with react-hook-form
- Display validation errors inline
- Match server-side validation rules

### 4. Security
- Store JWT tokens securely
- Implement token refresh mechanism
- Sanitize user inputs
- Use HTTPS in production

### 5. Performance
- Implement pagination for large lists
- Use React.memo for expensive components
- Lazy load routes and components
- Implement virtual scrolling for large tables

### 6. Accessibility
- Use semantic HTML
- Implement keyboard navigation
- Add ARIA labels
- Ensure color contrast ratios

### 7. Testing
- Write unit tests for utilities
- Component testing with React Testing Library
- E2E tests with Cypress or Playwright
- API mocking with MSW

### 8. Code Organization
- Follow consistent naming conventions
- Keep components small and focused
- Extract reusable logic into hooks
- Use TypeScript for type safety

---

## Environment Variables

```env
# .env.local
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_APP_NAME=WageCraft
NEXT_PUBLIC_JWT_EXPIRATION=86400
```

---

## Getting Started

1. **Initialize Project**
   ```bash
   npx create-next-app@latest wagecraft-frontend --typescript
   cd wagecraft-frontend
   ```

2. **Install Dependencies**
   ```bash
   npm install axios react-query react-hook-form
   npm install @mui/material @emotion/react @emotion/styled
   npm install date-fns
   npm install @tanstack/react-table
   ```

3. **Setup API Client** (as shown in API Integration section)

4. **Create Type Definitions** (from FRONTEND_DATA_MODELS.md)

5. **Implement Authentication** (login, protected routes)

6. **Build Core Features** (dashboard, clients, employees, payroll)

7. **Add Advanced Features** (reports, charts, notifications)

---

## Additional Resources

- See `FRONTEND_API_REFERENCE.md` for complete API documentation
- See `FRONTEND_DATA_MODELS.md` for TypeScript interfaces
- See `API_TESTING_GUIDE.md` for API testing examples


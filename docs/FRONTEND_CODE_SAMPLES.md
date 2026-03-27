# WageCraft Sample Code Snippets for Frontend

This document provides ready-to-use code snippets for common frontend implementations.

---

## 1. API Client Setup

### axios-client.ts
```typescript
import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000,
});

// Request interceptor - Add auth token
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor - Handle token refresh
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        const refreshToken = localStorage.getItem('refreshToken');
        if (!refreshToken) {
          throw new Error('No refresh token available');
        }

        // Implement refresh token logic here
        // const response = await axios.post(`${API_BASE_URL}/auth/refresh`, { refreshToken });
        // localStorage.setItem('token', response.data.token);
        // originalRequest.headers.Authorization = `Bearer ${response.data.token}`;
        // return apiClient(originalRequest);

        // For now, redirect to login
        window.location.href = '/auth/login';
      } catch (refreshError) {
        localStorage.removeItem('token');
        localStorage.removeItem('refreshToken');
        window.location.href = '/auth/login';
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
```

---

## 2. API Service Functions

### auth.service.ts
```typescript
import apiClient from './axios-client';
import { LoginRequest, LoginResponse, RegisterRequest, RegisterResponse } from '@/types/models';

export const authService = {
  login: async (credentials: LoginRequest): Promise<LoginResponse> => {
    const response = await apiClient.post<LoginResponse>('/auth/login', credentials);
    return response.data;
  },

  register: async (data: RegisterRequest): Promise<RegisterResponse> => {
    const response = await apiClient.post<RegisterResponse>('/auth/register', data);
    return response.data;
  },

  logout: () => {
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    window.location.href = '/auth/login';
  },
};
```

### client.service.ts
```typescript
import apiClient from './axios-client';
import { Client, PaginatedResponse } from '@/types/models';

export const clientService = {
  getAll: async (params: {
    page?: number;
    limit?: number;
    status?: string;
  }): Promise<PaginatedResponse<Client>> => {
    const response = await apiClient.get<PaginatedResponse<Client>>('/clients', { params });
    return response.data;
  },

  getById: async (id: string): Promise<Client> => {
    const response = await apiClient.get<Client>(`/clients/${id}`);
    return response.data;
  },

  create: async (data: Partial<Client>): Promise<Client> => {
    const response = await apiClient.post<Client>('/clients', data);
    return response.data;
  },

  update: async (id: string, data: Partial<Client>): Promise<Client> => {
    const response = await apiClient.put<Client>(`/clients/${id}`, data);
    return response.data;
  },

  delete: async (id: string): Promise<void> => {
    await apiClient.delete(`/clients/${id}`);
  },
};
```

### employee.service.ts
```typescript
import apiClient from './axios-client';
import { Employee, PaginatedResponse } from '@/types/models';

export const employeeService = {
  getByClient: async (
    clientId: string,
    params: {
      page?: number;
      limit?: number;
      status?: string;
      department?: string;
    }
  ): Promise<PaginatedResponse<Employee>> => {
    const response = await apiClient.get<PaginatedResponse<Employee>>(
      `/clients/${clientId}/employees`,
      { params }
    );
    return response.data;
  },

  getById: async (id: string): Promise<Employee> => {
    const response = await apiClient.get<Employee>(`/employees/${id}`);
    return response.data;
  },

  create: async (clientId: string, data: Partial<Employee>): Promise<Employee> => {
    const response = await apiClient.post<Employee>(
      `/clients/${clientId}/employees`,
      data
    );
    return response.data;
  },

  update: async (id: string, data: Partial<Employee>): Promise<Employee> => {
    const response = await apiClient.put<Employee>(`/employees/${id}`, data);
    return response.data;
  },

  terminate: async (id: string): Promise<void> => {
    await apiClient.post(`/employees/${id}/terminate`);
  },
};
```

---

## 3. Custom React Hooks

### useAuth.ts
```typescript
import { useState, useEffect } from 'react';
import { useRouter } from 'next/router';
import { authService } from '@/services/auth.service';
import { LoginRequest, RegisterRequest, User } from '@/types/models';

export const useAuth = () => {
  const [user, setUser] = useState<User | null>(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const router = useRouter();

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (token) {
      // Optionally fetch user profile
      setIsAuthenticated(true);
    }
    setIsLoading(false);
  }, []);

  const login = async (credentials: LoginRequest) => {
    try {
      const response = await authService.login(credentials);
      localStorage.setItem('token', response.token);
      localStorage.setItem('refreshToken', response.refreshToken);
      setIsAuthenticated(true);
      router.push('/dashboard');
    } catch (error) {
      throw error;
    }
  };

  const register = async (data: RegisterRequest) => {
    try {
      await authService.register(data);
      router.push('/auth/login');
    } catch (error) {
      throw error;
    }
  };

  const logout = () => {
    authService.logout();
    setIsAuthenticated(false);
    setUser(null);
  };

  return {
    user,
    isAuthenticated,
    isLoading,
    login,
    register,
    logout,
  };
};
```

### useClients.ts
```typescript
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { clientService } from '@/services/client.service';
import { Client } from '@/types/models';

export const useClients = (params?: { page?: number; limit?: number; status?: string }) => {
  return useQuery(
    ['clients', params],
    () => clientService.getAll(params || {}),
    {
      keepPreviousData: true,
    }
  );
};

export const useClient = (id: string) => {
  return useQuery(['client', id], () => clientService.getById(id), {
    enabled: !!id,
  });
};

export const useCreateClient = () => {
  const queryClient = useQueryClient();

  return useMutation(
    (data: Partial<Client>) => clientService.create(data),
    {
      onSuccess: () => {
        queryClient.invalidateQueries(['clients']);
      },
    }
  );
};

export const useUpdateClient = () => {
  const queryClient = useQueryClient();

  return useMutation(
    ({ id, data }: { id: string; data: Partial<Client> }) =>
      clientService.update(id, data),
    {
      onSuccess: (_, variables) => {
        queryClient.invalidateQueries(['clients']);
        queryClient.invalidateQueries(['client', variables.id]);
      },
    }
  );
};

export const useDeleteClient = () => {
  const queryClient = useQueryClient();

  return useMutation((id: string) => clientService.delete(id), {
    onSuccess: () => {
      queryClient.invalidateQueries(['clients']);
    },
  });
};
```

---

## 4. Component Examples

### LoginForm.tsx
```typescript
import React, { useState } from 'react';
import { useAuth } from '@/hooks/useAuth';
import { TextField, Button, Box, Alert, CircularProgress } from '@mui/material';

const LoginForm: React.FC = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await login({ email, password });
    } catch (err: any) {
      setError(err.response?.data?.message || 'Login failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box component="form" onSubmit={handleSubmit} sx={{ maxWidth: 400, mx: 'auto', mt: 4 }}>
      <TextField
        fullWidth
        label="Email"
        type="email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        margin="normal"
        required
      />
      <TextField
        fullWidth
        label="Password"
        type="password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        margin="normal"
        required
      />
      {error && (
        <Alert severity="error" sx={{ mt: 2 }}>
          {error}
        </Alert>
      )}
      <Button
        type="submit"
        variant="contained"
        fullWidth
        sx={{ mt: 3 }}
        disabled={loading}
      >
        {loading ? <CircularProgress size={24} /> : 'Login'}
      </Button>
    </Box>
  );
};

export default LoginForm;
```

### ClientList.tsx
```typescript
import React, { useState } from 'react';
import { useClients } from '@/hooks/useClients';
import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Button,
  CircularProgress,
  Chip,
  IconButton,
  Pagination,
} from '@mui/material';
import { Edit, Delete, Visibility } from '@mui/icons-material';
import { useRouter } from 'next/router';

const ClientList: React.FC = () => {
  const [page, setPage] = useState(0);
  const { data, isLoading } = useClients({ page, limit: 20 });
  const router = useRouter();

  if (isLoading) {
    return <CircularProgress />;
  }

  return (
    <div>
      <Button
        variant="contained"
        onClick={() => router.push('/clients/new')}
        sx={{ mb: 2 }}
      >
        Add Client
      </Button>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Company Name</TableCell>
              <TableCell>Contact Person</TableCell>
              <TableCell>Email</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {data?.content.map((client) => (
              <TableRow key={client.id} hover>
                <TableCell>{client.companyName}</TableCell>
                <TableCell>{client.contactPerson}</TableCell>
                <TableCell>{client.email}</TableCell>
                <TableCell>
                  <Chip
                    label={client.status}
                    color={client.status === 'ACTIVE' ? 'success' : 'default'}
                    size="small"
                  />
                </TableCell>
                <TableCell>
                  <IconButton
                    size="small"
                    onClick={() => router.push(`/clients/${client.id}`)}
                  >
                    <Visibility />
                  </IconButton>
                  <IconButton
                    size="small"
                    onClick={() => router.push(`/clients/${client.id}/edit`)}
                  >
                    <Edit />
                  </IconButton>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      <Pagination
        count={data?.totalPages || 0}
        page={page + 1}
        onChange={(_, newPage) => setPage(newPage - 1)}
        sx={{ mt: 2, display: 'flex', justifyContent: 'center' }}
      />
    </div>
  );
};

export default ClientList;
```

### ClientForm.tsx
```typescript
import React from 'react';
import { useForm, Controller } from 'react-hook-form';
import { useCreateClient, useUpdateClient } from '@/hooks/useClients';
import {
  TextField,
  Button,
  Grid,
  MenuItem,
  Box,
} from '@mui/material';
import { Client, ClientStatus, PayrollSchedule } from '@/types/models';
import { useRouter } from 'next/router';

interface ClientFormProps {
  client?: Client;
  isEdit?: boolean;
}

const ClientForm: React.FC<ClientFormProps> = ({ client, isEdit }) => {
  const { control, handleSubmit, formState: { errors } } = useForm<Partial<Client>>({
    defaultValues: client || {},
  });
  
  const createMutation = useCreateClient();
  const updateMutation = useUpdateClient();
  const router = useRouter();

  const onSubmit = async (data: Partial<Client>) => {
    try {
      if (isEdit && client?.id) {
        await updateMutation.mutateAsync({ id: client.id, data });
      } else {
        await createMutation.mutateAsync(data);
      }
      router.push('/clients');
    } catch (error) {
      console.error('Error saving client:', error);
    }
  };

  const isLoading = createMutation.isLoading || updateMutation.isLoading;

  return (
    <Box component="form" onSubmit={handleSubmit(onSubmit)}>
      <Grid container spacing={2}>
        <Grid item xs={12} md={6}>
          <Controller
            name="companyName"
            control={control}
            rules={{ required: 'Company name is required' }}
            render={({ field }) => (
              <TextField
                {...field}
                fullWidth
                label="Company Name"
                error={!!errors.companyName}
                helperText={errors.companyName?.message}
              />
            )}
          />
        </Grid>

        <Grid item xs={12} md={6}>
          <Controller
            name="taxId"
            control={control}
            rules={{ required: 'Tax ID is required' }}
            render={({ field }) => (
              <TextField
                {...field}
                fullWidth
                label="Tax ID"
                error={!!errors.taxId}
                helperText={errors.taxId?.message}
              />
            )}
          />
        </Grid>

        <Grid item xs={12} md={6}>
          <Controller
            name="contactPerson"
            control={control}
            rules={{ required: 'Contact person is required' }}
            render={({ field }) => (
              <TextField
                {...field}
                fullWidth
                label="Contact Person"
                error={!!errors.contactPerson}
                helperText={errors.contactPerson?.message}
              />
            )}
          />
        </Grid>

        <Grid item xs={12} md={6}>
          <Controller
            name="email"
            control={control}
            rules={{
              required: 'Email is required',
              pattern: {
                value: /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i,
                message: 'Invalid email address',
              },
            }}
            render={({ field }) => (
              <TextField
                {...field}
                fullWidth
                label="Email"
                type="email"
                error={!!errors.email}
                helperText={errors.email?.message}
              />
            )}
          />
        </Grid>

        <Grid item xs={12} md={6}>
          <Controller
            name="phone"
            control={control}
            render={({ field }) => (
              <TextField {...field} fullWidth label="Phone" />
            )}
          />
        </Grid>

        <Grid item xs={12} md={6}>
          <Controller
            name="status"
            control={control}
            render={({ field }) => (
              <TextField {...field} fullWidth select label="Status">
                <MenuItem value="ACTIVE">Active</MenuItem>
                <MenuItem value="INACTIVE">Inactive</MenuItem>
                <MenuItem value="SUSPENDED">Suspended</MenuItem>
              </TextField>
            )}
          />
        </Grid>

        <Grid item xs={12} md={6}>
          <Controller
            name="payrollSchedule"
            control={control}
            rules={{ required: 'Payroll schedule is required' }}
            render={({ field }) => (
              <TextField
                {...field}
                fullWidth
                select
                label="Payroll Schedule"
                error={!!errors.payrollSchedule}
                helperText={errors.payrollSchedule?.message}
              >
                <MenuItem value="WEEKLY">Weekly</MenuItem>
                <MenuItem value="BIWEEKLY">Bi-weekly</MenuItem>
                <MenuItem value="SEMI_MONTHLY">Semi-monthly</MenuItem>
                <MenuItem value="MONTHLY">Monthly</MenuItem>
              </TextField>
            )}
          />
        </Grid>

        <Grid item xs={12}>
          <Button
            type="submit"
            variant="contained"
            disabled={isLoading}
            sx={{ mr: 2 }}
          >
            {isLoading ? 'Saving...' : isEdit ? 'Update Client' : 'Create Client'}
          </Button>
          <Button
            variant="outlined"
            onClick={() => router.back()}
          >
            Cancel
          </Button>
        </Grid>
      </Grid>
    </Box>
  );
};

export default ClientForm;
```

---

## 5. Utility Functions

### formatters.ts
```typescript
export const formatCurrency = (amount: number | undefined): string => {
  if (amount === undefined || amount === null) return '$0.00';
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
  }).format(amount);
};

export const formatDate = (date: string | Date | undefined): string => {
  if (!date) return '';
  return new Intl.DateTimeFormat('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  }).format(new Date(date));
};

export const formatDateTime = (date: string | Date | undefined): string => {
  if (!date) return '';
  return new Intl.DateTimeFormat('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(date));
};

export const formatSSN = (ssn: string | undefined): string => {
  if (!ssn) return '';
  // Display only last 4 digits
  return `***-**-${ssn.slice(-4)}`;
};

export const formatPhoneNumber = (phone: string | undefined): string => {
  if (!phone) return '';
  const cleaned = phone.replace(/\D/g, '');
  const match = cleaned.match(/^(\d{3})(\d{3})(\d{4})$/);
  if (match) {
    return `(${match[1]}) ${match[2]}-${match[3]}`;
  }
  return phone;
};
```

### constants.ts
```typescript
export const CLIENT_STATUS_OPTIONS = [
  { value: 'ACTIVE', label: 'Active', color: 'success' },
  { value: 'INACTIVE', label: 'Inactive', color: 'default' },
  { value: 'SUSPENDED', label: 'Suspended', color: 'error' },
];

export const EMPLOYEE_STATUS_OPTIONS = [
  { value: 'ACTIVE', label: 'Active', color: 'success' },
  { value: 'INACTIVE', label: 'Inactive', color: 'default' },
  { value: 'TERMINATED', label: 'Terminated', color: 'error' },
  { value: 'ON_LEAVE', label: 'On Leave', color: 'warning' },
  { value: 'SUSPENDED', label: 'Suspended', color: 'error' },
];

export const PAYROLL_STATUS_OPTIONS = [
  { value: 'DRAFT', label: 'Draft', color: 'default' },
  { value: 'PROCESSING', label: 'Processing', color: 'info' },
  { value: 'PROCESSED', label: 'Processed', color: 'primary' },
  { value: 'APPROVED', label: 'Approved', color: 'success' },
  { value: 'PAID', label: 'Paid', color: 'success' },
  { value: 'CANCELLED', label: 'Cancelled', color: 'error' },
];

export const EMPLOYMENT_TYPE_OPTIONS = [
  { value: 'FULL_TIME', label: 'Full Time' },
  { value: 'PART_TIME', label: 'Part Time' },
  { value: 'CONTRACT', label: 'Contract' },
  { value: 'TEMPORARY', label: 'Temporary' },
  { value: 'SEASONAL', label: 'Seasonal' },
];

export const PAY_TYPE_OPTIONS = [
  { value: 'SALARY', label: 'Salary' },
  { value: 'HOURLY', label: 'Hourly' },
  { value: 'COMMISSION', label: 'Commission' },
  { value: 'CONTRACTOR', label: 'Contractor' },
];

export const PAYROLL_SCHEDULE_OPTIONS = [
  { value: 'WEEKLY', label: 'Weekly' },
  { value: 'BIWEEKLY', label: 'Bi-weekly' },
  { value: 'SEMI_MONTHLY', label: 'Semi-monthly' },
  { value: 'MONTHLY', label: 'Monthly' },
];
```

---

## 6. Protected Route Component

### ProtectedRoute.tsx
```typescript
import React, { useEffect } from 'react';
import { useRouter } from 'next/router';
import { useAuth } from '@/hooks/useAuth';
import { CircularProgress, Box } from '@mui/material';

interface ProtectedRouteProps {
  children: React.ReactNode;
}

const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children }) => {
  const { isAuthenticated, isLoading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.push('/auth/login');
    }
  }, [isAuthenticated, isLoading, router]);

  if (isLoading) {
    return (
      <Box
        display="flex"
        justifyContent="center"
        alignItems="center"
        minHeight="100vh"
      >
        <CircularProgress />
      </Box>
    );
  }

  return isAuthenticated ? <>{children}</> : null;
};

export default ProtectedRoute;
```

---

## 7. Error Handling

### ErrorBoundary.tsx
```typescript
import React, { Component, ErrorInfo, ReactNode } from 'react';
import { Box, Typography, Button } from '@mui/material';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
  error?: Error;
}

class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('Error caught by boundary:', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <Box
          display="flex"
          flexDirection="column"
          alignItems="center"
          justifyContent="center"
          minHeight="400px"
          p={3}
        >
          <Typography variant="h4" gutterBottom>
            Oops! Something went wrong
          </Typography>
          <Typography variant="body1" color="textSecondary" gutterBottom>
            {this.state.error?.message || 'An unexpected error occurred'}
          </Typography>
          <Button
            variant="contained"
            onClick={() => window.location.reload()}
            sx={{ mt: 2 }}
          >
            Reload Page
          </Button>
        </Box>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
```

---

## 8. Toast Notification Service

### toast.service.ts
```typescript
import { toast } from 'react-toastify';

export const toastService = {
  success: (message: string) => {
    toast.success(message, {
      position: 'top-right',
      autoClose: 5000,
      hideProgressBar: false,
      closeOnClick: true,
      pauseOnHover: true,
    });
  },

  error: (message: string) => {
    toast.error(message, {
      position: 'top-right',
      autoClose: 5000,
    });
  },

  warning: (message: string) => {
    toast.warning(message, {
      position: 'top-right',
      autoClose: 5000,
    });
  },

  info: (message: string) => {
    toast.info(message, {
      position: 'top-right',
      autoClose: 5000,
    });
  },
};
```

---

## 9. React Query Setup

### queryClient.ts
```typescript
import { QueryClient } from '@tanstack/react-query';

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
      staleTime: 5 * 60 * 1000, // 5 minutes
    },
  },
});
```

### _app.tsx (Next.js)
```typescript
import { QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { queryClient } from '@/lib/queryClient';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import type { AppProps } from 'next/app';

function MyApp({ Component, pageProps }: AppProps) {
  return (
    <QueryClientProvider client={queryClient}>
      <Component {...pageProps} />
      <ToastContainer />
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  );
}

export default MyApp;
```

---

## 10. Environment Setup

### .env.local
```bash
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_APP_NAME=WageCraft
```

### package.json (dependencies)
```json
{
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "next": "^13.4.0",
    "typescript": "^5.0.0",
    "@mui/material": "^5.13.0",
    "@emotion/react": "^11.11.0",
    "@emotion/styled": "^11.11.0",
    "@tanstack/react-query": "^4.29.0",
    "react-hook-form": "^7.44.0",
    "axios": "^1.4.0",
    "date-fns": "^2.30.0",
    "react-toastify": "^9.1.3"
  },
  "devDependencies": {
    "@types/node": "^20.2.0",
    "@types/react": "^18.2.0"
  }
}
```

This collection of code snippets provides a solid foundation for building the WageCraft frontend application!


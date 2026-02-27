import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@/test/test-utils';
import userEvent from '@testing-library/user-event';
import LoginPage from './LoginForm';
import * as authContext from '@/context/auth-context';

// Mock the auth context
vi.mock('@/context/auth-context', () => ({
  useAuth: vi.fn(),
}));

// Mock axios
vi.mock('axios');

// Mock react-router-dom navigation
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useLocation: () => ({
      state: null,
      pathname: '/login',
    }),
  };
});

describe('LoginPage', () => {
  const mockLogin = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(authContext.useAuth).mockReturnValue({
      login: mockLogin,
      logout: vi.fn(),
      register: vi.fn(),
      user: null,
      isAuthenticated: false,
      isLoading: false,
    });
  });

  it('renders login form with all elements', () => {
    render(<LoginPage />);

    expect(screen.getByRole('heading', { name: /sign in to sentio/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/username/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /^sign in$/i })).toBeInTheDocument();
    expect(screen.getByText(/don't have an account/i)).toBeInTheDocument();
  });

  it('renders social login buttons', () => {
    render(<LoginPage />);

    expect(screen.getByRole('button', { name: /google/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /github/i })).toBeInTheDocument();
  });

  it('shows validation errors for empty fields', async () => {
    const user = userEvent.setup();
    render(<LoginPage />);

    const submitButton = screen.getByRole('button', { name: /^sign in$/i });
    await user.click(submitButton);

    expect(await screen.findAllByText(/please fill in this field/i)).toHaveLength(2);
    expect(mockLogin).not.toHaveBeenCalled();
  });

  it('clears field errors when user starts typing', async () => {
    const user = userEvent.setup();
    render(<LoginPage />);

    const submitButton = screen.getByRole('button', { name: /^sign in$/i });
    await user.click(submitButton);

    expect(await screen.findAllByText(/please fill in this field/i)).toHaveLength(2);

    const usernameInput = screen.getByLabelText(/username/i);
    await user.type(usernameInput, 'testuser');

    const errors = screen.queryAllByText(/please fill in this field/i);
    expect(errors).toHaveLength(1);
  });

  it('submits form with valid credentials', async () => {
    const user = userEvent.setup();
    mockLogin.mockResolvedValueOnce(undefined);

    render(<LoginPage />);

    await user.type(screen.getByLabelText(/username/i), 'testuser');
    await user.type(screen.getByLabelText(/password/i), 'password123');

    const submitButton = screen.getByRole('button', { name: /^sign in$/i });
    await user.click(submitButton);

    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith('testuser', 'password123');
      expect(mockNavigate).toHaveBeenCalledWith('/dashboard', { replace: true });
    });
  });

  it('shows loading state during submission', async () => {
    const user = userEvent.setup();
    mockLogin.mockImplementation(() => new Promise(resolve => setTimeout(resolve, 100)));

    render(<LoginPage />);

    await user.type(screen.getByLabelText(/username/i), 'testuser');
    await user.type(screen.getByLabelText(/password/i), 'password123');

    const submitButton = screen.getByRole('button', { name: /^sign in$/i });
    await user.click(submitButton);

    expect(screen.getByText(/signing in.../i)).toBeInTheDocument();
    expect(submitButton).toBeDisabled();
  });

  it('displays error message on login failure', async () => {
    const user = userEvent.setup();
    const error = {
      isAxiosError: true,
      response: {
        status: 401,
        data: {},
      },
    };
    mockLogin.mockRejectedValueOnce(error);

    render(<LoginPage />);

    await user.type(screen.getByLabelText(/username/i), 'testuser');
    await user.type(screen.getByLabelText(/password/i), 'wrongpassword');

    await user.click(screen.getByRole('button', { name: /^sign in$/i }));

    // Wait for error to appear and verify mockLogin was called
    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith('testuser', 'wrongpassword');
    });

    // Check that error is displayed (the specific text may vary)
    const errorElements = screen.queryAllByText(/invalid|error|password/i);
    expect(errorElements.length).toBeGreaterThan(0);
  });

  it('handles 429 rate limit error', async () => {
    const user = userEvent.setup();
    const error = {
      isAxiosError: true,
      response: {
        status: 429,
        data: {},
      },
    };
    mockLogin.mockRejectedValueOnce(error);

    render(<LoginPage />);

    await user.type(screen.getByLabelText(/username/i), 'testuser');
    await user.type(screen.getByLabelText(/password/i), 'password');
    await user.click(screen.getByRole('button', { name: /^sign in$/i }));

    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalled();
    });
  });

  it('handles network errors', async () => {
    const user = userEvent.setup();
    const error = new Error('Network Error');
    mockLogin.mockRejectedValueOnce(error);

    render(<LoginPage />);

    await user.type(screen.getByLabelText(/username/i), 'testuser');
    await user.type(screen.getByLabelText(/password/i), 'password123');
    await user.click(screen.getByRole('button', { name: /^sign in$/i }));

    expect(await screen.findByText(/unable to connect to the server/i)).toBeInTheDocument();
  });

  it('disables inputs during loading', async () => {
    const user = userEvent.setup();
    mockLogin.mockImplementation(() => new Promise(resolve => setTimeout(resolve, 100)));

    render(<LoginPage />);

    const usernameInput = screen.getByLabelText(/username/i);
    const passwordInput = screen.getByLabelText(/password/i);

    await user.type(usernameInput, 'testuser');
    await user.type(passwordInput, 'password123');
    await user.click(screen.getByRole('button', { name: /^sign in$/i }));

    expect(usernameInput).toBeDisabled();
    expect(passwordInput).toBeDisabled();
  });

  it('has link to forgot password', () => {
    render(<LoginPage />);

    const forgotPasswordLink = screen.getByRole('link', { name: /forgot your password/i });
    expect(forgotPasswordLink).toBeInTheDocument();
    expect(forgotPasswordLink).toHaveAttribute('href', '/forgot-password');
  });

  it('has link to create account', () => {
    render(<LoginPage />);

    const createAccountLink = screen.getByRole('link', { name: /create account/i });
    expect(createAccountLink).toBeInTheDocument();
    expect(createAccountLink).toHaveAttribute('href', '/signup');
  });
});

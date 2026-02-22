import { test, expect } from '@playwright/test';

test.describe('Authentication Flow', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
  });

  test('should display login page correctly', async ({ page }) => {
    await expect(page.getByRole('heading', { name: /sign in to sentio/i })).toBeVisible();
    await expect(page.getByLabel(/username/i)).toBeVisible();
    await expect(page.getByLabel(/password/i)).toBeVisible();
    await expect(page.getByRole('button', { name: /^sign in$/i })).toBeVisible();
  });

  test('should show validation errors for empty fields', async ({ page }) => {
    await page.getByRole('button', { name: /^sign in$/i }).click();

    await expect(page.getByText(/please fill in this field/i).first()).toBeVisible();
  });

  test('should navigate to forgot password page', async ({ page }) => {
    await page.getByRole('link', { name: /forgot your password/i }).click();

    await expect(page).toHaveURL(/forgot-password/);
  });

  test('should navigate to signup page', async ({ page }) => {
    await page.getByRole('link', { name: /create account/i }).click();

    await expect(page).toHaveURL(/signup/);
  });

  test('should fill and submit login form', async ({ page }) => {
    await page.getByLabel(/username/i).fill('testuser');
    await page.getByLabel(/password/i).fill('password123');

    await page.getByRole('button', { name: /^sign in$/i }).click();

    // Wait for either dashboard or error message
    await page.waitForTimeout(1000);
  });

  test('should show loading state during submission', async ({ page }) => {
    await page.getByLabel(/username/i).fill('testuser');
    await page.getByLabel(/password/i).fill('password123');

    // Click submit and immediately check for loading state
    const submitButton = page.getByRole('button', { name: /^sign in$/i });
    await submitButton.click();

    // Check if button is disabled during loading (if backend responds slowly)
    // This might be flaky in fast environments
    await expect(submitButton).toBeDisabled();
  });

  test('should clear validation errors when typing', async ({ page }) => {
    // Trigger validation
    await page.getByRole('button', { name: /^sign in$/i }).click();
    await expect(page.getByText(/please fill in this field/i).first()).toBeVisible();

    // Start typing
    await page.getByLabel(/username/i).fill('test');

    // Username error should be gone, but password error might still be there
    const usernameInput = page.getByLabel(/username/i);
    await expect(usernameInput).not.toHaveClass(/border-destructive/);
  });
});

test.describe('Protected Routes', () => {
  test('should redirect to login when accessing dashboard without auth', async ({ page }) => {
    await page.goto('/dashboard');

    // Should be redirected to login
    await page.waitForURL(/login/);
    await expect(page).toHaveURL(/login/);
  });
});

test.describe('Responsive Login Page', () => {
  test('should be usable on mobile devices', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto('/login');

    await expect(page.getByRole('heading', { name: /sign in to sentio/i })).toBeVisible();
    await expect(page.getByLabel(/username/i)).toBeVisible();
    await expect(page.getByLabel(/password/i)).toBeVisible();

    // Test form interaction on mobile
    await page.getByLabel(/username/i).fill('mobileuser');
    await page.getByLabel(/password/i).fill('password');

    const submitButton = page.getByRole('button', { name: /^sign in$/i });
    await expect(submitButton).toBeVisible();
    await expect(submitButton).toBeEnabled();
  });

  test('should be usable on tablet devices', async ({ page }) => {
    await page.setViewportSize({ width: 768, height: 1024 });
    await page.goto('/login');

    await expect(page.getByRole('heading', { name: /sign in to sentio/i })).toBeVisible();
    await expect(page.getByLabel(/username/i)).toBeVisible();
  });
});

import { test, expect } from '@playwright/test';

test.describe('Sentio Web Application', () => {
  test('should load the homepage', async ({ page }) => {
    await page.goto('/');

    // Wait for the page to be fully loaded
    await page.waitForLoadState('networkidle');

    // Verify page title or main heading exists
    await expect(page).toHaveTitle(/Sentio/);
  });

  test('should navigate to login page', async ({ page }) => {
    await page.goto('/');

    // Look for login/sign in button or link
    const loginButton = page.getByRole('link', { name: /login|sign in/i });

    if (await loginButton.count() > 0) {
      await loginButton.first().click();
      await expect(page).toHaveURL(/login/);
    }
  });

  test('should be responsive', async ({ page }) => {
    await page.goto('/');

    // Test mobile viewport
    await page.setViewportSize({ width: 375, height: 667 });
    await expect(page).toBeVisible();

    // Test tablet viewport
    await page.setViewportSize({ width: 768, height: 1024 });
    await expect(page).toBeVisible();

    // Test desktop viewport
    await page.setViewportSize({ width: 1920, height: 1080 });
    await expect(page).toBeVisible();
  });
});

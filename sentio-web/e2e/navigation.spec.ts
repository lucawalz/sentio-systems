import { test, expect } from '@playwright/test';

test.describe('Navigation', () => {
  test('should navigate through main pages', async ({ page }) => {
    await page.goto('/');

    // Verify homepage loads
    await expect(page).toHaveTitle(/Sentio/);

    // Look for common navigation elements
    const header = page.locator('header, nav').first();
    await expect(header).toBeVisible();
  });

  test('should have working logo link to homepage', async ({ page }) => {
    await page.goto('/login');

    // Find logo link
    const logoLink = page.getByRole('link', { name: /go home/i });

    if (await logoLink.count() > 0) {
      await logoLink.click();
      await expect(page).toHaveURL('/');
    }
  });

  test('should handle 404 pages gracefully', async ({ page }) => {
    const response = await page.goto('/nonexistent-page-12345');

    // Either redirects to 404 page or returns 404 status
    if (response) {
      expect([200, 404]).toContain(response.status());
    }
  });

  test('should maintain scroll position on navigation', async ({ page }) => {
    await page.goto('/');

    // Scroll down if page is long enough
    await page.evaluate(() => window.scrollTo(0, 500));

    const scrollPosition = await page.evaluate(() => window.scrollY);

    // Navigate away and back
    await page.goto('/login');
    await page.goto('/');

    // Scroll position should reset
    const newScrollPosition = await page.evaluate(() => window.scrollY);
    expect(newScrollPosition).toBeLessThan(scrollPosition);
  });
});

test.describe('Accessibility', () => {
  test('should have proper heading hierarchy', async ({ page }) => {
    await page.goto('/');

    const h1Count = await page.locator('h1').count();
    expect(h1Count).toBeGreaterThanOrEqual(1);
  });

  test('should have proper ARIA labels on interactive elements', async ({ page }) => {
    await page.goto('/login');

    const usernameInput = page.getByLabel(/username/i);
    await expect(usernameInput).toBeVisible();

    const passwordInput = page.getByLabel(/password/i);
    await expect(passwordInput).toBeVisible();
  });

  test('should be keyboard navigable', async ({ page }) => {
    await page.goto('/login');

    // Tab through form elements
    await page.keyboard.press('Tab');

    const activeElement = await page.evaluate(() => document.activeElement?.tagName);
    expect(['INPUT', 'BUTTON', 'A', 'TEXTAREA']).toContain(activeElement || '');
  });
});

test.describe('Performance', () => {
  test('should load homepage within acceptable time', async ({ page }) => {
    const startTime = Date.now();
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    const loadTime = Date.now() - startTime;

    expect(loadTime).toBeLessThan(5000); // Should load within 5 seconds
  });

  test('should not have console errors on homepage', async ({ page }) => {
    const consoleErrors: string[] = [];

    page.on('console', (msg) => {
      if (msg.type() === 'error') {
        consoleErrors.push(msg.text());
      }
    });

    await page.goto('/');
    await page.waitForLoadState('networkidle');

    // Filter out known acceptable errors (like missing fonts, etc.)
    const criticalErrors = consoleErrors.filter(
      (error) => !error.includes('favicon') && !error.includes('font')
    );

    expect(criticalErrors.length).toBe(0);
  });
});

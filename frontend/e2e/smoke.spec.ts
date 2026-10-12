import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page } from '@playwright/test';

async function expectNoA11yViolations(page: Page) {
  const results = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa']).analyze();
  expect(results.violations.map((v) => `${v.id}: ${v.help}`)).toEqual([]);
}

test('main flow: menu -> positions -> history paging -> back -> exit', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByRole('heading', { level: 1, name: 'Portfolio Management System' })).toBeVisible();
  await expect(page.getByText('PINQUSR1')).toBeVisible();
  await expect(page.getByLabel('Select Option:')).toBeFocused();
  await expectNoA11yViolations(page);

  await page.getByLabel('Select Option:').fill('1');
  await page.keyboard.press('Enter');
  await expect(page).toHaveURL(/\/positions$/);
  await expect(page.getByLabel('Account:')).toBeFocused();
  await page.getByLabel('Account:').fill('1000000001');
  await page.keyboard.press('Enter');
  await expect(page).toHaveURL(/\/positions\/1000000001$/);
  const positions = page.getByTestId('positions-table');
  await expect(positions.getByText('IBM0000001')).toBeVisible();
  await expect(positions.getByText('65.7895')).toBeVisible();
  await expect(positions.getByText('$12,796.05')).toBeVisible();
  await expectNoA11yViolations(page);

  await page.getByRole('link', { name: /View transaction history/ }).click();
  await expect(page).toHaveURL(/\/history\/1000000001\?page=1$/);
  await expect(page.getByTestId('history-row')).toHaveCount(10);
  await expect(page.getByText('Page 1 of 2')).toBeVisible();
  await expectNoA11yViolations(page);

  await page.getByRole('button', { name: /Next/ }).click();
  await expect(page).toHaveURL(/page=2$/);
  await expect(page.getByTestId('history-row')).toHaveCount(2);
  await page.getByRole('button', { name: /Previous/ }).click();
  await expect(page.getByText('Page 1 of 2')).toBeVisible();

  await page.keyboard.press('Escape');
  await expect(page).toHaveURL(/\/$/);
  await page.getByRole('button', { name: '3. Exit' }).click();
  await expect(page.getByRole('heading', { name: 'Session ended' })).toBeVisible();
});

test('history at 360px only scrolls the table, not the page', async ({ page }) => {
  await page.setViewportSize({ width: 360, height: 740 });
  await page.goto('/history/1000000001?page=1');
  await expect(page.getByTestId('history-row')).toHaveCount(10);
  const widths = await page.evaluate(() => ({ doc: document.documentElement.scrollWidth, vw: window.innerWidth }));
  expect(widths.doc).toBeLessThanOrEqual(widths.vw);
});

test('invalid account is rejected inline', async ({ page }) => {
  await page.goto('/positions');
  await page.getByLabel('Account:').fill('0000000000');
  await page.getByRole('button', { name: 'Search' }).click();
  await expect(page.getByRole('alert')).toContainText('Invalid Account Number format');
  await expect(page.getByLabel('Account:')).toBeFocused();
  await expectNoA11yViolations(page);
});

test('empty results: position not found and no transactions', async ({ page }) => {
  await page.goto('/positions/1000000004');
  await expect(page.getByRole('alert')).toContainText('Position not found for account');
  await page.goto('/history/1000000004?page=1');
  await expect(page.getByRole('alert')).toContainText('No transactions found for account');
});

test('error paths: read error, DB unavailable, fatal, access denied, signed out', async ({ page }) => {
  await page.goto('/positions/9999999998');
  await expect(page.getByRole('alert')).toContainText('Error accessing position data');
  await expect(page.getByTestId('trace-id')).toBeVisible();

  await page.goto('/history/9999999997?page=1');
  await expect(page.getByRole('alert')).toContainText('Transaction history is temporarily unavailable');
  await expect(page.getByRole('button', { name: 'Retry' })).toBeVisible();

  await page.goto('/history/9999999999?page=1');
  await expect(page.getByRole('heading', { name: /System Error/ })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Continue' })).toBeFocused();
  await expectNoA11yViolations(page);
  await page.keyboard.press('Enter');
  await expect(page).toHaveURL(/\/$/);

  await page.goto('/?mockUser=denied');
  await expect(page.getByRole('heading', { name: 'Access denied' })).toBeVisible();

  await page.goto('/?mockUser=anon');
  await expect(page.getByRole('heading', { name: 'Sign in required' })).toBeVisible();
});

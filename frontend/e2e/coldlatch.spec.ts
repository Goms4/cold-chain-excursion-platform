import { test, expect, Page } from '@playwright/test';
import { mkdir } from 'node:fs/promises';
import path from 'node:path';

const opsPassword = process.env.COLDLATCH_OPERATIONS_PASSWORD;
const analystPassword = process.env.COLDLATCH_ANALYST_PASSWORD;
const screenshots = path.resolve('..', 'docs', 'screenshots');

async function signIn(page: Page, role: 'OPERATIONS' | 'ANALYST', password: string) {
  await page.goto('/login');
  await page.locator('#role').selectOption(role);
  await page.locator('#companyName').fill('Example Pharma');
  await page.locator('#password').fill(password);
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page).toHaveURL(/dashboard/);
}

test.describe.configure({ mode: 'serial' });

test('role/password login, generic errors, logout and browser back protection', async ({ page }) => {
  test.skip(!opsPassword || !analystPassword, 'Set COLDLATCH_OPERATIONS_PASSWORD and COLDLATCH_ANALYST_PASSWORD.');
  await page.goto('/login');
  await page.locator('#role').selectOption('OPERATIONS');
  await page.locator('#password').fill('definitely-invalid');
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page.getByRole('alert')).toHaveText('Incorrect role or password');
  await page.locator('#role').selectOption('ANALYST');
  await page.locator('#password').fill(opsPassword!);
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page.getByRole('alert')).toHaveText('Incorrect role or password');
  await signIn(page, 'OPERATIONS', opsPassword!);
  await page.getByRole('button', { name: 'Sign out' }).click();
  await expect(page).toHaveURL(/login/);
  await page.goBack();
  await expect(page).toHaveURL(/login/);
  await expect(page.getByRole('heading', { name: 'Log in to ColdLatch' })).toBeVisible();
});

test('Operations can open shipment temperature workflow and record an in-range reading', async ({ page }) => {
  test.skip(!opsPassword, 'Set COLDLATCH_OPERATIONS_PASSWORD.');
  await signIn(page, 'OPERATIONS', opsPassword!);
  await page.goto('/shipments/SHP-10001');
  await page.getByRole('button', { name: 'Add reading' }).click();
  await expect(page.getByRole('dialog')).toBeVisible();
  await page.getByLabel(/Temperature/).fill('5.2');
  await page.getByRole('button', { name: 'Save reading' }).click();
  await expect(page.getByText(/5.2°C/).first()).toBeVisible();
  await expect(page.getByText(/No temperature issue/i)).toHaveCount(0);
});

test('adding a lot refreshes the shipment list and enables temperature entry', async ({ page }) => {
  test.skip(!opsPassword, 'Set COLDLATCH_OPERATIONS_PASSWORD.');
  await signIn(page, 'OPERATIONS', opsPassword!);
  const trackingNumber = `TRK-UI-${Date.now()}`;
  const shipment = await page.evaluate(async (tracking) => {
    await fetch('/api/auth/csrf', { credentials: 'include' });
    const token = decodeURIComponent(document.cookie.split('; ').find(item => item.startsWith('XSRF-TOKEN='))?.split('=').slice(1).join('=') || '');
    const response = await fetch('/api/shipments', {
      method: 'POST', credentials: 'include', headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': token },
      body: JSON.stringify({ trackingNumber: tracking, shipmentReference: 'Lot visibility regression', origin: 'Hub A', destination: 'Clinic B', storageProfileId: 'PROFILE-REF' }),
    });
    if (!response.ok) throw new Error(`Shipment setup failed: ${response.status}`);
    return response.json();
  }, trackingNumber);
  try {
    await page.goto(`/shipments/${shipment.id}`);
    await expect(page.getByText('No lots have been registered yet.')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Add reading' })).toBeDisabled();
    await page.getByRole('button', { name: /add lot/i }).click();
    await page.getByLabel('Product').fill('Shipment detail visibility check');
    await page.getByLabel('Lot number').fill(`BATCH-${Date.now()}`);
    await page.getByLabel('Quantity').fill('1');
    await page.getByRole('button', { name: /add lot/i }).last().click();
    await expect(page.getByRole('cell', { name: 'Shipment detail visibility check' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Add reading' })).toBeEnabled();
  } finally {
    await page.evaluate(async (shipmentId) => {
      await fetch('/api/auth/csrf', { credentials: 'include' });
      const token = decodeURIComponent(document.cookie.split('; ').find(item => item.startsWith('XSRF-TOKEN='))?.split('=').slice(1).join('=') || '');
      const headers = { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': token };
      const lotsResponse = await fetch(`/api/shipments/${shipmentId}/lots`, { credentials: 'include' });
      const lots = await lotsResponse.json();
      for (const lot of lots) await fetch(`/api/lots/${lot.id}`, { method: 'DELETE', credentials: 'include', headers, body: JSON.stringify({ reason: 'Playwright regression cleanup.' }) });
      await fetch(`/api/shipments/${shipmentId}`, { method: 'DELETE', credentials: 'include', headers, body: JSON.stringify({ reason: 'Playwright regression cleanup.' }) });
    }, shipment.id);
  }
});

test('out-of-range reading changes condition once and replay reports a duplicate', async ({ page }) => {
  test.skip(!opsPassword, 'Set COLDLATCH_OPERATIONS_PASSWORD.');
  await signIn(page, 'OPERATIONS', opsPassword!);
  await page.goto('/shipments/SHP-10001');
  const selectedTime = new Date(Date.now() - 3 * 60_000);
  const local = new Date(selectedTime.getTime() - selectedTime.getTimezoneOffset() * 60_000).toISOString().slice(0, 16);
  for (let attempt = 0; attempt < 2; attempt++) {
    await page.getByRole('button', { name: 'Add reading' }).click();
    await page.getByLabel(/Lot/).selectOption({ label: /LOT-10001/ });
    await page.getByLabel(/Temperature/).fill('25');
    await page.getByLabel(/Event time/).fill(local);
    await page.getByRole('button', { name: 'Save reading' }).click();
    if (attempt === 0) await expect(page.getByText(/AT RISK|Temperature issue|Outside/i).first()).toBeVisible();
    else await expect(page.getByText(/already recorded/i).first()).toBeVisible();
  }
  const issues = await page.evaluate(async () => (await (await fetch('/api/shipments/SHP-10001/excursions', { credentials: 'include' })).json()).length);
  expect(issues).toBe(1);
});

test('Dashboard, shipment, issue and investigation data views render from service APIs', async ({ page }) => {
  test.skip(!opsPassword, 'Set COLDLATCH_OPERATIONS_PASSWORD.');
  await signIn(page, 'OPERATIONS', opsPassword!);
  for (const route of ['/dashboard', '/shipments', '/shipments/SHP-10001', '/issues', '/issues/ISS-10001', '/investigations', '/audit', '/configuration']) {
    await page.goto(route);
    await expect(page.locator('h1').first()).toBeVisible();
    await expect(page.locator('body')).not.toContainText(/undefined|null|NullPointerException|SQLException/);
  }
});

test('Dashboard attention link opens the filtered shipment list with condition reasons', async ({ page }) => {
  test.skip(!opsPassword, 'Set COLDLATCH_OPERATIONS_PASSWORD.');
  await signIn(page, 'OPERATIONS', opsPassword!);
  await page.getByRole('link', { name: /Need attention/i }).click();
  await expect(page).toHaveURL(/shipments\?filter=attention/);
  await expect(page.getByText(/AT RISK|Under review|Temperature issue/i).first()).toBeVisible();
});

test('Audit history defaults to business events and exposes grouped technical duplicates', async ({ page }) => {
  test.skip(!opsPassword, 'Set COLDLATCH_OPERATIONS_PASSWORD.');
  await signIn(page, 'OPERATIONS', opsPassword!);
  await page.goto('/audit');
  await expect(page.getByText(/Chain verified:/)).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Technical audit details' })).toHaveCount(0);
  await page.getByRole('button', { name: /Show technical events/i }).click();
  await expect(page.getByRole('heading', { name: 'Technical audit details' })).toBeVisible();
  await expect(page.getByText(/duplicate readings suppressed/i).first()).toBeVisible();
});

test('Analyst can record HOLD with a reason only and audit chain details are visible', async ({ page }) => {
  test.skip(!analystPassword, 'Set COLDLATCH_ANALYST_PASSWORD.');
  await signIn(page, 'ANALYST', analystPassword!);
  await page.goto('/investigations');
  await expect(page.getByRole('button', { name: /decision/i }).first()).toBeVisible();
  await page.getByRole('button', { name: /decision/i }).first().click();
  await page.getByLabel(/decision/i).selectOption('HOLD');
  await page.getByLabel(/reason/i).fill('Keep the affected lot on hold pending stability review.');
  await page.getByRole('button', { name: /record decision|save decision/i }).click();
  await expect(page.getByText(/SQL|Exception|stack trace/i)).toHaveCount(0);
  await page.goto('/audit');
  await expect(page.getByText(/Chain verified:/)).toBeVisible();
  await expect(page.getByRole('button', { name: /Show technical events/i })).toBeVisible();
});

test('route and modal screenshots at all configured widths', async ({ page }, testInfo) => {
  test.skip(!opsPassword, 'Set COLDLATCH_OPERATIONS_PASSWORD.');
  await mkdir(screenshots, { recursive: true });
  await page.goto('/login');
  await page.screenshot({ path: path.join(screenshots, `${testInfo.project.name}-login.png`), fullPage: true });
  await signIn(page, 'OPERATIONS', opsPassword!);
  const pages = [
    ['/dashboard', 'dashboard'], ['/shipments', 'shipments'], ['/shipments/SHP-10001', 'shipment-detail'],
    ['/issues', 'temperature-issues'], ['/issues/ISS-10001', 'incident-detail'], ['/investigations', 'investigations'],
    ['/audit', 'audit-history'], ['/configuration', 'configuration'],
  ] as const;
  for (const [route, name] of pages) {
    await page.goto(route);
    await page.locator('h1').first().waitFor();
    await page.screenshot({ path: path.join(screenshots, `${testInfo.project.name}-${name}.png`), fullPage: true });
    const overflow = await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth);
    expect(overflow, `${name} horizontal document overflow at ${testInfo.project.name}`).toBe(false);
  }
  await page.goto('/shipments');
  await page.getByRole('button', { name: /register shipment/i }).click();
  await expect(page.getByRole('dialog')).toBeVisible();
  await page.screenshot({ path: path.join(screenshots, `${testInfo.project.name}-modal-register-shipment.png`), fullPage: true });
  await page.getByRole('button', { name: /cancel/i }).click();
  await page.goto('/shipments/SHP-10001');
  await page.getByRole('button', { name: 'Add reading' }).click();
  await page.screenshot({ path: path.join(screenshots, `${testInfo.project.name}-modal-add-reading.png`), fullPage: true });
  await page.getByRole('button', { name: /cancel/i }).click();
  await page.goto('/investigations');
  const evidenceButton = page.getByRole('button', { name: /add evidence/i }).first();
  if (await evidenceButton.isVisible()) {
    await evidenceButton.click();
    await page.screenshot({ path: path.join(screenshots, `${testInfo.project.name}-modal-add-evidence.png`), fullPage: true });
    await page.getByRole('button', { name: /cancel/i }).click();
  }
  await page.goto('/shipments/SHP-10001');
  const edit = page.getByRole('button', { name: /edit shipment/i });
  if (await edit.isVisible()) {
    await edit.click();
    await page.screenshot({ path: path.join(screenshots, `${testInfo.project.name}-modal-edit-shipment.png`), fullPage: true });
    await page.getByRole('button', { name: /cancel/i }).click();
  }
  const addLot = page.getByRole('button', { name: /add lot/i });
  if (await addLot.isVisible()) {
    await addLot.click();
    await page.screenshot({ path: path.join(screenshots, `${testInfo.project.name}-modal-add-lot.png`), fullPage: true });
    await page.getByRole('button', { name: /cancel/i }).click();
  }
  if (analystPassword) {
    const analyst = await page.context().newPage();
    await signIn(analyst, 'ANALYST', analystPassword);
    await analyst.goto('/investigations');
    const decide = analyst.getByRole('button', { name: /decision/i }).first();
    if (await decide.isVisible()) {
      await decide.click();
      await analyst.screenshot({ path: path.join(screenshots, `${testInfo.project.name}-modal-decision.png`), fullPage: true });
    }
    await analyst.close();
  }
});

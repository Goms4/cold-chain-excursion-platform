import { defineConfig } from '@playwright/test';

const widths = [1440, 1280, 1024, 768];

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  retries: 0,
  reporter: 'list',
  use: {
    baseURL: 'http://localhost:3000',
    browserName: 'chromium',
    channel: 'chrome',
    headless: true,
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
  },
  outputDir: '../docs/screenshots/test-results',
  projects: widths.map(width => ({
    name: `chrome-${width}`,
    use: { viewport: { width, height: 1000 } },
  })),
});

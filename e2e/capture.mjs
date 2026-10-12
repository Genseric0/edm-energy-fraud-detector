import { chromium } from 'playwright';
import { mkdir } from 'node:fs/promises';
import assert from 'node:assert/strict';

const browser = await chromium.launch({ headless: true });
const page = await browser.newPage({ viewport: { width: 1440, height: 900 }, deviceScaleFactor: 1 });
await mkdir('artifacts/screenshots', { recursive: true });
try {
  await page.goto('http://127.0.0.1:3000', { waitUntil: 'networkidle' });
  await page.getByText('Auditoria Energética', { exact: true }).waitFor();
  await page.screenshot({ path: 'artifacts/screenshots/01-login.png', fullPage: true });

  await page.locator('input[type="text"]').fill('genserico');
  await page.locator('input[type="password"]').fill(process.env.EDM_ADMIN_PASSWORD);
  await page.getByRole('button', { name: /Acessar Dashboard/i }).click();
  await page.getByText('Cobertura e limites da detecção').waitFor({ timeout: 30000 });
  await page.getByText('Total Fornecido').waitFor({ timeout: 30000 });
  await page.screenshot({ path: 'artifacts/screenshots/02-dashboard.png', fullPage: true });

  const token = await page.evaluate(() => localStorage.getItem('gridaudit_jwt_token'));
  assert.ok(token, 'Token de autenticação em falta');
  const summary = await page.request.get('http://127.0.0.1:3000/api/energy/summary', {
    headers: { Authorization: 'Bearer ' + token },
  });
  assert.equal(summary.status(), 200, 'API de resumo falhou');
  const transformers = await page.request.get('http://127.0.0.1:3000/api/energy/transformers', {
    headers: { Authorization: 'Bearer ' + token },
  });
  assert.equal(transformers.status(), 200);
  assert.ok((await transformers.json()).length > 0, 'Não existem transformadores de demonstração');
  console.log('Login, dashboard e dados de transformadores verificados.');
} finally {
  await browser.close();
}

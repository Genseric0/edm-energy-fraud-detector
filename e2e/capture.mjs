import { chromium } from 'playwright';
import { mkdir } from 'node:fs/promises';
import assert from 'node:assert/strict';

const browser = await chromium.launch({ headless: true });
const page = await browser.newPage({ viewport: { width: 1440, height: 900 }, deviceScaleFactor: 1 });
await mkdir('artifacts/screenshots', { recursive: true });
try {
  const pageErrors = [];
  page.on('console', msg => { if (msg.type() === 'error') console.error('Browser console:', msg.text()); });
  page.on('pageerror', error => pageErrors.push(error.message));
  await page.goto('http://127.0.0.1:3000', { waitUntil: 'networkidle' });
  await page.getByText('Auditoria Energética', { exact: true }).waitFor();
  await page.screenshot({ path: 'artifacts/screenshots/01-login.png', fullPage: true });

  await page.locator('input[type="text"]').fill('genserico');
  await page.locator('input[type="password"]').fill(process.env.EDM_GENSERICO_PASSWORD);
  const loginResponse = page.waitForResponse(response => response.url().endsWith('/api/auth/login') && response.request().method() === 'POST');
  await page.getByRole('button', { name: /Acessar Dashboard/i }).click();
  const { token } = await (await loginResponse).json();
  try {
    await page.getByText('Cobertura e limites da detecção').waitFor({ timeout: 30000 });
  } catch (error) {
    await page.screenshot({ path: 'artifacts/screenshots/login-diagnostico.png', fullPage: true });
    console.error('Texto visível após login:', (await page.locator('body').innerText()).slice(0, 1500));
    console.error('URL após login:', page.url());
    throw error;
  }
  await page.getByText('Total Fornecido').waitFor({ timeout: 30000 });
  await page.getByText('Projecto independente, sem afiliação à EDM', { exact: false }).waitFor();
  const capabilities = await page.request.get('http://127.0.0.1:3000/api/energy/capabilities', {
    headers: { Authorization: 'Bearer ' + token },
  });
  assert.equal((await capabilities.json()).demoEnabled, false);
  assert.equal(await page.locator('button[title^="Simular"]').count(), 0,
    'A tabela não deve oferecer simulação fora do perfil demo');
  await page.mouse.move(0, 0);
  await page.screenshot({ path: 'artifacts/screenshots/02-dashboard.png', fullPage: true });

  assert.equal(await page.evaluate(() => localStorage.getItem('gridaudit_jwt_token')), null, 'Token não deve persistir no browser');
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
  await page.setViewportSize({ width: 390, height: 844 });
  await page.screenshot({ path: 'artifacts/screenshots/03-dashboard-mobile.png', fullPage: true });
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.getByRole('button', { name: 'Medidores', exact: true }).first().click();
  await page.locator('#btn-export-audit-pdf').waitFor({ timeout: 30000 });
  await page.getByText('Limiar demonstrativo:', { exact: false }).waitFor();
  assert.equal(await page.getByText('Margem legal tolerada:', { exact: false }).count(), 0);
  await page.mouse.move(0, 0);
  assert.equal(await page.locator('button[title^="Simular"]').count(), 0);
  await page.screenshot({ path: 'artifacts/screenshots/04-transformer-detail.png', fullPage: true });
  const downloadPromise = page.waitForEvent('download');
  await page.locator('#btn-export-audit-pdf').click();
  const download = await downloadPromise;
  assert.match(download.suggestedFilename(), /^GridAudit_Demonstracao_.*\.pdf$/);
  await download.saveAs('artifacts/demo-report.pdf');
  await page.getByRole('button', { name: 'Alterar password', exact: true }).click();
  const passwordDialog = page.getByRole('dialog');
  await passwordDialog.waitFor();
  await page.screenshot({ path: 'artifacts/screenshots/05-password-desktop.png', fullPage: true });
  await page.setViewportSize({ width: 390, height: 844 });
  await page.screenshot({ path: 'artifacts/screenshots/06-password-mobile.png', fullPage: true });
  await page.setViewportSize({ width: 1440, height: 900 });
  const rotatedPassword = 'Rotated-' + process.env.EDM_GENSERICO_PASSWORD;
  await passwordDialog.getByLabel('Password actual', { exact: true }).fill(process.env.EDM_GENSERICO_PASSWORD);
  await passwordDialog.getByLabel('Nova password', { exact: true }).fill(rotatedPassword);
  await passwordDialog.getByLabel('Confirmar nova password', { exact: true }).fill(rotatedPassword);
  await passwordDialog.getByRole('button', { name: 'Guardar password', exact: true }).click();
  await page.getByText('Auditoria Energética', { exact: true }).waitFor();
  const revoked = await page.request.get('http://127.0.0.1:3000/api/auth/me', {
    headers: { Authorization: 'Bearer ' + token },
  });
  assert.equal(revoked.status(), 401, 'Token anterior deve ser revogado após mudar password');
  const oldLogin = await page.request.post('http://127.0.0.1:3000/api/auth/login', {
    data: { username: 'genserico', password: process.env.EDM_GENSERICO_PASSWORD },
  });
  assert.equal(oldLogin.status(), 401, 'Password anterior deve ser recusada');
  await page.locator('input[type="text"]').fill('genserico');
  await page.locator('input[type="password"]').fill(rotatedPassword);
  await page.getByRole('button', { name: /Acessar Dashboard/i }).click();
  await page.getByText('Total Fornecido').waitFor();
  assert.deepEqual(pageErrors, [], 'Excepções JavaScript na interface');
  console.log('Login, persistência, dashboard, PDF, mudança de password e revogação verificados.');
} finally {
  await browser.close();
}

import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';

const defaultApiBaseUrl = 'http://localhost:8083';
const configuredApiBaseUrl = (process.env.FINFLOW_API_BASE_URL || '').trim();
const apiBaseUrl = (configuredApiBaseUrl || defaultApiBaseUrl).replace(/\/+$/, '');
const isVercelBuild = process.env.VERCEL === '1' || Boolean(process.env.VERCEL_ENV);
const configPath = resolve('public/config.js');

if (isVercelBuild && !configuredApiBaseUrl) {
  console.warn(
    'FINFLOW_API_BASE_URL is not set. The deployed app will use http://localhost:8083 and cannot reach the backend from Vercel.',
  );
}

mkdirSync(dirname(configPath), { recursive: true });
writeFileSync(
  configPath,
  `window.__FINFLOW_CONFIG__ = window.__FINFLOW_CONFIG__ || {\n  apiBaseUrl: ${JSON.stringify(apiBaseUrl)},\n};\n`,
);

console.log(`Using API base URL: ${apiBaseUrl}`);

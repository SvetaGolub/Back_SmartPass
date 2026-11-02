const assert = require('node:assert/strict');
const fs = require('node:fs');
const http = require('node:http');
const os = require('node:os');
const path = require('node:path');
const { after, before, test } = require('node:test');

const { createServer } = require('../server');

const frontendDir = fs.mkdtempSync(path.join(os.tmpdir(), 'smartpass-frontend-'));
const upstream = http.createServer((req, res) => {
  res.writeHead(200, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify({
    method: req.method,
    url: req.url,
    authorization: req.headers.authorization,
  }));
});

let server;
let serverInstance;
let baseUrl;
let upstreamUrl;

function listen(instance) {
  return new Promise((resolve, reject) => {
    instance.once('error', reject);
    instance.listen(0, '127.0.0.1', () => {
      instance.removeListener('error', reject);
      resolve(instance.address());
    });
  });
}

function close(instance) {
  return new Promise((resolve, reject) => {
    instance.close(error => error ? reject(error) : resolve());
  });
}

before(async () => {
  fs.mkdirSync(path.join(frontendDir, 'pages'));
  fs.writeFileSync(path.join(frontendDir, 'pages', 'login.html'), '<h1>ZooPass login</h1>');
  fs.writeFileSync(path.join(frontendDir, 'index.html'), '<h1>Frontend asset</h1>');
  const upstreamAddress = await listen(upstream);
  upstreamUrl = new URL(`http://127.0.0.1:${upstreamAddress.port}`);
  serverInstance = createServer({ frontendDir, apiTarget: upstreamUrl });
  const address = await listen(serverInstance);
  baseUrl = `http://127.0.0.1:${address.port}`;
  server = serverInstance;
});

after(async () => {
  await Promise.all([close(upstream), close(server)]);
  fs.rmSync(frontendDir, { recursive: true, force: true });
});

test('serves the login page from the site root', async () => {
  const response = await fetch(baseUrl);
  assert.equal(response.status, 200);
  assert.match(response.headers.get('content-type'), /text\/html/);
  assert.match(await response.text(), /ZooPass/);
});

test('returns the local API health status', async () => {
  const response = await fetch(`${baseUrl}/api/health`);
  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), { status: 'ok' });
});

test('proxies API methods, query strings, and authorization headers', async () => {
  const response = await fetch(`${baseUrl}/api/v1/users?search=alice`, {
    headers: { Authorization: 'Basic dGVzdDp0ZXN0' },
  });
  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), {
    method: 'GET',
    url: '/api/v1/users?search=alice',
    authorization: 'Basic dGVzdDp0ZXN0',
  });
});

test('returns 404 for missing frontend assets', async () => {
  const response = await fetch(`${baseUrl}/missing.css`);
  assert.equal(response.status, 404);
});

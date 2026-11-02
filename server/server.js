const fs = require('node:fs');
const http = require('node:http');
const https = require('node:https');
const path = require('node:path');

const MIME_TYPES = {
  '.css': 'text/css; charset=utf-8',
  '.html': 'text/html; charset=utf-8',
  '.ico': 'image/x-icon',
  '.js': 'text/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.svg': 'image/svg+xml',
  '.webp': 'image/webp',
};

function getConfig(env = process.env) {
  const port = Number(env.PORT ?? 3000);
  if (!Number.isInteger(port) || port < 0 || port > 65535) {
    throw new Error('PORT must be an integer between 0 and 65535.');
  }

  const apiTarget = new URL(env.API_TARGET ?? 'http://127.0.0.1:8080');
  if (!['http:', 'https:'].includes(apiTarget.protocol)) {
    throw new Error('API_TARGET must use the http or https protocol.');
  }

  return {
    host: env.HOST ?? '127.0.0.1',
    port,
    apiTarget,
    frontendDir: path.resolve(env.FRONTEND_DIR ?? path.join(__dirname, '..', 'Frontend')),
  };
}

function sendText(res, statusCode, message) {
  res.writeHead(statusCode, {
    'Content-Type': 'text/plain; charset=utf-8',
    'Content-Length': Buffer.byteLength(message),
  });
  res.end(message);
}

function proxyApiRequest(req, res, target) {
  const transport = target.protocol === 'https:' ? https : http;
  const upstreamUrl = new URL(req.url, target.origin);
  const headers = {
    ...req.headers,
    host: upstreamUrl.host,
    'x-forwarded-host': req.headers.host ?? '',
    'x-forwarded-proto': 'http',
  };

  if (req.socket.remoteAddress) {
    headers['x-forwarded-for'] = [
      req.headers['x-forwarded-for'],
      req.socket.remoteAddress,
    ].filter(Boolean).join(', ');
  }

  const upstream = transport.request(upstreamUrl, {
    method: req.method,
    headers,
  }, upstreamRes => {
    res.writeHead(upstreamRes.statusCode ?? 502, upstreamRes.headers);
    upstreamRes.pipe(res);
  });

  upstream.on('error', error => {
    console.error(`API proxy error: ${error.message}`);
    if (!res.headersSent) {
      res.writeHead(502, { 'Content-Type': 'application/json; charset=utf-8' });
      res.end(JSON.stringify({ error: 'API server is unavailable.' }));
    } else {
      res.destroy(error);
    }
  });

  req.pipe(upstream);
}

async function serveStatic(req, res, frontendDir, pathname) {
  let decodedPath;
  try {
    decodedPath = decodeURIComponent(pathname);
  } catch {
    sendText(res, 400, 'Bad Request');
    return;
  }

  const relativePath = decodedPath === '/' ? 'pages/login.html' : decodedPath.slice(1);
  const filePath = path.resolve(frontendDir, relativePath);
  const relativeToRoot = path.relative(frontendDir, filePath);
  if (relativeToRoot.startsWith(`..${path.sep}`) || relativeToRoot === '..' || path.isAbsolute(relativeToRoot)) {
    sendText(res, 403, 'Forbidden');
    return;
  }

  let fileInfo;
  try {
    fileInfo = await fs.promises.stat(filePath);
  } catch (error) {
    if (error.code === 'ENOENT' || error.code === 'ENOTDIR') {
      sendText(res, 404, 'Not Found');
    } else {
      console.error(`Static file error: ${error.message}`);
      sendText(res, 500, 'Internal Server Error');
    }
    return;
  }

  if (!fileInfo.isFile()) {
    sendText(res, 404, 'Not Found');
    return;
  }

  res.writeHead(200, {
    'Content-Type': MIME_TYPES[path.extname(filePath).toLowerCase()] ?? 'application/octet-stream',
    'Content-Length': fileInfo.size,
    'X-Content-Type-Options': 'nosniff',
  });

  if (req.method === 'HEAD') {
    res.end();
    return;
  }

  const stream = fs.createReadStream(filePath);
  stream.on('error', error => {
    console.error(`Static file error: ${error.message}`);
    res.destroy(error);
  });
  stream.pipe(res);
}

function createServer(config = getConfig()) {
  return http.createServer((req, res) => {
    let pathname;
    try {
      pathname = new URL(req.url, 'http://localhost').pathname;
    } catch {
      sendText(res, 400, 'Bad Request');
      return;
    }

    if (pathname === '/api/health') {
      if (req.method !== 'GET' && req.method !== 'HEAD') {
        res.setHeader('Allow', 'GET, HEAD');
        sendText(res, 405, 'Method Not Allowed');
        return;
      }

      const body = JSON.stringify({ status: 'ok' });
      res.writeHead(200, {
        'Content-Type': 'application/json; charset=utf-8',
        'Content-Length': Buffer.byteLength(body),
      });
      res.end(req.method === 'HEAD' ? undefined : body);
      return;
    }

    if (pathname === '/api' || pathname.startsWith('/api/')) {
      proxyApiRequest(req, res, config.apiTarget);
      return;
    }

    if (req.method !== 'GET' && req.method !== 'HEAD') {
      res.setHeader('Allow', 'GET, HEAD');
      sendText(res, 405, 'Method Not Allowed');
      return;
    }

    void serveStatic(req, res, config.frontendDir, pathname);
  });
}

if (require.main === module) {
  const config = getConfig();
  const server = createServer(config);
  server.listen(config.port, config.host, () => {
    console.log(`SmartPass server listening at http://${config.host}:${server.address().port}`);
    console.log(`Serving frontend from ${config.frontendDir}`);
    console.log(`Proxying API requests to ${config.apiTarget.origin}`);
  });
  server.on('error', error => {
    console.error(`Unable to start server: ${error.message}`);
    process.exitCode = 1;
  });
}

module.exports = { createServer, getConfig };

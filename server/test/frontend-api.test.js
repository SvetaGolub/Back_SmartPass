const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { TextEncoder } = require('node:util');
const vm = require('node:vm');
const { test } = require('node:test');

const source = fs.readFileSync(path.join(__dirname, '..', '..', 'Frontend', 'js', 'api.js'), 'utf8');

function createApi(fetch) {
  const values = new Map();
  const sessionStorage = {
    getItem: key => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, value),
    removeItem: key => values.delete(key),
  };
  const context = vm.createContext({ btoa, fetch, sessionStorage, TextEncoder });
  return {
    api: vm.runInContext(`${source}\nApi`, context),
    sessionStorage,
  };
}

function jsonResponse(status, value) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => value,
    text: async () => JSON.stringify(value),
  };
}

test('authenticates against the secured users API and uses the matching v1 route', async () => {
  const requests = [];
  const { api, sessionStorage } = createApi(async (url, options) => {
    requests.push({ url, options });
    return jsonResponse(200, { roleName: 'ADMIN' });
  });

  await api.login('operator', 'test-password');
  assert.equal(requests[0].url, '/api/v1/users/me');
  assert.equal(requests[0].options.headers.Authorization, `Basic ${btoa('operator:test-password')}`);
  assert.ok(sessionStorage.getItem('smartpass.authorization'));
  assert.equal(api.currentRole(), 'ADMIN');
});

test('clears credentials when authentication fails', async () => {
  const { api, sessionStorage } = createApi(async () => jsonResponse(401, {}));

  await assert.rejects(api.login('operator', 'wrong-password'), /Неверный логин или пароль/);
  assert.equal(sessionStorage.getItem('smartpass.authorization'), null);
});

test('loads all API pages and maps backend user DTOs for the table', async () => {
  const requests = [];
  const { api } = createApi(async url => {
    requests.push(url);
    if (url.includes('/access-zones')) {
      return jsonResponse(200, [{ id: 2, name: 'Проходная' }]);
    }
    if (url.includes('/attendance')) {
      return jsonResponse(200, [{ userId: 8, date: '2026-11-02', entry: '09:00', exit: null }]);
    }
    if (url.includes('page=0')) {
      return jsonResponse(200, {
        content: [{
          id: 8, firstName: 'Иван', lastName: 'Иванов', middleName: null,
          fullName: 'Иванов Иван', numberPhone: '+70000000000', email: 'ivanov@example.com',
          address: 'Дом', birthday: '1990-01-02T00:00:00', roleId: 2,
          position: 'Охрана', roleName: 'SECURITY', zoneId: 2, status: 'ACTIVE',
        }],
        totalPages: 2,
      });
    }
    return jsonResponse(200, {
      content: [{
        id: 9, firstName: 'Петр', lastName: 'Петров', middleName: null,
        fullName: 'Петров Петр', numberPhone: '+71111111111', email: 'petrov@example.com',
        address: 'Дом', birthday: '1980-03-04T00:00:00', roleId: 1,
        position: 'Инженер', roleName: 'EMPLOYEE', zoneId: 1, status: 'BLOCKED',
      }],
      totalPages: 2,
    });
  });

  const users = await api.getEmployees();
  assert.deepEqual(requests.filter(url => url.includes('/users?')), [
    '/api/v1/users?page=0&size=100&sortBy=lastName',
    '/api/v1/users?page=1&size=100&sortBy=lastName',
  ]);
  assert.ok(requests.some(url => url.startsWith('/api/v1/access-zones')));
  assert.ok(requests.some(url => url.startsWith('/api/v1/attendance?date=')));
  assert.equal(users.length, 2);
  assert.deepEqual(JSON.parse(JSON.stringify(users[0])), {
    id: 8,
    lastName: 'Иванов',
    firstName: 'Иван',
    middleName: '',
    position: 'Охрана',
    phone: '+70000000000',
    email: 'ivanov@example.com',
    address: 'Дом',
    birthDate: '1990-01-02',
    roleId: 2,
    role: 'SECURITY',
    zoneId: 2,
    zoneName: 'Проходная',
    zones: ['Проходная'],
    status: 'ACTIVE',
    timeIn: '09:00',
    timeOut: null,
  });
});

test('maps all employee, message, attendance and meeting actions to existing API contracts', async () => {
  const requests = [];
  const { api, sessionStorage } = createApi(async (url, options) => {
    requests.push({ url, options });
    return options.method === 'DELETE'
      ? jsonResponse(204, null)
      : jsonResponse(200, []);
  });
  sessionStorage.setItem('smartpass.authorization', 'Basic dGVzdDp0ZXN0');

  await api.getRoles();
  await api.getAccessZones();
  await api.createEmployee({
    roleId: 1, zoneId: 2, firstName: 'Иван', lastName: 'Иванов',
    numberPhone: '+70000000000', email: 'ivanov@example.com',
    position: 'Инженер', address: 'Дом',
    birthday: '1990-01-02T00:00:00', stage: '2020-01-01T00:00:00',
    login: 'ivanov', password: 'secure-password',
  });
  await api.updateEmployee(8, {
    roleId: 1, zoneId: '2', firstName: 'Иван', lastName: 'Иванов',
    middleName: '', phone: '+70000000000', email: 'ivanov@example.com',
    position: 'Инженер', address: 'Дом', birthDate: '1990-01-02',
  });
  await api.getMessages(8);
  await api.sendMessage(8, 'Уведомление');
  await api.getAccessLog(8, 2026, 11);
  await api.saveAccessRecord(8, '2026-11-02', '09:00', null);
  await api.deleteAccessRecord(8, '2026-11-02');
  await api.getMeetings(2026, 11);
  await api.addMeeting({ title: 'Планёрка', date: '2026-11-02', time: '10:00', note: '' });
  await api.updateMeeting(3, { title: 'Планёрка', date: '2026-11-02', time: '10:00', note: '' });
  await api.deleteMeeting(3);

  assert.deepEqual(requests.map(({ url, options }) => [options.method, url]), [
    ['GET', '/api/v1/roles'],
    ['GET', '/api/v1/access-zones'],
    ['POST', '/api/v1/users'],
    ['PUT', '/api/v1/users/8'],
    ['GET', '/api/v1/users/8/messages'],
    ['POST', '/api/v1/users/8/messages'],
    ['GET', '/api/v1/users/8/attendance?year=2026&month=11'],
    ['PUT', '/api/v1/users/8/attendance/2026-11-02'],
    ['DELETE', '/api/v1/users/8/attendance/2026-11-02'],
    ['GET', '/api/v1/meetings?year=2026&month=11'],
    ['POST', '/api/v1/meetings'],
    ['PUT', '/api/v1/meetings/3'],
    ['DELETE', '/api/v1/meetings/3'],
  ]);
  assert.equal(requests[3].options.body, JSON.stringify({
    roleId: 1, zoneId: 2, firstName: 'Иван', lastName: 'Иванов', middleName: '',
    numberPhone: '+70000000000', email: 'ivanov@example.com', position: 'Инженер',
    address: 'Дом', birthday: '1990-01-02T00:00:00',
  }));
  assert.equal(requests[5].options.body, JSON.stringify({ text: 'Уведомление' }));
  assert.equal(requests[7].options.body, JSON.stringify({ entry: '09:00', exit: null }));
});

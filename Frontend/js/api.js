const API_CONFIG = {
  BASE_URL: '/api',
};

const Api = (() => {
  const AUTH_KEY = 'smartpass.authorization';
  const ROLE_KEY = 'smartpass.role';
  const pad = n => String(n).padStart(2, '0');
  const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const localDate = date => `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;

  function encodeBasicCredentials(username, password) {
    const bytes = new TextEncoder().encode(`${username}:${password}`);
    let binary = '';
    bytes.forEach(byte => { binary += String.fromCharCode(byte); });
    return `Basic ${btoa(binary)}`;
  }

  function fio(e, full = false) {
    if (full) return [e.lastName, e.firstName, e.middleName].filter(Boolean).join(' ');
    const f = (e.firstName || '')[0], m = (e.middleName || '')[0];
    return `${e.lastName} ${f ? f + '.' : ''}${m ? m + '.' : ''}`;
  }

  async function request(method, path, body) {
    const headers = {};
    const authorization = sessionStorage.getItem(AUTH_KEY);
    if (authorization) headers.Authorization = authorization;
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    const res = await fetch(API_CONFIG.BASE_URL + path, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
    if (!res.ok) {
      const bodyText = await res.text();
      let detail = '';
      try {
        const body = JSON.parse(bodyText);
        detail = body.detail || body.error || Object.values(body.errors || {}).join('; ');
      } catch {
        detail = bodyText;
      }
      const message = res.status === 401 ? 'Неверный логин или пароль'
        : res.status === 403 ? `Доступ запрещён: ${method} ${path} (HTTP 403)`
          : detail || `Ошибка сервера ${res.status}`;
      if (res.status === 401) {
        sessionStorage.removeItem(AUTH_KEY);
        sessionStorage.removeItem(ROLE_KEY);
      }
      throw new Error(message);
    }
    if (res.status === 204) return null;
    return res.json();
  }

  async function login(username, password) {
    sessionStorage.setItem(AUTH_KEY, encodeBasicCredentials(username, password));
    try {
      const user = await request('GET', '/v1/users/me');
      sessionStorage.setItem(ROLE_KEY, user.roleName);
      return user;
    } catch (error) {
      sessionStorage.removeItem(AUTH_KEY);
      sessionStorage.removeItem(ROLE_KEY);
      throw error;
    }
  }

  function dateTime(date) {
    return date ? `${date}T00:00:00` : null;
  }

  async function getAllUsers() {
    const [firstPage, zones, attendance] = await Promise.all([
      request('GET', '/v1/users?page=0&size=100&sortBy=lastName'),
      request('GET', '/v1/access-zones'),
      request('GET', `/v1/attendance?date=${localDate(new Date())}`),
    ]);
    const users = [...firstPage.content];
    for (let page = 1; page < firstPage.totalPages; page++) {
      const result = await request('GET', `/v1/users?page=${page}&size=100&sortBy=lastName`);
      users.push(...result.content);
    }
    const zonesById = new Map(zones.map(zone => [zone.id, zone]));
    const attendanceByUser = new Map(attendance.map(record => [record.userId, record]));
    return users.map(user => {
      const zone = zonesById.get(user.zoneId);
      const todayAttendance = attendanceByUser.get(user.id);
      return {
        id: user.id,
        firstName: user.firstName,
        lastName: user.lastName,
        middleName: user.middleName || '',
        position: user.position,
        phone: user.numberPhone,
        email: user.email,
        address: user.address,
        birthDate: user.birthday?.slice(0, 10),
        roleId: user.roleId,
        role: user.roleName,
        zoneId: user.zoneId,
        zoneName: zone?.name || String(user.zoneId),
        zones: zone ? [zone.name] : [],
        status: user.status,
        timeIn: todayAttendance?.entry || null,
        timeOut: todayAttendance?.exit || null,
      };
    });
  }

  function monthQuery(year, month) {
    return `year=${encodeURIComponent(year)}&month=${encodeURIComponent(month)}`;
  }

  return {
    esc, fio, login, monthQuery,

    logout() {
      sessionStorage.removeItem(AUTH_KEY);
      sessionStorage.removeItem(ROLE_KEY);
    },

    currentRole() {
      return sessionStorage.getItem(ROLE_KEY);
    },

    async getEmployees() {
      return getAllUsers();
    },

    async getRoles() {
      return request('GET', '/v1/roles');
    },

    async getAccessZones() {
      return request('GET', '/v1/access-zones');
    },

    async createEmployee(data) {
      return request('POST', '/v1/users', {
        roleId: data.roleId,
        zoneId: data.zoneId,
        firstName: data.firstName,
        lastName: data.lastName,
        middleName: data.middleName,
        numberPhone: data.phone,
        email: data.email,
        position: data.position,
        address: data.address,
        birthday: data.birthday,
        stage: data.stage,
        login: data.login,
        password: data.password,
      });
    },

    async updateEmployee(id, data) {
      return request('PUT', `/v1/users/${id}`, {
        roleId: data.roleId,
        zoneId: Number(data.zoneId),
        firstName: data.firstName,
        lastName: data.lastName,
        middleName: data.middleName,
        numberPhone: data.phone,
        email: data.email,
        position: data.position,
        address: data.address,
        birthday: dateTime(data.birthDate),
      });
    },

    async getMessages(employeeId) {
      return request('GET', `/v1/users/${employeeId}/messages`);
    },

    async sendMessage(employeeId, text) {
      return request('POST', `/v1/users/${employeeId}/messages`, { text });
    },

    async getAccessLog(employeeId, year, month) {
      return request('GET', `/v1/users/${employeeId}/attendance?${monthQuery(year, month)}`);
    },

    async saveAccessRecord(employeeId, date, entry, exit) {
      return request('PUT', `/v1/users/${employeeId}/attendance/${date}`, { entry, exit });
    },

    async deleteAccessRecord(employeeId, date) {
      return request('DELETE', `/v1/users/${employeeId}/attendance/${date}`);
    },

    async getMeetings(year, month) {
      return request('GET', `/v1/meetings?${monthQuery(year, month)}`);
    },

    async addMeeting(meeting) {
      return request('POST', '/v1/meetings', meeting);
    },

    async updateMeeting(id, meeting) {
      return request('PUT', `/v1/meetings/${id}`, meeting);
    },

    async deleteMeeting(id) {
      return request('DELETE', `/v1/meetings/${id}`);
    },
  };
})();

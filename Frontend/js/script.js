document.addEventListener('DOMContentLoaded', async () => {
  const $ = id => document.getElementById(id);
  const searchInput = $('searchInput');
  const clearSearchBtn = $('clearSearch');
  const searchResultStats = $('searchResultStats');
  const userBody = $('employeesTbody');
  const userBadge = $('mainBadge');
  const pagination = $('pagination');
  const toast = $('toast');
  const isAdmin = Api.currentRole() === 'ADMIN';

  if (!Api.currentRole()) {
    window.location.replace('/');
    return;
  }

  const PAGE_SIZE = 10;
  const esc = Api.esc;
  let users = [];
  let page = 1;

  function showToast(message) {
    toast.textContent = message;
    toast.classList.add('show');
    setTimeout(() => toast.classList.remove('show'), 2600);
  }

  function timeCell(user, which) {
    const time = which === 'in' ? user.timeIn : user.timeOut;
    if (!isAdmin) return `<td class="time-cell">${esc(time || '—')}</td>`;
    return `<td class="time-cell"><button type="button" class="time-btn" data-time="${which}" title="Изменить время">${esc(time || '—')}</button></td>`;
  }

  function userRow(user) {
    return `<tr class="${isAdmin ? 'clickable' : ''}" ${isAdmin ? `tabindex="0" data-id="${user.id}" title="Открыть карточку пользователя"` : ''}>
      <td class="name-cell">${esc(Api.fio(user, true))}</td>
      <td><div class="zone-tags">${(user.zones || []).map(zone => `<span class="zone">${esc(zone)}</span>`).join('') || '—'}</div></td>
      ${timeCell(user, 'in')}
      ${timeCell(user, 'out')}
      <td class="position">${esc(user.position)}</td>
      <td>${esc(user.phone)}</td>
      <td>${esc(user.role)}</td>
      <td>${esc(user.status)}</td>
      <td>${isAdmin ? `<button type="button" class="row-action" data-journal="${user.id}">Журнал / сообщения</button>` : '—'}</td>
    </tr>`;
  }

  function matches(user, query) {
    if (!query) return true;
    return [
      Api.fio(user, true), user.position, user.phone, user.email, user.role,
      user.zoneName, user.status,
    ].join(' ').toLowerCase().includes(query);
  }

  function render() {
    const query = searchInput.value.trim().toLowerCase();
    clearSearchBtn.style.display = query ? 'flex' : 'none';
    const filtered = users.filter(user => matches(user, query));
    const pageCount = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
    if (page > pageCount) page = pageCount;
    const visible = filtered.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);
    userBody.innerHTML = visible.length
      ? visible.map(userRow).join('')
      : '<tr><td class="empty-row" colspan="9">Пользователи не найдены</td></tr>';
    pagination.innerHTML = pageCount > 1
      ? Array.from({ length: pageCount }, (_, index) =>
        `<button type="button" class="page-btn${index + 1 === page ? ' active' : ''}" data-page="${index + 1}">${index + 1}</button>`
      ).join('')
      : '';
    searchResultStats.textContent = query
      ? `Найдено: ${filtered.length} из ${users.length}`
      : 'Отображены все записи';
    userBadge.textContent = `всего ${users.length}`;
  }

  function onTableActivate(event) {
    if (!isAdmin) return;
    const row = event.target.closest('tr[data-id]');
    if (!row) return;
    const user = users.find(item => String(item.id) === row.dataset.id);
    if (!user) return;
    if (event.target.closest('[data-time]')) {
      Modal.openTime(user, event.target.closest('[data-time]').dataset.time);
      return;
    }
    Modal.openEmployee(user, event.target.closest('[data-journal]') ? 'journal' : 'edit');
  }

  Modal.init({ toast: showToast, onEmployeeChange: render });
  userBody.addEventListener('click', onTableActivate);
  userBody.addEventListener('keydown', event => {
    if ((event.key === 'Enter' || event.key === ' ') && event.target.matches('tr[data-id]')) {
      event.preventDefault();
      onTableActivate(event);
    }
  });
  searchInput.addEventListener('input', () => { page = 1; render(); });
  clearSearchBtn.addEventListener('click', () => {
    searchInput.value = '';
    searchInput.focus();
    page = 1;
    render();
  });
  pagination.addEventListener('click', event => {
    const button = event.target.closest('.page-btn');
    if (!button) return;
    page = Number(button.dataset.page);
    render();
  });
  $('btnLogout').addEventListener('click', () => {
    Api.logout();
    window.location.href = '/';
  });
  if (isAdmin) {
    $('btnAddEmployee').addEventListener('click', () => { window.location.href = '/pages/add-employee.html'; });
    $('btnMeetings').addEventListener('click', () => Modal.openMeetings());
  } else {
    $('btnAddEmployee').hidden = true;
    $('btnMeetings').hidden = true;
  }

  try {
    users = await Api.getEmployees();
    render();
  } catch (error) {
    userBody.innerHTML = `<tr><td class="empty-row" colspan="9">Не удалось загрузить данные: ${esc(error.message)}</td></tr>`;
  }
});

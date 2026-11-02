/* =====================================================================
   ZooPass · employee-modal.js
   Окно сотрудника (вкладки: Редактирование · Сообщения · Журнал проходов)
   и окно «Календарь собраний». Данные берёт ТОЛЬКО из api.js (Api.*),
   подключать БД здесь не нужно.
   ===================================================================== */
const Modal = (() => {
  const MONTHS = ['Январь', 'Февраль', 'Март', 'Апрель', 'Май', 'Июнь', 'Июль', 'Август', 'Сентябрь', 'Октябрь', 'Ноябрь', 'Декабрь'];
  const WD = ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Вс'];
  const pad = n => String(n).padStart(2, '0');
  const iso = (y, m, d) => `${y}-${pad(m)}-${pad(d)}`;
  const toMin = t => { const [h, m] = t.split(':').map(Number); return h * 60 + m; };
  const fmtMin = min => `${pad(Math.floor(min / 60))}:${pad(min % 60)}`;
  const fmtDur = min => `${Math.floor(min / 60)} ч ${pad(min % 60)} мин`;
  const esc = Api.esc;

  const hooks = { toast: () => {}, onEmployeeChange: () => {} };
  let root, titleEl, subEl, tabsEl, bodyEl;
  let ctx = {}, token = 0;

  function ensure() {
    if (root) return;
    root = document.createElement('div');
    root.className = 'modal-overlay';
    root.hidden = true;
    root.innerHTML = `
      <div class="modal" role="dialog" aria-modal="true" aria-labelledby="modalTitle">
        <div class="modal-head">
          <div><h2 id="modalTitle"></h2><p class="modal-sub"></p></div>
          <button type="button" class="modal-close" aria-label="Закрыть">&times;</button>
        </div>
        <div class="modal-tabs" role="tablist"></div>
        <div class="modal-body"></div>
      </div>`;
    document.body.appendChild(root);
    titleEl = root.querySelector('#modalTitle');
    subEl = root.querySelector('.modal-sub');
    tabsEl = root.querySelector('.modal-tabs');
    bodyEl = root.querySelector('.modal-body');
    root.querySelector('.modal-close').addEventListener('click', close);
    root.addEventListener('mousedown', e => { if (e.target === root) close(); });
    document.addEventListener('keydown', e => { if (e.key === 'Escape' && !root.hidden) close(); });
    tabsEl.addEventListener('click', e => {
      const b = e.target.closest('[data-tab]');
      if (b) selectTab(b.dataset.tab);
    });
  }

  function close() {
    token++;
    root.hidden = true;
    document.body.style.overflow = '';
  }

  function show(title, sub, tabs, startTab) {
    ensure();
    ctx.tabs = tabs;
    titleEl.textContent = title;
    subEl.textContent = sub;
    tabsEl.innerHTML = tabs.map(t => `<button type="button" role="tab" data-tab="${t.id}">${t.label}</button>`).join('');
    tabsEl.hidden = tabs.length < 2;
    root.hidden = false;
    document.body.style.overflow = 'hidden';
    selectTab(startTab || tabs[0].id);
  }

  function selectTab(id) {
    const tab = ctx.tabs.find(t => t.id === id);
    if (!tab) return;
    ctx.tab = id;
    tabsEl.querySelectorAll('button').forEach(b => {
      const on = b.dataset.tab === id;
      b.classList.toggle('active', on);
      b.setAttribute('aria-selected', on);
    });
    bodyEl.innerHTML = '<p class="m-empty">Загрузка…</p>';
    const my = ++token;
    Promise.resolve(tab.render(bodyEl, () => my === token)).catch(err => {
      if (my === token) bodyEl.innerHTML = `<p class="m-error">Не удалось загрузить данные: ${esc(err.message)}</p>`;
    });
  }

  function monthNav(y, m) {
    return `<div class="cal-nav">
      <button type="button" class="cal-nav-btn" data-nav="-1" aria-label="Предыдущий месяц">‹</button>
      <strong>${MONTHS[m - 1]} ${y}</strong>
      <button type="button" class="cal-nav-btn" data-nav="1" aria-label="Следующий месяц">›</button>
    </div>`;
  }

  function buildCalendar(y, m, cell) {
    const offset = (new Date(y, m - 1, 1).getDay() + 6) % 7;
    const dim = new Date(y, m, 0).getDate();
    let html = '<div class="cal">' + WD.map(w => `<div class="cal-wd">${w}</div>`).join('');
    for (let i = 0; i < offset; i++) html += '<div class="cal-day empty"></div>';
    for (let d = 1; d <= dim; d++) html += cell(d, iso(y, m, d), new Date(y, m - 1, d).getDay());
    return html + '</div>';
  }

  function shiftMonth(key, delta) {
    let y = ctx[key + 'y'], m = ctx[key + 'm'] + delta;
    if (m < 1) { m = 12; y--; } else if (m > 12) { m = 1; y++; }
    ctx[key + 'y'] = y; ctx[key + 'm'] = m;
  }

  function field(name, label, value, type = 'text', req = false, attrs = '') {
    return `<label class="m-field"><span>${label}${req ? ' *' : ''}</span>
      <input type="${type}" name="${name}" value="${esc(value)}"${req ? ' required' : ''}${attrs ? ' ' + attrs : ''}>
      <small class="m-err" data-err="${name}"></small></label>`;
  }

  async function renderEdit(body) {
    const e = ctx.emp;
    const zones = await Api.getAccessZones();
    body.innerHTML = `
      <form class="m-form" novalidate>
        <div class="m-grid">
          ${field('lastName', 'Фамилия', e.lastName, 'text', true)}
          ${field('firstName', 'Имя', e.firstName, 'text', true)}
          ${field('middleName', 'Отчество', e.middleName)}
          ${field('position', 'Должность', e.position, 'text', true)}
          ${field('phone', 'Телефон', e.phone, 'tel', true)}
          ${field('email', 'Email', e.email, 'email', true)}
          ${field('birthDate', 'Дата рождения', e.birthDate, 'date', true)}
          <div class="m-full">${field('address', 'Адрес проживания', e.address, 'text', true)}</div>
        </div>
        <fieldset class="m-fieldset">
          <legend>Зоны доступа</legend>
          <label class="m-field"><span>Зона доступа *</span>
            <select name="zoneId" required>${zones.map(zone =>
              `<option value="${zone.id}"${Number(e.zoneId) === zone.id ? ' selected' : ''}>${esc(zone.name)}</option>`
            ).join('')}</select>
            <small class="m-err" data-err="zoneId"></small>
          </label>
        </fieldset>
        <div class="m-actions"><button type="submit" class="btn-primary">Сохранить изменения</button></div>
      </form>`;

    const form = body.querySelector('form');
    const setErr = (name, msg) => { form.querySelector(`[data-err="${name}"]`).textContent = msg || ''; form.elements[name].classList.toggle('invalid', !!msg); };

    form.addEventListener('submit', async ev => {
      ev.preventDefault();
      const v = n => form.elements[n].value.trim();
      const checks = [
        ['lastName', v('lastName'), 'Введите фамилию'],
        ['firstName', v('firstName'), 'Введите имя'],
        ['position', v('position'), 'Укажите должность'],
        ['phone', v('phone').replace(/\D/g, '').length >= 10 ? 'ok' : '', 'Введите телефон полностью'],
        ['email', /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v('email')) ? 'ok' : '', 'Введите корректный email'],
        ['address', v('address'), 'Введите адрес'],
        ['birthDate', v('birthDate'), 'Укажите дату рождения'],
        ['zoneId', v('zoneId'), 'Выберите зону доступа'],
      ];
      let ok = true;
      checks.forEach(([n, val, msg]) => { setErr(n, val ? '' : msg); if (!val) ok = false; });
      if (!ok) return;

      const data = {
        lastName: v('lastName'), firstName: v('firstName'), middleName: v('middleName'),
        position: v('position'), phone: v('phone'), email: v('email'),
        address: v('address'), birthDate: v('birthDate'),
        roleId: e.roleId, zoneId: v('zoneId'),
      };
      const btn = form.querySelector('.btn-primary');
      btn.disabled = true;
      try {
        await Api.updateEmployee(e.id, data);
        Object.assign(e, data);
        e.zoneName = zones.find(zone => zone.id === Number(e.zoneId))?.name || String(e.zoneId);
        e.zones = [e.zoneName];
        titleEl.textContent = Api.fio(e, true);
        subEl.textContent = e.position;
        hooks.onEmployeeChange(e);
        hooks.toast('Изменения сохранены');
      } catch (err) {
        hooks.toast('Не удалось сохранить: ' + err.message);
      } finally { btn.disabled = false; }
    });
  }

  async function renderMessages(body, ok) {
    const list = await Api.getMessages(ctx.emp.id);
    if (!ok()) return;
    body.innerHTML = `
      <p class="m-hint">Сообщение появится в личном кабинете сотрудника.</p>
      <div class="msg-list" aria-live="polite"></div>
      <form class="msg-form">
        <textarea rows="3" maxlength="500" placeholder="Текст сообщения…" required></textarea>
        <div class="msg-actions"><span class="msg-count">0 / 500</span><button type="submit" class="btn-primary">Отправить</button></div>
      </form>`;
    const box = body.querySelector('.msg-list');
    const form = body.querySelector('form');
    const ta = form.querySelector('textarea');

    const draw = () => {
      box.innerHTML = list.length
        ? list.map(m => `<div class="msg"><p>${esc(m.text)}</p><span>${new Date(m.sentAt).toLocaleString('ru-RU', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' })} · ${esc(m.author || 'Сотрудник')}</span></div>`).join('')
        : '<p class="m-empty">Сообщений пока нет</p>';
      box.scrollTop = box.scrollHeight;
    };
    draw();

    ta.addEventListener('input', () => { form.querySelector('.msg-count').textContent = `${ta.value.length} / 500`; });
    ta.addEventListener('keydown', e => { if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) form.requestSubmit(); });
    form.addEventListener('submit', async ev => {
      ev.preventDefault();
      const text = ta.value.trim();
      if (!text) return;
      const btn = form.querySelector('.btn-primary');
      btn.disabled = true;
      try {
        list.push(await Api.sendMessage(ctx.emp.id, text));
        ta.value = ''; form.querySelector('.msg-count').textContent = '0 / 500';
        draw();
        hooks.toast('Сообщение отправлено');
      } catch (err) {
        hooks.toast('Не удалось отправить: ' + err.message);
      } finally { btn.disabled = false; }
    });
  }

  const todayIsoStr = () => { const n = new Date(); return iso(n.getFullYear(), n.getMonth() + 1, n.getDate()); };
  const dmy = di => `${di.slice(8)}.${di.slice(5, 7)}.${di.slice(0, 4)}`;

  function recordForm(box, { date, rec, lockDate, onDone, onCancel }) {
    const today = todayIsoStr();
    box.innerHTML = `
      <form class="m-form" novalidate>
        <div class="m-grid">
          ${field('date', 'Дата', date || today, 'date', true, `max="${today}"${lockDate ? ' disabled' : ''}`)}
          <span></span>
          ${field('entry', 'Вход', rec ? rec.entry : '', 'time', true)}
          ${field('exit', 'Выход', rec && rec.exit ? rec.exit : '', 'time')}
        </div>
        <p class="m-hint m-gap">Выход можно оставить пустым, если сотрудник ещё в здании.</p>
        <div class="m-actions m-split">
          ${rec ? '<button type="button" class="btn-danger" data-del>Удалить запись</button>' : '<span></span>'}
          <div class="m-btns">
            <button type="button" class="btn-ghost" data-cancel>Отмена</button>
            <button type="submit" class="btn-primary">Сохранить</button>
          </div>
        </div>
      </form>`;
    const form = box.querySelector('form');
    const err = (n, msg) => {
      form.querySelector(`[data-err="${n}"]`).textContent = msg || '';
      form.elements[n].classList.toggle('invalid', !!msg);
    };
    const touchToday = (d, entry, exit) => {
      if (d === today) { Object.assign(ctx.emp, { timeIn: entry, timeOut: exit }); hooks.onEmployeeChange(ctx.emp); }
    };

    form.querySelector('[data-cancel]').addEventListener('click', onCancel);

    const del = form.querySelector('[data-del]');
    if (del) del.addEventListener('click', async () => {
      if (!confirm('Удалить запись о проходе?')) return;
      try {
        await Api.deleteAccessRecord(ctx.emp.id, date);
        touchToday(date, null, null);
        hooks.toast('Запись удалена');
        onDone(date);
      } catch (e) { hooks.toast('Не удалось удалить: ' + e.message); }
    });

    form.addEventListener('submit', async ev => {
      ev.preventDefault();
      const d = form.elements.date.value;
      const entry = form.elements.entry.value;
      const exit = form.elements.exit.value || null;
      let valid = true;
      if (!d) { err('date', 'Укажите дату'); valid = false; }
      else if (d > today) { err('date', 'Нельзя указать будущую дату'); valid = false; }
      else err('date');
      if (!entry) { err('entry', 'Укажите время входа'); valid = false; } else err('entry');
      if (entry && exit && exit <= entry) { err('exit', 'Выход должен быть позже входа'); valid = false; } else err('exit');
      if (!valid) return;

      const btn = form.querySelector('.btn-primary');
      btn.disabled = true;
      try {
        await Api.saveAccessRecord(ctx.emp.id, d, entry, exit);
        touchToday(d, entry, exit);
        hooks.toast('Запись сохранена');
        onDone(d);
      } catch (e) {
        hooks.toast('Не удалось сохранить: ' + e.message);
        btn.disabled = false;
      }
    });
  }

  function renderJournal(body, ok) {
    const now = new Date();
    ctx.jy = now.getFullYear(); ctx.jm = now.getMonth() + 1;
    return drawJournal(body, ok);
  }

  async function drawJournal(body, ok) {
    const my = (ctx.jt = (ctx.jt || 0) + 1);
    const y = ctx.jy, m = ctx.jm;
    const [log, meetings] = await Promise.all([
      Api.getAccessLog(ctx.emp.id, y, m),
      Api.getMeetings(y, m),
    ]);
    if (!ok() || my !== ctx.jt) return;
    ctx.jlog = log;

    const byDate = Object.fromEntries(log.map(r => [r.date, r]));
    const meetByDate = {};
    meetings.forEach(mt => (meetByDate[mt.date] = meetByDate[mt.date] || []).push(mt));
    const todayIso = todayIsoStr();

    const dim = new Date(y, m, 0).getDate();
    let workdays = 0, absent = 0;
    for (let d = 1; d <= dim; d++) {
      const di = iso(y, m, d);
      if (di > todayIso) break;
      const wd = new Date(y, m - 1, d).getDay();
      if (wd === 0 || wd === 6) continue;
      workdays++;
      if (!byDate[di] && di < todayIso) absent++;
    }
    const done = log.filter(r => r.exit);
    const totalMin = done.reduce((s, r) => s + toMin(r.exit) - toMin(r.entry), 0);
    const avgIn = log.length ? fmtMin(Math.round(log.reduce((s, r) => s + toMin(r.entry), 0) / log.length)) : '—';

    const cal = buildCalendar(y, m, (d, di, wd) => {
      const rec = byDate[di];
      const future = di > todayIso;
      const cls = ['cal-day', future ? 'future' : 'editable'];
      if (wd === 0 || wd === 6) cls.push('weekend');
      if (rec) cls.push('present');
      else if (di < todayIso && wd !== 0 && wd !== 6) cls.push('absent');
      if (di === todayIso) cls.push('today');
      const mt = meetByDate[di];
      const title = [
        future ? '' : (rec ? `Вход ${rec.entry}${rec.exit ? ', выход ' + rec.exit : ''}. Нажмите, чтобы изменить` : 'Нет прохода. Нажмите, чтобы добавить'),
        ...(mt || []).map(x => `${x.time} ${x.title}`),
      ].filter(Boolean).join('\n');
      const inner = `<span class="cal-num">${d}</span>${rec ? `<span class="cal-time">${rec.entry}</span>` : ''}${mt ? '<span class="cal-dot" aria-label="Собрание"></span>' : ''}`;
      return future
        ? `<div class="${cls.join(' ')}" title="${esc(title)}">${inner}</div>`
        : `<button type="button" class="${cls.join(' ')}" data-date="${di}" title="${esc(title)}">${inner}</button>`;
    });

    const rows = [...log].reverse().map(r => {
      const [yy, mm, dd] = r.date.split('-').map(Number);
      const wdName = WD[(new Date(yy, mm - 1, dd).getDay() + 6) % 7];
      const dur = r.exit ? fmtDur(toMin(r.exit) - toMin(r.entry)) : 'в здании';
      return `<tr class="clickable" tabindex="0" data-date="${r.date}" title="Нажмите, чтобы изменить"><td>${wdName}, ${pad(dd)}.${pad(mm)}</td><td class="time-cell">${r.entry}</td><td class="time-cell">${r.exit || '—'}</td><td>${dur}</td><td><button type="button" class="row-action">Изменить</button></td></tr>`;
    }).join('');

    body.innerHTML = `
      ${monthNav(y, m)}
      <div class="stats">
        <div class="stat"><b>${log.length}</b><span>дней на работе из ${workdays}</span></div>
        <div class="stat"><b>${absent}</b><span>рабочих дней пропущено</span></div>
        <div class="stat"><b>${done.length ? fmtDur(totalMin) : '—'}</b><span>всего в здании</span></div>
        <div class="stat"><b>${avgIn}</b><span>средний приход</span></div>
      </div>
      ${cal}
      <p class="cal-legend"><i class="lg present"></i>был на работе <i class="lg absent"></i>не приходил <i class="lg meet"></i>собрание · нажмите на день</p>
      ${meetings.length ? `<ul class="m-meet-list">${meetings.map(x => `<li><b>${x.date.slice(8)}.${x.date.slice(5, 7)}</b> ${esc(x.time)} · ${esc(x.title)}</li>`).join('')}</ul>` : ''}
      <div class="m-row"><h3 class="m-h3">Проходы по дням</h3><button type="button" class="btn-primary btn-sm" data-add>+ Добавить проход</button></div>
      ${rows
        ? `<div class="table-responsive"><table class="m-table"><thead><tr><th>День</th><th>Вход</th><th>Выход</th><th>Время в здании</th><th></th></tr></thead><tbody>${rows}</tbody></table></div>`
        : '<p class="m-empty">Проходов за этот месяц нет</p>'}`;

    body.querySelectorAll('[data-nav]').forEach(b => b.addEventListener('click', () => {
      shiftMonth('j', Number(b.dataset.nav));
      drawJournal(body, ok);
    }));
    body.querySelectorAll('[data-date]').forEach(el => {
      el.addEventListener('click', () => drawRecordEditor(body, ok, el.dataset.date));
      if (el.tagName === 'TR') el.addEventListener('keydown', ev => {
        if (ev.key === 'Enter' && ev.target === el) drawRecordEditor(body, ok, el.dataset.date);
      });
    });
    body.querySelector('[data-add]').addEventListener('click', () => drawRecordEditor(body, ok, ''));
  }

  function drawRecordEditor(body, ok, date) {
    const rec = date ? (ctx.jlog || []).find(r => r.date === date) || null : null;
    body.innerHTML = `
      <button type="button" class="m-back">‹ К журналу</button>
      <h3 class="m-h3">${rec ? 'Изменить проход · ' + dmy(date) : 'Новый проход'}</h3>
      <div class="rf"></div>`;
    const back = d => {
      if (d) { ctx.jy = Number(d.slice(0, 4)); ctx.jm = Number(d.slice(5, 7)); }
      drawJournal(body, ok);
    };
    body.querySelector('.m-back').addEventListener('click', () => back());
    recordForm(body.querySelector('.rf'), { date, rec, lockDate: !!rec, onDone: back, onCancel: () => back() });
    const first = body.querySelector(rec || date ? 'input[name="entry"]' : 'input[name="date"]');
    if (first) first.focus();
  }

  function renderTime(body) {
    const di = todayIsoStr(), e = ctx.emp;
    const rec = e.timeIn ? { date: di, entry: e.timeIn, exit: e.timeOut || null } : null;
    recordForm(body, { date: di, rec, lockDate: true, onDone: close, onCancel: close });
    const inp = body.querySelector(`input[name="${ctx.focus === 'out' ? 'exit' : 'entry'}"]`);
    if (inp) inp.focus();
  }

  function renderMeetings(body, ok) {
    const now = new Date();
    ctx.my = now.getFullYear(); ctx.mm = now.getMonth() + 1; ctx.pickDate = ''; ctx.editId = null;
    return drawMeetings(body, ok);
  }

  async function drawMeetings(body, ok, scroll) {
    const tok = (ctx.mt = (ctx.mt || 0) + 1);
    const y = ctx.my, m = ctx.mm;
    const meetings = await Api.getMeetings(y, m);
    if (!ok() || tok !== ctx.mt) return;

    const byDate = {};
    meetings.forEach(mt => (byDate[mt.date] = byDate[mt.date] || []).push(mt));
    const todayIso = todayIsoStr();
    const editing = ctx.editId ? meetings.find(x => x.id === ctx.editId) || null : null;
    if (!editing) ctx.editId = null;
    const defDate = editing ? editing.date : (ctx.pickDate || (todayIso.startsWith(iso(y, m, 1).slice(0, 8)) ? todayIso : iso(y, m, 1)));
    const pickedCount = ctx.pickDate ? (byDate[ctx.pickDate] || []).length : 0;

    const cal = buildCalendar(y, m, (d, di, wd) => {
      const cls = ['cal-day', 'clickable'];
      if (wd === 0 || wd === 6) cls.push('weekend');
      if (di === todayIso) cls.push('today');
      if (di === ctx.pickDate) cls.push('picked');
      const mt = byDate[di] || [];
      return `<button type="button" class="${cls.join(' ')}" data-date="${di}" title="${mt.length ? 'Нажмите, чтобы изменить' : 'Нажмите, чтобы добавить собрание'}">${d}</button>`;
    });

    const list = meetings.map(x => `
      <li class="${x.date === ctx.pickDate ? 'hl' : ''}"><div><b>${x.date.slice(8)}.${x.date.slice(5, 7)} · ${esc(x.time)}</b> ${esc(x.title)}${x.note ? `<small>${esc(x.note)}</small>` : ''}</div>
      <div class="m-btns"><button type="button" class="row-action" data-edit="${x.id}">Изменить</button><button type="button" class="btn-danger" data-del="${x.id}">Удалить</button></div></li>`).join('');

    body.innerHTML = `
      ${monthNav(y, m)}
      ${cal}
      <h3 class="m-h3">Собрания месяца</h3>
      ${list ? `<ul class="m-meet-edit">${list}</ul>` : '<p class="m-empty">В этом месяце собраний нет</p>'}
      <h3 class="m-h3">${editing ? 'Изменить собрание' : 'Добавить собрание'}</h3>
      ${pickedCount > 1 && !editing ? `<p class="m-hint">В этот день собраний: ${pickedCount}. Чтобы изменить нужное, нажмите «Изменить» в списке.</p>` : ''}
      <form class="m-form" novalidate>
        <div class="m-grid">
          <div class="m-full">${field('title', 'Название', editing ? editing.title : '', 'text', true)}</div>
          ${field('date', 'Дата', defDate, 'date', true)}
          ${field('time', 'Время', editing ? editing.time : '10:00', 'time', true)}
          <div class="m-full">${field('note', 'Заметка (необязательно)', editing ? editing.note : '')}</div>
        </div>
        <div class="m-actions m-split">
          ${editing ? '<button type="button" class="btn-ghost" data-cancel>Отмена</button>' : '<span></span>'}
          <button type="submit" class="btn-primary">${editing ? 'Сохранить' : 'Добавить собрание'}</button>
        </div>
      </form>`;

    const redraw = sc => drawMeetings(body, ok, sc);

    body.querySelectorAll('[data-nav]').forEach(b => b.addEventListener('click', () => {
      ctx.editId = null; ctx.pickDate = '';
      shiftMonth('m', Number(b.dataset.nav));
      redraw();
    }));

    body.querySelectorAll('.cal-day[data-date]').forEach(c => c.addEventListener('click', () => {
      const ms = byDate[c.dataset.date] || [];
      ctx.pickDate = c.dataset.date;
      ctx.editId = ms.length === 1 ? ms[0].id : null;
      redraw(true);
    }));

    body.querySelectorAll('[data-edit]').forEach(b => b.addEventListener('click', () => {
      const mt = meetings.find(x => x.id === Number(b.dataset.edit));
      if (!mt) return;
      ctx.editId = mt.id; ctx.pickDate = mt.date;
      redraw(true);
    }));

    body.querySelectorAll('[data-del]').forEach(b => b.addEventListener('click', async () => {
      if (!confirm('Удалить собрание?')) return;
      try {
        await Api.deleteMeeting(Number(b.dataset.del));
        if (ctx.editId === Number(b.dataset.del)) ctx.editId = null;
        hooks.toast('Собрание удалено');
        redraw();
      } catch (err) { hooks.toast('Не удалось удалить: ' + err.message); }
    }));

    const form = body.querySelector('form');
    const cancel = form.querySelector('[data-cancel]');
    if (cancel) cancel.addEventListener('click', () => { ctx.editId = null; redraw(); });

    form.addEventListener('submit', async ev => {
      ev.preventDefault();
      const v = n => form.elements[n].value.trim();
      const bad = ['title', 'date', 'time'].filter(n => !v(n));
      form.querySelectorAll('.m-err').forEach(s => (s.textContent = ''));
      bad.forEach(n => { form.querySelector(`[data-err="${n}"]`).textContent = 'Заполните поле'; });
      if (bad.length) return;
      const item = { title: v('title'), date: v('date'), time: v('time'), note: v('note') };
      try {
        if (editing) await Api.updateMeeting(editing.id, item);
        else await Api.addMeeting(item);
        const [yy, mm] = item.date.split('-').map(Number);
        ctx.my = yy; ctx.mm = mm; ctx.editId = null; ctx.pickDate = '';
        hooks.toast(editing ? 'Собрание изменено' : 'Собрание добавлено');
        redraw();
      } catch (err) { hooks.toast('Не удалось сохранить: ' + err.message); }
    });

    if (scroll) form.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
  }

  return {
    init(h) { Object.assign(hooks, h); },

    openEmployee(emp, tab = 'edit') {
      ensure();
      ctx = { emp };
      show(Api.fio(emp, true), emp.position, [
        { id: 'edit', label: 'Редактирование', render: renderEdit },
        { id: 'messages', label: 'Сообщения', render: renderMessages },
        { id: 'journal', label: 'Журнал проходов', render: renderJournal },
      ], tab);
    },

    openTime(emp, which = 'in') {
      ensure();
      ctx = { emp, focus: which };
      show('Время прохода · ' + Api.fio(emp), `Сегодня, ${dmy(todayIsoStr())}`, [
        { id: 'time', label: 'Время', render: renderTime },
      ]);
    },

    openMeetings() {
      ensure();
      ctx = {};
      show('Календарь собраний', 'Важные собрания для всех сотрудников', [
        { id: 'meetings', label: 'Собрания', render: renderMeetings },
      ]);
    },
  };
})();

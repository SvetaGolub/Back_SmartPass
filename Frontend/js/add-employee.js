document.addEventListener('DOMContentLoaded', () => {
  const form = document.getElementById('employeeForm');
  const toast = document.getElementById('toast');

  function showToast(message) {
    toast.textContent = message;
    toast.classList.add('show');
    setTimeout(() => toast.classList.remove('show'), 2600);
  }

  document.getElementById('btnHome').addEventListener('click', () => {
    window.location.href = '/pages/admin.html';
  });

  document.getElementById('btnLogout').addEventListener('click', () => {
    Api.logout();
    window.location.href = '/';
  });

  const rules = [
    ['lastName',  v => v.length > 0, 'Введите фамилию'],
    ['firstName', v => v.length > 0, 'Введите имя'],
    ['position',  v => v.length > 0, 'Укажите должность'],
    ['email',     v => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v), 'Введите корректный email'],
    ['login',     v => v.length >= 3, 'Логин должен содержать не менее 3 символов'],
    ['password',  v => v.length >= 8 && v.length <= 72, 'Пароль должен содержать от 8 до 72 символов'],
    ['phone',     v => v.replace(/\D/g, '').length >= 10, 'Введите телефон полностью, не меньше 10 цифр'],
    ['address',   v => v.length > 0, 'Введите адрес'],
    ['birthDate', v => !!v && new Date(v) < new Date(), 'Укажите корректную дату рождения'],
    ['stage',     v => !!v && new Date(v) <= new Date(), 'Укажите дату начала работы не позднее сегодняшней'],
    ['roleId',    v => !!v, 'Выберите роль'],
    ['zoneId',    v => !!v, 'Выберите зону доступа'],
  ];

  function setError(id, message) {
    const input = document.getElementById(id);
    const err = form.querySelector(`[data-error-for="${id}"]`);
    input.classList.toggle('invalid', !!message);
    if (err) {
      err.textContent = message || '';
      err.classList.toggle('show', !!message);
    }
  }

  function validate() {
    let ok = true;
    let firstBad = null;
    rules.forEach(([id, test, message]) => {
      const value = document.getElementById(id).value.trim();
      if (test(value)) {
        setError(id, '');
      } else {
        setError(id, message);
        ok = false;
        if (!firstBad) firstBad = id;
      }
    });
    if (firstBad) document.getElementById(firstBad).focus();
    return ok;
  }

  rules.forEach(([id]) => {
    document.getElementById(id).addEventListener('input', () => setError(id, ''));
    document.getElementById(id).addEventListener('change', () => setError(id, ''));
  });

  async function loadOptions() {
    if (Api.currentRole() !== 'ADMIN') {
      window.location.replace('/');
      return;
    }
    try {
      const [roles, zones] = await Promise.all([Api.getRoles(), Api.getAccessZones()]);
      form.roleId.innerHTML = '<option value="">Выберите роль</option>' +
        roles.map(role => `<option value="${role.id}">${Api.esc(role.name)}</option>`).join('');
      form.zoneId.innerHTML = '<option value="">Выберите зону</option>' +
        zones.map(zone => `<option value="${zone.id}">${Api.esc(zone.name)}</option>`).join('');
    } catch (err) {
      showToast('Не удалось загрузить роли и зоны: ' + err.message);
    }
  }
  void loadOptions();

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    if (!validate()) return;

    const data = {
      lastName: form.lastName.value.trim(),
      firstName: form.firstName.value.trim(),
      middleName: form.middleName.value.trim(),
      roleId: Number(form.roleId.value),
      zoneId: Number(form.zoneId.value),
      position: form.position.value.trim(),
      phone: form.phone.value.trim(),
      email: form.email.value.trim(),
      login: form.login.value.trim(),
      password: form.password.value,
      address: form.address.value.trim(),
      birthday: `${form.birthDate.value}T00:00:00`,
      stage: `${form.stage.value}T00:00:00`,
    };

    try {
      await Api.createEmployee(data);
    } catch (err) {
      showToast('Не удалось добавить: ' + err.message);
      return;
    }

    showToast(`Сотрудник ${data.lastName} ${data.firstName} добавлен`);
    form.reset();
    setTimeout(() => { window.location.href = '/pages/admin.html'; }, 1200);
  });
});

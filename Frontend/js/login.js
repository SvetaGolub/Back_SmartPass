document.addEventListener('DOMContentLoaded', () => {
  const form = document.getElementById('loginForm');
  const error = document.getElementById('loginError');
  const submit = form.querySelector('[type="submit"]');

  form.addEventListener('submit', async event => {
    event.preventDefault();
    error.hidden = true;
    submit.disabled = true;

    try {
      await Api.login(form.elements.login.value.trim(), form.elements.password.value);
      window.location.href = '/pages/admin.html';
    } catch (err) {
      error.textContent = err.message;
      error.hidden = false;
    } finally {
      submit.disabled = false;
      form.elements.password.value = '';
    }
  });
});

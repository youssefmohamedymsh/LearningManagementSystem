// Enrollment desk: talks to the JSON API in WebServer.java.
const $ = (id) => document.querySelector(`[data-testid="${id}"]`);
const state = { students: [], courses: [], selected: null, filter: '', busy: false };

async function api(method, path, body) {
  const res = await fetch(path, {
    method,
    headers: body ? { 'Content-Type': 'application/json' } : {},
    body: body ? JSON.stringify(body) : undefined,
  });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    const err = new Error(data.message || 'Something went wrong');
    err.status = res.status;
    throw err;
  }
  return data;
}

let toastTimer;
function toast(message, kind = 'ok') {
  const el = $('toast');
  el.textContent = message;
  el.className = 'toast ' + kind;
  el.hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { el.hidden = true; }, 3500);
}

function cell(text, className) {
  const td = document.createElement('td');
  td.textContent = text;
  if (className) td.className = className;
  return td;
}

function button(label, testid, onClick, variant = '') {
  const b = document.createElement('button');
  b.type = 'button';
  b.className = ('btn ' + variant).trim();
  b.textContent = label;
  b.dataset.testid = testid;
  b.addEventListener('click', onClick);
  return b;
}

function courseRow(course, testid) {
  const tr = document.createElement('tr');
  tr.dataset.testid = testid;
  tr.dataset.courseId = course.id;
  tr.append(cell(course.id), cell(course.name, 'name'), cell(course.instructor),
    cell(`${course.duration} · ${course.time}`), cell(course.location));
  return tr;
}

// ---------- Student list ----------

function renderStudentList() {
  const list = $('student-list');
  const q = state.filter.trim().toLowerCase();
  const shown = state.students.filter((s) => !q
    || s.name.toLowerCase().includes(q) || String(s.id) === q || s.country.toLowerCase().includes(q));
  list.innerHTML = '';
  for (const s of shown) {
    const li = document.createElement('li');
    const b = document.createElement('button');
    b.type = 'button';
    b.dataset.testid = 'student-item';
    b.dataset.studentId = s.id;
    if (state.selected && state.selected.id === s.id) b.setAttribute('aria-current', 'true');
    const id = document.createElement('span'); id.className = 'sid'; id.textContent = '#' + s.id;
    const who = document.createElement('span');
    who.textContent = s.name;
    const meta = document.createElement('span'); meta.className = 'meta'; meta.textContent = `Grade ${s.grade} · ${s.country}`;
    who.append(meta);
    const badge = document.createElement('span');
    badge.className = 'count-badge' + (s.courseCount ? '' : ' zero');
    badge.textContent = s.courseCount;
    badge.title = `${s.courseCount} course${s.courseCount === 1 ? '' : 's'}`;
    b.append(id, who, badge);
    b.addEventListener('click', () => selectStudent(s.id));
    li.append(b);
    list.append(li);
  }
  $('student-count').textContent = `${shown.length} of ${state.students.length} students`;
}

function renderStats() {
  const enrolled = state.students.filter((s) => s.courseCount > 0).length;
  const total = state.students.reduce((n, s) => n + s.courseCount, 0);
  $('stats').textContent = `${state.students.length} students · ${state.courses.length} courses · ${enrolled} enrolled · ${total} enrollments`;
}

// ---------- Student panel ----------

function renderStudent() {
  const s = state.selected;
  $('no-student').hidden = !!s;
  $('student-panel').hidden = !s;
  if (!s) return;

  $('student-id').textContent = '#' + s.id;
  $('student-name').textContent = s.name;
  $('student-grade').textContent = s.grade;
  $('student-email').textContent = s.email;
  $('student-location').textContent = `${s.region}, ${s.country}`;

  const meter = $('slot-meter');
  meter.innerHTML = '';
  for (let i = 0; i < s.maxCourses; i++) {
    const li = document.createElement('li');
    if (i < s.courses.length) li.className = 'filled';
    meter.append(li);
  }
  $('slot-label').textContent = `${s.courses.length} of ${s.maxCourses} course slots used`;

  // Enrolled courses
  const enrolledIds = new Set(s.courses.map((c) => c.id));
  const enrolled = $('enrolled-table').querySelector('tbody');
  enrolled.innerHTML = '';
  $('no-courses').hidden = s.courses.length > 0;
  $('enrolled-table').hidden = s.courses.length === 0;
  for (const c of s.courses) {
    const tr = courseRow(c, 'enrolled-row');
    const actions = cell('', 'actions');
    const drop = button('Drop', 'drop', () => change('DELETE', `/api/students/${s.id}/courses/${c.id}`, null, `Dropped ${c.name}`), 'danger');
    if (s.courses.length === 1) {
      drop.disabled = true;
      drop.title = 'A student must stay enrolled in at least one course. Replace it instead.';
    }
    actions.append(button('Replace', 'replace', () => showReplace(actions, c), 'ghost'), drop);
    tr.append(actions);
    enrolled.append(tr);
  }

  // Catalogue
  const full = s.courses.length >= s.maxCourses;
  const catalogue = $('catalogue-table').querySelector('tbody');
  catalogue.innerHTML = '';
  for (const c of state.courses) {
    const tr = courseRow(c, 'catalogue-row');
    const actions = cell('', 'actions');
    const enroll = button('Enroll', 'enroll', () => change('POST', `/api/students/${s.id}/courses`, { courseId: c.id }, `Enrolled in ${c.name}`));
    if (enrolledIds.has(c.id)) {
      tr.classList.add('enrolled');
      enroll.disabled = true;
      enroll.title = 'Already enrolled';
    } else if (full) {
      enroll.disabled = true;
      enroll.title = `Already at the limit of ${s.maxCourses} courses`;
    }
    actions.append(enroll);
    tr.append(actions);
    catalogue.append(tr);
  }
  $('no-catalogue').hidden = state.courses.length > 0;
  $('catalogue-table').hidden = state.courses.length === 0;
}

function showReplace(actions, current) {
  const s = state.selected;
  const taken = new Set(s.courses.map((c) => c.id));
  const form = document.createElement('span');
  form.className = 'replace-form';
  const select = document.createElement('select');
  select.dataset.testid = 'replace-select';
  select.setAttribute('aria-label', `Replace ${current.name} with`);
  for (const c of state.courses.filter((c) => !taken.has(c.id))) {
    const o = document.createElement('option');
    o.value = c.id;
    o.textContent = `${c.id} · ${c.name}`;
    select.append(o);
  }
  const confirm = button('Swap', 'replace-confirm', () => {
    const target = state.courses.find((c) => c.id === Number(select.value));
    change('PUT', `/api/students/${s.id}/courses/${current.id}`, { newCourseId: target.id },
      `Replaced ${current.name} with ${target.name}`);
  });
  const cancel = button('Cancel', 'replace-cancel', () => renderStudent(), 'ghost');
  form.append(select, confirm, cancel);
  actions.replaceChildren(form);
  select.focus();
}

// ---------- Actions ----------

async function change(method, path, body, successMessage) {
  // One change at a time: a double click or a second click during a slow request is ignored.
  if (state.busy) return;
  state.busy = true;
  $('student-panel').querySelectorAll('button').forEach((b) => { b.disabled = true; });
  try {
    state.selected = await api(method, path, body);
    const summary = state.students.find((x) => x.id === state.selected.id);
    summary.courseCount = state.selected.courses.length;
    renderStudent();
    renderStudentList();
    renderStats();
    toast(successMessage);
  } catch (err) {
    toast(err.message, 'error');
    // 409: the server's view differs from ours (e.g. someone else changed this student). Catch up.
    if (err.status === 409) await refreshSelected();
  } finally {
    state.busy = false;
    renderStudent(); // re-applies each button's own enabled/disabled state
  }
}

async function refreshSelected() {
  try {
    state.selected = await api('GET', `/api/students/${state.selected.id}`);
    const summary = state.students.find((x) => x.id === state.selected.id);
    if (summary) summary.courseCount = state.selected.courses.length;
    renderStudentList();
    renderStats();
  } catch {
    // keep what we have; the error toast is already showing
  }
}

async function selectStudent(id) {
  try {
    state.selected = await api('GET', `/api/students/${id}`);
    history.replaceState(null, '', '#student-' + id);
    renderStudent();
    renderStudentList();
  } catch (err) {
    toast(err.message, 'error');
  }
}

document.getElementById('search').addEventListener('input', (e) => {
  state.filter = e.target.value;
  renderStudentList();
});

async function load() {
  $('load-error').hidden = true;
  $('student-count').textContent = 'Loading students…';
  try {
    [state.students, state.courses] = await Promise.all([api('GET', '/api/students'), api('GET', '/api/courses')]);
  } catch (err) {
    $('student-count').textContent = '';
    $('load-error-message').textContent = `Couldn't load the student list: ${err.message}`;
    $('load-error').hidden = false;
    return;
  }
  renderStudentList();
  renderStats();
  const fromHash = /^#student-(\d+)$/.exec(location.hash);
  if (fromHash) selectStudent(Number(fromHash[1]));
}

$('retry-load').addEventListener('click', load);
load();

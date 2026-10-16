/* ============================================================
 * 企业工单管理系统 - 前端单页应用逻辑
 * 技术栈：原生 JavaScript + Fetch API（前后端分离）
 * ============================================================ */
(function () {
  'use strict';

  const API = '/api';
  const TOKEN_KEY = 'qg_token';
  const USER_KEY = 'qg_user';

  const state = {
    user: null,           // 当前登录用户 { id, username, realName, roles }
    query: { page: 0, size: 10 },
    ticketTypes: [],
    currentTicket: null,
    currentView: 'dashboard',
  };

  // ---------- DOM helpers ----------
  const $ = (sel) => document.querySelector(sel);
  const $$ = (sel) => document.querySelectorAll(sel);
  const escapeHtml = (s) => String(s ?? '').replace(/[&<>"']/g,
      (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

  // ---------- Auth / API ----------
  function getToken() { return localStorage.getItem(TOKEN_KEY); }

  async function api(path, options = {}) {
    const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
    const token = getToken();
    if (token) headers['Authorization'] = 'Bearer ' + token;
    const res = await fetch(API + path, { ...options, headers });
    if (res.status === 401) { logout(); throw new Error('登录已过期，请重新登录'); }
    if (res.status === 403) { throw new Error('无权限执行该操作'); }
    const data = await res.json().catch(() => ({}));
    if (!res.ok) {
      const msg = data.message || '请求失败';
      if (data.errors) {
        throw new Error(msg + ': ' + Object.values(data.errors).join('; '));
      }
      throw new Error(msg);
    }
    return data.data ?? data;
  }

  function isAdmin() { return state.user && state.user.roles.includes('ADMIN'); }
  function isManager() { return state.user && state.user.roles.includes('MANAGER'); }
  function canManage() { return isAdmin() || isManager(); }

  // ---------- 视图切换 ----------
  const titles = {
    dashboard: '数据看板', tickets: '工单管理', create: '创建工单',
    users: '用户管理', logs: '操作日志',
  };

  function switchView(view) {
    state.currentView = view;
    $$('.nav-item').forEach((el) => el.classList.toggle('active', el.dataset.view === view));
    ['dashboard', 'tickets', 'create', 'users', 'logs'].forEach((v) => {
      $('#view-' + v).classList.toggle('hidden', v !== view);
    });
    $('#ticket-detail').classList.remove('open');
    setTimeout(() => $('#ticket-detail').classList.add('hidden'), 280);
    $('#view-title').textContent = titles[view];
    closeSidebar();
    if (view === 'dashboard') renderDashboard();
    else if (view === 'tickets') renderTicketList();
    else if (view === 'create') renderCreateForm();
    else if (view === 'users') renderUsers();
    else if (view === 'logs') renderLogs();
  }

  // ---------- 登录 / 注册 / 退出 ----------
  async function handleLogin(e) {
    e.preventDefault();
    hideError();
    try {
      const resp = await api('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ username: $('#login-username').value.trim(), password: $('#login-password').value }),
      });
      enterApp(resp);
    } catch (err) { showError('#auth-error', err.message); }
  }

  function enterApp(resp) {
    localStorage.setItem(TOKEN_KEY, resp.token);
    state.user = { id: resp.userId, username: resp.username, realName: resp.realName, roles: resp.roles || [] };
    localStorage.setItem(USER_KEY, JSON.stringify({ id: resp.userId, username: resp.username, realName: resp.realName, roles: state.user.roles }));
    $('#auth-view').classList.add('hidden');
    $('#app-view').classList.remove('hidden');
    $('#user-info').innerHTML = `<div class="name">👤 ${escapeHtml(resp.realName)}</div><span class="r">@${escapeHtml(resp.username)}</span>`;
    updateRoleUI();
    switchView('dashboard');
  }

  function logout() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    state.user = null;
    $('#app-view').classList.add('hidden');
    $('#auth-view').classList.remove('hidden');
  }

  function updateRoleUI() {
    const roles = state.user.roles.map((r) => ({ ADMIN: '管理员', MANAGER: '分配经理', AGENT: '处理员', REPORTER: '提交人' }[r] || r)).join(' / ');
    $('#topbar-role').textContent = roles;
    $$('.nav-admin').forEach((el) => el.classList.toggle('hidden', state.currentView === 'logs' ? !isAdmin() : !canManage()));
    $('#nav-users').classList.toggle('hidden', !isAdmin());
    $('#nav-logs').classList.toggle('hidden', !isAdmin());
  }

  function showError(sel, msg) {
    const el = $(sel);
    if (el) { el.textContent = msg; el.classList.remove('hidden'); }
  }
  function hideError(sel) { [sel || '#auth-error'].forEach((s) => { const el = $(s); if (el) el.classList.add('hidden'); }); }

  // ---------- 仪表盘 ----------
  async function renderDashboard() {
    const el = $('#view-dashboard');
    try {
      const [dash, trend] = await Promise.all([api('/statistics/dashboard'), api('/statistics/trend?days=' + 7)]);
      const st = (lbl) => {
        const arr = { '新建': 'c-open', '已分配': 'c-assigned', '处理中': 'c-progress', '已解决': 'c-resolved', '已关闭': 'c-closed' };
        const item = dash.byStatus.find((x) => x.name === lbl);
        return `<div class="stat-card ${arr[lbl] || ''}"><div class="num">${item ? item.value : 0}</div><div class="lbl">${lbl}</div></div>`;
      };
      el.innerHTML = `
        <div class="stat-grid">
          ${st('新建')}${st('已分配')}${st('处理中')}${st('已解决')}${st('已关闭')}
          <div class="stat-card c-total"><div class="num">${dash.total}</div><div class="lbl">工单总数</div></div>
        </div>
        <div class="chart-row">
          <div class="card">
            <h3>工单状态分布</h3>
            ${renderBars(dash.byStatus)}
          </div>
          <div class="card">
            <h3>优先级分布</h3>
            ${renderBars(dash.byPriority)}
          </div>
        </div>
        <div class="card">
          <h3>近 7 日创建趋势</h3>
          ${renderTrendBars(trend)}
        </div>`;
    } catch (err) {
      el.innerHTML = `<div class="card"><div class="alert alert-error">${escapeHtml(err.message)}</div></div>`;
    }
  }

  function renderBars(items) {
    const max = Math.max(...items.map((i) => i.value), 1);
    return `<div class="bar-list">
      ${items.map((i) => `
        <div class="bar-item">
          <span class="bar-name">${escapeHtml(i.name)}</span>
          <div class="bar-track"><div class="bar-fill" style="width:${(i.value / max * 100).toFixed(1)}%"></div></div>
          <span class="bar-val">${i.value}</span>
        </div>`).join('')}
    </div>`;
  }
  function renderTrendBars(trend) {
    const max = Math.max(...trend.map((i) => i.value), 1);
    return `<div class="bar-list">
      ${trend.map((i) => `
        <div class="bar-item">
          <span class="bar-name">${escapeHtml(i.name.slice(5))}</span>
          <div class="bar-track"><div class="bar-fill" style="width:${(i.value / max * 100).toFixed(1)}%"></div></div>
          <span class="bar-val">${i.value}</span>
        </div>`).join('')}
    </div>`;
  }

  // ---------- 工单列表 ----------
  async function renderTicketList() {
    const el = $('#view-tickets');
    const filters = filtersBlock();
    el.innerHTML = filters + '<div class="table-wrap" id="ticket-table"></div><div id="ticket-pagination"></div>';
    $('#ticket-table').innerHTML = '<div class="empty">加载中...</div>';
    await loadTickets();
    attachFilterEvents();
  }

  function filtersBlock() {
    const typeOpts = state.ticketTypes.map((t) => `<option value="${t.id}">${escapeHtml(t.name)}</option>`).join('');
    return `<div class="toolbar">
      <div class="filters">
        <input id="f-keyword" placeholder="搜索编号/标题" style="width:170px">
        <select id="f-status">
          <option value="">全部状态</option>
          <option>OPEN</option><option>ASSIGNED</option><option>IN_PROGRESS</option>
          <option>RESOLVED</option><option>CLOSED</option><option>CANCELED</option>
        </select>
        <select id="f-type"><option value="">全部类型</option>${typeOpts}</select>
        <input id="f-from" type="date" title="创建起始">
        <input id="f-to" type="date" title="创建截止">
        <button id="f-search" class="btn btn-primary btn-sm">查询</button>
        <button id="f-reset" class="btn btn-ghost btn-sm">重置</button>
      </div>
    </div>`;
  }

  function collectFilters() {
    return {
      keyword: $('#f-keyword').value.trim(),
      status: $('#f-status').value,
      typeId: $('#f-type').value,
      createFrom: $('#f-from').value,
      createTo: $('#f-to').value,
    };
  }

  async function loadTickets() {
    const params = new URLSearchParams({ page: state.query.page, size: state.query.size });
    Object.entries(collectFilters()).forEach(([k, v]) => { if (v) params.set(k, v); });
    try {
      const data = await api('/tickets?' + params.toString());
      renderTicketTable(data.items || [], data);
      renderPagination(data);
    } catch (err) {
      $('#ticket-table').innerHTML = `<div class="empty">${escapeHtml(err.message)}</div>`;
    }
  }

  function renderTicketTable(items, pageMeta) {
    if (!items.length) {
      $('#ticket-table').innerHTML = '<div class="empty">暂无工单数据</div>';
      return;
    }
    const rows = items.map((t) => `
      <tr data-id="${t.id}" class="ticket-row">
        <td><b>${escapeHtml(t.id)}</b></td>
        <td>${escapeHtml(t.number)}</td>
        <td>${escapeHtml(t.title)}</td>
        <td>${escapeHtml(t.typeName || '-')}</td>
        <td><span class="badge st-${t.status}">${t.status}</span></td>
        <td><span class="badge p-${t.priority}">${t.priority}</span></td>
        <td>${escapeHtml(t.assignee ? t.assignee.realName : '未分配')}</td>
        <td>${escapeHtml(t.createdAt ? t.createdAt.slice(0, 10) : '-')}</td>
      </tr>`).join('');
    $('#ticket-table').innerHTML = `<table>
      <thead><tr><th>ID</th><th>编号</th><th>标题</th><th>类型</th><th>状态</th><th>优先级</th><th>处理人</th><th>创建日期</th></tr></thead>
      <tbody>${rows}</tbody></table>`;
    $$('.ticket-row').forEach((row) => row.addEventListener('click', () => openTicketDetail(Number(row.dataset.id))));
  }

  function renderPagination(meta) {
    const totalPages = Math.max(1, Math.ceil(meta.total / meta.size));
    let pages = '';
    const x = Math.min(Math.max(meta.page, 1), totalPages);
    for (let i = 1; i <= totalPages; i++) {
      if (i === 1 || i === totalPages || Math.abs(i - x) <= 2) {
        pages += `<button class="page-btn ${i === x ? 'active' : ''}" data-page="${i - 1}">${i}</button>`;
      } else if (pages.endsWith('...') === false) { pages += '<span>...</span>'; }
    }
    $('#ticket-pagination').innerHTML = `<div class="pagination">
      <span>共 <b>${meta.total}</b> 条，第 <b>${x}</b>/${totalPages} 页</span>
      <div class="pages">${pages}</div>
    </div>`;
    $$('.page-btn').forEach((b) => b.addEventListener('click', () => {
      state.query.page = Number(b.dataset.page);
      loadTickets();
    }));
  }

  function attachFilterEvents() {
    $('#f-search').addEventListener('click', () => { state.query.page = 0; loadTickets(); });
    $('#f-reset').addEventListener('click', () => {
      ['#f-keyword', '#f-status', '#f-type', '#f-from', '#f-to'].forEach((s) => { $(s).value = ''; });
      state.query.page = 0; loadTickets();
    });
  }

  // ---------- 创建工单 ----------
  async function renderCreateForm() {
    const el = $('#view-create');
    const agentOpts = await fetchAgents();
    const typeOpts = state.ticketTypes.map((t) => `<option value="${t.id}">${escapeHtml(t.name)}</option>`).join('');
    el.innerHTML = `<div class="card" style="max-width:760px">
      <h3>创建新工单</h3>
      <div id="create-msg"></div>
      <form id="create-form" class="form">
        <div class="field"><label class="required">标题</label><input id="c-title" required maxlength="200" placeholder="简要描述问题或需求"></div>
        <div class="field"><label>详细描述</label><textarea id="c-desc" rows="4" placeholder="描述问题现象、影响范围等信息"></textarea></div>
        <div class="form-row">
          <div class="field"><label class="required">工单类型</label><select id="c-type" required>${typeOpts}</select></div>
          <div class="field"><label>优先级</label><select id="c-priority">
            <option value="LOW">低</option><option value="MEDIUM" selected>中</option>
            <option value="HIGH">高</option><option value="URGENT">紧急</option>
          </select></div>
        </div>
        <div id="c-custom"></div>
        <div class="form-row">
          <div class="field"><label>分配方式</label><select id="c-assign"><option value="auto">自动分配</option><option value="manual">手动分配</option></select></div>
          <div class="field" id="c-agent-field" style="display:none"><label>处理人</label><select id="c-agent">${agentOpts}</select></div>
        </div>
        <button type="submit" class="btn btn-primary">提交工单</button>
      </form>
    </div>`;
    $('#c-type').addEventListener('change', () => loadCustomFields(Number($('#c-type').value)));
    $('#c-assign').addEventListener('change', () => {
      const manual = $('#c-assign').value === 'manual';
      $('#c-agent-field').style.display = manual ? '' : 'none';
    });
    $('#create-form').addEventListener('submit', handleCreate);
    loadCustomFields(Number($('#c-type').value));
  }

  async function loadCustomFields(typeId) {
    try {
      const fields = await api('/ticket-types/' + typeId + '/fields');
      $('#c-custom').innerHTML = fields.map(fieldInput).join('');
    } catch (err) { $('#c-custom').innerHTML = ''; }
  }

  function fieldInput(f) {
    if (f.dataType === 'SELECT') {
      const opts = (f.options || '').split(',').map((o) => `<option value="${escapeHtml(o.trim())}">${escapeHtml(o.trim())}</option>`).join('');
      return `<div class="field"><label ${f.required ? 'class="required"' : ''}>${escapeHtml(f.label)}</label>
        <select data-key="${escapeHtml(f.fieldKey)}" ${f.required ? 'required' : ''}><option value="">请选择</option>${opts}</select></div>`;
    }
    if (f.dataType === 'BOOLEAN') {
      return `<div class="field"><label ${f.required ? 'class="required"' : ''}>${escapeHtml(f.label)}</label>
        <select data-key="${escapeHtml(f.fieldKey)}"><option value="true">是</option><option value="false">否</option></select></div>`;
    }
    if (f.dataType === 'DATE') {
      return `<div class="field"><label ${f.required ? 'class="required"' : ''}>${escapeHtml(f.label)}</label>
        <input type="date" data-key="${escapeHtml(f.fieldKey)}" ${f.required ? 'required' : ''}></div>`;
    }
    return `<div class="field"><label ${f.required ? 'class="required"' : ''}>${escapeHtml(f.label)}</label>
      <input data-key="${escapeHtml(f.fieldKey)}" ${f.required ? 'required' : ''} placeholder="请输入${escapeHtml(f.label)}"></div>`;
  }

  async function handleCreate(e) {
    e.preventDefault();
    const customFields = {};
    $$('#c-custom [data-key]').forEach((el) => { if (el.value) customFields[el.dataset.key] = el.value; });
    const manual = $('#c-assign').value === 'manual';
    const body = {
      title: $('#c-title').value.trim(),
      description: $('#c-desc').value.trim(),
      typeId: Number($('#c-type').value),
      priority: $('#c-priority').value,
      autoAssign: !manual,
      assigneeId: manual ? Number($('#c-agent').value) : null,
      customFields,
    };
    try {
      const t = await api('/tickets', { method: 'POST', body: JSON.stringify(body) });
      showSuccess('#create-msg', '工单创建成功：' + t.number);
      switchView('tickets');
    } catch (err) { showError('#create-msg', err.message); }
  }

  function showSuccess(sel, msg) {
    const el = $(sel);
    if (el) { el.innerHTML = `<div class="alert alert-success">${escapeHtml(msg)}</div>`; }
  }

  async function fetchAgents() {
    try { return await api('/users/agents'); }
    catch (err) { return []; }
  }

  // ---------- 工单详情 ----------
  async function openTicketDetail(id) {
    try {
      const t = await api('/tickets/' + id);
      state.currentTicket = t;
      renderTicketDetail(t);
      const drawer = $('#ticket-detail');
      drawer.classList.remove('hidden');
      setTimeout(() => drawer.classList.add('open'), 10);
    } catch (err) { alert(err.message); }
  }

  function canOperate(t) {
    const me = state.user;
    if (isAdmin() || isManager()) return true;
    if (me.roles.includes('AGENT') && t.assignee && t.assignee.id === me.id) return true;
    if (t.reporter && t.reporter.id === me.id) return true;
    return false;
  }

  function renderTicketDetail(t) {
    const drawer = $('#ticket-detail');
    const comments = (t.comments || []).map((c) => `
      <div class="comment-box"><div class="meta"><span>${escapeHtml(c.authorName || '')}</span><span>${escapeHtml(c.createdAt)}</span></div>${escapeHtml(c.content)}</div>`).join('');
    const logs = (t.logs || []).map((l) => `
      <div class="flow-item"><div class="who">${escapeHtml(l.operatorName)}</div>
        <div class="act">${escapeHtml(l.action)}</div><div class="when">${escapeHtml(l.createdAt)}</div>
        ${l.detail && l.detail !== l.action ? `<div class="muted">${escapeHtml(l.detail)}</div>` : ''}</div>`).join('');
    const custom = Object.keys(t.customFields || {}).length
      ? `<div class="field-preview">${Object.entries(t.customFields).map(([k, v]) => `<div class="row"><span class="muted">${escapeHtml(k)}</span><span>${escapeHtml(v)}</span></div>`).join('')}</div>`
      : '<span class="muted">无自定义字段</span>';

    drawer.innerHTML = `
      <div class="drawer-head">
        <div>
          <h2>${escapeHtml(t.title)}</h2>
          <span class="muted">${escapeHtml(t.number)} · ${escapeHtml(t.typeName)}</span>
        </div>
        <button class="icon-btn" id="detail-close">&times;</button>
      </div>
      <div class="drawer-body">
        <div class="section-title">基本信息</div>
        <div class="detail-grid">
          <div><span class="k">状态</span><div><span class="badge st-${t.status}">${t.status}</span></div></div>
          <div><span class="k">优先级</span><div><span class="badge p-${t.priority}">${t.priority}</span></div></div>
          <div><span class="k">提交人</span><div>${escapeHtml(t.reporter ? t.reporter.realName : '-')}</div></div>
          <div><span class="k">处理人</span><div>${escapeHtml(t.assignee ? t.assignee.realName : '未分配')}</div></div>
          <div><span class="k">创建时间</span><div>${escapeHtml(t.createdAt)}</div></div>
          <div><span class="k">分配方式</span><div>${escapeHtml(t.assignStrategy || '-')}</div></div>
        </div>
        ${t.description ? `<div class="section-title">描述</div><div style="white-space:pre-wrap">${escapeHtml(t.description)}</div>` : ''}
        <div class="section-title">自定义字段</div>${custom}
        ${renderDetailOps(t)}
        <div class="section-title">处理流程</div>
        <div class="flow-line">${logs || '<div class="muted">暂无记录</div>'}</div>
      </div>`;
    $('#detail-close').addEventListener('click', closeDrawer);
    attachDetailActions(t);
  }

  function renderDetailOps(t) {
    const me = state.user;
    if (!canOperate(t)) return '';
    const statusBtn = {
      OPEN: () => '<button class="btn btn-warning btn-sm" data-act="status" data-to="IN_PROGRESS">开始处理</button>',
      ASSIGNED: () => canManage() && t.status === 'ASSIGNED' ? '<button class="btn btn-warning btn-sm" data-act="status" data-to="IN_PROGRESS">开始处理</button>' : '',
      IN_PROGRESS: () => '<button class="btn btn-success btn-sm" data-act="status" data-to="RESOLVED">标记解决</button>',
      RESOLVED: () => '<button class="btn btn-sm" data-act="status" data-to="CLOSED">关闭工单</button>',
    }[t.status] || (() => '');
    let actions = '';
    if (statusBtn) actions += statusBtn();
    if (canManage()) actions += `<button class="btn btn-ghost btn-sm" data-act="reassign">重新分配</button>`;
    const isReporterOrAgent = !isAdmin() && !isManager() && (t.status === 'OPEN' || t.status === 'ASSIGNED') && t.reporter && t.reporter.id === me.id;
    if (isReporterOrAgent) actions += `<button class="btn btn-danger btn-sm" data-act="status" data-to="CANCELED">取消工单</button>`;
    if (!actions) return '';
    return `<div class="section-title">操作</div><div class="actions">${actions}</div>`;
  }

  function attachDetailActions(t) {
    $$('#ticket-detail [data-act]').forEach((btn) => btn.addEventListener('click', async () => {
      const act = btn.dataset.act;
      if (act === 'status') await doChangeStatus(t.id, btn.dataset.to);
      else if (act === 'reassign') showReassignModal(t);
    }));
  }

  async function doChangeStatus(id, to) {
    const remark = prompt('请输入流转备注（可选）', '') || '';
    try {
      const t = await api(`/tickets/${id}/status`, { method: 'PUT', body: JSON.stringify({ toStatus: to, remark }) });
      renderTicketDetail(t);
    } catch (err) { alert(err.message); }
  }

  async function showReassignModal(t) {
    const agents = await fetchAgents();
    const agentOpts = agents.map((a) => `<option value="${a.id}">${escapeHtml(a.realName)}</option>`).join('');
    const back = document.createElement('div');
    back.innerHTML = `<div class="modal-backdrop" id="reassign-wrap">
      <div class="modal">
        <div class="modal-head"><h3>重新分配工单</h3><button class="modal-close" data-close>&times;</button></div>
        <div class="modal-body">
          <form id="reassign-form" class="form">
            <div class="field"><label class="required">选择处理人</label>
              <select id="re-assignee" required>${agentOpts}</select></div>
            <button type="submit" class="btn btn-primary btn-block">确认分配</button>
          </form>
        </div>
      </div>
    </div>`;
    document.body.appendChild(back);
    back.querySelector('[data-close]').addEventListener('click', () => back.remove());
    back.querySelector('#reassign-form').addEventListener('submit', async (e) => {
      e.preventDefault();
      try {
        await api(`/tickets/${t.id}/assign`, {
          method: 'PUT',
          body: JSON.stringify({ assigneeId: Number($('#re-assignee').value) }),
        });
        back.remove();
        const updated = await api('/tickets/' + t.id);
        renderTicketDetail(updated);
      } catch (err) { alert(err.message); }
    });
  }

  function closeDrawer() {
    const d = $('#ticket-detail');
    d.classList.remove('open');
    setTimeout(() => d.classList.add('hidden'), 280);
  }

  // ---------- 用户管理 ----------
  async function renderUsers() {
    const el = $('#view-users');
    el.innerHTML = `<div class="toolbar"><h3>用户管理</h3>
      <button id="user-add" class="btn btn-primary btn-sm">添加用户</button></div>
      <div class="card"><div class="table-wrap" id="user-table"><div class="empty">加载中...</div></div></div>`;
    $('#user-add').addEventListener('click', () => showUserModal());
    await loadUsers();
  }

  async function loadUsers() {
    try {
      const users = await api('/users');
      const rows = users.map((u) => `
        <tr data-id="${u.id}">
          <td><b>${u.id}</b></td>
          <td>${escapeHtml(u.username)}</td>
          <td>${escapeHtml(u.realName)}</td>
          <td>${escapeHtml(u.department || '-')}</td>
          <td>${(u.roles || []).map((r) => `<span class="badge st-ASSIGNED" style="margin:2px">${r}</span>`).join(' ')}</td>
          <td>${u.enabled ? '<span style="color:var(--success)">启用</span>' : '<span style="color:var(--danger)">禁用</span>'}</td>
          <td><div class="actions">
            <button class="btn btn-ghost btn-sm" data-edit="${u.id}">编辑</button>
            ${state.user.id !== u.id ? `<button class="btn btn-danger btn-sm" data-del="${u.id}">删除</button>` : ''}
          </div></td>
        </tr>`).join('');
      $('#user-table').innerHTML = `<table>
        <thead><tr><th>ID</th><th>用户名</th><th>姓名</th><th>部门</th><th>角色</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>${rows}</tbody></table>`;
      $$('#user-table [data-edit]').forEach((b) => b.addEventListener('click', () => {
        showUserModal(users.find((u) => u.id === Number(b.dataset.edit)));
      }));
      $$('#user-table [data-del]').forEach((b) => b.addEventListener('click', async () => {
        if (confirm('确认删除该用户？')) {
          try { await api('/users/' + b.dataset.del, { method: 'DELETE' }); loadUsers(); }
          catch (err) { alert(err.message); }
        }
      }));
    } catch (err) { $('#user-table').innerHTML = `<div class="empty">${escapeHtml(err.message)}</div>`; }
  }

  function showUserModal(user) {
    const editing = !!user;
    const roleOpts = ['ADMIN', 'MANAGER', 'AGENT', 'REPORTER'].map((r) =>
      `<label style="margin-right:10px"><input type="checkbox" name="role" value="${r}" ${user && user.roles && user.roles.includes(r) ? 'checked' : ''}> ${r}</label>`).join('');
    const back = document.createElement('div');
    back.innerHTML = `<div class="modal-backdrop"><div class="modal">
      <div class="modal-head"><h3>${editing ? '编辑用户' : '添加用户'}</h3><button class="modal-close" data-close>&times;</button></div>
      <div class="modal-body">
        <form id="user-form" class="form">
          <div class="form-row">
            <div class="field"><label class="required">用户名</label><input id="u-name" value="${user ? escapeHtml(user.username) : ''}" ${editing ? 'disabled' : ''} required></div>
            <div class="field"><label class="required">密码</label><input id="u-pass" type="password" ${editing ? '' : 'required'} placeholder="${editing ? '留空则不修改' : '至少6位'}"></div>
          </div>
          <div class="form-row">
            <div class="field"><label class="required">姓名</label><input id="u-real" value="${user ? escapeHtml(user.realName) : ''}" required></div>
            <div class="field"><label>部门</label><input id="u-dept" value="${user ? escapeHtml(user.department || '') : ''}"></div>
          </div>
          <div class="form-row">
            <div class="field"><label>邮箱</label><input id="u-email" value="${user ? escapeHtml(user.email || '') : ''}"></div>
            <div class="field"><label>电话</label><input id="u-phone" value="${user ? escapeHtml(user.phone || '') : ''}"></div>
          </div>
          <div class="field"><label class="required">角色</label><div>${roleOpts}</div></div>
          ${editing ? `<div class="field"><label>状态</label><label><input type="checkbox" id="u-enabled" ${user.enabled ? 'checked' : ''}> 启用</label></div>` : ''}
          <button type="submit" class="btn btn-primary btn-block">保存</button>
        </form>
      </div>
    </div></div>`;
    document.body.appendChild(back);
    back.querySelector('[data-close]').addEventListener('click', () => back.remove());
    back.querySelector('#user-form').addEventListener('submit', async (e) => {
      e.preventDefault();
      const roles = [...$$('#user-form input[name=role]:checked')].map((c) => c.value);
      if (!roles.length) { alert('请至少选择一个角色'); return; }
      try {
        if (editing) {
          await api('/users/' + user.id, {
            method: 'PUT', body: JSON.stringify({
              password: $('#u-pass').value || null,
              realName: $('#u-real').value, department: $('#u-dept').value,
              email: $('#u-email').value, phone: $('#u-phone').value,
              enabled: $('#u-enabled').checked, roles,
            }),
          });
        } else {
          if (!$('#u-pass').value) { alert('请为初始密码'); return; }
          if ($('#u-pass').value.length < 6) { alert('密码至少需要6位'); return; }
          await api('/users', {
            method: 'POST', body: JSON.stringify({
              username: $('#u-name').value.trim(), password: $('#u-pass').value,
              realName: $('#u-real').value, department: $('#u-dept').value,
              email: $('#u-email').value, phone: $('#u-phone').value, roles,
            }),
          });
        }
        back.remove(); loadUsers();
      } catch (err) { alert(err.message); }
    });
  }

  // ---------- 操作日志 ----------
  async function renderLogs() {
    const el = $('#view-logs');
    el.innerHTML = `<div class="toolbar"><input id="lg-user" placeholder="按用户筛选" style="width:150px">
      <input id="lg-module" placeholder="模块" style="width:120px">
      <button id="lg-search" class="btn btn-primary btn-sm">查询</button></div>
      <div class="card"><div class="table-wrap" id="log-table"><div class="empty">加载中...</div></div></div>`;
    $('#lg-search').addEventListener('click', loadLogs);
    await loadLogs();
  }

  async function loadLogs() {
    const params = new URLSearchParams({ page: 0, size: 20 });
    if ($('#lg-user').value) params.set('username', $('#lg-user').value);
    if ($('#lg-module').value) params.set('module', $('#lg-module').value);
    try {
      const page = await api('/logs?' + params.toString());
      const rows = (page.content || []).map((l) => `
        <tr><td>${l.id}</td><td>${escapeHtml(l.username)}</td><td>${escapeHtml(l.module)}</td>
        <td>${escapeHtml(l.action)}</td><td>${escapeHtml(l.detail || '')}</td><td>${escapeHtml(l.ip)}</td>
        <td>${escapeHtml(l.createdAt)}</td></tr>`).join('');
      $('#log-table').innerHTML = page.content.length ? `<table>
        <thead><tr><th>ID</th><th>用户</th><th>模块</th><th>动作</th><th>详情</th><th>IP</th><th>时间</th></tr></thead>
        <tbody>${rows}</tbody></table>` : '<div class="empty">暂无日志</div>';
    } catch (err) { $('#log-table').innerHTML = `<div class="empty">${escapeHtml(err.message)}</div>`; }
  }

  // ---------- 工具 ----------
  function closeSidebar() {
    $('#sidebar').classList.remove('open');
  }

  // ---------- 事件绑定 ----------
  function init() {
    $('#login-form').addEventListener('submit', handleLogin);
    $('#show-register').addEventListener('click', () => $('#register-modal').classList.remove('hidden'));
    $('[data-close="register-modal"]').addEventListener('click', () => $('#register-modal').classList.add('hidden'));
    $('#register-modal').addEventListener('click', (e) => { if (e.target.id === 'register-modal') $('#register-modal').classList.add('hidden'); });
    $('#register-form').addEventListener('submit', async (e) => {
      e.preventDefault();
      try {
        const resp = await api('/auth/register', {
          method: 'POST',
          body: JSON.stringify({ username: $('#reg-username').value.trim(), password: $('#reg-password').value }),
        });
        $('#register-modal').classList.add('hidden');
        enterApp(resp);
      } catch (err) { alert(err.message); }
    });
    $('#logout-btn').addEventListener('click', logout);
    $('#menu-toggle').addEventListener('click', () => $('#sidebar').classList.toggle('open'));
    $$('.nav-item').forEach((el) => el.addEventListener('click', () => switchView(el.dataset.view)));
  }

  // ---------- 启动 ----------
  async function boot() {
    init();
    const saved = localStorage.getItem(USER_KEY);
    const token = getToken();
    if (saved && token) {
      try {
        state.user = JSON.parse(saved);
        $('#auth-view').classList.add('hidden');
        $('#app-view').classList.remove('hidden');
        $('#user-info').innerHTML = `<div class="name">👤 ${escapeHtml(state.user.realName)}</div><span class="r">@${escapeHtml(state.user.username)}</span>`;
        updateRoleUI();
        switchView('dashboard');
      } catch (err) { logout(); }
    }
    try {
      state.ticketTypes = await api('/ticket-types');
    } catch (err) { state.ticketTypes = []; }
    // 预加载类型（未登录也允许匿名获取，用于了解系统）
    if (!state.user) {
      $('#auth-view').classList.remove('hidden');
      $('#login-username').focus();
    }
  }

  boot();
})();
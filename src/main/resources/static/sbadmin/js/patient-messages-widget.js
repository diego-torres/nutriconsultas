/* Floating patient messages chat widget for admin pages */

(function () {
  'use strict';

  var POLL_INTERVAL_MS = 60000;
  var state = {
    open: false,
    unreadCount: 0,
    unreadSummaries: [],
    thread: [],
    activePacienteId: null,
    activePacienteName: null,
    profileMode: false,
    pollTimer: null
  };

  function $(selector) {
    return document.querySelector(selector);
  }

  function escapeHtml(text) {
    var div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }

  function formatTime(iso) {
    if (!iso) {
      return '';
    }
    try {
      return new Date(iso).toLocaleString('es-MX', {
        day: '2-digit',
        month: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch (e) {
      return iso;
    }
  }

  function truncate(text, max) {
    if (!text || text.length <= max) {
      return text || '';
    }
    return text.substring(0, max - 1) + '…';
  }

  function fetchJson(url, options) {
    return fetch(url, options || {}).then(function (response) {
      if (!response.ok) {
        throw new Error('Request failed: ' + response.status);
      }
      return response.json();
    });
  }

  function initContext() {
    if (window.PATIENT_CHAT_CONTEXT && window.PATIENT_CHAT_CONTEXT.pacienteId) {
      state.profileMode = true;
      state.activePacienteId = window.PATIENT_CHAT_CONTEXT.pacienteId;
      state.activePacienteName = window.PATIENT_CHAT_CONTEXT.pacienteName || 'Paciente';
    }
  }

  function updateBadge() {
    var badge = $('#patientChatBadge');
    if (!badge) {
      return;
    }
    if (state.unreadCount > 0) {
      badge.textContent = state.unreadCount > 99 ? '99+' : String(state.unreadCount);
      badge.hidden = false;
    } else {
      badge.hidden = true;
    }
  }

  function renderUnreadList() {
    var container = $('#patientChatUnreadList');
    if (!container) {
      return;
    }
    if (state.unreadSummaries.length === 0) {
      container.innerHTML = '<p class="patient-chat-empty">No hay mensajes nuevos de pacientes.</p>';
      return;
    }
    container.innerHTML = state.unreadSummaries.map(function (item) {
      return '<button type="button" class="patient-chat-unread-item" data-paciente-id="' + item.pacienteId + '" data-paciente-name="' + escapeHtml(item.pacienteName) + '">' +
        '<strong>' + escapeHtml(item.pacienteName) + '</strong>' +
        '<span class="patient-chat-unread-preview">' + escapeHtml(truncate(item.preview, 80)) + '</span>' +
        '<span class="patient-chat-unread-meta">' + formatTime(item.sentAt) +
        (item.unreadCount > 1 ? ' · ' + item.unreadCount + ' nuevos' : '') + '</span>' +
        '</button>';
    }).join('');

    container.querySelectorAll('.patient-chat-unread-item').forEach(function (button) {
      button.addEventListener('click', function () {
        openThread(Number(button.getAttribute('data-paciente-id')), button.getAttribute('data-paciente-name'));
      });
    });
  }

  function renderThread() {
    var container = $('#patientChatMessages');
    if (!container) {
      return;
    }
    if (state.thread.length === 0) {
      container.innerHTML = '<p class="patient-chat-empty">Aún no hay mensajes en esta conversación.</p>';
      return;
    }
    container.innerHTML = state.thread.map(function (message) {
      var roleClass = message.senderRole === 'NUTRITIONIST' ? 'outgoing' : 'incoming';
      return '<div class="patient-chat-message ' + roleClass + '">' +
        '<div class="patient-chat-bubble">' + escapeHtml(message.body) + '</div>' +
        '<div class="patient-chat-time">' + formatTime(message.sentAt) + '</div>' +
        '</div>';
    }).join('');
    container.scrollTop = container.scrollHeight;
  }

  function setPanelTitle(title) {
    var titleEl = $('#patientChatTitle');
    if (titleEl) {
      titleEl.textContent = title;
    }
  }

  function showGlobalView() {
    $('#patientChatGlobalView').hidden = false;
    $('#patientChatThreadView').hidden = true;
    $('#patientChatBackBtn').hidden = true;
    setPanelTitle('Mensajes de pacientes');
    renderUnreadList();
  }

  function showThreadView() {
    $('#patientChatGlobalView').hidden = true;
    $('#patientChatThreadView').hidden = false;
    $('#patientChatBackBtn').hidden = state.profileMode;
    setPanelTitle(state.activePacienteName || 'Conversación');
    renderThread();
  }

  function loadUnread() {
    return fetchJson('/rest/patient-messages/unread').then(function (summaries) {
      state.unreadSummaries = summaries || [];
      return fetchJson('/rest/patient-messages/unread/count');
    }).then(function (payload) {
      state.unreadCount = payload.count || 0;
      updateBadge();
      if (state.open && !state.profileMode && $('#patientChatThreadView').hidden) {
        renderUnreadList();
      }
    }).catch(function (err) {
      console.warn('Could not load unread patient messages', err);
    });
  }

  function openThread(pacienteId, pacienteName) {
    state.activePacienteId = pacienteId;
    state.activePacienteName = pacienteName;
    return fetchJson('/rest/patient-messages/thread/' + pacienteId).then(function (messages) {
      state.thread = messages || [];
      showThreadView();
      return fetchJson('/rest/patient-messages/thread/' + pacienteId + '/read', { method: 'POST' });
    }).then(function () {
      return loadUnread();
    }).catch(function (err) {
      console.warn('Could not open patient message thread', err);
    });
  }

  function sendMessage() {
    var input = $('#patientChatInput');
    if (!input || !state.activePacienteId) {
      return;
    }
    var body = input.value.trim();
    if (!body) {
      return;
    }
    input.disabled = true;
    fetchJson('/rest/patient-messages/thread/' + state.activePacienteId, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ body: body })
    }).then(function (message) {
      state.thread.push(message);
      input.value = '';
      renderThread();
    }).catch(function (err) {
      console.warn('Could not send patient message', err);
      alert('No se pudo enviar el mensaje. Intenta de nuevo.');
    }).finally(function () {
      input.disabled = false;
      input.focus();
    });
  }

  function togglePanel(forceOpen) {
    var panel = $('#patientChatPanel');
    if (!panel) {
      return;
    }
    state.open = forceOpen !== undefined ? forceOpen : !state.open;
    panel.hidden = !state.open;
    if (state.open) {
      if (state.profileMode && state.activePacienteId) {
        openThread(state.activePacienteId, state.activePacienteName);
      } else {
        showGlobalView();
        loadUnread();
      }
    }
  }

  function bindEvents() {
    var toggleBtn = $('#patientChatToggle');
    var closeBtn = $('#patientChatClose');
    var backBtn = $('#patientChatBackBtn');
    var sendBtn = $('#patientChatSendBtn');
    var input = $('#patientChatInput');

    if (toggleBtn) {
      toggleBtn.addEventListener('click', function () {
        togglePanel();
      });
    }
    if (closeBtn) {
      closeBtn.addEventListener('click', function () {
        togglePanel(false);
      });
    }
    if (backBtn) {
      backBtn.addEventListener('click', function () {
        state.activePacienteId = null;
        state.activePacienteName = null;
        state.thread = [];
        showGlobalView();
        loadUnread();
      });
    }
    if (sendBtn) {
      sendBtn.addEventListener('click', sendMessage);
    }
    if (input) {
      input.addEventListener('keydown', function (event) {
        if (event.key === 'Enter' && !event.shiftKey) {
          event.preventDefault();
          sendMessage();
        }
      });
    }
  }

  function startPolling() {
    if (state.pollTimer) {
      clearInterval(state.pollTimer);
    }
    state.pollTimer = setInterval(loadUnread, POLL_INTERVAL_MS);
  }

  function init() {
    initContext();
    bindEvents();
    loadUnread();
    startPolling();
    if (state.profileMode && state.activePacienteId) {
      var hint = $('#patientChatProfileHint');
      if (hint) {
        hint.hidden = false;
      }
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();

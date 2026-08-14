/* Floating patient messages chat widget for admin pages */

(function () {
  'use strict';

  var POLL_INTERVAL_MS = 60000;
  var MAX_BODY_LENGTH = 2000;
  var EMOJI_GROUPS = [
    {
      label: 'Reacciones',
      emojis: ['😊', '🙂', '😄', '😁', '😍', '🤗', '😌', '😎', '🥳', '😇', '😉', '😅',
        '😂', '🤩', '🥰', '😘', '🤔', '😮', '😴', '👍', '👎', '👏', '🙌', '💪',
        '🙏', '❤️', '💚', '💙', '💛', '✨', '🌟', '🎉', '✅', '⚠️', '💯']
    },
    {
      label: 'Alimentos',
      emojis: ['🥗', '🍎', '🥦', '🥑', '🥕', '🍇', '🍊', '🍓', '🍌', '🍉', '🥝', '🍅',
        '🥒', '🌽', '🥜', '🥚', '🥛', '🧀', '🐟', '🍗', '🥩', '🍞', '🍠', '🥣',
        '🍽️', '💧', '☕', '🍵', '🥤']
    },
    {
      label: 'Hábitos',
      emojis: ['🏃', '🚶', '🧘', '🏋️', '🚴', '🛌', '☀️', '🌙', '⏰', '📝', '🚰', '💊']
    }
  ];
  var state = {
    open: false,
    unreadCount: 0,
    unreadSummaries: [],
    thread: [],
    activePacienteId: null,
    activePacienteName: null,
    profileMode: false,
    pollTimer: null,
    emojiPickerOpen: false
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

  function renderEmojiPicker() {
    var picker = $('#patientChatEmojiPicker');
    if (!picker || picker.dataset.ready === 'true') {
      return;
    }
    picker.innerHTML = EMOJI_GROUPS.map(function (group) {
      return '<div class="patient-chat-emoji-group">' +
        '<span class="patient-chat-emoji-group-label">' + escapeHtml(group.label) + '</span>' +
        '<div class="patient-chat-emoji-grid">' +
        group.emojis.map(function (emoji) {
          return '<button type="button" class="patient-chat-emoji-item" data-emoji="' + emoji +
            '" aria-label="' + emoji + '">' + emoji + '</button>';
        }).join('') +
        '</div></div>';
    }).join('');
    picker.querySelectorAll('.patient-chat-emoji-item').forEach(function (button) {
      button.addEventListener('click', function () {
        insertEmoji(button.getAttribute('data-emoji'));
      });
    });
    picker.dataset.ready = 'true';
  }

  function setEmojiPickerOpen(open) {
    var picker = $('#patientChatEmojiPicker');
    var button = $('#patientChatEmojiBtn');
    state.emojiPickerOpen = !!open;
    if (picker) {
      picker.hidden = !state.emojiPickerOpen;
    }
    if (button) {
      button.setAttribute('aria-expanded', state.emojiPickerOpen ? 'true' : 'false');
    }
  }

  function insertEmoji(emoji) {
    var input = $('#patientChatInput');
    if (!input || input.disabled || !emoji) {
      return;
    }
    var start = typeof input.selectionStart === 'number' ? input.selectionStart : input.value.length;
    var end = typeof input.selectionEnd === 'number' ? input.selectionEnd : start;
    var next = input.value.slice(0, start) + emoji + input.value.slice(end);
    if (next.length > MAX_BODY_LENGTH) {
      return;
    }
    input.value = next;
    var caret = start + emoji.length;
    input.focus();
    input.setSelectionRange(caret, caret);
  }

  function showGlobalView() {
    setEmojiPickerOpen(false);
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
    var emojiBtn = $('#patientChatEmojiBtn');
    if (emojiBtn) {
      emojiBtn.disabled = true;
    }
    setEmojiPickerOpen(false);
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
      if (typeof swal === 'function') {
        swal({
          title: 'Error',
          text: 'No se pudo enviar el mensaje. Intenta de nuevo.',
          type: 'error',
          timer: 5000
        });
      }
    }).finally(function () {
      input.disabled = false;
      if (emojiBtn) {
        emojiBtn.disabled = false;
      }
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
    } else {
      setEmojiPickerOpen(false);
    }
  }

  function bindEvents() {
    var toggleBtn = $('#patientChatToggle');
    var closeBtn = $('#patientChatClose');
    var backBtn = $('#patientChatBackBtn');
    var sendBtn = $('#patientChatSendBtn');
    var emojiBtn = $('#patientChatEmojiBtn');
    var input = $('#patientChatInput');
    renderEmojiPicker();

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
    if (emojiBtn) {
      emojiBtn.addEventListener('click', function () {
        setEmojiPickerOpen(!state.emojiPickerOpen);
      });
    }
    if (input) {
      input.addEventListener('keydown', function (event) {
        if (event.key === 'Enter' && !event.shiftKey) {
          event.preventDefault();
          sendMessage();
        }
      });
    }
    document.addEventListener('click', function (event) {
      if (!state.emojiPickerOpen) {
        return;
      }
      var picker = $('#patientChatEmojiPicker');
      if ((picker && picker.contains(event.target)) || (emojiBtn && emojiBtn.contains(event.target))) {
        return;
      }
      setEmojiPickerOpen(false);
    });
    document.addEventListener('keydown', function (event) {
      if (event.key === 'Escape' && state.emojiPickerOpen) {
        setEmojiPickerOpen(false);
      }
    });
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

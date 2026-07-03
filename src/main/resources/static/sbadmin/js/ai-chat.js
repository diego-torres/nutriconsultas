/* AI nutritionist chat window (#389) */

(function () {
  'use strict';

  var API_BASE = '/rest/nutritionist/ai/chat';
  var STORAGE_KEY = 'nutriconsultas.ai.threadId';

  var state = {
    threadId: null,
    threadTitle: null,
    messages: [],
    busy: false
  };

  function $(selector) {
    return document.querySelector(selector);
  }

  function escapeHtml(text) {
    var div = document.createElement('div');
    div.textContent = text == null ? '' : String(text);
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

  function roleLabel(role) {
    if (role === 'USER') {
      return 'Tú';
    }
    if (role === 'ASSISTANT') {
      return 'Asistente';
    }
    return role;
  }

  function visibleMessages(messages) {
    return (messages || []).filter(function (message) {
      return message.role === 'USER' || message.role === 'ASSISTANT';
    });
  }

  function requestJson(url, options) {
    return fetch(url, options || {}).then(function (response) {
      return response.json().then(function (data) {
        if (!response.ok || data.success === false) {
          var error = new Error(data.message || 'No se pudo completar la solicitud.');
          error.status = response.status;
          error.errorCode = data.errorCode;
          throw error;
        }
        return data;
      });
    });
  }

  function showError(message) {
    if (typeof swal === 'function') {
      swal({
        title: 'Error',
        text: message,
        type: 'error',
        timer: 5000
      });
    } else {
      window.alert(message);
    }
  }

  function setBusy(busy) {
    state.busy = busy;
    var textarea = $('#aiChatInput');
    var sendBtn = $('#aiChatSendBtn');
    var newBtn = $('#aiChatNewBtn');
    if (textarea) {
      textarea.disabled = busy;
    }
    if (sendBtn) {
      sendBtn.disabled = busy;
      sendBtn.innerHTML = busy
        ? '<span class="spinner-border spinner-border-sm mr-1" role="status" aria-hidden="true"></span>Enviando…'
        : '<i class="fas fa-paper-plane mr-1" aria-hidden="true"></i>Enviar';
    }
    if (newBtn) {
      newBtn.disabled = busy;
    }
    renderMessages(state.showLoading === true);
  }

  function persistThreadId(threadId) {
    state.threadId = threadId;
    if (threadId) {
      sessionStorage.setItem(STORAGE_KEY, String(threadId));
      if (window.history && window.history.replaceState) {
        var url = new URL(window.location.href);
        url.searchParams.set('threadId', String(threadId));
        window.history.replaceState({}, '', url.toString());
      }
    } else {
      sessionStorage.removeItem(STORAGE_KEY);
      if (window.history && window.history.replaceState) {
        var cleanUrl = new URL(window.location.href);
        cleanUrl.searchParams.delete('threadId');
        window.history.replaceState({}, '', cleanUrl.pathname + cleanUrl.search);
      }
    }
  }

  function updateThreadTitle(title) {
    state.threadTitle = title;
    var titleEl = $('#aiChatThreadTitle');
    if (titleEl) {
      titleEl.textContent = title || 'Nueva conversación';
    }
  }

  function renderMessages(showLoading) {
    var container = $('#aiChatMessages');
    if (!container) {
      return;
    }

    var items = visibleMessages(state.messages);
    if (items.length === 0 && !showLoading) {
      container.innerHTML = '<p class="ai-chat-empty">Escribe un mensaje para comenzar. ' +
        'El asistente redactará borradores que deberás revisar antes de usarlos.</p>';
      return;
    }

    var html = items.map(function (message) {
      var roleClass = message.role === 'USER' ? 'user' : 'assistant';
      return '<article class="ai-chat-message ' + roleClass + '" aria-label="' + roleLabel(message.role) + '">' +
        '<div class="ai-chat-bubble">' + escapeHtml(message.content) + '</div>' +
        '<div class="ai-chat-meta">' + escapeHtml(roleLabel(message.role)) +
        (message.createdAt ? ' · ' + formatTime(message.createdAt) : '') + '</div>' +
        '</article>';
    }).join('');

    if (showLoading) {
      html += '<article class="ai-chat-message assistant loading" aria-live="polite" aria-busy="true">' +
        '<div class="ai-chat-bubble">' +
        '<span class="spinner-border spinner-border-sm mr-2" role="status" aria-hidden="true"></span>' +
        'El asistente está pensando…</div></article>';
    }

    container.innerHTML = html;
    container.scrollTop = container.scrollHeight;
  }

  function applyThreadPayload(data) {
    persistThreadId(data.threadId);
    updateThreadTitle(data.title);
    state.messages = data.messages || [];
    renderMessages(false);
  }

  function loadThread(threadId) {
    setBusy(true);
    return requestJson(API_BASE + '/' + threadId, { method: 'GET' })
      .then(function (data) {
        applyThreadPayload(data);
      })
      .catch(function (error) {
        if (error.status === 404) {
          persistThreadId(null);
          state.messages = [];
          updateThreadTitle(null);
          renderMessages(false);
          return;
        }
        showError(error.message);
      })
      .finally(function () {
        setBusy(false);
      });
  }

  function startThread() {
    return requestJson(API_BASE + '/start', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ title: 'Conversación', patientId: null, clinicId: null })
    }).then(function (data) {
      persistThreadId(data.threadId);
      updateThreadTitle(data.title);
      state.messages = [];
      renderMessages(false);
      return data.threadId;
    });
  }

  function ensureThread() {
    if (state.threadId) {
      return Promise.resolve(state.threadId);
    }
    return startThread();
  }

  function sendMessage(text) {
    var trimmed = (text || '').trim();
    if (!trimmed || state.busy) {
      return;
    }

    var textarea = $('#aiChatInput');
    state.messages.push({
      role: 'USER',
      content: trimmed,
      createdAt: new Date().toISOString()
    });
    if (textarea) {
      textarea.value = '';
    }
    state.showLoading = true;
    setBusy(true);

    ensureThread()
      .then(function (threadId) {
        return requestJson(API_BASE + '/message', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ threadId: threadId, message: trimmed })
        });
      })
      .then(function (data) {
        state.showLoading = false;
        persistThreadId(data.threadId);
        state.messages.push({
          role: 'ASSISTANT',
          content: data.content,
          createdAt: new Date().toISOString()
        });
        renderMessages(false);
      })
      .catch(function (error) {
        state.showLoading = false;
        state.messages.pop();
        renderMessages(false);
        showError(error.message);
      })
      .finally(function () {
        state.showLoading = false;
        setBusy(false);
        if (textarea) {
          textarea.focus();
        }
      });
  }

  function startNewConversation() {
    if (state.busy) {
      return;
    }
    if (typeof swal === 'function') {
      swal({
        title: 'Nueva conversación',
        text: '¿Deseas iniciar una conversación nueva? El historial actual permanecerá guardado.',
        type: 'info',
        showCancelButton: true,
        confirmButtonText: 'Sí, iniciar',
        cancelButtonText: 'Cancelar',
        closeOnConfirm: true
      }, function (isConfirm) {
        if (isConfirm) {
          resetConversation();
        }
      });
    } else if (window.confirm('¿Iniciar una conversación nueva?')) {
      resetConversation();
    }
  }

  function resetConversation() {
    persistThreadId(null);
    state.messages = [];
    updateThreadTitle(null);
    renderMessages(false);
    var textarea = $('#aiChatInput');
    if (textarea) {
      textarea.focus();
    }
  }

  function resolveInitialThreadId() {
    var config = window.AI_CHAT_CONFIG || {};
    if (config.initialThreadId) {
      return Number(config.initialThreadId);
    }
    var stored = sessionStorage.getItem(STORAGE_KEY);
    if (stored) {
      return Number(stored);
    }
    return null;
  }

  function bindEvents() {
    var form = $('#aiChatForm');
    var textarea = $('#aiChatInput');
    var newBtn = $('#aiChatNewBtn');

    if (form) {
      form.addEventListener('submit', function (event) {
        event.preventDefault();
        sendMessage(textarea ? textarea.value : '');
      });
    }

    if (textarea) {
      textarea.addEventListener('keydown', function (event) {
        if (event.key === 'Enter' && !event.shiftKey) {
          event.preventDefault();
          sendMessage(textarea.value);
        }
      });
    }

    if (newBtn) {
      newBtn.addEventListener('click', startNewConversation);
    }
  }

  function init() {
    bindEvents();
    var threadId = resolveInitialThreadId();
    if (threadId) {
      loadThread(threadId);
    } else {
      renderMessages(false);
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();

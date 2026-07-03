/* Floating AI assistant widget for patient, dieta, and platillo pages */

(function () {
  'use strict';

  var API_BASE = '/rest/nutritionist/ai/chat';
  var STORAGE_PREFIX = 'nutriconsultas.ai.widget.thread.';

  var state = {
    threadId: null,
    messages: [],
    busy: false,
    open: false,
    context: null,
    showLoading: false,
    loadingThread: false
  };

  function $(selector) {
    return document.querySelector(selector);
  }

  function escapeHtml(text) {
    if (window.NutriAiMarkdown && window.NutriAiMarkdown.escapeHtml) {
      return window.NutriAiMarkdown.escapeHtml(text);
    }
    var div = document.createElement('div');
    div.textContent = text == null ? '' : String(text);
    return div.innerHTML;
  }

  function formatMessageBubbleContent(message) {
    if (window.NutriAiMarkdown && window.NutriAiMarkdown.formatMessageContent) {
      return window.NutriAiMarkdown.formatMessageContent(message.role, message.content);
    }
    return escapeHtml(message.content);
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

  function promptContextPayload() {
    if (!state.context) {
      return {};
    }
    return {
      patientId: state.context.patientId || null,
      dietaId: state.context.dietaId || null,
      platilloId: state.context.platilloId || null
    };
  }

  function storageKey() {
    var scope = state.context && state.context.storageScopeKey ? state.context.storageScopeKey : 'general';
    return STORAGE_PREFIX + scope;
  }

  function persistThreadId(threadId) {
    state.threadId = threadId;
    if (threadId) {
      sessionStorage.setItem(storageKey(), String(threadId));
    } else {
      sessionStorage.removeItem(storageKey());
    }
  }

  function updateSendButton(busy) {
    var sendBtn = $('#aiAssistantSendBtn');
    if (!sendBtn) {
      return;
    }
    sendBtn.disabled = busy;
    if (busy && state.showLoading) {
      sendBtn.innerHTML = '<i class="fas fa-spinner fa-spin" aria-hidden="true"></i><span> Enviando…</span>';
      sendBtn.setAttribute('aria-busy', 'true');
      return;
    }
    sendBtn.innerHTML = '<i class="fas fa-paper-plane" aria-hidden="true"></i><span> Enviar</span>';
    sendBtn.removeAttribute('aria-busy');
  }

  function setBusy(busy) {
    state.busy = busy;
    var textarea = $('#aiAssistantInput');
    var newBtn = $('#aiAssistantNewBtn');
    if (textarea) {
      textarea.disabled = busy;
    }
    updateSendButton(busy);
    if (newBtn) {
      newBtn.disabled = busy;
    }
    renderMessages();
  }

  function renderMessages() {
    var container = $('#aiAssistantMessages');
    if (!container) {
      return;
    }

    var waitingForAssistant = state.showLoading === true;
    var loadingThread = state.loadingThread === true;
    var items = visibleMessages(state.messages);
    if (items.length === 0 && !waitingForAssistant && !loadingThread) {
      container.innerHTML = '<p class="ai-assistant-empty">Pregunta sobre el contexto de esta pantalla. ' +
        'El asistente usará los datos del paciente, dieta o platillo activo para redactar borradores.</p>';
      return;
    }

    var html = items.map(function (message) {
      var roleClass = message.role === 'USER' ? 'user' : 'assistant';
      return '<article class="ai-assistant-message ' + roleClass + '" aria-label="' + roleLabel(message.role) + '">' +
        '<div class="ai-assistant-bubble">' + formatMessageBubbleContent(message) + '</div>' +
        '<div class="ai-assistant-meta">' + escapeHtml(roleLabel(message.role)) +
        (message.createdAt ? ' · ' + formatTime(message.createdAt) : '') + '</div>' +
        '</article>';
    }).join('');

    if (waitingForAssistant) {
      html += '<article class="ai-assistant-message assistant loading" aria-live="polite" aria-busy="true" ' +
        'aria-label="Generando respuesta">' +
        '<div class="ai-assistant-bubble ai-assistant-loading-bubble">' +
        '<i class="fas fa-spinner fa-spin ai-assistant-loading-icon" aria-hidden="true"></i>' +
        '<span class="ai-assistant-loading-text">El asistente está pensando…</span>' +
        '</div></article>';
    } else if (loadingThread) {
      html += '<article class="ai-assistant-message assistant loading" aria-live="polite" aria-busy="true" ' +
        'aria-label="Cargando conversación">' +
        '<div class="ai-assistant-bubble ai-assistant-loading-bubble">' +
        '<i class="fas fa-spinner fa-spin ai-assistant-loading-icon" aria-hidden="true"></i>' +
        '<span class="ai-assistant-loading-text">Cargando conversación…</span>' +
        '</div></article>';
    }

    container.innerHTML = html;
    container.scrollTop = container.scrollHeight;
  }

  function loadThread(threadId) {
    state.loadingThread = true;
    setBusy(true);
    return requestJson(API_BASE + '/' + threadId, { method: 'GET' })
      .then(function (data) {
        persistThreadId(data.threadId);
        state.messages = data.messages || [];
        renderMessages();
      })
      .catch(function (error) {
        if (error.status === 404) {
          persistThreadId(null);
          state.messages = [];
          renderMessages();
          return;
        }
        showError(error.message);
      })
      .finally(function () {
        state.loadingThread = false;
        setBusy(false);
      });
  }

  function startThread() {
    var payload = promptContextPayload();
    payload.title = state.context && state.context.scopeLabel ? state.context.scopeLabel : 'Conversación';
    payload.clinicId = null;
    return requestJson(API_BASE + '/start', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    }).then(function (data) {
      persistThreadId(data.threadId);
      state.messages = [];
      renderMessages();
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

    var textarea = $('#aiAssistantInput');
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

    var payload = promptContextPayload();
    payload.message = trimmed;

    ensureThread()
      .then(function (threadId) {
        payload.threadId = threadId;
        return requestJson(API_BASE + '/message', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
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
        renderMessages();
      })
      .catch(function (error) {
        state.showLoading = false;
        state.messages.pop();
        renderMessages();
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

  function resetConversation() {
    persistThreadId(null);
    state.messages = [];
    renderMessages();
  }

  function togglePanel(forceOpen) {
    var panel = $('#aiAssistantPanel');
    if (!panel) {
      return;
    }
    var shouldOpen = typeof forceOpen === 'boolean' ? forceOpen : !state.open;
    state.open = shouldOpen;
    panel.hidden = !shouldOpen;
    if (shouldOpen) {
      var textarea = $('#aiAssistantInput');
      if (textarea) {
        textarea.focus();
      }
    }
  }

  function confirmNewConversation() {
    if (state.busy) {
      return;
    }
    if (typeof swal === 'function') {
      swal({
        title: 'Nueva conversación',
        text: '¿Deseas iniciar una conversación nueva en este contexto?',
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

  function bindEvents() {
    var toggleBtn = $('#aiAssistantToggle');
    var closeBtn = $('#aiAssistantClose');
    var form = $('#aiAssistantForm');
    var textarea = $('#aiAssistantInput');
    var newBtn = $('#aiAssistantNewBtn');

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
      newBtn.addEventListener('click', confirmNewConversation);
    }
  }

  function init() {
    if (!window.AI_ASSISTANT_WIDGET_CONTEXT) {
      return;
    }
    state.context = window.AI_ASSISTANT_WIDGET_CONTEXT;
    bindEvents();
    renderMessages();
    var stored = sessionStorage.getItem(storageKey());
    if (stored) {
      loadThread(Number(stored));
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();

/* AI nutritionist chat window (#389) */

(function () {
  'use strict';

  var API_BASE = '/rest/nutritionist/ai/chat';
  var DRAFT_API = '/rest/nutritionist/ai/drafts';
  var STORAGE_KEY = 'nutriconsultas.ai.threadId';
  var REVIEW_LABEL = 'Borrador IA — revisión del nutriólogo requerida';

  var state = {
    threadId: null,
    threadTitle: null,
    messages: [],
    drafts: [],
    selectedDraftId: null,
    selectedPreview: null,
    busy: false
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

  function draftTypeLabel(type) {
    if (type === 'DISH') {
      return 'Platillo (receta)';
    }
    if (type === 'MENU') {
      return 'Menú';
    }
    if (type === 'DIET_PLAN') {
      return 'Plan alimentario';
    }
    return type;
  }

  function draftStatusLabel(status) {
    if (status === 'DRAFT') {
      return 'Pendiente de revisión';
    }
    if (status === 'ACCEPTED') {
      return 'Aceptado';
    }
    if (status === 'DISCARDED') {
      return 'Descartado';
    }
    return status;
  }

  function formatNutrients(nutrients) {
    if (!nutrients) {
      return '';
    }
    var parts = [];
    if (nutrients.energiaKcal != null) {
      parts.push(nutrients.energiaKcal + ' kcal');
    }
    if (nutrients.proteinaG != null) {
      parts.push('Proteína ' + nutrients.proteinaG + ' g');
    }
    if (nutrients.lipidosG != null) {
      parts.push('Lípidos ' + nutrients.lipidosG + ' g');
    }
    if (nutrients.hidratosDeCarbonoG != null) {
      parts.push('H. de carbono ' + nutrients.hidratosDeCarbonoG + ' g');
    }
    return parts.join(' · ');
  }

  function renderListSection(title, items) {
    if (!items || items.length === 0) {
      return '';
    }
    return '<section class="ai-chat-draft-section"><h5>' + escapeHtml(title) + '</h5><ul>' +
      items.map(function (item) {
        return '<li>' + escapeHtml(String(item)) + '</li>';
      }).join('') +
      '</ul></section>';
  }

  function renderDraftList() {
    var container = $('#aiChatDraftList');
    if (!container) {
      return;
    }
    if (!state.threadId) {
      container.innerHTML = '<p class="ai-chat-draft-empty">Inicia una conversación para ver borradores.</p>';
      hideDraftPreview();
      return;
    }
    if (!state.drafts || state.drafts.length === 0) {
      container.innerHTML = '<p class="ai-chat-draft-empty">Aún no hay borradores en esta conversación.</p>';
      hideDraftPreview();
      return;
    }
    container.innerHTML = state.drafts.map(function (draft) {
      var active = state.selectedDraftId === draft.draftId ? ' active' : '';
      var title = draft.summary || draftTypeLabel(draft.draftType);
      return '<button type="button" class="ai-chat-draft-card' + active + '" data-draft-id="' + draft.draftId + '">' +
        '<p class="ai-chat-draft-card-title">' + escapeHtml(title) + '</p>' +
        '<p class="ai-chat-draft-card-meta">' + escapeHtml(draftTypeLabel(draft.draftType)) +
        ' · ' + escapeHtml(draftStatusLabel(draft.status)) + '</p></button>';
    }).join('');
    container.querySelectorAll('[data-draft-id]').forEach(function (button) {
      button.addEventListener('click', function () {
        selectDraft(Number(button.getAttribute('data-draft-id')));
      });
    });
  }

  function hideDraftPreview() {
    state.selectedDraftId = null;
    state.selectedPreview = null;
    var panel = $('#aiChatDraftPreview');
    if (panel) {
      panel.hidden = true;
    }
  }

  function renderDraftPreview(preview) {
    var panel = $('#aiChatDraftPreview');
    var body = $('#aiChatDraftPreviewBody');
    var reviewLabel = $('#aiChatDraftReviewLabel');
    var titleEl = $('#aiChatDraftPreviewTitle');
    var typeEl = $('#aiChatDraftPreviewType');
    var actions = $('#aiChatDraftActions');
    if (!panel || !body) {
      return;
    }
    panel.hidden = false;
    if (reviewLabel) {
      reviewLabel.textContent = preview.reviewLabel || REVIEW_LABEL;
    }
    if (titleEl) {
      titleEl.textContent = preview.title || preview.summary || draftTypeLabel(preview.draftType);
    }
    if (typeEl) {
      typeEl.textContent = (preview.draftTypeLabel || draftTypeLabel(preview.draftType)) +
        ' · ' + draftStatusLabel(preview.status);
    }

    var html = '';
    if (preview.portions != null) {
      html += '<section class="ai-chat-draft-section"><h5>Porciones</h5><p>' +
        escapeHtml(String(preview.portions)) + '</p></section>';
    }
    if (preview.dayCount != null) {
      html += '<section class="ai-chat-draft-section"><h5>Días</h5><p>' +
        escapeHtml(String(preview.dayCount)) + '</p></section>';
    }
    var nutrientsText = formatNutrients(preview.nutrients);
    if (nutrientsText) {
      html += '<section class="ai-chat-draft-section"><h5>Nutrientes</h5><p>' +
        escapeHtml(nutrientsText) + '</p></section>';
    }
    if (preview.ingredients && preview.ingredients.length > 0) {
      html += '<section class="ai-chat-draft-section"><h5>Ingredientes</h5><ul>' +
        preview.ingredients.map(function (line) {
          var text = 'Alimento #' + line.alimentoId + ': ' + line.cantidad;
          if (line.unidad) {
            text += ' ' + line.unidad;
          }
          return '<li>' + escapeHtml(text) + '</li>';
        }).join('') + '</ul></section>';
    }
    if (preview.mealSlots && preview.mealSlots.length > 0) {
      html += '<section class="ai-chat-draft-section"><h5>Ingestas / comidas</h5><ul>' +
        preview.mealSlots.map(function (slot) {
          if (slot.ingestas) {
            var dayLabel = slot.label || ('Día ' + slot.dayIndex);
            return '<li>' + escapeHtml(String(dayLabel)) + ': ' + slot.ingestas.length + ' ingestas</li>';
          }
          var ingestaName = slot.nombre || ('Ingesta ' + (slot.orden != null ? slot.orden : ''));
          var itemCount = slot.items ? slot.items.length : 0;
          return '<li>' + escapeHtml(String(ingestaName)) + ' (' + itemCount + ' ítems)</li>';
        }).join('') + '</ul></section>';
    }
    html += renderListSection('Pasos de preparación', preview.preparationSteps);
    html += renderListSection('Supuestos', preview.assumptions);
    html += renderListSection('Advertencias', preview.warnings);
    if (preview.validationSummary) {
      html += '<section class="ai-chat-draft-section"><h5>Validación</h5><p>' +
        escapeHtml(preview.validationSummary) + '</p></section>';
    }
    body.innerHTML = html;

    if (actions) {
      actions.hidden = preview.status !== 'DRAFT';
    }
  }

  function loadDrafts(threadId) {
    if (!threadId) {
      state.drafts = [];
      renderDraftList();
      return Promise.resolve();
    }
    return requestJson(API_BASE + '/' + threadId + '/drafts', { method: 'GET' })
      .then(function (data) {
        state.drafts = data.drafts || [];
        if (state.selectedDraftId && !state.drafts.some(function (d) {
          return d.draftId === state.selectedDraftId;
        })) {
          hideDraftPreview();
        }
        renderDraftList();
        if (state.selectedDraftId) {
          return loadDraftPreview(state.selectedDraftId);
        }
        return null;
      })
      .catch(function (error) {
        showError(error.message);
      });
  }

  function loadDraftPreview(draftId) {
    return requestJson(DRAFT_API + '/' + draftId, { method: 'GET' })
      .then(function (preview) {
        state.selectedDraftId = draftId;
        state.selectedPreview = preview;
        renderDraftList();
        renderDraftPreview(preview);
      })
      .catch(function (error) {
        showError(error.message);
      });
  }

  function selectDraft(draftId) {
    if (state.busy) {
      return;
    }
    loadDraftPreview(draftId);
  }

  function confirmDraftAction(options, onConfirm) {
    if (typeof swal === 'function') {
      swal({
        title: options.title,
        text: options.text,
        type: 'warning',
        showCancelButton: true,
        confirmButtonColor: options.confirmColor || '#1cc88a',
        confirmButtonText: options.confirmText,
        cancelButtonText: 'Cancelar',
        closeOnConfirm: true
      }, function (isConfirm) {
        if (isConfirm) {
          onConfirm();
        }
      });
    } else if (window.confirm(options.text)) {
      onConfirm();
    }
  }

  function acceptSelectedDraft() {
    if (!state.selectedDraftId || !state.selectedPreview || state.selectedPreview.status !== 'DRAFT') {
      return;
    }
    confirmDraftAction({
      title: '¿Aceptar borrador?',
      text: 'Se creará un registro en tu catálogo a partir de este borrador. Revisa que los datos sean correctos.',
      confirmText: 'Sí, aceptar',
      confirmColor: '#1cc88a'
    }, function () {
      setBusy(true);
      requestJson(DRAFT_API + '/' + state.selectedDraftId + '/accept', { method: 'POST' })
        .then(function (data) {
          if (typeof swal === 'function') {
            swal({
              title: 'Borrador aceptado',
              text: data.summary || 'El borrador se guardó en tu catálogo.',
              type: 'success',
              timer: 2500
            });
          }
          return loadDrafts(state.threadId);
        })
        .catch(function (error) {
          showError(error.message);
        })
        .finally(function () {
          setBusy(false);
        });
    });
  }

  function discardSelectedDraft() {
    if (!state.selectedDraftId || !state.selectedPreview || state.selectedPreview.status !== 'DRAFT') {
      return;
    }
    confirmDraftAction({
      title: '¿Descartar borrador?',
      text: 'Esta acción no se puede deshacer.',
      confirmText: 'Sí, descartar',
      confirmColor: '#e74a3b'
    }, function () {
      setBusy(true);
      requestJson(DRAFT_API + '/' + state.selectedDraftId + '/discard', { method: 'POST' })
        .then(function () {
          if (typeof swal === 'function') {
            swal({
              title: 'Borrador descartado',
              type: 'success',
              timer: 2000
            });
          }
          hideDraftPreview();
          return loadDrafts(state.threadId);
        })
        .catch(function (error) {
          showError(error.message);
        })
        .finally(function () {
          setBusy(false);
        });
    });
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
        '<div class="ai-chat-bubble">' + formatMessageBubbleContent(message) + '</div>' +
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
    loadDrafts(data.threadId);
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
        loadDrafts(data.threadId);
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
    state.drafts = [];
    hideDraftPreview();
    renderDraftList();
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
    var acceptBtn = $('#aiChatDraftAcceptBtn');
    var discardBtn = $('#aiChatDraftDiscardBtn');

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
    if (acceptBtn) {
      acceptBtn.addEventListener('click', acceptSelectedDraft);
    }
    if (discardBtn) {
      discardBtn.addEventListener('click', discardSelectedDraft);
    }
  }

  function init() {
    bindEvents();
    var threadId = resolveInitialThreadId();
    if (threadId) {
      loadThread(threadId);
    } else {
      renderMessages(false);
      renderDraftList();
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();

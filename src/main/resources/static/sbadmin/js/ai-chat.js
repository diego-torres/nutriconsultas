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
    busy: false,
    showLoading: false,
    activeStreamRequest: null,
    cancelling: false,
    editingMessageId: null,
    editingMessageIndex: null
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

  function findMessageIndexById(messageId) {
    var items = visibleMessages(state.messages);
    for (var i = 0; i < items.length; i++) {
      if (items[i].id === messageId) {
        return i;
      }
    }
    return -1;
  }

  function updateComposerForEditMode() {
    var textarea = $('#aiChatInput');
    var hint = document.querySelector('.ai-chat-hint');
    if (state.editingMessageId) {
      if (textarea) {
        textarea.placeholder = 'Edita tu mensaje y reenvía…';
      }
      if (hint) {
        hint.textContent = 'Editando un mensaje anterior. Los mensajes posteriores se descartarán al reenviar.';
      }
    } else {
      if (textarea) {
        textarea.placeholder = 'Describe la receta, menú o plan que necesitas…';
      }
      if (hint) {
        hint.textContent = 'Enter para enviar · Shift+Enter para nueva línea · Los borradores requieren tu revisión antes de usarse.';
      }
    }
  }

  function cancelEditMessage() {
    state.editingMessageId = null;
    state.editingMessageIndex = null;
    var textarea = $('#aiChatInput');
    if (textarea) {
      textarea.value = '';
    }
    updateComposerForEditMode();
    renderMessages(state.showLoading === true);
  }

  function startEditMessage(messageId) {
    if (state.busy || !messageId) {
      return;
    }
    var index = findMessageIndexById(messageId);
    if (index < 0) {
      return;
    }
    var message = visibleMessages(state.messages)[index];
    state.editingMessageId = messageId;
    state.editingMessageIndex = index;
    var textarea = $('#aiChatInput');
    if (textarea) {
      textarea.value = message.content || '';
      textarea.focus();
    }
    updateComposerForEditMode();
    renderMessages(state.showLoading === true);
  }

  function requestJson(url, options) {
    return fetch(url, options || {}).then(function (response) {
      return response.json().then(function (data) {
        if (!response.ok || data.success === false) {
          if (window.NutriAiChatErrors && window.NutriAiChatErrors.createApiError) {
            throw window.NutriAiChatErrors.createApiError(data, response.status);
          }
          var error = new Error(data.message || data.error || 'No se pudo completar la solicitud.');
          error.status = response.status;
          error.errorCode = data.errorCode;
          throw error;
        }
        return data;
      });
    });
  }

  function showError(message, title) {
    if (typeof swal === 'function') {
      swal({
        title: title || 'Error',
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
        showError(error.message, error.title);
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
        showError(error.message, error.title);
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
          showError(error.message, error.title);
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
          showError(error.message, error.title);
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
      var canCancel = busy && state.activeStreamRequest;
      sendBtn.disabled = busy && !canCancel;
      if (canCancel) {
        sendBtn.innerHTML = '<i class="fas fa-stop mr-1" aria-hidden="true"></i>Detener';
        sendBtn.setAttribute('aria-label', 'Detener generación');
      } else if (busy) {
        sendBtn.innerHTML = '<span class="spinner-border spinner-border-sm mr-1" role="status" aria-hidden="true"></span>Enviando…';
        sendBtn.setAttribute('aria-label', 'Enviando mensaje al asistente');
      } else {
        sendBtn.innerHTML = '<i class="fas fa-paper-plane mr-1" aria-hidden="true"></i>Enviar';
        sendBtn.setAttribute('aria-label', 'Enviar mensaje al asistente');
      }
    }
    if (newBtn) {
      newBtn.disabled = busy;
    }
    renderMessages(state.showLoading === true);
  }

  function showCancelledNotice() {
    if (typeof swal === 'function') {
      swal({
        title: 'Generación cancelada',
        text: 'Puedes enviar un nuevo mensaje cuando quieras.',
        type: 'info',
        timer: 2500
      });
    }
  }

  function cancelActiveStream() {
    if (!state.activeStreamRequest) {
      return;
    }
    state.cancelling = true;
    state.activeStreamRequest.abort();
  }

  function handleStreamAbort(assistantIndex) {
    state.showLoading = false;
    if (assistantIndex !== null && assistantIndex < state.messages.length) {
      state.messages.splice(assistantIndex, 1);
    }
    state.activeStreamRequest = null;
    state.cancelling = false;
    setBusy(false);
    showCancelledNotice();
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
      var editControl = '';
      if (message.role === 'USER' && message.id && !state.busy && !state.editingMessageId) {
        editControl = ' <button type="button" class="btn btn-link btn-sm ai-chat-edit-btn p-0 ml-2" ' +
          'data-message-id="' + message.id + '" aria-label="Editar mensaje">Editar</button>';
      }
      return '<article class="ai-chat-message ' + roleClass + '" aria-label="' + roleLabel(message.role) + '">' +
        '<div class="ai-chat-bubble">' + formatMessageBubbleContent(message) + '</div>' +
        '<div class="ai-chat-meta">' + escapeHtml(roleLabel(message.role)) +
        (message.createdAt ? ' · ' + formatTime(message.createdAt) : '') + editControl + '</div>' +
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
        showError(error.message, error.title);
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

  function executeAssistantStream(threadId, trimmed, streamUrl, payload, onAfterTruncate) {
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

    var assistantIndex = null;

    function appendAssistantDelta(content) {
      state.showLoading = false;
      if (assistantIndex === null) {
        state.messages.push({
          role: 'ASSISTANT',
          content: content,
          createdAt: new Date().toISOString()
        });
        assistantIndex = state.messages.length - 1;
      } else {
        state.messages[assistantIndex].content += content;
      }
      renderMessages(false);
    }

    function finalizeAssistant(data) {
      state.showLoading = false;
      if (data && data.content != null) {
        if (assistantIndex === null) {
          state.messages.push({
            role: 'ASSISTANT',
            content: data.content,
            createdAt: new Date().toISOString()
          });
        } else {
          state.messages[assistantIndex].content = data.content;
        }
      }
      if (data && data.threadId) {
        persistThreadId(data.threadId);
        loadDrafts(data.threadId);
      }
      renderMessages(false);
    }

    return ensureThread()
      .then(function (resolvedThreadId) {
        var effectiveThreadId = threadId || resolvedThreadId;
        var body = payload || { threadId: effectiveThreadId, message: trimmed };
        if (window.NutriAiChatStream && typeof NutriAiChatStream.streamMessage === 'function') {
          var streamRequest = NutriAiChatStream.streamMessage(streamUrl, body, {
            onStatus: function () {
              renderMessages(true);
            },
            onDelta: appendAssistantDelta,
            onDone: finalizeAssistant,
            onAbort: function () {
              handleStreamAbort(assistantIndex);
            },
            onError: function (error) {
              throw error;
            }
          });
          state.activeStreamRequest = streamRequest;
          setBusy(true);
          return streamRequest;
        }
        return requestJson(streamUrl.replace('/stream', ''), {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(body)
        }).then(function (data) {
          finalizeAssistant(data);
        });
      })
      .catch(function (error) {
        if (state.cancelling) {
          return;
        }
        state.showLoading = false;
        state.messages.pop();
        renderMessages(false);
        showError(error.message, error.title);
      })
      .finally(function () {
        state.showLoading = false;
        state.activeStreamRequest = null;
        state.cancelling = false;
        setBusy(false);
        if (typeof onAfterTruncate === 'function') {
          onAfterTruncate();
        }
        if (textarea) {
          textarea.focus();
        }
      });
  }

  function resubmitEditedMessage(text) {
    var trimmed = (text || '').trim();
    if (!trimmed || state.busy || !state.editingMessageId || state.threadId == null) {
      return;
    }
    var editIndex = state.editingMessageIndex;
    var messageId = state.editingMessageId;
    var hasLaterMessages = editIndex >= 0 &&
      editIndex < visibleMessages(state.messages).length - 1;

    function performResubmit() {
      state.messages = state.messages.slice(0, editIndex);
      cancelEditMessage();
      executeAssistantStream(state.threadId, trimmed, API_BASE + '/message/edit/stream', {
        threadId: state.threadId,
        messageId: messageId,
        message: trimmed
      });
    }

    if (hasLaterMessages) {
      confirmDraftAction({
        title: '¿Reenviar mensaje editado?',
        text: 'Se eliminarán este mensaje, las respuestas posteriores y los borradores pendientes generados después.',
        confirmText: 'Sí, reenviar',
        confirmColor: '#4e73df'
      }, performResubmit);
      return;
    }
    performResubmit();
  }

  function sendMessage(text) {
    if (state.editingMessageId) {
      resubmitEditedMessage(text);
      return;
    }
    var trimmed = (text || '').trim();
    if (!trimmed || state.busy) {
      return;
    }
    executeAssistantStream(null, trimmed, API_BASE + '/message/stream', null);
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
    cancelEditMessage();
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
    var sendBtn = $('#aiChatSendBtn');
    var newBtn = $('#aiChatNewBtn');
    var acceptBtn = $('#aiChatDraftAcceptBtn');
    var discardBtn = $('#aiChatDraftDiscardBtn');

    if (form) {
      form.addEventListener('submit', function (event) {
        event.preventDefault();
        if (state.busy && state.activeStreamRequest) {
          cancelActiveStream();
          return;
        }
        sendMessage(textarea ? textarea.value : '');
      });
    }

    if (sendBtn) {
      sendBtn.addEventListener('click', function (event) {
        if (state.busy && state.activeStreamRequest) {
          event.preventDefault();
          cancelActiveStream();
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

    var messagesContainer = $('#aiChatMessages');
    if (messagesContainer) {
      messagesContainer.addEventListener('click', function (event) {
        var target = event.target;
        if (!target || !target.classList || !target.classList.contains('ai-chat-edit-btn')) {
          return;
        }
        var messageId = Number(target.getAttribute('data-message-id'));
        if (messageId) {
          startEditMessage(messageId);
        }
      });
    }

    if (textarea) {
      textarea.addEventListener('keydown', function (event) {
        if (event.key === 'Escape' && state.editingMessageId) {
          event.preventDefault();
          cancelEditMessage();
          return;
        }
        if (event.key === 'Enter' && !event.shiftKey) {
          event.preventDefault();
          sendMessage(textarea.value);
        }
      });
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

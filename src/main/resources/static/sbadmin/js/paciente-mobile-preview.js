'use strict';

/**
 * Patient mobile-app preview (phone-frame modal) for nutritionist web.
 */
(function (window, $) {
  if (!$) {
    return;
  }

  var MONTHS_SHORT = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];
  var WEEKDAYS = ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];
  var WEEKDAYS_SHORT = ['Dom', 'Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb'];

  function previewUrl(pacienteId) {
    return '/rest/pacientes/' + pacienteId + '/mobile-preview';
  }

  function escapeHtml(value) {
    if (value == null) {
      return '';
    }
    return String(value)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  function greetingForHour(hour) {
    if (hour < 12) {
      return 'Buenos días';
    }
    if (hour < 19) {
      return 'Buenas tardes';
    }
    return 'Buenas noches';
  }

  function formatDateEyebrow(date) {
    return (WEEKDAYS[date.getDay()] + ', ' + MONTHS_SHORT[date.getMonth()] + ' ' + date.getDate()).toUpperCase();
  }

  function formatShortDate(iso) {
    if (!iso) {
      return null;
    }
    var d = new Date(iso);
    if (isNaN(d.getTime())) {
      return null;
    }
    return WEEKDAYS_SHORT[d.getDay()] + ' ' + d.getDate() + ', ' + d.getFullYear();
  }

  function formatLocalDate(isoDate) {
    if (!isoDate) {
      return null;
    }
    var parts = String(isoDate).split('-');
    if (parts.length !== 3) {
      return isoDate;
    }
    var monthIndex = parseInt(parts[1], 10) - 1;
    var day = parseInt(parts[2], 10);
    if (monthIndex < 0 || monthIndex > 11) {
      return isoDate;
    }
    return day + ' ' + MONTHS_SHORT[monthIndex] + ' ' + parts[0];
  }

  function formatDateTime(iso) {
    if (!iso) {
      return '';
    }
    var d = new Date(iso);
    if (isNaN(d.getTime())) {
      return '';
    }
    var hours = d.getHours();
    var minutes = d.getMinutes();
    var hh = hours < 10 ? '0' + hours : String(hours);
    var mm = minutes < 10 ? '0' + minutes : String(minutes);
    return formatLocalDate(d.getFullYear() + '-' + pad2(d.getMonth() + 1) + '-' + pad2(d.getDate()))
      + ' · ' + hh + ':' + mm;
  }

  function pad2(n) {
    return n < 10 ? '0' + n : String(n);
  }

  function formatNumber(value, digits) {
    if (value == null || value === '' || isNaN(Number(value))) {
      return '—';
    }
    return Number(value).toFixed(digits == null ? 1 : digits);
  }

  function formatDelta(value, suffix) {
    if (value == null || isNaN(Number(value))) {
      return '';
    }
    var n = Number(value);
    var sign = n > 0 ? '+' : '';
    var cls = n > 0 ? 'mnp-delta-up' : (n < 0 ? 'mnp-delta-down' : '');
    return '<span class="mnp-delta ' + cls + '">' + sign + formatNumber(n, 1) + (suffix || '') + '</span>';
  }

  function imcGaugePercent(bmi) {
    if (bmi == null || isNaN(Number(bmi))) {
      return 50;
    }
    var v = Number(bmi);
    // Map ~15–40 IMC onto 0–100 for a simple marker.
    var pct = ((v - 15) / 25) * 100;
    if (pct < 2) {
      return 2;
    }
    if (pct > 98) {
      return 98;
    }
    return pct;
  }

  function itemCount(ingesta) {
    var platillos = (ingesta && ingesta.platillos) ? ingesta.platillos.length : 0;
    var alimentos = (ingesta && ingesta.alimentos) ? ingesta.alimentos.length : 0;
    return platillos + alimentos;
  }

  var WEEKLY_PLAN_NAME = 'Plan semanal';

  var state = {
    pacienteId: null,
    data: null,
    activeOnly: false,
    detailCache: {},
    stack: []
  };

  function statusLabel(status) {
    if (status === 'ACTIVE') {
      return 'Activo';
    }
    if (status === 'SCHEDULED') {
      return 'Programada';
    }
    if (status === 'COMPLETED') {
      return 'Completada';
    }
    if (status === 'CANCELLED') {
      return 'Cancelada';
    }
    return status || '';
  }

  function chipClass(status) {
    if (status === 'ACTIVE') {
      return 'mnp-chip-active';
    }
    if (status === 'COMPLETED') {
      return 'mnp-chip-completed';
    }
    if (status === 'CANCELLED') {
      return 'mnp-chip-cancelled';
    }
    return '';
  }

  function isWeeklyPlan(plan) {
    return !!(plan && plan.dietaName === WEEKLY_PLAN_NAME);
  }

  function planDisplayName(plan) {
    if (isWeeklyPlan(plan)) {
      return WEEKLY_PLAN_NAME;
    }
    return (plan && plan.dietaName) ? plan.dietaName : 'Plan alimentario';
  }

  function formatPlanDates(plan) {
    if (!plan) {
      return '';
    }
    var start = formatLocalDate(plan.startDate);
    if (!start) {
      return '';
    }
    if (!plan.endDate) {
      return 'Desde ' + start;
    }
    var end = formatLocalDate(plan.endDate);
    return start + (end ? ' – ' + end : '');
  }

  function firstPlatilloImage(ingesta) {
    var platillos = (ingesta && ingesta.platillos) ? ingesta.platillos : [];
    var i;
    for (i = 0; i < platillos.length; i++) {
      if (platillos[i] && platillos[i].imageUrl) {
        return platillos[i].imageUrl;
      }
    }
    return null;
  }

  function portionLabel(count, unit) {
    if (unit) {
      return String(count != null ? count : 1) + ' ' + unit;
    }
    return String(count != null ? count : 1) + ' porción(es)';
  }

  function cachePlanDetail(detail) {
    if (detail && detail.assignmentId != null) {
      state.detailCache[String(detail.assignmentId)] = detail;
    }
  }

  function renderAvatar(avatarUrl, firstName) {
    var initial = (firstName && firstName.charAt(0)) ? firstName.charAt(0).toUpperCase() : 'P';
    if (avatarUrl) {
      return '<img class="mnp-avatar" src="' + escapeHtml(avatarUrl) + '" alt="">';
    }
    return '<div class="mnp-avatar mnp-avatar-fallback" aria-hidden="true">' + escapeHtml(initial) + '</div>';
  }

  function renderHome(data) {
    var now = new Date();
    var progress = data.progress || {};
    var circ = progress.circumferences || {};
    var plan = data.activePlan;
    var visit = data.nextVisit;
    var lastVisit = formatShortDate(progress.latestMeasurementAt);
    var html = '';

    html += '<div class="mnp-greeting-date">' + escapeHtml(formatDateEyebrow(now)) + '</div>';
    html += '<div class="mnp-greeting-row">';
    html += '<div class="mnp-greeting-text"><span>' + escapeHtml(greetingForHour(now.getHours())) + ',</span>'
      + escapeHtml(data.firstName || 'Paciente') + '.</div>';
    html += renderAvatar(data.avatarUrl, data.firstName);
    html += '</div>';

    html += '<div class="mnp-card">';
    html += '<div class="mnp-card-title">Tu progreso<span class="mnp-link">Gráfica</span></div>';
    if (lastVisit) {
      html += '<div class="mnp-muted mb-2">Última visita: ' + escapeHtml(lastVisit) + '</div>';
    }
    html += '<div class="mnp-imc-row">';
    html += '<span class="mnp-muted">IMC</span>';
    html += '<span class="mnp-imc-value">' + escapeHtml(formatNumber(progress.bmi, 1)) + '</span>';
    if (progress.imcLabel) {
      html += '<span class="mnp-imc-label">' + escapeHtml(progress.imcLabel) + '</span>';
    }
    html += formatDelta(progress.deltaImc, '');
    html += '</div>';
    html += '<div class="mnp-gauge"><div class="mnp-gauge-marker" style="left:'
      + imcGaugePercent(progress.bmi) + '%"></div></div>';
    html += '<div class="mnp-muted">Peso</div>';
    html += '<div class="d-flex align-items-baseline">';
    html += '<div class="mnp-weight">' + escapeHtml(formatNumber(progress.weightKg, 1)) + ' kg</div>';
    html += formatDelta(progress.deltaPeso, ' kg');
    html += '</div>';
    if (circ.waistCm != null || circ.hipCm != null) {
      html += '<div class="mnp-muted mt-2">';
      if (circ.waistCm != null) {
        html += 'Cintura ' + escapeHtml(formatNumber(circ.waistCm, 0)) + ' cm';
      }
      if (circ.waistCm != null && circ.hipCm != null) {
        html += ' · ';
      }
      if (circ.hipCm != null) {
        html += 'Cadera ' + escapeHtml(formatNumber(circ.hipCm, 0)) + ' cm';
      }
      html += '</div>';
    }
    html += '</div>';

    html += '<div class="mnp-card-title">Tu plan alimenticio</div>';
    if (plan) {
      html += renderPlanCard(plan, true);
    } else {
      html += '<div class="mnp-card mnp-card-sage"><div class="mnp-empty">'
        + 'Tu nutriólogo está trabajando en construir tu plan alimentario.</div></div>';
    }

    if (visit) {
      html += '<div class="mnp-card-title">Tu próxima visita<span class="mnp-link">Abrir agenda</span></div>';
      html += '<div class="mnp-card mnp-card-forest">';
      html += '<div class="mnp-visit-eyebrow">Tu cita</div>';
      html += '<div class="mnp-visit-title">' + escapeHtml(visit.title || 'Consulta') + '</div>';
      if (data.nutritionistDisplayName) {
        html += '<div class="mnp-visit-meta">' + escapeHtml(data.nutritionistDisplayName) + '</div>';
      }
      html += '<div class="mnp-visit-meta">' + escapeHtml(formatDateTime(visit.eventDateTime)) + '</div>';
      html += '<div class="mnp-visit-meta mt-1">';
      if (visit.durationMinutes != null) {
        html += escapeHtml(String(visit.durationMinutes)) + ' min · ';
      }
      html += escapeHtml(statusLabel(visit.status));
      html += '</div></div>';
    } else if (data.nutritionistDisplayName) {
      html += '<div class="mnp-card-title">Tu nutriólogo</div>';
      html += '<div class="mnp-card"><div class="mnp-plan-name">'
        + escapeHtml(data.nutritionistDisplayName) + '</div>';
      html += '<div class="mnp-empty mt-1">Los detalles de tu próxima visita aparecerán aquí.</div></div>';
    }

    return html;
  }

  function renderPlanCard(plan, tappable) {
    var html = '<div class="mnp-card';
    if (tappable && plan.assignmentId != null) {
      html += ' mnp-tappable" data-mnp-open-plan="' + escapeHtml(String(plan.assignmentId))
        + '" role="button" tabindex="0">';
    } else {
      html += '">';
    }
    html += '<div class="mnp-plan-card-row">';
    html += '<div class="mnp-plan-card-body">';
    html += '<div class="mnp-plan-name">' + escapeHtml(planDisplayName(plan)) + '</div>';
    var dates = formatPlanDates(plan);
    if (dates) {
      html += '<div class="mnp-plan-dates">' + escapeHtml(dates) + '</div>';
    }
    if (plan.totalKcal != null) {
      html += '<div class="mnp-plan-kcal">' + escapeHtml(String(plan.totalKcal)) + ' kcal/día</div>';
    } else if (isWeeklyPlan(plan)) {
      html += '<div class="mnp-plan-kcal">Menú según el día de la semana</div>';
    }
    html += '</div>';
    html += '<span class="mnp-chip ' + chipClass(plan.status) + '">' + escapeHtml(statusLabel(plan.status)) + '</span>';
    if (tappable && plan.assignmentId != null) {
      html += '<i class="fas fa-chevron-right mnp-chevron" aria-hidden="true"></i>';
    }
    html += '</div></div>';
    return html;
  }

  function visiblePlans(data) {
    var plans = (data && data.plans && data.plans.length) ? data.plans.slice() : [];
    if (!plans.length && data && data.activePlan) {
      plans = [data.activePlan];
    }
    if (state.activeOnly) {
      plans = plans.filter(function (plan) {
        return plan && plan.status === 'ACTIVE';
      });
    }
    return plans;
  }

  function renderDiet(data) {
    var plans = visiblePlans(data);
    var html = '<div class="mnp-screen-title mb-2">Planes de dieta</div>';
    html += '<label class="mnp-filter">';
    html += '<span>Solo planes activos</span>';
    html += '<input type="checkbox" id="mnpActiveOnly"' + (state.activeOnly ? ' checked' : '') + '>';
    html += '</label>';
    if (!plans.length) {
      html += '<div class="mnp-card mnp-card-sage"><div class="mnp-empty">No hay planes con estos filtros.</div></div>';
      return html;
    }
    plans.forEach(function (plan) {
      html += renderPlanCard(plan, true);
    });
    return html;
  }

  function renderStackHeader(title) {
    return '<button type="button" class="mnp-back" id="mnpStackBack">'
      + '<i class="fas fa-chevron-left" aria-hidden="true"></i>'
      + '<span>' + escapeHtml(title) + '</span></button>';
  }

  function renderIngestaCard(ingesta, index) {
    var count = itemCount(ingesta);
    var kcal = ingesta.totalKcal != null ? ingesta.totalKcal : '—';
    var thumb = firstPlatilloImage(ingesta);
    var html = '<div class="mnp-card mnp-tappable mnp-ingesta-card" data-mnp-open-ingesta="'
      + index + '" role="button" tabindex="0">';
    html += '<div class="mnp-ingesta">';
    if (thumb) {
      html += '<img class="mnp-thumb" src="' + escapeHtml(thumb) + '" alt="">';
    } else {
      html += '<div class="mnp-ingesta-icon"><i class="fas fa-utensils"></i></div>';
    }
    html += '<div class="mnp-ingesta-body">';
    html += '<div class="mnp-ingesta-tipo">' + escapeHtml(ingesta.tipo || 'Ingesta') + '</div>';
    html += '<div class="mnp-ingesta-meta">' + escapeHtml(String(count)) + ' elementos · '
      + escapeHtml(String(kcal)) + ' kcal</div>';
    html += '</div>';
    html += '<i class="fas fa-chevron-right mnp-chevron" aria-hidden="true"></i>';
    html += '</div></div>';
    return html;
  }

  function renderMealsScreen(detail) {
    var weekly = isWeeklyPlan(detail);
    var html = renderStackHeader('Plan alimentario');
    html += '<div class="mnp-screen-title">' + escapeHtml(planDisplayName(detail)) + '</div>';
    if (weekly) {
      html += '<div class="mnp-muted mb-2">Aquí ves el menú de hoy. Tu nutriólogo asignó un plan por día de la semana.</div>';
    } else if (detail.totalKcal != null) {
      html += '<div class="mnp-muted mb-2">' + escapeHtml(String(detail.totalKcal)) + ' kcal/día</div>';
    }
    html += '<div class="d-flex align-items-center mb-3">';
    html += '<span class="mnp-chip ' + chipClass(detail.status) + '">' + escapeHtml(statusLabel(detail.status)) + '</span>';
    var dates = formatPlanDates(detail);
    if (dates) {
      html += '<span class="mnp-plan-dates ml-2">' + escapeHtml(dates) + '</span>';
    }
    html += '</div>';
    html += '<div class="mnp-card-title">' + (weekly ? 'Menú de hoy' : 'Ingestas del día') + '</div>';
    var ingestas = detail.ingestas || [];
    if (!ingestas.length) {
      html += '<div class="mnp-empty">No hay ingestas en este plan.</div>';
    } else {
      ingestas.forEach(function (ingesta, index) {
        html += renderIngestaCard(ingesta, index);
      });
    }
    return html;
  }

  function renderFoodTile(nombre, quantity, kcal, imageUrl) {
    var html = '<div class="mnp-card">';
    html += '<div class="mnp-food-tile">';
    if (imageUrl) {
      html += '<img class="mnp-thumb" src="' + escapeHtml(imageUrl) + '" alt="">';
    } else {
      html += '<div class="mnp-ingesta-icon"><i class="fas fa-utensils"></i></div>';
    }
    html += '<div class="mnp-ingesta-body">';
    html += '<div class="mnp-ingesta-tipo">' + escapeHtml(nombre || '') + '</div>';
    html += '<div class="mnp-ingesta-meta">' + escapeHtml(quantity) + '</div>';
    html += '</div>';
    if (kcal != null) {
      html += '<div class="mnp-food-kcal">' + escapeHtml(String(kcal)) + ' kcal</div>';
    }
    html += '</div></div>';
    return html;
  }

  function renderIngestaScreen(detail, index) {
    var ingestas = (detail && detail.ingestas) ? detail.ingestas : [];
    var ingesta = ingestas[index];
    if (!ingesta) {
      return renderStackHeader('Ingesta') + '<div class="mnp-empty">No se encontró la ingesta.</div>';
    }
    var count = itemCount(ingesta);
    var kcal = ingesta.totalKcal != null ? ingesta.totalKcal : '—';
    var html = renderStackHeader(ingesta.tipo || 'Ingesta');
    html += '<div class="mnp-muted mb-3">' + escapeHtml(String(count)) + ' elementos · '
      + escapeHtml(String(kcal)) + ' kcal</div>';
    var platillos = ingesta.platillos || [];
    var alimentos = ingesta.alimentos || [];
    if (!platillos.length && !alimentos.length) {
      html += '<div class="mnp-empty">No hay platillos ni alimentos en esta ingesta.</div>';
      return html;
    }
    platillos.forEach(function (platillo) {
      html += renderFoodTile(platillo.nombre, portionLabel(platillo.porciones, null), platillo.kcal, platillo.imageUrl);
    });
    alimentos.forEach(function (alimento) {
      html += renderFoodTile(alimento.nombre, portionLabel(alimento.porciones, alimento.unidad), alimento.kcal, null);
    });
    return html;
  }

  function hideStack() {
    state.stack = [];
    $('#mnpPreviewContent').removeClass('is-stacked');
    $('#mnpStackPanel').attr('hidden', true).empty();
  }

  function renderStack() {
    var top = state.stack.length ? state.stack[state.stack.length - 1] : null;
    if (!top) {
      hideStack();
      return;
    }
    var detail = state.detailCache[String(top.assignmentId)];
    var html = '';
    if (top.type === 'loading') {
      html = renderStackHeader('Plan alimentario') + '<div class="mnp-empty">Cargando plan…</div>';
    } else if (top.type === 'error') {
      html = renderStackHeader('Plan alimentario')
        + '<div class="mnp-empty">' + escapeHtml(top.message || 'No se pudo cargar el plan.') + '</div>';
    } else if (!detail) {
      html = renderStackHeader('Plan alimentario')
        + '<div class="mnp-empty">No se pudo cargar el plan.</div>';
    } else if (top.type === 'ingesta') {
      html = renderIngestaScreen(detail, top.ingestaIndex);
    } else {
      html = renderMealsScreen(detail);
    }
    $('#mnpHomePanel, #mnpDietPanel').attr('hidden', true);
    $('#mnpStackPanel').html(html).removeAttr('hidden');
    $('#mnpPreviewContent').addClass('is-stacked');
  }

  function openPlan(assignmentId, fromTab) {
    if (assignmentId == null || assignmentId === '') {
      return;
    }
    var cached = state.detailCache[String(assignmentId)];
    if (cached) {
      state.stack = [{type: 'meals', assignmentId: assignmentId, from: fromTab || 'diet'}];
      renderStack();
      return;
    }
    state.stack = [{type: 'loading', assignmentId: assignmentId, from: fromTab || 'diet'}];
    renderStack();
    $.ajax({
      url: previewUrl(state.pacienteId) + '/diet-plans/' + assignmentId,
      method: 'GET',
      dataType: 'json'
    }).done(function (payload) {
      if (!isCurrentPlanRequest(assignmentId)) {
        return;
      }
      if (!payload || payload.success === false || !payload.plan) {
        state.stack = [{
          type: 'error',
          assignmentId: assignmentId,
          from: fromTab || 'diet',
          message: (payload && payload.error) || 'No se pudo cargar el plan.'
        }];
        renderStack();
        return;
      }
      cachePlanDetail(payload.plan);
      state.stack = [{type: 'meals', assignmentId: assignmentId, from: fromTab || 'diet'}];
      renderStack();
    }).fail(function (xhr) {
      if (!isCurrentPlanRequest(assignmentId)) {
        return;
      }
      var msg = 'No se pudo cargar el plan.';
      if (xhr.responseJSON) {
        msg = xhr.responseJSON.message || xhr.responseJSON.error || msg;
      }
      state.stack = [{
        type: 'error',
        assignmentId: assignmentId,
        from: fromTab || 'diet',
        message: msg
      }];
      renderStack();
    });
  }

  function isCurrentPlanRequest(assignmentId) {
    var top = state.stack.length ? state.stack[0] : null;
    return !!(top && String(top.assignmentId) === String(assignmentId));
  }

  function openIngesta(index) {
    var top = state.stack.length ? state.stack[state.stack.length - 1] : null;
    if (!top || top.assignmentId == null) {
      return;
    }
    state.stack.push({
      type: 'ingesta',
      assignmentId: top.assignmentId,
      from: top.from,
      ingestaIndex: index
    });
    renderStack();
  }

  function popStack() {
    if (!state.stack.length) {
      return;
    }
    var leaving = state.stack.pop();
    if (state.stack.length) {
      renderStack();
      return;
    }
    hideStack();
    activateTab(leaving && leaving.from === 'home' ? 'home' : 'diet');
  }

  function showError(message) {
    $('#mnpPreviewLoading').hide();
    $('#mnpPreviewContent').removeClass('is-visible');
    $('#mnpPreviewError').text(message || 'No se pudo cargar el preview.').show();
  }

  function showLoading() {
    $('#mnpPreviewError').hide().text('');
    $('#mnpPreviewContent').removeClass('is-visible');
    $('#mnpPreviewLoading').show();
  }

  function showContent(data) {
    state.data = data;
    state.detailCache = {};
    cachePlanDetail(data && data.activePlanDetail);
    hideStack();
    $('#mnpPreviewLoading').hide();
    $('#mnpPreviewError').hide();
    $('#mnpHomePanel').html(renderHome(data));
    $('#mnpDietPanel').html(renderDiet(data));
    activateTab('home');
    $('#mnpPreviewContent').addClass('is-visible');
  }

  function activateTab(tab) {
    $('#mnpPreviewContent').removeClass('is-stacked');
    $('#mnpStackPanel').attr('hidden', true);
    $('.mnp-tab').removeClass('active');
    $('#mnpHomePanel, #mnpDietPanel').attr('hidden', true);
    if (tab === 'diet') {
      $('#mnpTabDiet').addClass('active');
      $('#mnpDietPanel').removeAttr('hidden');
    } else {
      $('#mnpTabHome').addClass('active');
      $('#mnpHomePanel').removeAttr('hidden');
    }
  }

  function openPreview(pacienteId) {
    var $modal = $('#pacienteMobilePreviewModal');
    state.pacienteId = pacienteId;
    state.activeOnly = false;
    state.detailCache = {};
    state.stack = [];
    showLoading();
    $modal.modal('show');

    $.ajax({
      url: previewUrl(pacienteId),
      method: 'GET',
      dataType: 'json'
    }).done(function (data) {
      if (!data || data.success === false) {
        showError((data && data.error) || 'No se pudo cargar el preview.');
        return;
      }
      showContent(data);
    }).fail(function (xhr) {
      var msg = 'No se pudo cargar el preview.';
      if (xhr.responseJSON) {
        msg = xhr.responseJSON.message || xhr.responseJSON.error || msg;
      }
      showError(msg);
    });
  }

  function bind() {
    $(document).on('click', '.paciente-mobile-preview-btn', function (e) {
      e.preventDefault();
      var pacienteId = $(this).data('id');
      if (pacienteId == null) {
        return;
      }
      openPreview(pacienteId);
    });

    $(document).on('click', '#mnpTabDiet', function (e) {
      e.preventDefault();
      hideStack();
      activateTab('diet');
    });

    $(document).on('click', '#mnpTabHome', function (e) {
      e.preventDefault();
      hideStack();
      activateTab('home');
    });

    $(document).on('click', '[data-mnp-open-plan]', function (e) {
      e.preventDefault();
      var assignmentId = $(this).attr('data-mnp-open-plan');
      var fromTab = $('#mnpHomePanel').attr('hidden') ? 'diet' : 'home';
      openPlan(assignmentId, fromTab);
    });

    $(document).on('click', '[data-mnp-open-ingesta]', function (e) {
      e.preventDefault();
      openIngesta(Number($(this).attr('data-mnp-open-ingesta')));
    });

    $(document).on('click', '#mnpStackBack', function (e) {
      e.preventDefault();
      popStack();
    });

    $(document).on('change', '#mnpActiveOnly', function () {
      state.activeOnly = $(this).is(':checked');
      if (state.data) {
        $('#mnpDietPanel').html(renderDiet(state.data));
      }
    });

    $(document).on('keydown', '.mnp-tappable', function (e) {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        $(this).trigger('click');
      }
    });

    $('#pacienteMobilePreviewModal').on('hidden.bs.modal', function () {
      hideStack();
      state.data = null;
      state.detailCache = {};
    });
  }

  window.PacienteMobilePreview = {
    bind: bind,
    open: openPreview
  };

  $(function () {
    bind();
  });
})(window, window.jQuery);

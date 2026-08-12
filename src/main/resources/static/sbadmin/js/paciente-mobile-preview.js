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

  function statusLabel(status) {
    if (status === 'ACTIVE') {
      return 'Activo';
    }
    if (status === 'SCHEDULED') {
      return 'Programada';
    }
    return status || '';
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
      html += '<div class="mnp-card">';
      html += '<div class="d-flex justify-content-between align-items-start">';
      html += '<div class="mnp-plan-name">' + escapeHtml(plan.dietaName || 'Plan alimentario') + '</div>';
      html += '<span class="mnp-chip mnp-chip-active">' + escapeHtml(statusLabel(plan.status)) + '</span>';
      html += '</div>';
      if (plan.startDate) {
        html += '<div class="mnp-muted">Desde ' + escapeHtml(formatLocalDate(plan.startDate)) + '</div>';
      }
      if (plan.totalKcal != null) {
        html += '<div class="mnp-plan-kcal mt-1">' + escapeHtml(String(plan.totalKcal)) + ' kcal/día</div>';
      }
      html += '</div>';
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

  function renderDiet(data) {
    var plan = data.activePlan;
    var detail = data.activePlanDetail;
    var html = '';

    html += '<div class="mnp-card-title mb-2" style="font-size:1.1rem">Planes de dieta</div>';

    if (!plan) {
      html += '<div class="mnp-card mnp-card-sage"><div class="mnp-empty">No hay plan activo.</div></div>';
      return html;
    }

    html += '<div class="mnp-card">';
    html += '<div class="d-flex justify-content-between align-items-start">';
    html += '<div class="mnp-plan-name">' + escapeHtml(plan.dietaName || 'Plan alimentario') + '</div>';
    html += '<span class="mnp-chip mnp-chip-active">' + escapeHtml(statusLabel(plan.status)) + '</span>';
    html += '</div>';
    if (plan.totalKcal != null) {
      html += '<div class="mnp-plan-kcal">' + escapeHtml(String(plan.totalKcal)) + ' kcal/día</div>';
    }
    if (plan.startDate) {
      html += '<div class="mnp-muted mt-1">Desde ' + escapeHtml(formatLocalDate(plan.startDate)) + '</div>';
    }
    html += '</div>';

    html += '<div class="mnp-card-title">Ingestas del día</div>';
    html += '<div class="mnp-card">';
    var ingestas = (detail && detail.ingestas) ? detail.ingestas : [];
    if (!ingestas.length) {
      html += '<div class="mnp-empty">Este plan aún no tiene ingestas.</div>';
    } else {
      ingestas.forEach(function (ingesta) {
        var count = itemCount(ingesta);
        var kcal = ingesta.totalKcal != null ? ingesta.totalKcal : '—';
        html += '<div class="mnp-ingesta">';
        html += '<div class="mnp-ingesta-icon"><i class="fas fa-utensils"></i></div>';
        html += '<div class="mnp-ingesta-body">';
        html += '<div class="mnp-ingesta-tipo">' + escapeHtml(ingesta.tipo || 'Ingesta') + '</div>';
        html += '<div class="mnp-ingesta-meta">' + escapeHtml(String(count)) + ' elementos · '
          + escapeHtml(String(kcal)) + ' kcal</div>';
        html += '</div></div>';
      });
    }
    html += '</div>';

    return html;
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
    $('#mnpPreviewLoading').hide();
    $('#mnpPreviewError').hide();
    $('#mnpHomePanel').html(renderHome(data));
    $('#mnpDietPanel').html(renderDiet(data));
    activateTab('home');
    $('#mnpPreviewContent').addClass('is-visible');
  }

  function activateTab(tab) {
    $('.mnp-tab').removeClass('active');
    $('.mnp-panel').attr('hidden', true);
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

    $(document).on('click', '#mnpTabHome', function (e) {
      e.preventDefault();
      activateTab('home');
    });

    $(document).on('click', '#mnpTabDiet', function (e) {
      e.preventDefault();
      activateTab('diet');
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

'use strict';

/**
 * Send / resend / revoke mobile app invitations from patient grid and profile (#341).
 */
(function ($) {
  if (!$) {
    return;
  }

  function invitationUrl(pacienteId) {
    return '/rest/pacientes/' + pacienteId + '/mobile-invitation';
  }

  function errorMessage(xhr, fallback) {
    if (xhr.responseJSON) {
      if (xhr.responseJSON.message) {
        return xhr.responseJSON.message;
      }
      if (xhr.responseJSON.error && typeof xhr.responseJSON.error === 'string') {
        return xhr.responseJSON.error;
      }
    }
    return fallback;
  }

  function confirmSend(isResend, recipientEmailRedacted, callback) {
    var title = isResend ? 'Reenviar invitación' : 'Invitar a la app móvil';
    var text = isResend
      ? 'Se revocará la invitación anterior y se enviará un nuevo correo al paciente.'
      : 'Se enviará un correo al paciente con un enlace para registrarse en la app móvil.';
    if (recipientEmailRedacted) {
      text += ' Destinatario: ' + recipientEmailRedacted + '.';
    }
    swal({
      title: title,
      text: text,
      type: 'warning',
      showCancelButton: true,
      confirmButtonText: isResend ? 'Reenviar' : 'Enviar invitación',
      cancelButtonText: 'Cancelar',
      closeOnConfirm: false,
      showLoaderOnConfirm: true
    }, function (isConfirm) {
      if (isConfirm) {
        callback();
      }
    });
  }

  function confirmRevoke(callback) {
    swal({
      title: 'Revocar invitación',
      text: 'El enlace de invitación dejará de ser válido. ¿Continuar?',
      type: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#d33',
      confirmButtonText: 'Revocar',
      cancelButtonText: 'Cancelar',
      closeOnConfirm: false,
      showLoaderOnConfirm: true
    }, function (isConfirm) {
      if (isConfirm) {
        callback();
      }
    });
  }

  function showSuccess(title, text, onClose) {
    swal({
      title: title,
      text: text,
      type: 'success',
      timer: 2500,
      showConfirmButton: false
    });
    if (onClose) {
      setTimeout(onClose, 2600);
    }
  }

  function showError(message) {
    swal({
      title: 'No se pudo completar',
      text: message,
      type: 'error',
      timer: 5000
    });
  }

  function postInvitation(pacienteId, options) {
    $.ajax({
      url: invitationUrl(pacienteId),
      type: 'POST',
      success: function (data) {
        var codeText = data.humanCode ? ' Código: ' + data.humanCode + '.' : '';
        showSuccess('Invitación enviada', (data.message || 'Correo enviado correctamente.') + codeText,
          options && options.onChanged);
      },
      error: function (xhr) {
        if (typeof swal.enableButtons === 'function') {
          swal.enableButtons();
        }
        showError(errorMessage(xhr, 'Error al enviar la invitación.'));
      }
    });
  }

  function deleteInvitation(pacienteId, options) {
    $.ajax({
      url: invitationUrl(pacienteId),
      type: 'DELETE',
      success: function (data) {
        showSuccess('Invitación revocada', data.message || 'La invitación fue revocada.', options && options.onChanged);
      },
      error: function (xhr) {
        if (typeof swal.enableButtons === 'function') {
          swal.enableButtons();
        }
        showError(errorMessage(xhr, 'Error al revocar la invitación.'));
      }
    });
  }

  function sendFromGrid(pacienteId, options) {
    confirmSend(false, null, function () {
      postInvitation(pacienteId, options);
    });
  }

  function bindGrid(options) {
    $(document).on('click', '.paciente-mobile-invite-btn', function (event) {
      event.preventDefault();
      var pacienteId = $(this).data('id');
      if (pacienteId) {
        sendFromGrid(pacienteId, options);
      }
    });
  }

  function refreshProfileUi(data) {
    var badgeEl = document.getElementById('mobile-invitation-status-badge');
    var detailsEl = document.getElementById('mobile-invitation-details');
    var sendBtn = document.getElementById('mobile-invitation-send-btn');
    var resendBtn = document.getElementById('mobile-invitation-resend-btn');
    var revokeBtn = document.getElementById('mobile-invitation-revoke-btn');
    if (!badgeEl) {
      return;
    }
    badgeEl.textContent = data.stateLabel || '';
    badgeEl.className = 'badge ' + profileBadgeClass(data.stateCode);
    if (detailsEl) {
      var parts = [];
      if (data.humanCode) {
        parts.push('Código: ' + data.humanCode);
      }
      if (data.expiresAt) {
        parts.push('Vence: ' + data.expiresAt.replace('T', ' ').replace('Z', ' UTC'));
      }
      if (data.recipientEmailRedacted) {
        parts.push('Correo: ' + data.recipientEmailRedacted);
      }
      detailsEl.textContent = parts.join(' · ');
      detailsEl.classList.toggle('d-none', parts.length === 0);
    }
    if (sendBtn) {
      sendBtn.classList.toggle('d-none', !data.canSend);
    }
    if (resendBtn) {
      resendBtn.classList.toggle('d-none', !data.canResend);
    }
    if (revokeBtn) {
      revokeBtn.classList.toggle('d-none', !data.canRevoke);
    }
  }

  function profileBadgeClass(stateCode) {
    switch (stateCode) {
      case 'LINKED':
        return 'badge-success';
      case 'ONBOARDING':
        return 'badge-info';
      case 'PENDING':
        return 'badge-warning';
      case 'REVOKED':
        return 'badge-dark';
      case 'NO_EMAIL':
        return 'badge-secondary';
      default:
        return 'badge-secondary';
    }
  }

  function reloadProfileStatus(pacienteId) {
    return $.getJSON(invitationUrl(pacienteId)).done(function (data) {
      if (data && data.success) {
        refreshProfileUi(data);
      }
    });
  }

  function bindProfile(pacienteId) {
    var sendBtn = document.getElementById('mobile-invitation-send-btn');
    var resendBtn = document.getElementById('mobile-invitation-resend-btn');
    var revokeBtn = document.getElementById('mobile-invitation-revoke-btn');
    var options = {
      onChanged: function () {
        reloadProfileStatus(pacienteId);
      }
    };

    if (sendBtn) {
      sendBtn.addEventListener('click', function () {
        var recipient = sendBtn.getAttribute('data-recipient') || '';
        confirmSend(false, recipient, function () {
          postInvitation(pacienteId, options);
        });
      });
    }
    if (resendBtn) {
      resendBtn.addEventListener('click', function () {
        var recipient = resendBtn.getAttribute('data-recipient') || '';
        confirmSend(true, recipient, function () {
          postInvitation(pacienteId, options);
        });
      });
    }
    if (revokeBtn) {
      revokeBtn.addEventListener('click', function () {
        confirmRevoke(function () {
          deleteInvitation(pacienteId, options);
        });
      });
    }
  }

  window.PacienteMobileInvitation = {
    bindGrid: bindGrid,
    bindProfile: bindProfile,
    sendInvitation: postInvitation,
    revokeInvitation: deleteInvitation
  };
})(window.jQuery);

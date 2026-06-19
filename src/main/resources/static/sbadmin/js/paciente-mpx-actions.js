'use strict';

/**
 * Export / delete patient actions with optional pre-delete MPX export (#223).
 */
(function ($) {
  if (!$) {
    return;
  }

  var HISTORY_WARNING = 'Si elimina este paciente, <strong>perderá todo su historial</strong> en Minutriporcion '
    + '(consultas, dietas asignadas, mediciones, mensajes, etc.). Solo podrá recuperar '
    + '<strong>los datos de registro</strong> si exportó o exporta ahora un archivo <code>.mpx</code> '
    + 'e importa ese archivo más adelante.';

  var EXPORT_CHECKBOX_HTML = '<div class="text-left mt-3">'
    + '<label class="d-flex align-items-start">'
    + '<input type="checkbox" id="mpx-export-before-delete" class="mr-2 mt-1" checked>'
    + '<span>Exportar registro (.mpx) antes de eliminar</span>'
    + '</label>'
    + '</div>';

  function parseFilename(disposition) {
    if (!disposition) {
      return 'paciente.mpx';
    }
    var match = disposition.match(/filename="([^"]+)"/);
    return match ? match[1] : 'paciente.mpx';
  }

  function downloadMpx(pacienteId) {
    return fetch('/admin/pacientes/' + pacienteId + '/export.mpx', {
      credentials: 'same-origin'
    }).then(function (response) {
      if (!response.ok) {
        throw new Error('export_failed');
      }
      return response.blob().then(function (blob) {
        var filename = parseFilename(response.headers.get('Content-Disposition'));
        var url = window.URL.createObjectURL(blob);
        var link = document.createElement('a');
        link.href = url;
        link.download = filename;
        document.body.appendChild(link);
        link.click();
        link.remove();
        window.URL.revokeObjectURL(url);
      });
    });
  }

  function confirmFinalDelete(pacienteId, options) {
    swal({
      title: 'Confirmar eliminación',
      text: 'Esta acción no se puede deshacer. ¿Eliminar el paciente de forma permanente?',
      type: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#d33',
      confirmButtonText: 'Sí, eliminar',
      cancelButtonText: 'Cancelar',
      closeOnConfirm: false
    }, function (isConfirm) {
      if (!isConfirm) {
        return;
      }
      $.ajax({
        url: '/rest/pacientes/' + pacienteId,
        type: 'DELETE',
        success: function () {
          swal({
            title: 'Paciente eliminado',
            text: 'El paciente y su historial clínico fueron eliminados.',
            type: 'success',
            timer: 2000
          });
          if (options && options.onDeleted) {
            options.onDeleted();
          }
        },
        error: function (xhr) {
          var message = 'Error al eliminar el paciente.';
          if (xhr.responseJSON && xhr.responseJSON.error) {
            message = xhr.responseJSON.error;
          }
          swal({
            title: 'No se pudo eliminar',
            text: message,
            type: 'error',
            timer: 5000
          });
        }
      });
    });
  }

  function startDeleteFlow(pacienteId, options) {
    if (typeof swal === 'undefined') {
      return;
    }
    swal({
      title: 'Eliminar paciente',
      text: HISTORY_WARNING + EXPORT_CHECKBOX_HTML,
      html: true,
      type: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Continuar',
      cancelButtonText: 'Cancelar',
      closeOnConfirm: false
    }, function (isConfirm) {
      if (!isConfirm) {
        return;
      }
      var exportCheckbox = document.getElementById('mpx-export-before-delete');
      var exportBeforeDelete = exportCheckbox && exportCheckbox.checked;
      swal.close();
      if (exportBeforeDelete) {
        downloadMpx(pacienteId).then(function () {
          confirmFinalDelete(pacienteId, options);
        }).catch(function () {
          swal({
            title: 'Exportación requerida',
            text: 'No se pudo exportar el archivo .mpx. El paciente no fue eliminado.',
            type: 'error',
            timer: 5000
          });
        });
      } else {
        confirmFinalDelete(pacienteId, options);
      }
    });
  }

  function exportPaciente(pacienteId) {
    window.location.href = '/admin/pacientes/' + pacienteId + '/export.mpx';
  }

  function bindPacienteMpxActions(options) {
    $(document).on('click', '.paciente-export-btn', function (event) {
      event.preventDefault();
      var pacienteId = $(this).data('id');
      if (pacienteId) {
        exportPaciente(pacienteId);
      }
    });

    $(document).on('click', '.paciente-delete-btn', function (event) {
      event.preventDefault();
      var pacienteId = $(this).data('id');
      if (pacienteId) {
        startDeleteFlow(pacienteId, options);
      }
    });
  }

  window.PacienteMpxActions = {
    bind: bindPacienteMpxActions,
    exportPaciente: exportPaciente,
    startDeleteFlow: startDeleteFlow
  };
})(window.jQuery);

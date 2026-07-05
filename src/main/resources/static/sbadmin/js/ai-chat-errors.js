/* AI chat error parsing and dialog titles (#399) */

(function (global) {
  'use strict';

  var GENERIC = 'No se pudo completar la solicitud.';

  function extractApiError(data, fallback) {
    if (!data || typeof data !== 'object') {
      return fallback || GENERIC;
    }
    if (data.message && String(data.message).trim()) {
      return String(data.message);
    }
    if (data.error && String(data.error).trim()) {
      return String(data.error);
    }
    return fallback || GENERIC;
  }

  function errorTitle(errorCode, status) {
    if (errorCode === 'RATE_LIMIT' || status === 429) {
      return 'Límite alcanzado';
    }
    if (errorCode === 'FORBIDDEN' || status === 403) {
      return 'Acceso restringido';
    }
    if (errorCode === 'NOT_FOUND' || status === 404) {
      return 'No encontrado';
    }
    if (errorCode === 'INTERNAL' || status === 503 || status === 502 || status === 504) {
      return 'Servicio no disponible';
    }
    return 'Error';
  }

  function createApiError(data, status) {
    var message = extractApiError(data);
    var error = new Error(message);
    error.status = status;
    if (data && data.errorCode) {
      error.errorCode = data.errorCode;
    }
    error.title = errorTitle(error.errorCode, status);
    return error;
  }

  global.NutriAiChatErrors = {
    GENERIC: GENERIC,
    extractApiError: extractApiError,
    errorTitle: errorTitle,
    createApiError: createApiError
  };
})(typeof window !== 'undefined' ? window : this);

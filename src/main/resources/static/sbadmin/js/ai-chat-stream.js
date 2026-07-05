/* AI chat SSE streaming client (#435, #436) */

(function (global) {
  'use strict';

  var GENERIC = 'No se pudo completar la solicitud.';

  function parseEventBlock(block) {
    var lines = block.split('\n');
    var eventName = 'message';
    var dataLines = [];
    lines.forEach(function (line) {
      if (line.indexOf('event:') === 0) {
        eventName = line.slice(6).trim();
      } else if (line.indexOf('data:') === 0) {
        dataLines.push(line.slice(5).trim());
      }
    });
    if (dataLines.length === 0) {
      return null;
    }
    var raw = dataLines.join('\n');
    try {
      return { event: eventName, data: JSON.parse(raw) };
    } catch (e) {
      return { event: eventName, data: { raw: raw } };
    }
  }

  function buildStreamError(data) {
    if (global.NutriAiChatErrors && global.NutriAiChatErrors.createApiError) {
      return global.NutriAiChatErrors.createApiError(data || {});
    }
    var message = data && data.message ? data.message : GENERIC;
    return new Error(message);
  }

  function dispatchEvent(parsed, handlers) {
    if (!parsed) {
      return;
    }
    if (parsed.event === 'status' && handlers.onStatus) {
      handlers.onStatus(parsed.data);
      return;
    }
    if (parsed.event === 'delta' && handlers.onDelta && parsed.data && parsed.data.content) {
      handlers.onDelta(parsed.data.content);
      return;
    }
    if (parsed.event === 'done' && handlers.onDone) {
      handlers.onDone(parsed.data);
      return;
    }
    if (parsed.event === 'error' && handlers.onError) {
      handlers.onError(buildStreamError(parsed.data));
    }
  }

  function isAbortError(error) {
    return error && (error.name === 'AbortError' || error.code === 20);
  }

  function streamMessage(url, payload, handlers) {
    var controller = new AbortController();
    var fetchPromise = fetch(url, {
      method: 'POST',
      credentials: 'same-origin',
      signal: controller.signal,
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream'
      },
      body: JSON.stringify(payload)
    }).then(function (response) {
      var contentType = response.headers.get('content-type') || '';
      if (!response.ok) {
        if (contentType.indexOf('application/json') >= 0) {
          return response.json().then(function (data) {
            throw buildStreamError(data);
          });
        }
        throw new Error(GENERIC);
      }
      if (!response.body || !response.body.getReader) {
        throw new Error('Streaming no disponible en este navegador.');
      }

      var reader = response.body.getReader();
      var decoder = new TextDecoder('utf-8');
      var buffer = '';

      function readNext() {
        return reader.read().then(function (result) {
          if (result.done) {
            if (buffer.trim()) {
              buffer.split(/\n\n/).forEach(function (block) {
                dispatchEvent(parseEventBlock(block.trim()), handlers);
              });
            }
            return null;
          }
          buffer += decoder.decode(result.value, { stream: true });
          var parts = buffer.split(/\n\n/);
          buffer = parts.pop() || '';
          parts.forEach(function (block) {
            dispatchEvent(parseEventBlock(block.trim()), handlers);
          });
          return readNext();
        });
      }

      return readNext();
    }).catch(function (error) {
      if (isAbortError(error)) {
        if (handlers.onAbort) {
          handlers.onAbort();
        }
        return null;
      }
      throw error;
    });

    fetchPromise.abort = function () {
      controller.abort();
    };
    return fetchPromise;
  }

  global.NutriAiChatStream = {
    streamMessage: streamMessage,
    isAbortError: isAbortError
  };
})(typeof window !== 'undefined' ? window : this);

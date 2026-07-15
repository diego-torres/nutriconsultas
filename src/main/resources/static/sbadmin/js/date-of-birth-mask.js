'use strict';

/**
 * Auto-inserts slashes while typing a date of birth as dd/mm/yyyy.
 * Keeps the numeric mobile keyboard usable (no manual "/").
 */
(function (root, factory) {
  if (typeof module === 'object' && module.exports) {
    module.exports = factory();
  }
  else {
    var api = factory();
    root.DateOfBirthMask = api;
    if (typeof document !== 'undefined') {
      if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', function () {
          api.bindAll();
        });
      }
      else {
        api.bindAll();
      }
    }
  }
})(typeof self !== 'undefined' ? self : this, function () {
  var MAX_DIGITS = 8;
  var MAX_LENGTH = 10;

  /**
   * Formats raw input into dd/mm/yyyy with automatic slashes.
   * @param {string} value
   * @returns {string}
   */
  function format(value) {
    var digits = String(value == null ? '' : value).replace(/\D/g, '').slice(0, MAX_DIGITS);
    var length = digits.length;
    var result = '';

    if (length === 0) {
      result = '';
    }
    else if (length <= 2) {
      result = digits;
    }
    else if (length <= 4) {
      result = digits.slice(0, 2) + '/' + digits.slice(2);
    }
    else {
      result = digits.slice(0, 2) + '/' + digits.slice(2, 4) + '/' + digits.slice(4);
    }

    return result;
  }

  /**
   * @param {HTMLInputElement} input
   */
  function bind(input) {
    if (!input || input.getAttribute('data-dob-mask-bound') === 'true') {
      return;
    }

    input.setAttribute('data-dob-mask-bound', 'true');
    input.setAttribute('inputmode', 'numeric');
    input.setAttribute('maxlength', String(MAX_LENGTH));
    input.setAttribute('autocomplete', input.getAttribute('autocomplete') || 'bday');

    if (!input.getAttribute('placeholder')) {
      input.setAttribute('placeholder', 'dd/mm/aaaa');
    }

    input.addEventListener('input', function () {
      var formatted = format(input.value);
      if (input.value !== formatted) {
        input.value = formatted;
      }
    });

    input.addEventListener('blur', function () {
      input.value = format(input.value);
    });

    if (input.value) {
      input.value = format(input.value);
    }
  }

  /**
   * @param {ParentNode} [root]
   */
  function bindAll(root) {
    var scope = root || document;
    if (!scope || !scope.querySelectorAll) {
      return;
    }
    var inputs = scope.querySelectorAll('input[data-dob-mask], input[name="dob"]');
    for (var i = 0; i < inputs.length; i++) {
      bind(inputs[i]);
    }
  }

  return {
    format: format,
    bind: bind,
    bindAll: bindAll,
    MAX_LENGTH: MAX_LENGTH
  };
});

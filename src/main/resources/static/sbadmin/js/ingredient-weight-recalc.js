'use strict';

/**
 * Recalculate ingredient net weight (g) when portion quantity changes (#239).
 */
(function (global) {
  function parseFractionalQuantity(given) {
    if (!given) {
      return null;
    }
    var trimmed = String(given).trim();
    if (!trimmed) {
      return null;
    }

    var hasInteger = trimmed.indexOf(' ') >= 0 || trimmed.indexOf('/') < 0;
    var hasFraction = trimmed.indexOf('/') >= 0;
    var integerPart = hasInteger ? parseInt(trimmed.split(' ')[0], 10) : 0;
    var numeratorPart;
    var denominatorPart;

    if (hasInteger) {
      if (hasFraction) {
        var fractionParts = trimmed.split(' ')[1].split('/');
        numeratorPart = parseInt(fractionParts[0], 10);
        denominatorPart = parseInt(fractionParts[1], 10);
      } else {
        numeratorPart = 0;
        denominatorPart = 0;
      }
    } else {
      var slashParts = trimmed.split('/');
      numeratorPart = parseInt(slashParts[0], 10);
      denominatorPart = parseInt(slashParts[1], 10);
    }

    var fractionalValue = hasFraction ? numeratorPart / denominatorPart : 0;
    return integerPart + fractionalValue;
  }

  function recalculatePesoNeto(referenceCantSugerida, referencePesoNeto, newCantSugerida) {
    if (!referenceCantSugerida || referencePesoNeto == null || newCantSugerida == null) {
      return referencePesoNeto;
    }
    var factor = newCantSugerida / referenceCantSugerida;
    return Math.round(referencePesoNeto * factor);
  }

  function recalculatePesoFromQuantityInput(referenceCantSugerida, referencePesoNeto, quantityInput) {
    var parsedQuantity = parseFractionalQuantity(quantityInput);
    if (parsedQuantity == null) {
      return null;
    }
    return recalculatePesoNeto(referenceCantSugerida, referencePesoNeto, parsedQuantity);
  }

  global.IngredientWeightRecalc = {
    parseFractionalQuantity: parseFractionalQuantity,
    recalculatePesoNeto: recalculatePesoNeto,
    recalculatePesoFromQuantityInput: recalculatePesoFromQuantityInput
  };
}(typeof window !== 'undefined' ? window : this));

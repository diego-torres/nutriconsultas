(function ($) {
    'use strict';
  
    if ($(".js-example-basic-single").length) {
      $(".js-example-basic-single").select2({
        width: '100%'
      });
    }
    if ($(".select2-entity-modal").length) {
      $(".select2-entity-modal").select2({
        width: '100%',
        dropdownParent: $("#entityModal")
      });
    }
    if ($(".js-example-basic-multiple").length) {
      $(".js-example-basic-multiple").select2({
        width: '100%'
      });
    }
  })(jQuery);
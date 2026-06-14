/**
* PHP Email Form Validation - v3.4 (Minutriporcion: reCAPTCHA v2 checkbox + v3 fallback)
* URL: https://bootstrapmade.com/php-email-form/
*/
(function () {
  "use strict";

  const RECAPTCHA_MISSING_MSG = "Por favor, completa la verificación reCAPTCHA.";
  const RECAPTCHA_API_MSG = "No se pudo cargar reCAPTCHA. Recarga la página e inténtalo de nuevo.";

  let forms = document.querySelectorAll('.php-email-form');

  forms.forEach(function (form) {
    form.addEventListener('submit', function (event) {
      event.preventDefault();

      const thisForm = this;
      const action = thisForm.getAttribute('action');

      if (!action) {
        displayError(thisForm, 'The form action property is not set!');
        return;
      }

      thisForm.querySelector('.loading').classList.add('d-block');
      thisForm.querySelector('.error-message').classList.remove('d-block');
      thisForm.querySelector('.sent-message').classList.remove('d-block');

      const formData = new FormData(thisForm);
      const recaptchaWidget = thisForm.querySelector('.g-recaptcha');
      const recaptchaSiteKey = thisForm.getAttribute('data-recaptcha-site-key');

      if (recaptchaWidget) {
        submitWithRecaptchaV2(thisForm, action, formData, recaptchaWidget);
      } else if (recaptchaSiteKey) {
        submitWithRecaptchaV3(thisForm, action, formData, recaptchaSiteKey);
      } else {
        phpEmailFormSubmit(thisForm, action, formData);
      }
    });
  });

  function submitWithRecaptchaV2(thisForm, action, formData, recaptchaWidget) {
    if (typeof grecaptcha === "undefined") {
      displayError(thisForm, RECAPTCHA_API_MSG);
      return;
    }
    const token = grecaptcha.getResponse();
    if (!token) {
      thisForm.querySelector('.loading').classList.remove('d-block');
      displayError(thisForm, RECAPTCHA_MISSING_MSG);
      return;
    }
    formData.set('recaptcha-response', token);
    phpEmailFormSubmit(thisForm, action, formData, true);
  }

  function submitWithRecaptchaV3(thisForm, action, formData, recaptchaSiteKey) {
    if (typeof grecaptcha === "undefined") {
      displayError(thisForm, RECAPTCHA_API_MSG);
      return;
    }
    grecaptcha.ready(function () {
      try {
        grecaptcha.execute(recaptchaSiteKey, { action: 'php_email_form_submit' })
          .then(function (token) {
            formData.set('recaptcha-response', token);
            phpEmailFormSubmit(thisForm, action, formData);
          });
      } catch (error) {
        displayError(thisForm, error);
      }
    });
  }

  function phpEmailFormSubmit(thisForm, action, formData, resetCaptchaOnComplete) {
    fetch(action, {
      method: 'POST',
      body: formData,
      headers: { 'X-Requested-With': 'XMLHttpRequest' }
    })
      .then(function (response) {
        return response.text().then(function (text) {
          if (response.ok) {
            return text;
          }
          throw new Error(text && text.trim() ? text.trim()
            : response.status + ' ' + response.statusText);
        });
      })
      .then(function (data) {
        thisForm.querySelector('.loading').classList.remove('d-block');
        if (data.trim() === 'OK') {
          thisForm.querySelector('.sent-message').classList.add('d-block');
          thisForm.reset();
          resetRecaptcha(resetCaptchaOnComplete);
        } else {
          throw new Error(data ? data : 'Form submission failed and no error message returned from: ' + action);
        }
      })
      .catch(function (error) {
        resetRecaptcha(resetCaptchaOnComplete);
        displayError(thisForm, error);
      });
  }

  function resetRecaptcha(shouldReset) {
    if (shouldReset && typeof grecaptcha !== "undefined") {
      grecaptcha.reset();
    }
  }

  function displayError(thisForm, error) {
    thisForm.querySelector('.loading').classList.remove('d-block');
    const message = error && error.message ? error.message : String(error);
    thisForm.querySelector('.error-message').textContent = message;
    thisForm.querySelector('.error-message').classList.add('d-block');
  }

})();

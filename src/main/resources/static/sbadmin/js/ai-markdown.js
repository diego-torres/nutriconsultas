/* XSS-safe Markdown rendering for AI assistant messages (#434) */

(function (global) {
  'use strict';

  var ALLOWED_TAGS = [
    'p', 'br', 'strong', 'b', 'em', 'i', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
    'ul', 'ol', 'li', 'code', 'pre', 'blockquote', 'hr', 'a', 'span'
  ];

  var ALLOWED_ATTR = ['href', 'title', 'class', 'rel', 'target'];

  function escapeHtml(text) {
    var div = document.createElement('div');
    div.textContent = text == null ? '' : String(text);
    return div.innerHTML;
  }

  function renderPlain(text) {
    return escapeHtml(text).replace(/\n/g, '<br>');
  }

  function configureLinkSanitizer() {
    if (typeof DOMPurify === 'undefined' || !DOMPurify.addHook) {
      return;
    }
    if (global.__nutriAiMarkdownHookInstalled) {
      return;
    }
    DOMPurify.addHook('afterSanitizeAttributes', function (node) {
      if (node.tagName === 'A') {
        var href = node.getAttribute('href') || '';
        if (/^https?:/i.test(href)) {
          node.setAttribute('target', '_blank');
          node.setAttribute('rel', 'noopener noreferrer');
        } else {
          node.removeAttribute('target');
          node.removeAttribute('rel');
        }
      }
    });
    global.__nutriAiMarkdownHookInstalled = true;
  }

  function sanitizeHtml(html) {
    if (typeof DOMPurify === 'undefined' || !DOMPurify.sanitize) {
      return renderPlain(String(html).replace(/<[^>]*>/g, ''));
    }
    configureLinkSanitizer();
    return DOMPurify.sanitize(html, {
      ALLOWED_TAGS: ALLOWED_TAGS,
      ALLOWED_ATTR: ALLOWED_ATTR,
      ALLOW_DATA_ATTR: false,
      FORBID_TAGS: ['script', 'style', 'iframe', 'object', 'embed', 'form', 'input'],
      ALLOWED_URI_REGEXP: /^(?:(?:https?|mailto):|[^a-z]|[a-z+.\-]+(?:[^a-z+.\-:]|$))/i
    });
  }

  function parseMarkdown(text) {
    if (typeof marked === 'undefined') {
      return null;
    }
    if (typeof marked.parse === 'function') {
      return marked.parse(text, { async: false, breaks: true, gfm: true });
    }
    if (typeof marked === 'function') {
      marked.setOptions({ breaks: true, gfm: true });
      return marked(text);
    }
    return null;
  }

  function renderAssistantMarkdown(text) {
    if (text == null || text === '') {
      return '';
    }
    var raw = String(text);
    try {
      var parsed = parseMarkdown(raw);
      if (parsed != null) {
        return sanitizeHtml(parsed);
      }
    } catch (e) {
      /* fall through to plain text */
    }
    return renderPlain(raw);
  }

  function formatMessageContent(role, content) {
    if (role === 'ASSISTANT') {
      return '<div class="ai-markdown">' + renderAssistantMarkdown(content) + '</div>';
    }
    return escapeHtml(content);
  }

  global.NutriAiMarkdown = {
    escapeHtml: escapeHtml,
    renderAssistantMarkdown: renderAssistantMarkdown,
    formatMessageContent: formatMessageContent
  };
})(typeof window !== 'undefined' ? window : this);

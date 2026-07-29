/**
 * Reports a GA4 `code_copy` event whenever a visitor copies a code snippet.
 *
 * Docusaurus v1 renders pages to static markup and never hydrates React on the
 * client, so there are no component-level handlers to hook into. Instead we
 * listen for the native `copy` event on the document, which covers Ctrl/Cmd+C,
 * the context menu and the browser menu, and works on every page including the
 * snippets inside markdown docs.
 *
 * Copies touching no <pre> at all (prose, headings, table cells) are ignored, as
 * are copies touching several snippets at once, which belong to no single one.
 *
 * Event parameters, all derived from the rendered DOM:
 *   snippet_section  - slug of the nearest preceding heading, e.g. `quick-start`
 *   snippet_language - from the highlight.js `language-*` class, e.g. `xml`
 *   snippet_tab      - code tab label when the snippet is tabbed, e.g. `Maven`
 *   copy_scope       - `full`, `includes_full` or `partial`
 *   chars_copied     - length of the copied text
 *   debug_mode       - set on local dev only, to route events to DebugView
 *
 * The copied text itself is deliberately never sent.
 */
(function () {
  // Only content headings carry an `a.anchor`; the navbar and sidebar headings
  // do not, which keeps site chrome out of the section lookup.
  var HEADING_ANCHORS = 'a.anchor[id]';
  var LANGUAGE_PREFIX = 'language-';
  // Copies made against a local dev server are flagged so that they show up in
  // GA4 DebugView. Traffic from the published site is never flagged.
  var DEBUG_HOSTS = ['localhost', '127.0.0.1', '[::1]'];

  // GA4 truncates parameter values at 100 characters.
  function trim(value) {
    return String(value || '').replace(/\s+/g, ' ').trim().slice(0, 100);
  }

  function toElement(node) {
    if (!node) return null;
    return node.nodeType === Node.TEXT_NODE ? node.parentElement : node;
  }

  // Slug of the last heading appearing before the snippet in document order.
  function sectionOf(pre) {
    var anchors = document.querySelectorAll(HEADING_ANCHORS);
    var slug = 'none';
    for (var i = 0; i < anchors.length; i++) {
      var position = anchors[i].compareDocumentPosition(pre);
      if (position & Node.DOCUMENT_POSITION_FOLLOWING) {
        slug = anchors[i].id;
      } else {
        break;
      }
    }
    return slug;
  }

  function languageOf(pre) {
    var code = pre.querySelector('code');
    var classes = code ? code.classList : [];
    for (var i = 0; i < classes.length; i++) {
      if (classes[i].indexOf(LANGUAGE_PREFIX) === 0) {
        return classes[i].slice(LANGUAGE_PREFIX.length);
      }
    }
    return 'unknown';
  }

  // Tabbed snippets sit in a `.tab-pane` whose id is referenced by the
  // `data-tab` of the nav item holding the human readable label.
  function tabOf(pre) {
    var pane = pre.closest('.tab-pane');
    if (!pane || !pane.id) return null;
    var navItem = document.querySelector('.nav-link[data-tab="' + pane.id + '"]');
    return navItem ? navItem.textContent : null;
  }

  // Which snippet, if any, a selection belongs to. A selection made entirely
  // inside a snippet has its common ancestor below the <pre>, but dragging past
  // the edges of a block, triple clicking or Ctrl+A puts the ancestor above it,
  // so fall back to the snippets the selection actually touches. Selections
  // covering several snippets belong to no single one and are dropped.
  // Note that Range.intersectsNode is used rather than Selection.containsNode:
  // the latter reports partial containment for nodes the range never touches.
  function resolvePre(selection) {
    var range = selection.getRangeAt(0);
    var origin = toElement(range.commonAncestorContainer);
    var within = origin && origin.closest ? origin.closest('pre') : null;
    if (within) return within;

    var pres = document.querySelectorAll('pre');
    var touched = null;
    for (var i = 0; i < pres.length; i++) {
      if (!range.intersectsNode(pres[i])) continue;
      if (touched) return null;
      touched = pres[i];
    }
    return touched;
  }

  // `full` if the snippet and nothing else was taken, `includes_full` if it came
  // along with surrounding text, `partial` if only a fragment was taken.
  function scopeOf(copied, whole) {
    if (copied === whole) return 'full';
    return copied.indexOf(whole) === -1 ? 'partial' : 'includes_full';
  }

  document.addEventListener('copy', function () {
    if (!window.gtag) return;

    var selection = window.getSelection();
    if (!selection || selection.isCollapsed || !selection.rangeCount) return;

    var copied = selection.toString();
    if (!copied) return;

    var pre = resolvePre(selection);
    if (!pre) return;

    // `innerText` respects the <br> tags Docusaurus emits for newlines, so the
    // comparison is against the snippet as the visitor sees it.
    var whole = pre.innerText || '';
    var params = {
      snippet_section: trim(sectionOf(pre)),
      snippet_language: trim(languageOf(pre)),
      copy_scope: scopeOf(copied.trim(), whole.trim()),
      chars_copied: copied.length,
    };

    var tab = tabOf(pre);
    if (tab) params.snippet_tab = trim(tab);

    if (DEBUG_HOSTS.indexOf(window.location.hostname) !== -1) {
      params.debug_mode = true;
    }

    window.gtag('event', 'code_copy', params);
  });
})();

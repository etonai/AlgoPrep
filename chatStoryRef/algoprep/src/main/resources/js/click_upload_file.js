window.chatStoryClickUploadFile = function(options) {
    var selectors = (options && options.selectors && options.selectors.uploadButton) || [];

    // Strategy 1: try known button selectors from chatgpt_selectors.json
    for (var i = 0; i < selectors.length; i++) {
        var el = document.querySelector(selectors[i]);
        if (el) {
            console.log('[ChatStory] upload: clicking via selector:', selectors[i]);
            el.click();
            return;
        }
    }

    // Strategy 2: click a file input directly (bypasses the visible button entirely)
    var fileInputs = document.querySelectorAll('input[type="file"]');
    if (fileInputs.length > 0) {
        console.log('[ChatStory] upload: clicking file input directly');
        fileInputs[0].click();
        return;
    }

    // Strategy 3: search every button for an upload/attach aria-label
    var allButtons = document.querySelectorAll('button');
    for (var j = 0; j < allButtons.length; j++) {
        var btn = allButtons[j];
        var label = (btn.getAttribute('aria-label') || '').toLowerCase();
        if (label.indexOf('attach') !== -1 || label.indexOf('upload') !== -1 || label.indexOf('file') !== -1) {
            console.log('[ChatStory] upload: clicking button by aria-label:', btn.getAttribute('aria-label'));
            btn.click();
            return;
        }
    }

    console.warn('[ChatStory] upload: no upload button found.',
                 'Tried selectors:', selectors,
                 '| file inputs on page:', fileInputs.length,
                 '| total buttons on page:', allButtons.length);
};

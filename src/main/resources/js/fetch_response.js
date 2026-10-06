(function() {
    function post(message) {
        if (typeof window.cefQuery !== 'function') {
            console.error('[bridge] cefQuery unavailable for manualFetch', message);
            return;
        }
        window.cefQuery({
            request: JSON.stringify(message),
            onFailure: function(code, msg) {
                console.error('[bridge] manualFetch delivery failed:', code, msg);
            }
        });
    }

    function textOf(node) {
        if (!node) return '';
        var content = node.querySelector('.markdown')
            || node.querySelector('[data-message-content]')
            || node;
        return (content.innerText || content.textContent || '').replace(/\r\n/g, '\n').trim();
    }

    function htmlOf(node) {
        if (!node) return '';
        var content = node.querySelector('.markdown')
            || node.querySelector('[data-message-content]')
            || node;
        return content.innerHTML || '';
    }

    window.chatStoryFetchResponse = function(options) {
        var requestId = options && options.requestId || 0;
        console.log('[manualFetch] start requestId=' + requestId);
        try {
            var assistants = document.querySelectorAll("[data-message-author-role='assistant']");
            var last = assistants.length > 0 ? assistants[assistants.length - 1] : null;
            var text = textOf(last);
            console.log('[manualFetch] complete requestId=' + requestId
                + ' found=' + !!last
                + ' textLen=' + text.length);
            post({
                type: 'manualFetch',
                requestId: requestId,
                ok: true,
                text: text,
                html: htmlOf(last),
                message: text ? 'Response fetched' : 'No assistant response found in DOM'
            });
        } catch (e) {
            console.error('[manualFetch] threw requestId=' + requestId, e);
            post({
                type: 'manualFetch',
                requestId: requestId,
                ok: false,
                errorCode: 'fetch_failed',
                message: String(e && e.message || e)
            });
        }
    };
})();

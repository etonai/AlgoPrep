(function() {
    function post(message) {
        if (typeof window.cefQuery !== 'function') {
            console.error('[bridge] cefQuery unavailable for responseComplete', message);
            return;
        }
        window.cefQuery({
            request: JSON.stringify(message),
            onFailure: function(code, msg) {
                console.error('[bridge] responseComplete delivery failed:', code, msg);
            }
        });
    }

    function allMatches(selectors) {
        var result = [];
        if (!Array.isArray(selectors)) return result;
        for (var i = 0; i < selectors.length; i++) {
            document.querySelectorAll(selectors[i]).forEach(function(node) {
                if (result.indexOf(node) < 0) result.push(node);
            });
        }
        return result;
    }

    function visible(node) {
        if (!node) return false;
        var rect = node.getBoundingClientRect();
        var style = window.getComputedStyle(node);
        return rect.width > 0
            && rect.height > 0
            && style.display !== 'none'
            && style.visibility !== 'hidden'
            && style.opacity !== '0'
            && node.getAttribute('aria-hidden') !== 'true';
    }

    function anyVisible(selectors) {
        var nodes = allMatches(selectors);
        for (var i = 0; i < nodes.length; i++) {
            if (visible(nodes[i])) return true;
        }
        return false;
    }

    function enabled(node) {
        if (!visible(node)) return false;
        return !node.disabled
            && node.getAttribute('aria-disabled') !== 'true'
            && !node.hasAttribute('disabled');
    }

    function anyEnabled(selectors) {
        var nodes = allMatches(selectors);
        for (var i = 0; i < nodes.length; i++) {
            if (enabled(nodes[i])) return true;
        }
        return false;
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

    window.chatStoryExtractResponse = function(options) {
        var requestId = options && options.requestId || 0;
        console.log('[extract] start requestId=' + requestId);
        try {
            var selectors = options.selectors || {};
            var assistantBeforeCount = options.assistantBeforeCount || 0;
            var pollIntervalMs = options.pollIntervalMs || 500;
            var stabilityWindowMs = options.stabilityWindowMs || 1500;
            var timeoutMs = options.timeoutMs || 180000;
            var fallbackStabilityWindowMs = Math.max(stabilityWindowMs * 4, 8000);
            var startedAt = Date.now();
            var stableSince = 0;
            var lastText = '';
            var latestText = '';
            var pollCount = 0;
            var lastLogAt = 0;

            var timer = window.setInterval(function() {
                try {
                    pollCount++;
                    var assistants = allMatches(selectors.assistantMsg);
                    var target = assistants.length > assistantBeforeCount
                        ? assistants[assistantBeforeCount]
                        : null;
                    var generating = anyVisible(selectors.stopButton);
                    var sendReady = anyEnabled(selectors.sendButton);

                    if (Date.now() - lastLogAt >= 3000) {
                        lastLogAt = Date.now();
                        console.log('[extract] polling requestId=' + requestId
                            + ' poll#=' + pollCount
                            + ' hasTarget=' + !!target
                            + ' generating=' + generating
                            + ' sendReady=' + sendReady
                            + ' textLen=' + latestText.length
                            + ' stableForMs=' + (stableSince ? (Date.now() - stableSince) : 0));
                    }

                    if (target) {
                        latestText = textOf(target);
                        if (latestText && latestText === lastText) {
                            if (!stableSince) stableSince = Date.now();
                        } else {
                            lastText = latestText;
                            stableSince = latestText ? Date.now() : 0;
                        }

                        var stableForMs = stableSince ? Date.now() - stableSince : 0;
                        var completeByControls = !generating || sendReady;
                        var completeByFallback = stableForMs >= fallbackStabilityWindowMs;

                        if (latestText
                                && (completeByControls || completeByFallback)
                                && stableSince
                                && stableForMs >= stabilityWindowMs) {
                            window.clearInterval(timer);
                            console.log('[extract] complete requestId=' + requestId
                                + ' poll#=' + pollCount
                                + ' textLen=' + latestText.length
                                + ' elapsedMs=' + (Date.now() - startedAt));
                            post({
                                type: 'responseComplete',
                                requestId: requestId,
                                ok: true,
                                text: latestText,
                                html: htmlOf(target),
                                message: 'Response extracted'
                            });
                            return;
                        }
                    }

                    if (Date.now() - startedAt >= timeoutMs) {
                        window.clearInterval(timer);
                        console.warn('[extract] timeout requestId=' + requestId
                            + ' poll#=' + pollCount
                            + ' elapsedMs=' + (Date.now() - startedAt));
                        post({
                            type: 'responseComplete',
                            requestId: requestId,
                            ok: false,
                            text: latestText,
                            errorCode: 'timeout',
                            message: 'Timed out waiting for assistant response'
                        });
                    }
                } catch (e) {
                    window.clearInterval(timer);
                    console.error('[extract] poll iteration threw requestId=' + requestId, e);
                    post({
                        type: 'responseComplete',
                        requestId: requestId,
                        ok: false,
                        errorCode: 'extraction_failed',
                        message: String(e && e.message || e)
                    });
                }
            }, pollIntervalMs);
        } catch (e) {
            console.error('[extract] setup threw requestId=' + requestId, e);
            post({
                type: 'responseComplete',
                requestId: requestId,
                ok: false,
                errorCode: 'extraction_failed',
                message: String(e && e.message || e)
            });
        }
    };
})();

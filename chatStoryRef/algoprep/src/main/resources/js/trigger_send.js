(function() {
    function post(message) {
        if (typeof window.cefQuery !== 'function') {
            console.error('[bridge] cefQuery unavailable for sendResult', message);
            return;
        }
        window.cefQuery({
            request: JSON.stringify(message),
            onFailure: function(code, msg) {
                console.error('[bridge] sendResult delivery failed:', code, msg);
            }
        });
    }

    function firstMatch(selectors) {
        if (!Array.isArray(selectors)) return null;
        for (var i = 0; i < selectors.length; i++) {
            var found = document.querySelector(selectors[i]);
            if (found) return found;
        }
        return null;
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

    function textOf(node) {
        return (node && (node.innerText || node.textContent) || '').replace(/\r\n/g, '\n').trim();
    }

    function normalize(text) {
        return (text || '').replace(/\r\n/g, '\n').trim();
    }

    function isEnabled(button) {
        if (!button) return false;
        var rect = button.getBoundingClientRect();
        return rect.width > 0
            && rect.height > 0
            && !button.disabled
            && button.getAttribute('aria-disabled') !== 'true'
            && !button.hasAttribute('disabled');
    }

    function firstEnabled(selectors) {
        var nodes = allMatches(selectors);
        for (var i = 0; i < nodes.length; i++) {
            if (isEnabled(nodes[i])) return nodes[i];
        }
        return firstMatch(selectors);
    }

    function findMatchingUserMessage(selectors, expectedText, beforeCount) {
        var nodes = allMatches(selectors);
        for (var i = beforeCount; i < nodes.length; i++) {
            var actual = textOf(nodes[i]);
            if (actual === normalize(expectedText)) return nodes[i];
            if (actual.replace(/\n/g, '\n') === normalize(expectedText)) return nodes[i];
        }
        return null;
    }

    window.chatStoryTriggerSend = function(options) {
        var requestId = options && options.requestId || 0;
        try {
            var selectors = options.selectors || {};
            var expectedText = options.expectedText || '';
            var timeoutMs = options.timeoutMs || 5000;
            var beforeCount = allMatches(selectors.userMsg).length;
            var assistantBeforeCount = allMatches(selectors.assistantMsg).length;
            var button = firstEnabled(selectors.sendButton);

            if (!button) {
                post({
                    type: 'sendResult',
                    requestId: requestId,
                    ok: false,
                    errorCode: 'send_button_not_found',
                    message: 'Send button not found'
                });
                return;
            }
            if (!isEnabled(button)) {
                post({
                    type: 'sendResult',
                    requestId: requestId,
                    ok: false,
                    errorCode: 'send_button_disabled',
                    message: 'Send button was found but disabled'
                });
                return;
            }

            try {
                button.click();
            } catch (e) {
                post({
                    type: 'sendResult',
                    requestId: requestId,
                    ok: false,
                    errorCode: 'send_click_failed',
                    message: String(e && e.message || e)
                });
                return;
            }

            var startedAt = Date.now();
            var timer = window.setInterval(function() {
                if (findMatchingUserMessage(selectors.userMsg, expectedText, beforeCount)) {
                    window.clearInterval(timer);
                    post({
                        type: 'sendResult',
                        requestId: requestId,
                        ok: true,
                        text: String(assistantBeforeCount),
                        message: 'Prompt submitted'
                    });
                    return;
                }
                if (Date.now() - startedAt >= timeoutMs) {
                    window.clearInterval(timer);
                    post({
                        type: 'sendResult',
                        requestId: requestId,
                        ok: false,
                        errorCode: 'user_message_not_confirmed',
                        message: 'No matching user message appeared after send'
                    });
                }
            }, 100);
        } catch (e) {
            post({
                type: 'sendResult',
                requestId: requestId,
                ok: false,
                errorCode: 'send_click_failed',
                message: String(e && e.message || e)
            });
        }
    };
})();

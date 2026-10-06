(function() {
    function post(message) {
        if (typeof window.cefQuery !== 'function') {
            console.error('[bridge] cefQuery unavailable for injectResult', message);
            return;
        }
        window.cefQuery({
            request: JSON.stringify(message),
            onFailure: function(code, msg) {
                console.error('[bridge] injectResult delivery failed:', code, msg);
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

    function firstVisible(selectors) {
        var nodes = allMatches(selectors);
        for (var i = 0; i < nodes.length; i++) {
            var rect = nodes[i].getBoundingClientRect();
            if (rect.width > 0 && rect.height > 0) return nodes[i];
        }
        return nodes[0] || null;
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

    function sendButtonEnabled(selectors) {
        var buttons = allMatches(selectors);
        for (var i = 0; i < buttons.length; i++) {
            if (isEnabled(buttons[i])) return true;
        }
        return false;
    }

    function editorText(editor) {
        return (editor.value || editor.innerText || editor.textContent || '').replace(/\r\n/g, '\n');
    }

    function selectEditorContents(editor) {
        editor.focus();
        var selection = window.getSelection();
        var range = document.createRange();
        range.selectNodeContents(editor);
        selection.removeAllRanges();
        selection.addRange(range);
    }

    function setNativeValue(input, text) {
        var proto = input instanceof HTMLTextAreaElement
            ? HTMLTextAreaElement.prototype
            : HTMLInputElement.prototype;
        var descriptor = Object.getOwnPropertyDescriptor(proto, 'value');
        descriptor.set.call(input, text);
        input.dispatchEvent(new InputEvent('input', {
            bubbles: true,
            cancelable: false,
            inputType: 'insertText',
            data: text
        }));
        input.dispatchEvent(new Event('change', { bubbles: true }));
        return true;
    }

    function setWithExecCommand(editor, text) {
        if (editor instanceof HTMLTextAreaElement || editor instanceof HTMLInputElement) {
            return setNativeValue(editor, text);
        }
        selectEditorContents(editor);
        return document.execCommand('insertText', false, text);
    }

    function setWithPaste(editor, text) {
        editor.focus();
        selectEditorContents(editor);
        var data = new DataTransfer();
        data.setData('text/plain', text);
        var event = new ClipboardEvent('paste', {
            clipboardData: data,
            bubbles: true,
            cancelable: true
        });
        return editor.dispatchEvent(event);
    }

    function setWithManualEvents(editor, text) {
        editor.focus();
        selectEditorContents(editor);
        editor.dispatchEvent(new KeyboardEvent('keydown', {
            bubbles: true,
            cancelable: true,
            key: text.length === 1 ? text : 'Process'
        }));
        editor.dispatchEvent(new InputEvent('beforeinput', {
            bubbles: true,
            cancelable: true,
            inputType: 'insertText',
            data: text
        }));
        if (editor.isContentEditable) {
            editor.textContent = text;
        } else {
            editor.value = text;
        }
        editor.dispatchEvent(new InputEvent('input', {
            bubbles: true,
            cancelable: false,
            inputType: 'insertText',
            data: text
        }));
        editor.dispatchEvent(new KeyboardEvent('keyup', {
            bubbles: true,
            cancelable: true,
            key: text.length === 1 ? text : 'Process'
        }));
        return true;
    }

    function waitForEnabled(selectors, callback) {
        var attempts = 0;
        var timer = window.setInterval(function() {
            attempts++;
            if (sendButtonEnabled(selectors) || attempts >= 10) {
                window.clearInterval(timer);
                callback(sendButtonEnabled(selectors));
            }
        }, 100);
    }

    window.chatStoryInjectPrompt = function(options) {
        var requestId = options && options.requestId || 0;
        try {
            var selectors = options.selectors || {};
            var text = options.text || '';
            var editor = firstVisible(selectors.promptEditor);
            if (!editor) {
                post({
                    type: 'injectResult',
                    requestId: requestId,
                    ok: false,
                    errorCode: 'editor_not_found',
                    message: 'Prompt editor not found'
                });
                return;
            }

            var attempts = [
                { name: 'execCommand', run: function() { return setWithExecCommand(editor, text); } },
                { name: 'paste', run: function() { return setWithPaste(editor, text); } },
                { name: 'manualEvents', run: function() { return setWithManualEvents(editor, text); } }
            ];

            var index = 0;
            function tryNext() {
                if (index >= attempts.length) {
                    post({
                        type: 'injectResult',
                        requestId: requestId,
                        ok: false,
                        errorCode: 'prompt_injection_failed',
                        message: 'Prompt injection did not enable the send button; editor text=' + editorText(editor)
                    });
                    return;
                }
                var attempt = attempts[index++];
                try {
                    attempt.run();
                } catch (e) {
                    console.warn('[bridge] inject attempt failed:', attempt.name, e);
                }
                waitForEnabled(selectors.sendButton, function(enabled) {
                    if (enabled) {
                        post({
                            type: 'injectResult',
                            requestId: requestId,
                            ok: true,
                            message: 'Prompt injected using ' + attempt.name
                        });
                    } else {
                        tryNext();
                    }
                });
            }

            tryNext();
        } catch (e) {
            post({
                type: 'injectResult',
                requestId: requestId,
                ok: false,
                errorCode: 'prompt_injection_failed',
                message: String(e && e.message || e)
            });
        }
    };
})();

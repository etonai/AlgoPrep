(function() {
    if (typeof window.cefQuery !== 'function') {
        console.warn('[bridge] cefQuery not available on this page');
        return;
    }
    window.cefQuery({
        request: JSON.stringify({ type: 'ping', requestId: 0 }),
        onSuccess: function(r) { console.log('[bridge] ping ok'); },
        onFailure: function(e, m) { console.error('[bridge] ping fail:', e, m); }
    });
})();

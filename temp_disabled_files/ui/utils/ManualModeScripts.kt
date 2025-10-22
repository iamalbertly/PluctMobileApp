package app.pluct.ui.utils

object ManualModeScripts {
    fun getManualModeScript(): String {
        return """
            (function() {
                console.log('Manual mode monitoring script loaded');
                
                window.simulateProperInput = function(element, text) {
                    element.focus();
                    element.click();
                    element.value = '';
                    element.dispatchEvent(new Event('input', { bubbles: true }));
                    element.dispatchEvent(new Event('change', { bubbles: true }));
                    
                    const chars = text.split('');
                    let index = 0;
                    function typeNextChar() {
                        if (index < chars.length) {
                            const char = chars[index];
                            element.value += char;
                            element.dispatchEvent(new Event('input', { bubbles: true }));
                            element.dispatchEvent(new Event('keydown', { bubbles: true, key: char, keyCode: char.charCodeAt(0) }));
                            element.dispatchEvent(new Event('keypress', { bubbles: true, key: char, keyCode: char.charCodeAt(0) }));
                            element.dispatchEvent(new Event('keyup', { bubbles: true, key: char, keyCode: char.charCodeAt(0) }));
                            index++;
                            setTimeout(typeNextChar, 50);
                        } else {
                            element.dispatchEvent(new Event('change', { bubbles: true }));
                            element.dispatchEvent(new Event('blur', { bubbles: true }));
                            element.dispatchEvent(new Event('focus', { bubbles: true }));
                            console.log('Manual mode: Completed typing simulation');
                        }
                    }
                    typeNextChar();
                };
                
                const observer = new MutationObserver(function(mutations) {
                    mutations.forEach(function(mutation) {
                        if (mutation.type === 'childList') {
                            const transcriptElements = document.querySelectorAll('textarea[readonly], pre, [class*="transcript"], [class*="result"]');
                            transcriptElements.forEach(function(element) {
                                const text = (element.value || element.textContent || '').trim();
                                if (text.length > 50 && 
                                    !text.includes('script.tokaudit.io') &&
                                    !text.includes('TikTok Transcript Generator') &&
                                    !text.includes('Enter Video') &&
                                    !text.includes('START') &&
                                    !text.includes('Download') &&
                                    !text.includes('Loading') &&
                                    !text.includes('Processing') &&
                                    (text.includes('.') || text.includes('!') || text.includes('?'))) {
                                    console.log('Manual transcript detected, length: ' + text.length);
                                    if (window.ManualBridge && typeof window.ManualBridge.onManualTranscriptReceived === 'function') {
                                        window.ManualBridge.onManualTranscriptReceived(text);
                                    }
                                }
                            });
                        }
                    });
                });
                observer.observe(document.body, { childList: true, subtree: true });
                
                document.addEventListener('click', function(e) {
                    if (e.target && e.target.innerText && e.target.innerText.toUpperCase().includes('COPY')) {
                        console.log('Copy button clicked in manual mode');
                        setTimeout(function() {
                            const transcriptElements = document.querySelectorAll('textarea[readonly], pre, [class*="transcript"], [class*="result"]');
                            transcriptElements.forEach(function(element) {
                                const text = (element.value || element.textContent || '').trim();
                                if (text.length > 50) {
                                    console.log('Transcript found after copy click, length: ' + text.length);
                                    if (window.ManualBridge && typeof window.ManualBridge.onManualTranscriptReceived === 'function') {
                                        window.ManualBridge.onManualTranscriptReceived(text);
                                    }
                                }
                            });
                        }, 1000);
                    }
                });
            })();
        """.trimIndent()
    }
}

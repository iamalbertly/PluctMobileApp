package app.pluct.ui.utils.scripts

/**
 * Modal handling logic for TokAudit automation
 */
object ModalHandler {
    
    fun getModalDismissalScript(): String {
        return """
            // Dismiss modals and overlays
            function dismissModals() {
                try {
                    log('dismissing_modals');
                    
                    // Common modal selectors
                    const modalSelectors = [
                        '[role="dialog"]',
                        '.modal',
                        '.overlay',
                        '.popup',
                        '[data-testid*="modal"]',
                        '[class*="modal"]',
                        '[class*="overlay"]',
                        '[class*="popup"]',
                        '.backdrop',
                        '[aria-modal="true"]'
                    ];
                    
                    let modalsFound = 0;
                    let modalsDismissed = 0;
                    
                    modalSelectors.forEach(selector => {
                        const elements = document.querySelectorAll(selector);
                        elements.forEach(element => {
                            modalsFound++;
                            
                            // Try different dismissal methods
                            if (element.style) {
                                element.style.display = 'none';
                                element.style.visibility = 'hidden';
                                element.style.opacity = '0';
                                modalsDismissed++;
                            }
                            
                            // Try clicking close buttons
                            const closeButtons = element.querySelectorAll('button, [role="button"], .close, [aria-label*="close"], [aria-label*="Close"]');
                            closeButtons.forEach(btn => {
                                try {
                                    btn.click();
                                    modalsDismissed++;
                                } catch (e) {
                                    // Ignore click errors
                                }
                            });
                            
                            // Try pressing Escape
                            try {
                                const escapeEvent = new KeyboardEvent('keydown', { key: 'Escape', keyCode: 27 });
                                element.dispatchEvent(escapeEvent);
                            } catch (e) {
                                // Ignore event errors
                            }
                        });
                    });
                    
                    if (modalsFound > 0) {
                        log('phase=modal_dismissed found=' + modalsFound + ' dismissed=' + modalsDismissed);
                    } else {
                        log('phase=no_modal');
                    }
                    
                    // Small delay to let modals close
                    setTimeout(() => {
                        log('modals_processed');
                    }, 500);
                    
                } catch (e) {
                    log('error_in_dismissModals: ' + e.message);
                }
            }
        """.trimIndent()
    }
}

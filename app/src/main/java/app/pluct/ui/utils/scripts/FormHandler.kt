package app.pluct.ui.utils.scripts

/**
 * Form handling logic for TokAudit automation
 */
object FormHandler {
    
    fun getFormHandlingScript(): String {
        return """
            // Fill and submit URL form
            function fillAndSubmitUrl() {
                try {
                    log('filling_form');
                    
                    // Find input field
                    const input = findInput();
                    if (!input) {
                        log('input_not_found');
                        if (window.Android && window.Android.onError) {
                            try {
                                window.Android.onError('Input field not found');
                            } catch (e) {
                                log('error_calling_onError: ' + e.message);
                            }
                        }
                        return;
                    }
                    
                    log('input_found');
                    
                    // Set the URL value
                    const success = setInputValue(input, TARGET_URL.url);
                    if (!success) {
                        log('failed_to_set_value');
                        return;
                    }
                    
                    log('value_verified');
                    
                    // Find and click submit button
                    const submitBtn = findSubmitButton();
                    if (!submitBtn) {
                        log('submit_button_not_found');
                        return;
                    }
                    
                    log('btn_start_found');
                    
                    // Submit the form
                    submitForm(input);
                    
                } catch (e) {
                    log('error_in_fillAndSubmitUrl: ' + e.message);
                }
            }
            
            // Find input field - React-safe selectors
            function findInput() {
                const selectors = [
                    'textarea[placeholder*="Video" i]',
                    'input[type="url"]',
                    'input[type="text"]',
                    'input[placeholder*="URL"]',
                    'input[placeholder*="url"]',
                    'input[placeholder*="TikTok"]',
                    'input[placeholder*="tiktok"]',
                    'input[name*="url"]',
                    'input[id*="url"]',
                    'input[class*="url"]',
                    'textarea[placeholder*="URL"]',
                    'textarea[placeholder*="url"]',
                    'textarea[placeholder*="TikTok"]',
                    'textarea[placeholder*="tiktok"]'
                ];
                
                for (const selector of selectors) {
                    const input = document.querySelector(selector);
                    if (input && input.offsetParent !== null) {
                        log('input_found sel=' + selector);
                        return input;
                    }
                }
                
                return null;
            }
            
            // Set input value with React compatibility - native setter approach
            function setInputValue(input, value) {
                try {
                    // Clear existing value
                    input.value = '';
                    input.focus();
                    
                    // Use native setter for React compatibility
                    const nativeInputValueSetter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;
                    if (nativeInputValueSetter) {
                        nativeInputValueSetter.call(input, value);
                    } else {
                        input.value = value;
                    }
                    
                    // Dispatch events
                    const events = ['input', 'change'];
                    events.forEach(eventType => {
                        const event = new Event(eventType, { bubbles: true });
                        input.dispatchEvent(event);
                    });
                    
                    // Verify after focus->delay->blur->refocus cycle
                    setTimeout(() => {
                        input.blur();
                        setTimeout(() => {
                            input.focus();
                            setTimeout(() => {
                                if (input.value !== value) {
                                    log('value_reset');
                                    // Retry up to 3 times
                                    for (let i = 0; i < 3; i++) {
                                        if (nativeInputValueSetter) {
                                            nativeInputValueSetter.call(input, value);
                                        } else {
                                            input.value = value;
                                        }
                                        const event = new Event('input', { bubbles: true });
                                        input.dispatchEvent(event);
                                        if (input.value === value) break;
                                    }
                                    
                                    // Final fallback: type by character
                                    if (input.value !== value) {
                                        input.value = '';
                                        for (let i = 0; i < value.length; i++) {
                                            input.value += value[i];
                                            const event = new Event('input', { bubbles: true });
                                            input.dispatchEvent(event);
                                            // 20ms per character
                                            if (i < value.length - 1) {
                                                setTimeout(() => {}, 20);
                                            }
                                        }
                                    }
                                }
                                log('value_verified');
                            }, 100);
                        }, 100);
                    }, 400);
                    
                    return true;
                    
                } catch (e) {
                    log('error_setting_input_value: ' + e.message);
                    return false;
                }
            }
            
            // Find submit button - visible buttons with specific text
            function findSubmitButton() {
                const selectors = [
                    'button[type="submit"]',
                    'input[type="submit"]',
                    'button',
                    '[data-testid*="submit"]',
                    '[data-testid*="start"]',
                    '[class*="submit"]',
                    '[class*="start"]',
                    '[id*="submit"]',
                    '[id*="start"]'
                ];
                
                for (const selector of selectors) {
                    const buttons = document.querySelectorAll(selector);
                    for (const button of buttons) {
                        if (button && button.offsetParent !== null) {
                            const text = (button.textContent || button.innerText || '').trim().toLowerCase();
                            if (/(^|\b)(start|analyze|check|go|submit|get transcript)(\b|$)/i.test(text)) {
                                log('btn_start_found txt="' + text + '"');
                                return button;
                            }
                        }
                    }
                }
                
                return null;
            }
            
            // Submit form
            function submitForm(input) {
                try {
                    // Try clicking submit button first
                    const submitBtn = findSubmitButton();
                    if (submitBtn) {
                        log('submit_clicked');
                        submitBtn.click();
                        return;
                    }
                    
                    // Try form submission
                    const form = input.closest('form');
                    if (form) {
                        log('form_submitted');
                        form.submit();
                        return;
                    }
                    
                    // Try Enter key
                    log('enter_fired');
                    const enterEvent = new KeyboardEvent('keydown', { key: 'Enter', keyCode: 13 });
                    input.dispatchEvent(enterEvent);
                    
                } catch (e) {
                    log('error_in_submitForm: ' + e.message);
                }
            }
        """.trimIndent()
    }
}

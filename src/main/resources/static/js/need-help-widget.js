/**
 * EduTake LMS — "Need Help?" AI Support & Assistance System
 * Production-ready, context-aware global learning assistance engine.
 */
(function () {
    'use strict';

    // State
    const state = {
        isOpen: false,
        activeTab: 'quick',
        context: null,
        userInfo: null,
        isAiLoading: false,
        aiHistory: [],
        currentPathInfo: null
    };

    // DOM Elements Cache
    let elements = {};

    function init() {
        cacheElements();
        if (!elements.launcher || !elements.drawer) return;

        checkBottomNavOffset();
        bindEvents();
        detectInitialContext();
    }

    function checkBottomNavOffset() {
        const hasBottomBar = !!document.getElementById('mobileBottomNav') ||
                             !!document.querySelector('nav[id*="mobileBottom"]') ||
                             !!document.querySelector('footer.player-footer') ||
                             !!document.querySelector('footer.h-14');
        if (hasBottomBar) {
            document.body.classList.add('has-mobile-bottom-nav');
        }
    }

    function cacheElements() {
        elements = {
            launcher: document.getElementById('need-help-launcher'),
            drawer: document.getElementById('need-help-drawer'),
            closeBtn: document.getElementById('nh-close-btn'),
            headerWhatsappBtn: document.getElementById('nh-header-whatsapp-btn'),
            tabWhatsapp: document.getElementById('nh-tab-whatsapp'),
            persistentWhatsappBtn: document.getElementById('nh-persistent-whatsapp-btn'),
            contextLabel: document.getElementById('nh-context-label'),
            contextTitle: document.getElementById('nh-context-title'),
            contextDesc: document.getElementById('nh-context-desc'),
            chipsGrid: document.getElementById('nh-chips-grid'),
            whatsappBtn: document.getElementById('nh-whatsapp-btn'),
            tabButtons: document.querySelectorAll('.nh-tab-btn:not(.nh-tab-whatsapp)'),
            tabViews: document.querySelectorAll('.nh-tab-view'),
            faqSearchInput: document.getElementById('nh-faq-search'),
            faqContainer: document.getElementById('nh-faq-container'),
            chatThread: document.getElementById('nh-chat-thread'),
            chatInput: document.getElementById('nh-chat-input'),
            chatSendBtn: document.getElementById('nh-chat-send'),
            ticketForm: document.getElementById('nh-ticket-form'),
            ticketAlert: document.getElementById('nh-ticket-alert'),
            ticketSubmitBtn: document.getElementById('nh-ticket-submit-btn'),
            reportForm: document.getElementById('nh-report-form'),
            reportAlert: document.getElementById('nh-report-alert'),
            reportSubmitBtn: document.getElementById('nh-report-submit-btn')
        };
    }

    function bindEvents() {
        // Toggle Card
        elements.launcher.addEventListener('click', toggleDrawer);
        elements.closeBtn.addEventListener('click', closeDrawer);

        // Click outside to dismiss (no background blur needed)
        document.addEventListener('pointerdown', (e) => {
            if (state.isOpen &&
                !elements.drawer.contains(e.target) &&
                !elements.launcher.contains(e.target)) {
                closeDrawer();
            }
        });

        // Esc to dismiss
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && state.isOpen) {
                closeDrawer();
            }
        });

        // Tabs
        elements.tabButtons.forEach(btn => {
            btn.addEventListener('click', () => {
                const targetTab = btn.getAttribute('data-tab');
                if (targetTab) {
                    switchTab(targetTab);
                }
            });
        });

        // FAQ Search (Debounced)
        let searchTimeout;
        if (elements.faqSearchInput) {
            elements.faqSearchInput.addEventListener('input', (e) => {
                clearTimeout(searchTimeout);
                searchTimeout = setTimeout(() => {
                    searchFaqs(e.target.value);
                }, 250);
            });
        }

        // AI Chat
        if (elements.chatSendBtn && elements.chatInput) {
            elements.chatSendBtn.addEventListener('click', sendAiQuery);
            elements.chatInput.addEventListener('keydown', (e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    sendAiQuery();
                }
            });
        }

        // Ticket Submission
        if (elements.ticketForm) {
            elements.ticketForm.addEventListener('submit', handleTicketSubmit);
        }

        // Problem Reporting
        if (elements.reportForm) {
            elements.reportForm.addEventListener('submit', handleProblemReport);
        }
    }

    function toggleDrawer() {
        if (state.isOpen) closeDrawer();
        else openDrawer();
    }

    function openDrawer() {
        state.isOpen = true;
        elements.drawer.classList.add('active');
        elements.launcher.setAttribute('aria-expanded', 'true');

        if (!state.context) {
            fetchContext();
        }
    }

    function closeDrawer() {
        state.isOpen = false;
        elements.drawer.classList.remove('active');
        elements.launcher.setAttribute('aria-expanded', 'false');
    }

    function switchTab(tabId) {
        state.activeTab = tabId;
        elements.tabButtons.forEach(b => {
            b.classList.toggle('active', b.getAttribute('data-tab') === tabId);
        });
        elements.tabViews.forEach(v => {
            v.classList.toggle('active', v.getAttribute('id') === `nh-view-${tabId}`);
        });

        if (tabId === 'faq' && elements.faqContainer && elements.faqContainer.children.length === 0) {
            searchFaqs('');
        }
        if (tabId === 'ai' && state.aiHistory.length === 0) {
            renderInitialAiGreeting();
        }
        if (tabId === 'ai' && elements.chatInput) {
            setTimeout(() => elements.chatInput.focus(), 100);
        }
    }

    // --- Context Engine ---
    function detectInitialContext() {
        const path = window.location.pathname;
        const urlParams = new URLSearchParams(window.location.search);
        
        let courseId = null;
        let lessonId = urlParams.get('lessonId');
        
        const courseMatch = path.match(/\/(?:courses|student\/courses)\/(\d+)/);
        if (courseMatch) courseId = courseMatch[1];

        state.currentPathInfo = {
            route: path,
            courseId: courseId,
            lessonId: lessonId,
            title: document.title
        };
    }

    function fetchContext() {
        detectInitialContext();
        const info = state.currentPathInfo;
        const params = new URLSearchParams({
            route: info.route,
            pageTitle: info.title
        });
        if (info.courseId) params.set('courseId', info.courseId);
        if (info.lessonId) params.set('lessonId', info.lessonId);

        fetch(`/api/support/context?${params.toString()}`)
            .then(res => res.ok ? res.json() : Promise.reject(res))
            .then(data => {
                state.context = data;
                renderContext(data);
            })
            .catch(err => {
                console.warn('Could not resolve support context, using fallback:', err);
                renderFallbackContext();
            });
    }

    function renderContext(ctx) {
        if (elements.contextLabel) elements.contextLabel.textContent = ctx.contextType || 'GENERAL SUPPORT';
        if (elements.contextTitle) elements.contextTitle.textContent = ctx.contextTitle || 'EduTake Learning Platform';
        if (elements.contextDesc) elements.contextDesc.textContent = ctx.contextDescription || "We're here to help you keep learning.";

        // Update all WhatsApp buttons
        const waUrl = ctx.whatsAppUrl || "https://wa.me/918130279213";
        if (elements.whatsappBtn) elements.whatsappBtn.href = waUrl;
        if (elements.headerWhatsappBtn) elements.headerWhatsappBtn.href = waUrl;
        if (elements.tabWhatsapp) elements.tabWhatsapp.href = waUrl;
        if (elements.persistentWhatsappBtn) elements.persistentWhatsappBtn.href = waUrl;

        // Pre-fill user details if authenticated
        if (ctx.isAuthenticated) {
            state.userInfo = { name: ctx.userName, email: ctx.userEmail, role: ctx.userRole };
            const nameInput = document.getElementById('nh-ticket-name');
            const emailInput = document.getElementById('nh-ticket-email');
            if (nameInput && ctx.userName) nameInput.value = ctx.userName;
            if (emailInput && ctx.userEmail) emailInput.value = ctx.userEmail;
        }

        // Render Action Chips
        if (elements.chipsGrid && ctx.quickActions) {
            elements.chipsGrid.innerHTML = '';
            ctx.quickActions.forEach(action => {
                const chip = document.createElement('button');
                chip.type = 'button';
                chip.className = 'nh-action-chip';
                chip.innerHTML = `
                    <div class="nh-chip-icon"><i class="bi ${action.icon || 'bi-chevron-right'}"></i></div>
                    <span class="flex-1">${escapeHtml(action.label)}</span>
                    <i class="bi bi-arrow-right text-xs opacity-40"></i>
                `;
                chip.addEventListener('click', () => handleActionChipClick(action));
                elements.chipsGrid.appendChild(chip);
            });
        }
    }

    function renderFallbackContext() {
        const defaultWhatsApp = "https://wa.me/918130279213?text=" + encodeURIComponent("Hello EduTake Support, I need assistance with my learning account.");
        if (elements.whatsappBtn) elements.whatsappBtn.href = defaultWhatsApp;
        if (elements.headerWhatsappBtn) elements.headerWhatsappBtn.href = defaultWhatsApp;
        if (elements.tabWhatsapp) elements.tabWhatsapp.href = defaultWhatsApp;
        if (elements.persistentWhatsappBtn) elements.persistentWhatsappBtn.href = defaultWhatsApp;
    }

    function handleActionChipClick(action) {
        if (action.actionType === 'whatsapp') {
            openWhatsApp();
        } else if (action.actionType === 'ai') {
            switchTab('ai');
            askAiDirectly(action.label);
        } else if (action.actionType === 'video') {
            switchTab('ai');
            askAiDirectly("I'm having trouble with video playback in this lesson.");
        } else if (action.actionType === 'progress') {
            switchTab('ai');
            askAiDirectly("My course completion progress is not updating.");
        } else if (action.actionType === 'report') {
            switchTab('report');
        } else if (action.actionType === 'ticket') {
            switchTab('ticket');
        } else if (action.actionType === 'search') {
            switchTab('faq');
        } else if (action.actionType && action.actionType.startsWith('navigate:')) {
            window.location.href = action.actionType.replace('navigate:', '');
        }
    }

    // --- AI Assistant Engine ---
    function renderInitialAiGreeting() {
        if (!elements.chatThread) return;
        elements.chatThread.innerHTML = '';

        const greeting = document.createElement('div');
        greeting.className = 'nh-chat-msg assistant';
        greeting.innerHTML = `
            <div class="nh-msg-bubble">
                <p class="font-semibold text-brand-400 mb-1">Hello! How can I help you continue learning?</p>
                <p class="text-xs text-slate-300">Ask about lessons, quiz passing criteria (80%), certificates, assignments, or video issues.</p>
            </div>
        `;
        elements.chatThread.appendChild(greeting);
    }

    function sendAiQuery() {
        if (!elements.chatInput || state.isAiLoading) return;
        const msg = elements.chatInput.value.trim();
        if (!msg) return;

        elements.chatInput.value = '';
        askAiDirectly(msg);
    }

    function askAiDirectly(promptText) {
        if (state.isAiLoading) return;
        state.isAiLoading = true;

        // Append user bubble
        appendChatMessage('user', promptText);

        // Thinking placeholder
        const thinkingId = 'nh-ai-thinking-' + Date.now();
        const thinkingEl = document.createElement('div');
        thinkingEl.id = thinkingId;
        thinkingEl.className = 'nh-chat-msg assistant';
        thinkingEl.innerHTML = `
            <div class="nh-msg-bubble flex items-center gap-2 text-xs text-slate-400">
                <i class="bi bi-arrow-repeat animate-spin text-brand-400"></i> Formulating answer...
            </div>
        `;
        elements.chatThread.appendChild(thinkingEl);
        elements.chatThread.scrollTop = elements.chatThread.scrollHeight;

        const info = state.currentPathInfo || {};
        const payload = {
            message: promptText,
            currentRoute: info.route || window.location.pathname,
            pageTitle: document.title,
            courseId: info.courseId,
            lessonId: info.lessonId
        };

        fetch('/api/support/ask', {
            method: 'POST',
            headers: getHeaders(),
            body: JSON.stringify(payload)
        })
            .then(res => res.ok ? res.json() : Promise.reject(res))
            .then(data => {
                const placeholder = document.getElementById(thinkingId);
                if (placeholder) placeholder.remove();

                renderAiResponse(data);
            })
            .catch(() => {
                const placeholder = document.getElementById(thinkingId);
                if (placeholder) placeholder.remove();

                appendChatMessage('assistant',
                    "Our AI assistant is momentarily unreachable. You can continue speaking directly with our support team on WhatsApp at **+91 81302 79213**.",
                    true
                );
            })
            .finally(() => {
                state.isAiLoading = false;
            });
    }

    function renderAiResponse(data) {
        let contentHtml = formatMarkdown(data.answer || 'I am here to help you keep learning.');

        if (data.source) {
            contentHtml += `<div class="mt-1.5 text-[10.5px] text-brand-400 font-medium flex items-center gap-1.5"><i class="bi bi-book"></i> Source: ${escapeHtml(data.source)}</div>`;
        }

        if (data.requiresHumanEscalation) {
            contentHtml += `
                <div class="mt-2.5 pt-2 border-t border-white/10 flex flex-wrap gap-2">
                    <a href="${data.whatsAppUrl || 'https://wa.me/918130279213'}" target="_blank" rel="noopener noreferrer" class="inline-flex items-center gap-1.5 px-2.5 py-1 bg-emerald-600 hover:bg-emerald-500 text-white rounded text-xs font-semibold shadow-sm transition-colors">
                        <i class="bi bi-whatsapp"></i> Chat on WhatsApp
                    </a>
                    <button type="button" onclick="window.EduTakeSupport.openTicketTab()" class="inline-flex items-center gap-1.5 px-2.5 py-1 bg-brand-600 hover:bg-brand-500 text-white rounded text-xs font-semibold shadow-sm transition-colors">
                        <i class="bi bi-ticket-detailed"></i> Submit Ticket
                    </button>
                </div>
            `;
        }

        appendChatMessage('assistant', contentHtml, false, true);
    }

    function appendChatMessage(role, text, isEscalation, isHtml) {
        if (!elements.chatThread) return;
        const msgEl = document.createElement('div');
        msgEl.className = `nh-chat-msg ${role}`;
        
        let body = isHtml ? text : `<p>${escapeHtml(text)}</p>`;
        msgEl.innerHTML = `<div class="nh-msg-bubble">${body}</div>`;

        elements.chatThread.appendChild(msgEl);
        elements.chatThread.scrollTop = elements.chatThread.scrollHeight;
        state.aiHistory.push({ role, text });
    }

    // --- FAQ Engine ---
    function searchFaqs(query) {
        if (!elements.faqContainer) return;

        elements.faqContainer.innerHTML = `
            <div class="p-4 text-center text-xs text-slate-400">
                <i class="bi bi-arrow-repeat animate-spin text-brand-400 mr-1.5"></i> Loading knowledge base...
            </div>
        `;

        const params = new URLSearchParams();
        if (query && query.trim()) params.set('query', query.trim());

        fetch(`/api/support/faqs?${params.toString()}`)
            .then(res => res.ok ? res.json() : [])
            .then(faqs => {
                elements.faqContainer.innerHTML = '';
                if (!faqs || faqs.length === 0) {
                    elements.faqContainer.innerHTML = `
                        <div class="p-5 text-center text-xs text-slate-400 bg-slate-900/40 rounded-xl border border-white/5">
                            <i class="bi bi-search text-base text-slate-500 block mb-1.5"></i>
                            No matching FAQs found. You can ask our AI Assistant or WhatsApp support.
                        </div>
                    `;
                    return;
                }

                faqs.forEach(faq => {
                    const item = document.createElement('div');
                    item.className = 'nh-faq-item';
                    item.innerHTML = `
                        <div class="nh-faq-question">
                            <span>${escapeHtml(faq.question)}</span>
                            <i class="bi bi-chevron-down nh-faq-chevron"></i>
                        </div>
                        <div class="nh-faq-answer">
                            ${faq.answer}
                        </div>
                    `;
                    item.querySelector('.nh-faq-question').addEventListener('click', () => {
                        item.classList.toggle('open');
                    });
                    elements.faqContainer.appendChild(item);
                });
            })
            .catch(() => {
                elements.faqContainer.innerHTML = `
                    <div class="p-3 text-xs text-red-400 text-center">Could not load FAQs. Please try WhatsApp support.</div>
                `;
            });
    }

    // --- Ticket Submission Engine ---
    function handleTicketSubmit(e) {
        e.preventDefault();
        const form = elements.ticketForm;
        const alertBox = elements.ticketAlert;
        const submitBtn = elements.ticketSubmitBtn;

        alertBox.className = 'nh-alert hidden';
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<i class="bi bi-arrow-repeat animate-spin"></i> Submitting...';

        const info = state.currentPathInfo || {};
        const payload = {
            name: form.name.value.trim(),
            email: form.email.value.trim(),
            category: form.category.value,
            priority: 'MEDIUM',
            subject: form.subject.value.trim(),
            description: form.description.value.trim(),
            pageUrl: window.location.href,
            browserInfo: navigator.userAgent.substring(0, 150),
            deviceInfo: `${window.screen.width}x${window.screen.height}`,
            courseId: info.courseId,
            lessonId: info.lessonId
        };

        fetch('/api/support/ticket', {
            method: 'POST',
            headers: getHeaders(),
            body: JSON.stringify(payload)
        })
            .then(res => res.json().then(data => ({ status: res.status, body: data })))
            .then(({ status, body }) => {
                if (status >= 200 && status < 300 && body.success) {
                    alertBox.className = 'nh-alert nh-alert-success';
                    alertBox.innerHTML = `<i class="bi bi-check-circle-fill"></i> ${body.message}`;
                    form.reset();
                    if (state.userInfo) {
                        form.name.value = state.userInfo.name || '';
                        form.email.value = state.userInfo.email || '';
                    }
                } else {
                    alertBox.className = 'nh-alert nh-alert-error';
                    alertBox.innerHTML = `<i class="bi bi-exclamation-triangle-fill"></i> ${body.error || 'Submission failed. Please try WhatsApp support.'}`;
                }
            })
            .catch(() => {
                alertBox.className = 'nh-alert nh-alert-error';
                alertBox.innerHTML = `<i class="bi bi-wifi-off"></i> Network error. Please contact us on WhatsApp at +91 81302 79213.`;
            })
            .finally(() => {
                submitBtn.disabled = false;
                submitBtn.innerHTML = '<i class="bi bi-send-fill"></i> Submit Support Request';
            });
    }

    // --- Problem Reporting Engine ---
    function handleProblemReport(e) {
        e.preventDefault();
        const form = elements.reportForm;
        const alertBox = elements.reportAlert;
        const submitBtn = elements.reportSubmitBtn;

        alertBox.className = 'nh-alert hidden';
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<i class="bi bi-arrow-repeat animate-spin"></i> Submitting...';

        const info = state.currentPathInfo || {};
        const payload = {
            name: state.userInfo?.name || 'Learner',
            email: state.userInfo?.email || 'learner@edutake.com',
            issueType: form.issueType.value,
            description: form.description.value.trim(),
            pageUrl: window.location.href,
            browserInfo: navigator.userAgent.substring(0, 150),
            deviceInfo: `${window.screen.width}x${window.screen.height}`,
            courseId: info.courseId,
            lessonId: info.lessonId
        };

        fetch('/api/support/report-problem', {
            method: 'POST',
            headers: getHeaders(),
            body: JSON.stringify(payload)
        })
            .then(res => res.json().then(data => ({ status: res.status, body: data })))
            .then(({ status, body }) => {
                if (status >= 200 && status < 300 && body.success) {
                    alertBox.className = 'nh-alert nh-alert-success';
                    alertBox.innerHTML = `<i class="bi bi-check-circle-fill"></i> ${body.message}`;
                    form.reset();
                } else {
                    alertBox.className = 'nh-alert nh-alert-error';
                    alertBox.innerHTML = `<i class="bi bi-exclamation-triangle-fill"></i> ${body.error || 'Submission failed.'}`;
                }
            })
            .catch(() => {
                alertBox.className = 'nh-alert nh-alert-error';
                alertBox.innerHTML = `<i class="bi bi-wifi-off"></i> Connection error. Please chat with support on WhatsApp.`;
            })
            .finally(() => {
                submitBtn.disabled = false;
                submitBtn.innerHTML = '<i class="bi bi-flag-fill"></i> Submit Problem Report';
            });
    }

    function openWhatsApp() {
        const url = (state.context && state.context.whatsAppUrl) 
            ? state.context.whatsAppUrl 
            : (elements.whatsappBtn ? elements.whatsappBtn.href : "https://wa.me/918130279213");
        window.open(url, '_blank', 'noopener,noreferrer');
    }

    // --- Utilities ---
    function getHeaders() {
        const headers = { 'Content-Type': 'application/json' };
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';
        if (csrfToken) {
            headers[csrfHeader] = csrfToken;
        }
        return headers;
    }

    function escapeHtml(str) {
        if (!str) return '';
        return str
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    function formatMarkdown(text) {
        if (!text) return '';
        let escaped = escapeHtml(text);
        escaped = escaped.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
        escaped = escaped.replace(/^\s*•\s*(.*)$/gm, '<li class="ml-4 list-disc">$1</li>');
        escaped = escaped.replace(/^\s*\d+\.\s*(.*)$/gm, '<li class="ml-4 list-decimal">$1</li>');
        escaped = escaped.replace(/`([^`]+)`/g, '<code class="px-1 py-0.5 bg-slate-800 rounded text-brand-300 text-xs">$1</code>');
        escaped = escaped.replace(/\n\n/g, '</p><p class="mt-1.5">');
        escaped = escaped.replace(/\n/g, '<br>');
        return `<p>${escaped}</p>`;
    }

    // Expose global controller
    window.EduTakeSupport = {
        open: openDrawer,
        close: closeDrawer,
        openTab: (tabId) => {
            openDrawer();
            switchTab(tabId);
        },
        openTicketTab: () => {
            openDrawer();
            switchTab('ticket');
        },
        openWhatsApp: openWhatsApp
    };

    // Auto-init
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();

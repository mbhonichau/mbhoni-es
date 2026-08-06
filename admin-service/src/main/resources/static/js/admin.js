/**
 * MBHONI Creative Admin Platform - Modern JavaScript & Masonry Engine
 */

document.addEventListener('DOMContentLoaded', function () {
    // 1. Highlight active navigation link
    const currentPath = window.location.pathname;
    document.querySelectorAll('.sidebar-nav .nav-link').forEach(link => {
        const href = link.getAttribute('href');
        if (href === currentPath || (href !== '/dashboard' && href !== '/' && currentPath.startsWith(href))) {
            link.classList.add('active');
            const parentSection = link.closest('.nav-section');
            if (parentSection) {
                parentSection.classList.add('open');
                const toggle = parentSection.querySelector('.nav-section-toggle');
                if (toggle) {
                    toggle.setAttribute('aria-expanded', 'true');
                }
            }
        }
    });

    // 2. Masonry Layout Grid Auto-Calculations
    initMasonryGrids();
    window.addEventListener('resize', debounce(initMasonryGrids, 150));

    // 3. Auto-dismiss Alert Notifications
    const alerts = document.querySelectorAll('.alert-dismissible');
    alerts.forEach(alert => {
        setTimeout(() => {
            if (typeof bootstrap !== 'undefined' && bootstrap.Alert) {
                const bsAlert = new bootstrap.Alert(alert);
                bsAlert.close();
            }
        }, 5000);
    });

    // 4. Global CRUD Confirmation Popup & Warning Interceptor
    initGlobalCrudConfirmations();

    // 5. Billing & Subscription Auto Date Calculations
    initBillingDateAutoCalculators();

    // 6. Global Email & Phone Input Form Validation
    initGlobalFormValidation();

    // 7. Ease-of-Use Tooltips & Locked Feature Help Guidance
    initUserEaseOfUseGuidance();

    // 8. Percentage Auto-Calculators for Taxes, VAT, PAYE & UIF
    initFinancialPercentageAutoCalculators();

    // 9. Global Show/Hide Password Feature for all password textboxes
    initPasswordToggleButtons();
});

/**
 * Dynamic Masonry Layout Manager
 * Arranges grid items into optimal vertical columns
 */
function initMasonryGrids() {
    const masonryContainers = document.querySelectorAll('.masonry-grid');

    masonryContainers.forEach(container => {
        const items = container.querySelectorAll('.masonry-card, .masonry-item');
        if (!items || items.length === 0) return;

        // Apply dynamic staggered entrance animation
        items.forEach((item, index) => {
            item.style.animationDelay = `${index * 50}ms`;
        });
    });
}

/**
 * Debounce helper for window resize events
 */
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

/**
 * Global CRUD Confirmation Modal & Warning Interceptor
 * Intercepts forms and buttons marked with data-confirm or delete endpoints
 */
function initGlobalCrudConfirmations() {
    const modalEl = document.getElementById('globalConfirmModal');
    if (!modalEl || typeof bootstrap === 'undefined') return;

    const confirmModal = new bootstrap.Modal(modalEl);
    const titleEl = document.getElementById('globalConfirmTitle');
    const messageEl = document.getElementById('globalConfirmMessage');
    const warningBannerEl = document.getElementById('globalConfirmWarningBanner');
    const warningTextEl = document.getElementById('globalConfirmWarningText');
    const submitBtn = document.getElementById('globalConfirmSubmitBtn');
    const iconEl = document.getElementById('globalConfirmIcon');

    let targetForm = null;

    // Listen for form submits across the app
    document.addEventListener('submit', function (e) {
        const form = e.target;
        
        // Skip if form explicitly opts out or has already been approved
        if (form.getAttribute('data-confirm-approved') === 'true' || form.hasAttribute('data-no-confirm')) {
            return;
        }

        const action = form.getAttribute('action') || '';
        const isDeleteAction = action.includes('/delete') || form.classList.contains('delete-form') || form.hasAttribute('data-confirm-delete');
        const hasConfirmAttr = form.hasAttribute('data-confirm');

        if (isDeleteAction || hasConfirmAttr) {
            e.preventDefault();
            targetForm = form;

            const title = form.getAttribute('data-confirm-title') || (isDeleteAction ? 'Confirm Permanent Deletion' : 'Confirm Action');
            const message = form.getAttribute('data-confirm-message') || (isDeleteAction ? 'Are you sure you want to delete this resource? This operation cannot be undone.' : 'Please confirm if you wish to proceed with this operation.');
            const warning = form.getAttribute('data-confirm-warning') || (isDeleteAction ? 'Warning: Deleting this item will also remove all connected child data and configuration records.' : null);
            const btnText = form.getAttribute('data-confirm-btn') || (isDeleteAction ? 'Delete Permanently' : 'Confirm & Proceed');
            const btnClass = form.getAttribute('data-confirm-class') || (isDeleteAction ? 'btn btn-danger px-4' : 'btn btn-primary px-4');

            titleEl.textContent = title;
            messageEl.textContent = message;

            if (warning) {
                warningTextEl.textContent = warning;
                warningBannerEl.style.display = 'flex';
            } else {
                warningBannerEl.style.display = 'none';
            }

            submitBtn.textContent = btnText;
            submitBtn.className = btnClass;

            if (isDeleteAction) {
                iconEl.className = 'bi bi-trash3-fill text-danger fs-4';
            } else {
                iconEl.className = 'bi bi-exclamation-triangle-fill text-warning fs-4';
            }

            confirmModal.show();
        }
    });

    // Handle Confirm button click inside the modal
    if (submitBtn) {
        submitBtn.addEventListener('click', function () {
            if (targetForm) {
                targetForm.setAttribute('data-confirm-approved', 'true');
                confirmModal.hide();
                targetForm.submit();
            }
        });
    }
}

/**
 * Auto-Calculate Billing & Subscription End Dates / Due Dates
 * Uses global event delegation for instant change & input updates across all forms/modals
 */
function initBillingDateAutoCalculators() {
    function handleAutoDateCalc(e) {
        const target = e.target;
        if (!target) return;

        const name = target.getAttribute('name');
        if (!name) return;

        const form = target.closest('form') || document;

        // 1. Subscription End Date Calculation (Start Date / Billing Cycle -> End Date)
        if (name === 'startDate' || name === 'billingCycle' || name === 'cycle') {
            const startDateInput = form.querySelector('input[name="startDate"]');
            const endDateInput = form.querySelector('input[name="endDate"]');
            const cycleSelect = form.querySelector('select[name="billingCycle"], select[name="cycle"]');

            if (startDateInput && endDateInput && startDateInput.value) {
                const parts = startDateInput.value.split('-');
                if (parts.length === 3) {
                    const year = parseInt(parts[0], 10);
                    const month = parseInt(parts[1], 10) - 1; // 0-indexed
                    const day = parseInt(parts[2], 10);

                    const date = new Date(year, month, day);
                    const cycle = cycleSelect && cycleSelect.value ? cycleSelect.value.toUpperCase() : 'MONTHLY';

                    if (cycle === 'MONTHLY' || cycle === 'MONTH') {
                        date.setMonth(date.getMonth() + 1);
                    } else if (cycle === 'ANNUALLY' || cycle === 'YEARLY' || cycle === 'YEAR') {
                        date.setFullYear(date.getFullYear() + 1);
                    } else if (cycle === 'QUARTERLY') {
                        date.setMonth(date.getMonth() + 3);
                    } else if (cycle === 'WEEKLY') {
                        date.setDate(date.getDate() + 7);
                    } else {
                        date.setMonth(date.getMonth() + 1);
                    }

                    const yyyy = date.getFullYear();
                    const mm = String(date.getMonth() + 1).padStart(2, '0');
                    const dd = String(date.getDate()).padStart(2, '0');
                    endDateInput.value = `${yyyy}-${mm}-${dd}`;
                }
            }
        }

        // 2. Invoice Due Date Calculation (Issue Date -> Due Date + 30 Days)
        if (name === 'issueDate') {
            const issueDateInput = form.querySelector('input[name="issueDate"]');
            const dueDateInput = form.querySelector('input[name="dueDate"]');
            if (issueDateInput && dueDateInput && issueDateInput.value) {
                const parts = issueDateInput.value.split('-');
                if (parts.length === 3) {
                    const year = parseInt(parts[0], 10);
                    const month = parseInt(parts[1], 10) - 1;
                    const day = parseInt(parts[2], 10);

                    const date = new Date(year, month, day);
                    date.setDate(date.getDate() + 30);

                    const yyyy = date.getFullYear();
                    const mm = String(date.getMonth() + 1).padStart(2, '0');
                    const dd = String(date.getDate()).padStart(2, '0');
                    dueDateInput.value = `${yyyy}-${mm}-${dd}`;
                }
            }
        }
    }

    document.addEventListener('change', handleAutoDateCalc);
    document.addEventListener('input', handleAutoDateCalc);
}

/**
 * Global Email, Phone Number & Form Input Validation Engine
 */
function initGlobalFormValidation() {
    const EMAIL_REGEX = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
    const PHONE_REGEX = /^\+?[0-9\s\-\(\)]{7,20}$/;

    function validateField(input) {
        if (!input) return true;

        const val = input.value.trim();
        const type = input.getAttribute('type');
        const name = input.getAttribute('name') ? input.getAttribute('name').toLowerCase() : '';
        const isRequired = input.hasAttribute('required');

        let isValid = true;
        let errorMessage = '';

        if (isRequired && !val) {
            isValid = false;
            errorMessage = 'This field is required.';
        } else if (val) {
            if (type === 'email' || name.includes('email')) {
                if (!EMAIL_REGEX.test(val)) {
                    isValid = false;
                    errorMessage = 'Please enter a valid email address (e.g. user@domain.com).';
                }
            } else if (type === 'tel' || name.includes('phone') || name.includes('mobile')) {
                if (!PHONE_REGEX.test(val)) {
                    isValid = false;
                    errorMessage = 'Please enter a valid phone number (e.g. +27 82 123 4567).';
                }
            }
        }

        // Apply UI feedback
        let feedbackEl = input.nextElementSibling;
        if (!feedbackEl || !feedbackEl.classList.contains('invalid-feedback')) {
            feedbackEl = document.createElement('div');
            feedbackEl.className = 'invalid-feedback';
            if (input.parentNode) {
                input.parentNode.insertBefore(feedbackEl, input.nextSibling);
            }
        }

        if (!isValid) {
            input.classList.add('is-invalid');
            input.classList.remove('is-valid');
            if (feedbackEl) {
                feedbackEl.textContent = errorMessage;
                feedbackEl.style.display = 'block';
            }
        } else {
            input.classList.remove('is-invalid');
            if (val) input.classList.add('is-valid');
            if (feedbackEl) {
                feedbackEl.style.display = 'none';
            }
        }

        return isValid;
    }

    // Attach listeners to all forms across the app
    document.querySelectorAll('form').forEach(form => {
        const inputs = form.querySelectorAll('input[type="email"], input[type="tel"], input[name*="email"], input[name*="phone"], input[name*="Phone"]');

        inputs.forEach(input => {
            input.addEventListener('input', () => validateField(input));
            input.addEventListener('blur', () => validateField(input));
        });

        form.addEventListener('submit', function (e) {
            let isFormValid = true;
            inputs.forEach(input => {
                if (!validateField(input)) {
                    isFormValid = false;
                }
            });

            if (!isFormValid) {
                e.preventDefault();
                e.stopPropagation();
                const firstInvalid = form.querySelector('.is-invalid');
                if (firstInvalid) firstInvalid.focus();
            }
        });
    });
}

/**
 * Ease-of-Use Guidance & Tooltip Manager for New System Users
 */
function initUserEaseOfUseGuidance() {
    // 1. Auto-initialize Bootstrap Tooltips
    if (typeof bootstrap !== 'undefined' && bootstrap.Tooltip) {
        document.querySelectorAll('[data-bs-toggle="tooltip"]').forEach(el => {
            try {
                if (!el.hasAttribute('data-bs-original-title')) {
                    new bootstrap.Tooltip(el);
                }
            } catch (err) {}
        });
    }

    // 2. Click Interceptor for Locked or Disabled Actions
    document.addEventListener('click', function (e) {
        const lockedEl = e.target.closest('.disabled, [disabled], [data-locked-reason]');
        if (lockedEl && !lockedEl.hasAttribute('data-bs-toggle')) {
            const reason = lockedEl.getAttribute('data-locked-reason') || lockedEl.getAttribute('title') || 'This action is currently locked or unavailable for your account role/subscription tier.';
            showUserHelpToast('Feature Locked', reason);
        }
    });
}

/**
 * User Ease-of-Use Notification Toast / Alert Pop-Up
 */
function showUserHelpToast(title, message) {
    let container = document.getElementById('userHelpToastContainer');
    if (!container) {
        container = document.createElement('div');
        container.id = 'userHelpToastContainer';
        container.style.cssText = 'position: fixed; bottom: 20px; right: 20px; z-index: 1100; max-width: 360px;';
        document.body.appendChild(container);
    }

    const alertEl = document.createElement('div');
    alertEl.className = 'alert alert-warning alert-dismissible fade show shadow-lg border-0 rounded-3 mb-2 p-3';
    alertEl.role = 'alert';
    alertEl.innerHTML = `
        <div class="d-flex align-items-start gap-2">
            <i class="bi bi-info-circle-fill text-warning fs-5 flex-shrink-0 mt-0.5"></i>
            <div>
                <strong class="d-block text-dark small font-weight-bold">${title}</strong>
                <span class="small text-secondary d-block mt-0.5">${message}</span>
            </div>
        </div>
        <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
    `;

    container.appendChild(alertEl);

    setTimeout(() => {
        if (alertEl.parentNode) {
            alertEl.classList.remove('show');
            setTimeout(() => alertEl.remove(), 200);
        }
    }, 6000);
}

/**
 * Auto-Calculate Statutory Taxes, PAYE, UIF, VAT & Net Totals based on percentages
 */
function initFinancialPercentageAutoCalculators() {
    function handleFinancialCalc(e) {
        const target = e.target;
        if (!target) return;

        const name = target.getAttribute('name');
        if (!name) return;

        const form = target.closest('form') || document;

        // 1. Employee Payslip Calculation (Basic Salary -> Auto PAYE 18%, Auto UIF 1%)
        if (name === 'basicSalary' || name === 'allowances' || name === 'overtime' || name === 'taxPaye' || name === 'uifDeduction' || name === 'otherDeductions') {
            const basicInput = form.querySelector('input[name="basicSalary"]');
            const payeInput = form.querySelector('input[name="taxPaye"]');
            const uifInput = form.querySelector('input[name="uifDeduction"]');

            if (basicInput && basicInput.value) {
                const basic = parseFloat(basicInput.value) || 0;

                // Auto-calculate statutory PAYE Tax (18% default rate) if basic salary changed
                if (name === 'basicSalary' && payeInput && (!payeInput.value || payeInput.value === '0' || payeInput.value === '0.00' || payeInput.dataset.autoCalculated === 'true')) {
                    payeInput.value = (basic * 0.18).toFixed(2);
                    payeInput.dataset.autoCalculated = 'true';
                }

                // Auto-calculate statutory 1% UIF (capped at max 177.12 ZAR per month)
                if (name === 'basicSalary' && uifInput && (!uifInput.value || uifInput.value === '0' || uifInput.value === '0.00' || uifInput.dataset.autoCalculated === 'true')) {
                    uifInput.value = Math.min(basic * 0.01, 177.12).toFixed(2);
                    uifInput.dataset.autoCalculated = 'true';
                }
            }
        }

        // 2. Invoice VAT Auto-Calculation (Subtotal -> 15% VAT Amount)
        if (name === 'subtotal') {
            const subtotalInput = form.querySelector('input[name="subtotal"]');
            const taxInput = form.querySelector('input[name="taxAmount"]');
            if (subtotalInput && taxInput) {
                const subtotal = parseFloat(subtotalInput.value) || 0;
                if (!taxInput.value || taxInput.value === '0' || taxInput.value === '0.00' || taxInput.dataset.autoCalculated === 'true') {
                    taxInput.value = (subtotal * 0.15).toFixed(2);
                    taxInput.dataset.autoCalculated = 'true';
                }
            }
        }

        // 3. Business Expense VAT Auto-Calculation
        if (name === 'amount') {
            const amountInput = form.querySelector('input[name="amount"]');
            const taxInput = form.querySelector('input[name="taxAmount"]');
            if (amountInput && taxInput) {
                const amount = parseFloat(amountInput.value) || 0;
                if (!taxInput.value || taxInput.value === '0' || taxInput.value === '0.00' || taxInput.dataset.autoCalculated === 'true') {
                    taxInput.value = (amount * (15 / 115)).toFixed(2);
                    taxInput.dataset.autoCalculated = 'true';
                }
            }
        }
    }

    document.addEventListener('input', handleFinancialCalc);
    document.addEventListener('change', handleFinancialCalc);
}

/**
 * Global Show/Hide Password Feature for all password textboxes
 */
function initPasswordToggleButtons() {
    function setupPasswordInput(input) {
        if (!input || input.dataset.passwordToggleInitialized === 'true') return;
        input.dataset.passwordToggleInitialized = 'true';

        let parent = input.parentElement;
        if (!parent) return;

        if (!parent.classList.contains('input-group') && !parent.classList.contains('position-relative')) {
            const wrapper = document.createElement('div');
            wrapper.className = 'position-relative d-flex align-items-center w-100';
            parent.insertBefore(wrapper, input);
            wrapper.appendChild(input);
            parent = wrapper;
        } else {
            parent.classList.add('position-relative');
        }

        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'btn btn-link text-decoration-none p-0 text-secondary position-absolute end-0 me-3 shadow-none border-0 bg-transparent';
        btn.style.cssText = 'z-index: 5; cursor: pointer; top: 50%; transform: translateY(-50%); opacity: 0.75;';
        btn.innerHTML = '<i class="bi bi-eye fs-6"></i>';
        btn.title = 'Show / Hide Password';

        input.style.paddingRight = '2.5rem';

        btn.addEventListener('click', function (e) {
            e.preventDefault();
            e.stopPropagation();
            const icon = btn.querySelector('i');
            if (input.type === 'password') {
                input.type = 'text';
                if (icon) icon.className = 'bi bi-eye-slash fs-6 text-primary';
            } else {
                input.type = 'password';
                if (icon) icon.className = 'bi bi-eye fs-6 text-secondary';
            }
        });

        parent.appendChild(btn);
    }

    document.querySelectorAll('input[type="password"]').forEach(setupPasswordInput);

    const observer = new MutationObserver(mutations => {
        mutations.forEach(mutation => {
            mutation.addedNodes.forEach(node => {
                if (node.nodeType === 1) {
                    if (node.tagName === 'INPUT' && node.type === 'password') {
                        setupPasswordInput(node);
                    } else if (node.querySelectorAll) {
                        node.querySelectorAll('input[type="password"]').forEach(setupPasswordInput);
                    }
                }
            });
        });
    });

    if (document.body) {
        observer.observe(document.body, { childList: true, subtree: true });
    }
}
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
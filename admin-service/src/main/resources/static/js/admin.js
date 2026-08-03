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
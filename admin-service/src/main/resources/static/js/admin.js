// admin.js

document.addEventListener('DOMContentLoaded', function () {
    // Highlight active sidebar link
    const currentPath = window.location.pathname;
    document.querySelectorAll('.nav-link').forEach(link => {
        if (link.getAttribute('href') === currentPath) {
            link.classList.add('active');
        }
    });

    // Optional: collapse sidebar on mobile (if you add toggle button later)
});
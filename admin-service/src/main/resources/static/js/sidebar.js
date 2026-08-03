document.addEventListener("DOMContentLoaded", function () {
    const sidebar = document.querySelector(".admin-sidebar");
    const mobileToggle = document.getElementById("mobileSidebarToggle");
    const sectionToggles = document.querySelectorAll(".nav-section-toggle");

    sectionToggles.forEach(function (toggle) {
        toggle.addEventListener("click", function () {
            const section = toggle.closest(".nav-section");
            const isOpen = section.classList.toggle("open");

            toggle.setAttribute("aria-expanded", String(isOpen));
        });
    });

    if (mobileToggle && sidebar) {
        mobileToggle.addEventListener("click", function () {
            sidebar.classList.toggle("mobile-open");
        });
    }

    document.addEventListener("click", function (event) {
        if (!sidebar || !sidebar.classList.contains("mobile-open")) {
            return;
        }

        const clickedInsideSidebar = sidebar.contains(event.target);
        const clickedToggle = mobileToggle && mobileToggle.contains(event.target);

        if (!clickedInsideSidebar && !clickedToggle) {
            sidebar.classList.remove("mobile-open");
        }
    });
});

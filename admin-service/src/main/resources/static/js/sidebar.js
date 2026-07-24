document.addEventListener("DOMContentLoaded", function () {
    const sidebar = document.querySelector(".admin-sidebar");
    const mobileToggle = document.getElementById("mobileSidebarToggle");

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
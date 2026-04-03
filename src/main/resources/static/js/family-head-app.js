/**
 * Sidebar: chuyển tab tin / dòng họ / thành viên (?tab=news|families|members).
 * Ẩn tab theo permissions (localStorage): MANAGE_FAMILY_NEWS, MANAGE_FAMILY_MEMBERS.
 */
(function () {
  const PANELS = ["news", "families", "members"];

  function hasPerm(name) {
    try {
      const perms = JSON.parse(localStorage.getItem("permissions") || "[]");
      return Array.isArray(perms) && perms.indexOf(name) >= 0;
    } catch (e) {
      return false;
    }
  }

  function roleHeadFallback() {
    return localStorage.getItem("role") === "FAMILY_HEAD";
  }

  function canNews() {
    return hasPerm("MANAGE_FAMILY_NEWS") || roleHeadFallback();
  }

  function canMembers() {
    return hasPerm("MANAGE_FAMILY_MEMBERS") || roleHeadFallback();
  }

  function applyTabPermissions() {
    const showNews = canNews();
    const showFam = canMembers();
    const showMem = canMembers();
    const linkNews = document.querySelector('.fh-side-link[data-panel="news"]');
    const linkFam = document.querySelector('.fh-side-link[data-panel="families"]');
    const linkMem = document.querySelector('.fh-side-link[data-panel="members"]');
    if (linkNews) linkNews.style.display = showNews ? "" : "none";
    if (linkFam) linkFam.style.display = showFam ? "" : "none";
    if (linkMem) linkMem.style.display = showMem ? "" : "none";
  }

  function isTabAllowed(name) {
    if (name === "news") return canNews();
    if (name === "families" || name === "members") return canMembers();
    return false;
  }

  function defaultTab() {
    if (canNews()) return "news";
    if (canMembers()) return "families";
    return "news";
  }

  function showPanel(name) {
    if (name === "news" && !canNews()) name = defaultTab();
    if ((name === "families" || name === "members") && !canMembers()) name = defaultTab();
    if (!PANELS.includes(name)) name = defaultTab();
    PANELS.forEach((p) => {
      const panel = document.getElementById(
        "fhPanel" + p.charAt(0).toUpperCase() + p.slice(1)
      );
      const link = document.querySelector('.fh-side-link[data-panel="' + p + '"]');
      if (panel) panel.hidden = p !== name;
      if (link) link.classList.toggle("active", p === name);
    });
    const u = new URL(window.location.href);
    u.searchParams.set("tab", name);
    window.history.replaceState({}, "", u.pathname + u.search);
    document.dispatchEvent(new CustomEvent("fh:panel", { detail: { panel: name } }));
  }

  function init() {
    if (!window.location.pathname.startsWith("/family-head")) return;
    applyTabPermissions();
    const params = new URLSearchParams(window.location.search);
    let tab = params.get("tab") || "news";
    if (!isTabAllowed(tab)) tab = defaultTab();
    document.querySelectorAll(".fh-side-link").forEach(function (a) {
      a.addEventListener("click", function (e) {
        e.preventDefault();
        showPanel(a.getAttribute("data-panel") || "news");
      });
    });
    /* Sau các script panel (cùng DOMContentLoaded) để listener fh:panel đã đăng ký */
    setTimeout(function () {
      showPanel(tab);
    }, 0);
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();

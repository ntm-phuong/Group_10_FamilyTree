/**
 * Trang /news: tạo / sửa / xóa tin khi có MANAGE_FAMILY_NEWS (hoặc FAMILY_HEAD).
 * API: POST /api/family-head/news, GET /api/news/{id}, PUT/DELETE /api/family-head/news/{id}
 */
(function () {
  function hasPerm(name) {
    try {
      const perms = JSON.parse(localStorage.getItem("permissions") || "[]");
      return Array.isArray(perms) && perms.indexOf(name) >= 0;
    } catch (e) {
      return false;
    }
  }

  function canManageNews() {
    return hasPerm("MANAGE_FAMILY_NEWS") || localStorage.getItem("role") === "FAMILY_HEAD";
  }

  function authHeaders() {
    const token = localStorage.getItem("token");
    const h = { "Content-Type": "application/json" };
    if (token) h["Authorization"] = "Bearer " + token;
    return h;
  }

  async function npReadFetchError(res, fallback) {
    const raw = await res.text();
    if (!raw || !String(raw).trim()) return fallback || "Lỗi.";
    try {
      const j = JSON.parse(raw);
      if (j && typeof j.message === "string" && j.message.trim()) return j.message.trim();
      if (j && typeof j.detail === "string" && j.detail.trim()) return j.detail.trim();
    } catch (e) {
      /* không phải JSON */
    }
    return String(raw).trim() || fallback || "Lỗi.";
  }

  function resolveNpFamilyId() {
    const wrap = document.querySelector(".news-page-wrap");
    const fromPage = wrap?.dataset?.pageFamilyId?.trim();
    if (fromPage) return fromPage;
    return (localStorage.getItem("my_family_id") || "").trim();
  }

  function pad2(n) {
    return String(n).padStart(2, "0");
  }

  function toDatetimeLocal(value) {
    if (value == null || value === "") return "";
    const d = new Date(value);
    if (Number.isNaN(d.getTime())) return "";
    return (
      d.getFullYear() +
      "-" +
      pad2(d.getMonth() + 1) +
      "-" +
      pad2(d.getDate()) +
      "T" +
      pad2(d.getHours()) +
      ":" +
      pad2(d.getMinutes())
    );
  }

  function fromDatetimeLocal(val) {
    if (!val || !String(val).trim()) return null;
    const s = String(val).trim();
    if (s.length === 16) return s + ":00";
    return s;
  }

  function syncNpPublicFields() {
    const vis = document.getElementById("npNewsVisibility").value;
    const box = document.getElementById("npPublicFields");
    if (!box) return;
    box.style.display = vis === "PUBLIC_SITE" || vis === "DRAFT" ? "block" : "none";
  }

  function resetNpForm() {
    document.getElementById("npNewsEditId").value = "";
    document.getElementById("npNewsFamilyId").value = "";
    document.getElementById("npNewsTitle").value = "";
    document.getElementById("npNewsSummary").value = "";
    document.getElementById("npNewsContent").value = "";
    document.getElementById("npNewsLocation").value = "";
    document.getElementById("npNewsRemindBefore").value = "";
    document.getElementById("npNewsStartAt").value = "";
    document.getElementById("npNewsEndAt").value = "";
    document.getElementById("npNewsVisibility").value = "FAMILY_ONLY";
    document.getElementById("npNewsPublicCategory").value = "";
    document.getElementById("npNewsFeatured").checked = false;
    syncNpPublicFields();
  }

  function fillNpForm(detail) {
    document.getElementById("npNewsEditId").value = detail.id || "";
    document.getElementById("npNewsFamilyId").value = detail.familyId || "";
    document.getElementById("npNewsTitle").value = detail.title || "";
    document.getElementById("npNewsSummary").value = detail.summary || "";
    document.getElementById("npNewsContent").value = detail.content || "";
    document.getElementById("npNewsLocation").value = detail.location != null ? detail.location : "";
    document.getElementById("npNewsRemindBefore").value =
      detail.remindBefore != null ? String(detail.remindBefore) : "";
    document.getElementById("npNewsStartAt").value = toDatetimeLocal(detail.startAt);
    document.getElementById("npNewsEndAt").value = toDatetimeLocal(detail.endAt);
    if (detail.visibility) document.getElementById("npNewsVisibility").value = detail.visibility;
    const catSel = document.getElementById("npNewsCategoryId");
    if (catSel && detail.categoryId) catSel.value = detail.categoryId;
    document.getElementById("npNewsPublicCategory").value = detail.publicCategory || "";
    document.getElementById("npNewsFeatured").checked = detail.featured === true;
    syncNpPublicFields();
  }

  function collectNpPayload(familyId, authorId) {
    const remindVal = document.getElementById("npNewsRemindBefore").value.trim();
    let remindBefore = null;
    if (remindVal !== "") {
      const n = parseInt(remindVal, 10);
      if (!Number.isNaN(n)) remindBefore = n;
    }
    const vis = document.getElementById("npNewsVisibility").value;
    const body = {
      title: document.getElementById("npNewsTitle").value.trim(),
      summary: document.getElementById("npNewsSummary").value.trim() || null,
      content: document.getElementById("npNewsContent").value.trim() || null,
      familyId: familyId,
      categoryId: document.getElementById("npNewsCategoryId").value || null,
      authorId: authorId,
      startAt: fromDatetimeLocal(document.getElementById("npNewsStartAt").value),
      endAt: fromDatetimeLocal(document.getElementById("npNewsEndAt").value),
      location: document.getElementById("npNewsLocation").value.trim() || null,
      remindBefore: remindBefore,
      visibility: vis,
    };
    if (vis === "PUBLIC_SITE" || vis === "DRAFT") {
      body.publicCategory = document.getElementById("npNewsPublicCategory").value || null;
      body.featured = document.getElementById("npNewsFeatured").checked === true;
    }
    return body;
  }

  function openModal(el) {
    el.classList.add("open");
    el.setAttribute("aria-hidden", "false");
  }

  function closeModal(el) {
    el.classList.remove("open");
    el.setAttribute("aria-hidden", "true");
  }

  let manageMode = null;

  function setManageMode(mode) {
    manageMode = mode;
    const wrap = document.querySelector(".news-page-wrap");
    const hint = document.getElementById("npModeHint");
    const btnEdit = document.getElementById("npModeEdit");
    const btnDel = document.getElementById("npModeDelete");
    document.querySelectorAll(".np-news-cb").forEach((cb) => {
      cb.checked = false;
    });
    if (wrap) {
      wrap.classList.toggle("news-page--edit-mode", mode === "edit");
      wrap.classList.toggle("news-page--delete-mode", mode === "delete");
    }
    if (btnEdit) btnEdit.classList.toggle("btn-primary", mode === "edit");
    if (btnEdit) btnEdit.classList.toggle("btn-white", mode !== "edit");
    if (btnDel) btnDel.classList.toggle("btn-primary", mode === "delete");
    if (btnDel) btnDel.classList.toggle("btn-white", mode !== "delete");
    if (hint) {
      if (mode === "edit") hint.textContent = "Nhấp vào thẻ tin để sửa.";
      else if (mode === "delete") hint.textContent = "Chọn bài rồi bấm Xóa đã chọn.";
      else hint.textContent = "";
    }
  }

  async function loadNpCategories() {
    const sel = document.getElementById("npNewsCategoryId");
    if (!sel) return;
    const res = await fetch("/api/public/categories");
    if (!res.ok) return;
    const cats = await res.json();
    sel.innerHTML =
      '<option value="">— Danh mục —</option>' +
      cats
        .map(function (c) {
          return '<option value="' + c.id + '">' + (c.name || "") + "</option>";
        })
        .join("");
  }

  async function openEditForCard(card) {
    const id = card.getAttribute("data-news-id");
    if (!id) return;
    if (!localStorage.getItem("token")) {
      window.location.href = "/login?redirect=" + encodeURIComponent(window.location.pathname + window.location.search);
      return;
    }
    const modal = document.getElementById("npNewsFormModal");
    try {
      const res = await fetch("/api/news/" + encodeURIComponent(id));
      if (!res.ok) throw new Error("Không tải được bài viết.");
      const detail = await res.json();
      resetNpForm();
      fillNpForm(detail);
      document.getElementById("npNewsModalTitle").textContent = "Sửa tin / sự kiện";
      openModal(modal);
    } catch (err) {
      alert(err.message || "Lỗi.");
    }
  }

  async function openCreateModal() {
    if (!localStorage.getItem("token")) {
      window.location.href = "/login?redirect=" + encodeURIComponent(window.location.pathname + window.location.search);
      return;
    }
    const familyId = resolveNpFamilyId();
    if (!familyId) {
      alert(
        "Không lấy được mã dòng họ trên trang (data-page-family-id). Kiểm tra cấu hình app.clan.family-id hoặc đăng nhập tài khoản gắn đúng dòng họ."
      );
      return;
    }
    const modal = document.getElementById("npNewsFormModal");
    try {
      await loadNpCategories();
      resetNpForm();
      document.getElementById("npNewsFamilyId").value = familyId;
      document.getElementById("npNewsModalTitle").textContent = "Tạo tin / sự kiện";
      openModal(modal);
    } catch (err) {
      alert(err.message || "Lỗi.");
    }
  }

  function init() {
    if (!window.location.pathname.startsWith("/news")) return;
    if (!canManageNews()) return;

    const toolbar = document.getElementById("newsManageToolbar");
    if (toolbar) toolbar.hidden = false;

    loadNpCategories().catch(function () {});

    const modal = document.getElementById("npNewsFormModal");
    document.getElementById("npNewsVisibility")?.addEventListener("change", syncNpPublicFields);

    document.getElementById("npCreateNews")?.addEventListener("click", function () {
      openCreateModal();
    });

    document.getElementById("npModeEdit")?.addEventListener("click", function () {
      if (manageMode === "edit") {
        setManageMode(null);
        return;
      }
      setManageMode("edit");
    });

    document.getElementById("npModeDelete")?.addEventListener("click", function () {
      if (manageMode === "delete") {
        setManageMode(null);
        return;
      }
      setManageMode("delete");
    });

    document.getElementById("npDeleteSelected")?.addEventListener("click", async function () {
      const ids = [];
      document.querySelectorAll(".np-news-cb:checked").forEach(function (cb) {
        const id = cb.getAttribute("data-news-id");
        if (id) ids.push(id);
      });
      if (!ids.length) {
        alert("Chưa chọn bài nào.");
        return;
      }
      if (!confirm("Xóa " + ids.length + " bài đã chọn?")) return;
      const token = localStorage.getItem("token");
      if (!token) return;
      for (let i = 0; i < ids.length; i++) {
        const res = await fetch("/api/family-head/news/" + encodeURIComponent(ids[i]), {
          method: "DELETE",
          headers: authHeaders(),
        });
        if (!res.ok && res.status !== 404) {
          alert("Lỗi xóa bài " + (i + 1));
          return;
        }
      }
      window.location.reload();
    });

    document.querySelector(".news-page-wrap")?.addEventListener(
      "click",
      function (e) {
        if (manageMode !== "edit") return;
        const card = e.target.closest(".news-card");
        if (!card || !card.getAttribute("data-news-id")) return;
        if (e.target.closest(".np-news-cb") || e.target.closest(".np-news-cb-wrap")) return;
        if (e.target.closest("a")) e.preventDefault();
        e.stopPropagation();
        openEditForCard(card);
      },
      true
    );

    document.getElementById("npNewsModalClose")?.addEventListener("click", function () {
      closeModal(modal);
    });
    document.getElementById("npNewsModalCancel")?.addEventListener("click", function () {
      closeModal(modal);
    });
    modal?.addEventListener("click", function (e) {
      if (e.target === modal) closeModal(modal);
    });

    document.getElementById("npNewsForm")?.addEventListener("submit", async function (ev) {
      ev.preventDefault();
      const token = localStorage.getItem("token");
      const userId = localStorage.getItem("user_id");
      if (!token || !userId) {
        window.location.href = "/login?redirect=" + encodeURIComponent(window.location.pathname + window.location.search);
        return;
      }
      const familyId = document.getElementById("npNewsFamilyId").value.trim();
      if (!familyId) {
        alert("Thiếu dòng họ gắn bài.");
        return;
      }
      const vis = document.getElementById("npNewsVisibility").value;
      if (vis === "PUBLIC_SITE" && !document.getElementById("npNewsPublicCategory").value) {
        alert("Tin công khai cần chọn tab (danh mục trang tin).");
        return;
      }
      const editId = document.getElementById("npNewsEditId").value.trim();
      const payload = collectNpPayload(familyId, userId);
      const url = editId
        ? "/api/family-head/news/" + encodeURIComponent(editId)
        : "/api/family-head/news";
      const method = editId ? "PUT" : "POST";
      try {
        const res = await fetch(url, {
          method: method,
          headers: authHeaders(),
          body: JSON.stringify(payload),
        });
        if (res.status === 403) {
          alert("Không có quyền lưu tin này.");
          return;
        }
        if (!res.ok) {
          const msg = await npReadFetchError(res, editId ? "Lưu thất bại." : "Tạo tin thất bại.");
          alert(msg);
          return;
        }
        closeModal(modal);
        setManageMode(null);
        window.location.reload();
      } catch (err) {
        alert(err.message || "Lỗi lưu.");
      }
    });
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();

/**
 * Trang /news: tạo / sửa / xóa tin chỉ dành cho FAMILY_HEAD.
 * API giữ nguyên: POST /api/family-head/news, GET /api/news/{id}, PUT/DELETE /api/family-head/news/{id}
 */
(function () {
  let previewObjectUrl = null;

  function hasPerm(name) {
    try {
      const perms = JSON.parse(localStorage.getItem("permissions") || "[]");
      return Array.isArray(perms) && perms.indexOf(name) >= 0;
    } catch (e) {
      return false;
    }
  }

  function canManageNews() {
    const r = (localStorage.getItem("role") || "").trim();
    return (
      hasPerm("MANAGE_FAMILY_NEWS") ||
      hasPerm("MANAGE_CLAN") ||
      hasPerm("FAMILY_HEAD") ||
      r === "FAMILY_BRANCH_MANAGER" ||
      r === "FAMILY_NEWS_MANAGER" ||
      r === "ADMIN"
    );
  }

  function ensureNewsManagePermission() {
    if (canManageNews()) return true;
    alert("Bạn không có quyền thực hiện hành động này");
    return false;
  }

  function authHeaders() {
    const token = localStorage.getItem("token");
    const h = { "Content-Type": "application/json" };
    if (token) h.Authorization = "Bearer " + token;
    return h;
  }

  function authOnlyHeaders() {
    const token = localStorage.getItem("token");
    const h = {};
    if (token) h.Authorization = "Bearer " + token;
    return h;
  }

  async function syncNewsPagePermissionsFromMe() {
    const token = localStorage.getItem("token");
    if (!token) return;
    try {
      const res = await fetch("/api/auth/me", { headers: authHeaders() });
      if (!res.ok) return;
      const me = await res.json();
      if (me.permissions) localStorage.setItem("permissions", JSON.stringify(me.permissions));
      if (me.role != null) localStorage.setItem("role", String(me.role));
    } catch (e) {
      /* ignore */
    }
  }

  function applyNewsPagePermissionUi() {
    const wrap = document.querySelector(".news-page-wrap");
    const can = canManageNews();
    if (wrap) {
      wrap.classList.toggle("news-page--no-manage-perm", !can);
    }
    const createBtn = document.getElementById("npCreateNews");
    if (createBtn) {
      createBtn.hidden = !can;
      createBtn.setAttribute("aria-hidden", can ? "false" : "true");
    }
    document.querySelectorAll(".news-card-actions").forEach(function (actions) {
      actions.hidden = !can;
      actions.setAttribute("aria-hidden", can ? "false" : "true");
    });
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
    const fromPage = wrap && wrap.dataset ? String(wrap.dataset.pageFamilyId || "").trim() : "";
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
    const visInput = document.getElementById("npNewsVisibility");
    const box = document.getElementById("npPublicFields");
    if (!visInput || !box) return;
    const vis = visInput.value;
    box.style.display = vis === "PUBLIC_SITE" || vis === "DRAFT" ? "block" : "none";
  }

  function clearPreviewObjectUrl() {
    if (previewObjectUrl) {
      URL.revokeObjectURL(previewObjectUrl);
      previewObjectUrl = null;
    }
  }

  function setNpImageError(message) {
    const errorEl = document.getElementById("npNewsImageError");
    if (!errorEl) return;
    if (message && String(message).trim()) {
      errorEl.textContent = String(message).trim();
      errorEl.classList.remove("hidden");
    } else {
      errorEl.textContent = "";
      errorEl.classList.add("hidden");
    }
  }

  function setNpImagePreview(url) {
    const img = document.getElementById("npNewsImagePreview");
    const placeholder = document.getElementById("npNewsImagePlaceholder");
    if (!img || !placeholder) return;
    if (url && String(url).trim()) {
      img.src = url;
      img.classList.remove("hidden");
      placeholder.classList.add("hidden");
    } else {
      img.src = "";
      img.classList.add("hidden");
      placeholder.classList.remove("hidden");
    }
  }

  function handleImageFileChange() {
    const input = document.getElementById("npNewsImage");
    if (!input || !input.files || !input.files[0]) {
      clearPreviewObjectUrl();
      setNpImageError("");
      setNpImagePreview(document.getElementById("npNewsCoverImage").value);
      return;
    }
    clearPreviewObjectUrl();
    previewObjectUrl = URL.createObjectURL(input.files[0]);
    setNpImageError("");
    setNpImagePreview(previewObjectUrl);
  }

  function resetNpForm() {
    document.getElementById("npNewsEditId").value = "";
    document.getElementById("npNewsFamilyId").value = "";
    document.getElementById("npNewsCoverImage").value = "";
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
    const imageInput = document.getElementById("npNewsImage");
    if (imageInput) imageInput.value = "";
    clearPreviewObjectUrl();
    setNpImageError("");
    setNpImagePreview("");
    syncNpPublicFields();
  }

  function fillNpForm(detail) {
    document.getElementById("npNewsEditId").value = detail.id || "";
    document.getElementById("npNewsFamilyId").value = detail.familyId || "";
    document.getElementById("npNewsCoverImage").value = detail.coverImage || "";
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
    const imageInput = document.getElementById("npNewsImage");
    if (imageInput) imageInput.value = "";
    clearPreviewObjectUrl();
    setNpImageError("");
    setNpImagePreview(detail.coverImage || "");
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
      coverImage: document.getElementById("npNewsCoverImage").value.trim() || null,
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
    window.setTimeout(function () {
      document.getElementById("npNewsTitle")?.focus();
    }, 40);
  }

  function closeModal(el) {
    el.classList.remove("open");
    el.setAttribute("aria-hidden", "true");
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

  async function openEditForId(id) {
    if (!id) return;
    if (!localStorage.getItem("token")) {
      window.location.href = "/login?redirect=" + encodeURIComponent(window.location.pathname + window.location.search);
      return;
    }
    const modal = document.getElementById("npNewsFormModal");
    try {
      await loadNpCategories();
      const res = await fetch("/api/news/" + encodeURIComponent(id));
      if (!res.ok) throw new Error("Không tải được bài viết.");
      const detail = await res.json();
      resetNpForm();
      fillNpForm(detail);
      document.getElementById("npNewsModalTitle").textContent = "Chỉnh sửa tin";
      const subtitle = document.getElementById("npNewsModalSubtitle");
      if (subtitle) subtitle.textContent = "Cập nhật lại nội dung bài viết trước khi lưu thay đổi.";
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
      document.getElementById("npNewsModalTitle").textContent = "Tạo tin tức";
      const subtitle = document.getElementById("npNewsModalSubtitle");
      if (subtitle) subtitle.textContent = "Nhập các thông tin chính để tạo bài viết mới.";
      openModal(modal);
    } catch (err) {
      alert(err.message || "Lỗi.");
    }
  }

  async function deleteNewsById(id) {
    if (!id) return;
    const token = localStorage.getItem("token");
    if (!token) {
      window.location.href = "/login?redirect=" + encodeURIComponent(window.location.pathname + window.location.search);
      return;
    }
    if (!confirm("Xóa bài viết này?")) return;
    try {
      const res = await fetch("/api/family-head/news/" + encodeURIComponent(id), {
        method: "DELETE",
        headers: authHeaders(),
      });
      if (!res.ok && res.status !== 404) {
        const msg = await npReadFetchError(res, "Xóa thất bại.");
        alert(msg);
        return;
      }
      window.location.reload();
    } catch (err) {
      alert(err.message || "Lỗi xóa.");
    }
  }

  async function uploadSelectedImageIfNeeded() {
    const imageInput = document.getElementById("npNewsImage");
    const current = document.getElementById("npNewsCoverImage").value.trim();
    if (!imageInput || !imageInput.files || !imageInput.files[0]) {
      return current || null;
    }
    const formData = new FormData();
    formData.append("file", imageInput.files[0]);
    const res = await fetch("/api/family-head/news/upload-image", {
      method: "POST",
      headers: authOnlyHeaders(),
      body: formData,
    });
    if (!res.ok) {
      throw new Error(await npReadFetchError(res, "Upload ảnh thất bại."));
    }
    const data = await res.json();
    const imageUrl = data && typeof data.url === "string" ? data.url.trim() : "";
    if (!imageUrl) {
      throw new Error("Upload ảnh thất bại.");
    }
    document.getElementById("npNewsCoverImage").value = imageUrl;
    clearPreviewObjectUrl();
    setNpImagePreview(imageUrl);
    imageInput.value = "";
    return imageUrl;
  }

  async function init() {
    if (!window.location.pathname.startsWith("/news")) return;
    await syncNewsPagePermissionsFromMe();
    applyNewsPagePermissionUi();

    const modal = document.getElementById("npNewsFormModal");
    const createBtn = document.getElementById("npCreateNews");
    const imageInput = document.getElementById("npNewsImage");

    document.getElementById("npNewsVisibility")?.addEventListener("change", syncNpPublicFields);
    imageInput?.addEventListener("change", handleImageFileChange);

    createBtn?.addEventListener("click", function () {
      if (!ensureNewsManagePermission()) return;
      openCreateModal();
    });

    document.querySelector(".news-page-wrap")?.addEventListener("click", function (e) {
      const editBtn = e.target.closest(".btn-edit");
      if (editBtn) {
        e.preventDefault();
        e.stopPropagation();
        if (!ensureNewsManagePermission()) return;
        openEditForId(editBtn.getAttribute("data-id"));
        return;
      }

      const deleteBtn = e.target.closest(".btn-delete");
      if (deleteBtn) {
        e.preventDefault();
        e.stopPropagation();
        if (!ensureNewsManagePermission()) return;
        deleteNewsById(deleteBtn.getAttribute("data-id"));
      }
    });

    document.getElementById("npNewsModalClose")?.addEventListener("click", function () {
      closeModal(modal);
    });
    document.getElementById("npNewsModalCancel")?.addEventListener("click", function () {
      closeModal(modal);
    });
    modal?.addEventListener("click", function (e) {
      if (e.target === modal) closeModal(modal);
    });
    document.addEventListener("keydown", function (e) {
      if (e.key === "Escape" && modal?.classList.contains("open")) {
        closeModal(modal);
      }
    });

    document.getElementById("npNewsForm")?.addEventListener("submit", async function (ev) {
      ev.preventDefault();
      const token = localStorage.getItem("token");
      const userId = localStorage.getItem("user_id");
      if (!token || !userId) {
        window.location.href = "/login?redirect=" + encodeURIComponent(window.location.pathname + window.location.search);
        return;
      }
      if (!ensureNewsManagePermission()) {
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

      const submitBtn = this.querySelector('button[type="submit"]');
      const originalLabel = submitBtn ? submitBtn.textContent : "";
      if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.textContent = "Đang lưu...";
      }

      try {
        await uploadSelectedImageIfNeeded();
        const editId = document.getElementById("npNewsEditId").value.trim();
        const payload = collectNpPayload(familyId, userId);
        const url = editId
          ? "/api/family-head/news/" + encodeURIComponent(editId)
          : "/api/family-head/news";
        const method = editId ? "PUT" : "POST";
        const res = await fetch(url, {
          method: method,
          headers: authHeaders(),
          body: JSON.stringify(payload),
        });
        if (res.status === 403) {
          alert("Bạn không có quyền thực hiện hành động này");
          return;
        }
        if (!res.ok) {
          const msg = await npReadFetchError(res, editId ? "Lưu thất bại." : "Tạo tin thất bại.");
          alert(msg);
          return;
        }
        closeModal(modal);
        window.location.reload();
      } catch (err) {
        alert(err.message || "Lỗi lưu.");
      } finally {
        if (submitBtn) {
          submitBtn.disabled = false;
          submitBtn.textContent = originalLabel || "Lưu";
        }
      }
    });
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();

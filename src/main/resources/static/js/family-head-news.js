/**
 * Quản lý tin / sự kiện — cần MANAGE_FAMILY_NEWS; phạm vi chi mình + chi con (API đã kiểm tra).
 * - Danh sách đầy đủ (kể cả nháp): GET /api/family-head/news/family/{familyId} + JWT
 * - CRUD: POST/PUT/DELETE /api/family-head/news
 */
(function () {
  const VIS_LABELS = {
    DRAFT: "Nháp",
    FAMILY_ONLY: "Nội bộ (/news, cần đăng nhập)",
    PUBLIC_SITE: "Công khai (/news)",
  };

  const PUB_CAT_LABELS = {
    EVENT: "Sự kiện",
    ANNOUNCEMENT: "Thông báo",
    HISTORY: "Dòng họ",
    GENERAL: "Tin chung",
  };

  function authHeaders() {
    const token = localStorage.getItem("token");
    const h = { "Content-Type": "application/json" };
    if (token) h["Authorization"] = "Bearer " + token;
    return h;
  }

  function showAlert(message, kind) {
    const el = document.getElementById("fhAlert");
    if (!el) return;
    el.textContent = message;
    el.className = "fh-alert " + (kind || "info");
    el.style.display = "block";
  }

  function hideAlert() {
    const el = document.getElementById("fhAlert");
    if (el) el.style.display = "none";
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

  function formatDisplayDate(value) {
    if (value == null || value === "") return "—";
    const d = new Date(value);
    if (Number.isNaN(d.getTime())) return "—";
    return d.toLocaleString("vi-VN", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  }

  function escapeHtml(s) {
    if (s == null) return "";
    const div = document.createElement("div");
    div.textContent = s;
    return div.innerHTML;
  }

  function openModal(overlay) {
    overlay.classList.add("open");
    overlay.setAttribute("aria-hidden", "false");
  }

  function closeModal(overlay) {
    overlay.classList.remove("open");
    overlay.setAttribute("aria-hidden", "true");
  }

  function syncPublicFieldsVisibility() {
    const vis = document.getElementById("newsVisibility").value;
    const box = document.getElementById("fhPublicFields");
    if (!box) return;
    const show = vis === "PUBLIC_SITE" || vis === "DRAFT";
    box.style.display = show ? "block" : "none";
  }

  async function loadFamilies(selectEl) {
    const token = localStorage.getItem("token");
    let rows;
    if (token) {
      const res = await fetch("/api/family-head/families", { headers: authHeaders() });
      if (!res.ok) {
        const t = await res.text();
        throw new Error(t || "Không tải được dòng họ (cần quyền trưởng họ).");
      }
      rows = await res.json();
    } else {
      const res = await fetch("/api/public/families");
      if (!res.ok) throw new Error("Không tải được danh sách dòng họ.");
      const pub = await res.json();
      rows = pub.map(function (f) {
        return { familyId: f.id, familyName: f.name };
      });
    }

    const q = new URLSearchParams(window.location.search);
    const queryFamily = q.get("familyId");
    const saved = localStorage.getItem("selectedFamilyId");
    const myFam = localStorage.getItem("my_family_id");
    let familyId = queryFamily || saved || myFam || "";

    selectEl.innerHTML =
      '<option value="">Chọn dòng họ</option>' +
      rows
        .map(function (f) {
          return (
            '<option value="' +
            escapeHtml(f.familyId) +
            '">' +
            escapeHtml(f.familyName) +
            "</option>"
          );
        })
        .join("");

    if (!familyId && rows.length) familyId = rows[0].familyId;
    if (familyId) selectEl.value = familyId;
    return familyId;
  }

  async function loadCategories(filterSelect, formSelect) {
    const res = await fetch("/api/public/categories");
    if (!res.ok) throw new Error("Không tải được danh mục.");
    const cats = await res.json();
    const opts =
      '<option value="">Tất cả danh mục</option>' +
      cats
        .map(function (c) {
          return (
            '<option value="' +
            escapeHtml(c.id) +
            '">' +
            escapeHtml(c.name) +
            "</option>"
          );
        })
        .join("");
    filterSelect.innerHTML = opts;
    formSelect.innerHTML = cats
      .map(function (c) {
        return (
          '<option value="' +
          escapeHtml(c.id) +
          '">' +
          escapeHtml(c.name) +
          "</option>"
        );
      })
      .join("");
    if (!formSelect.value && cats.length) formSelect.selectedIndex = 0;
  }

  async function fetchNewsList(familyId, search, categoryId) {
    const res = await fetch(
      "/api/family-head/news/family/" + encodeURIComponent(familyId),
      { headers: authHeaders() }
    );
    if (res.status === 401 || res.status === 403) {
      throw new Error("Cần đăng nhập (quyền quản trị dòng họ) để xem danh sách đầy đủ.");
    }
    if (!res.ok) {
      const t = await res.text();
      throw new Error(t || "Không tải được danh sách tin.");
    }
    let list = await res.json();
    if (search && search.trim()) {
      const q = search.trim().toLowerCase();
      list = list.filter(function (n) {
        return (
          (n.title && n.title.toLowerCase().indexOf(q) >= 0) ||
          (n.summary && n.summary.toLowerCase().indexOf(q) >= 0)
        );
      });
    }
    if (categoryId && categoryId.trim()) {
      list = list.filter(function (n) {
        return n.categoryId === categoryId;
      });
    }
    return list;
  }

  async function fetchNewsDetail(id) {
    const res = await fetch("/api/news/" + encodeURIComponent(id));
    if (res.status === 404) return null;
    if (!res.ok) {
      const t = await res.text();
      throw new Error(t || "Không tải chi tiết tin.");
    }
    return res.json();
  }

  function categoryCell(row) {
    const vis = row.visibility || "FAMILY_ONLY";
    if (vis === "PUBLIC_SITE" || vis === "DRAFT") {
      if (row.publicCategory) {
        return PUB_CAT_LABELS[row.publicCategory] || row.publicCategory;
      }
      return "—";
    }
    return row.categoryName || "—";
  }

  function renderTable(tbody, items) {
    const empty = document.getElementById("fhEmptyState");
    if (!items || !items.length) {
      tbody.innerHTML = "";
      if (empty) empty.style.display = "block";
      return;
    }
    if (empty) empty.style.display = "none";
    tbody.innerHTML = items
      .map(function (row) {
        const vis = row.visibility || "FAMILY_ONLY";
        const visLabel = VIS_LABELS[vis] || vis;
        const feat =
          row.featured === true ? "Có" : row.featured === false ? "Không" : "—";
        return (
          '<tr data-id="' +
          escapeHtml(row.id) +
          '">' +
          '<td class="fh-title">' +
          escapeHtml(row.title || "") +
          '<div class="fh-muted">' +
          escapeHtml((row.summary || "").slice(0, 80)) +
          ((row.summary || "").length > 80 ? "…" : "") +
          "</div></td>" +
          "<td>" +
          escapeHtml(categoryCell(row)) +
          "</td>" +
          "<td>" +
          escapeHtml(visLabel) +
          "</td>" +
          "<td>" +
          escapeHtml(feat) +
          "</td>" +
          "<td>" +
          formatDisplayDate(row.startAt) +
          "</td>" +
          "<td>" +
          formatDisplayDate(row.createdAt) +
          "</td>" +
          '<td class="fh-actions">' +
          '<button type="button" class="btn btn-white btn-sm fh-edit" data-id="' +
          escapeHtml(row.id) +
          '"><i class="fas fa-pen"></i></button> ' +
          '<button type="button" class="btn btn-white btn-sm fh-del" data-id="' +
          escapeHtml(row.id) +
          '"><i class="fas fa-trash-alt"></i></button>' +
          "</td>" +
          "</tr>"
        );
      })
      .join("");
  }

  function resetForm() {
    document.getElementById("newsEditId").value = "";
    document.getElementById("newsTitle").value = "";
    document.getElementById("newsSummary").value = "";
    document.getElementById("newsContent").value = "";
    document.getElementById("newsLocation").value = "";
    document.getElementById("newsRemindBefore").value = "";
    document.getElementById("newsStartAt").value = "";
    document.getElementById("newsEndAt").value = "";
    document.getElementById("newsVisibility").value = "FAMILY_ONLY";
    document.getElementById("newsPublicCategory").value = "";
    document.getElementById("newsFeatured").checked = false;
    syncPublicFieldsVisibility();
  }

  function fillFormFromDetail(detail) {
    document.getElementById("newsEditId").value = detail.id || "";
    document.getElementById("newsTitle").value = detail.title || "";
    document.getElementById("newsSummary").value = detail.summary || "";
    document.getElementById("newsContent").value = detail.content || "";
    document.getElementById("newsLocation").value = detail.location != null ? detail.location : "";
    document.getElementById("newsRemindBefore").value =
      detail.remindBefore != null ? String(detail.remindBefore) : "";
    document.getElementById("newsStartAt").value = toDatetimeLocal(detail.startAt);
    document.getElementById("newsEndAt").value = toDatetimeLocal(detail.endAt);
    if (detail.visibility) document.getElementById("newsVisibility").value = detail.visibility;
    if (detail.categoryId) document.getElementById("newsCategoryId").value = detail.categoryId;
    document.getElementById("newsPublicCategory").value = detail.publicCategory || "";
    document.getElementById("newsFeatured").checked = detail.featured === true;
    syncPublicFieldsVisibility();
  }

  function collectPayload(familyId, authorId) {
    const remindVal = document.getElementById("newsRemindBefore").value.trim();
    let remindBefore = null;
    if (remindVal !== "") {
      const n = parseInt(remindVal, 10);
      if (!Number.isNaN(n)) remindBefore = n;
    }
    const vis = document.getElementById("newsVisibility").value;
    const body = {
      title: document.getElementById("newsTitle").value.trim(),
      summary: document.getElementById("newsSummary").value.trim() || null,
      content: document.getElementById("newsContent").value.trim() || null,
      familyId: familyId,
      categoryId: document.getElementById("newsCategoryId").value || null,
      authorId: authorId,
      startAt: fromDatetimeLocal(document.getElementById("newsStartAt").value),
      endAt: fromDatetimeLocal(document.getElementById("newsEndAt").value),
      location: document.getElementById("newsLocation").value.trim() || null,
      remindBefore: remindBefore,
      visibility: vis,
    };
    if (vis === "PUBLIC_SITE" || vis === "DRAFT") {
      body.publicCategory = document.getElementById("newsPublicCategory").value || null;
      body.featured = document.getElementById("newsFeatured").checked === true;
    }
    return body;
  }

  async function init() {
    if (!window.location.pathname.startsWith("/family-head")) return;

    const familySelect = document.getElementById("fhFamilyFilter");
    const searchInput = document.getElementById("fhSearch");
    const categoryFilter = document.getElementById("fhCategoryFilter");
    const tbody = document.getElementById("fhNewsBody");
    const modal = document.getElementById("newsFormModal");
    const token = localStorage.getItem("token");

    if (!token) {
      showAlert("Đăng nhập (trưởng họ / admin) để xem và chỉnh sửa tin — kể cả bản nháp.", "info");
    }

    document.getElementById("newsVisibility").addEventListener("change", syncPublicFieldsVisibility);

    try {
      await loadFamilies(familySelect);
      await loadCategories(categoryFilter, document.getElementById("newsCategoryId"));
    } catch (e) {
      showAlert(e.message || "Lỗi tải dữ liệu.", "error");
      if (tbody) tbody.innerHTML = '<tr><td colspan="7" class="fh-muted">Không tải được.</td></tr>';
      return;
    }

    async function refreshList() {
      hideAlert();
      const familyId = familySelect.value;
      if (!familyId) {
        if (tbody) {
          tbody.innerHTML = '<tr><td colspan="7" class="fh-muted">Chọn dòng họ.</td></tr>';
        }
        const emptyEl = document.getElementById("fhEmptyState");
        if (emptyEl) emptyEl.style.display = "none";
        return;
      }
      if (!localStorage.getItem("token")) {
        if (tbody) {
          tbody.innerHTML =
            '<tr><td colspan="7" class="fh-muted">Đăng nhập để xem danh sách đầy đủ.</td></tr>';
        }
        return;
      }
      if (tbody) tbody.innerHTML = '<tr><td colspan="7" class="fh-muted">Đang tải...</td></tr>';
      const emptyEl = document.getElementById("fhEmptyState");
      if (emptyEl) emptyEl.style.display = "none";
      try {
        const list = await fetchNewsList(familyId, searchInput.value, categoryFilter.value);
        renderTable(tbody, list);
      } catch (e) {
        showAlert(e.message || "Lỗi tải danh sách.", "error");
        if (tbody) tbody.innerHTML = '<tr><td colspan="7" class="fh-muted">Lỗi.</td></tr>';
      }
    }

    familySelect.addEventListener("change", function () {
      const v = this.value;
      if (v) localStorage.setItem("selectedFamilyId", v);
      else localStorage.removeItem("selectedFamilyId");
      const u = new URL(window.location.href);
      if (v) u.searchParams.set("familyId", v);
      else u.searchParams.delete("familyId");
      window.history.replaceState({}, "", u.pathname + u.search);
      refreshList();
    });

    let searchTimer;
    searchInput.addEventListener("input", function () {
      clearTimeout(searchTimer);
      searchTimer = setTimeout(refreshList, 350);
    });

    categoryFilter.addEventListener("change", refreshList);
    document.getElementById("fhBtnReload").addEventListener("click", refreshList);

    document.getElementById("fhBtnAdd").addEventListener("click", function () {
      if (!localStorage.getItem("token")) {
        window.location.href = "/login?redirect=" + encodeURIComponent("/family-head");
        return;
      }
      const familyId = familySelect.value;
      if (!familyId) {
        showAlert("Chọn dòng họ trước.", "error");
        return;
      }
      document.getElementById("newsModalTitle").textContent = "Thêm tin / sự kiện";
      resetForm();
      openModal(modal);
    });

    document.getElementById("newsModalClose").addEventListener("click", function () {
      closeModal(modal);
    });
    document.getElementById("newsModalCancel").addEventListener("click", function () {
      closeModal(modal);
    });
    modal.addEventListener("click", function (e) {
      if (e.target === modal) closeModal(modal);
    });

    tbody.addEventListener("click", async function (e) {
      const editBtn = e.target.closest(".fh-edit");
      const delBtn = e.target.closest(".fh-del");
      if (editBtn) {
        if (!localStorage.getItem("token")) {
          window.location.href = "/login?redirect=" + encodeURIComponent("/family-head");
          return;
        }
        const id = editBtn.getAttribute("data-id");
        document.getElementById("newsModalTitle").textContent = "Sửa tin / sự kiện";
        resetForm();
        try {
          const detail = await fetchNewsDetail(id);
          if (!detail) {
            showAlert("Không tìm thấy bài viết.", "error");
            return;
          }
          fillFormFromDetail(detail);
          openModal(modal);
        } catch (err) {
          showAlert(err.message || "Lỗi tải chi tiết.", "error");
        }
        return;
      }
      if (delBtn) {
        if (!localStorage.getItem("token")) {
          window.location.href = "/login?redirect=" + encodeURIComponent("/family-head");
          return;
        }
        const id = delBtn.getAttribute("data-id");
        if (!confirm("Xóa bài viết này?")) return;
        try {
          const res = await fetch("/api/family-head/news/" + encodeURIComponent(id), {
            method: "DELETE",
            headers: authHeaders(),
          });
          if (res.status === 403) {
            showAlert("Không có quyền xóa tin.", "error");
            return;
          }
          if (!res.ok) {
            const t = await res.text();
            throw new Error(t || "Xóa thất bại.");
          }
          showAlert("Đã xóa.", "success");
          refreshList();
        } catch (err) {
          showAlert(err.message || "Lỗi xóa.", "error");
        }
      }
    });

    document.getElementById("newsForm").addEventListener("submit", async function (ev) {
      ev.preventDefault();
      const tokenNow = localStorage.getItem("token");
      const userId = localStorage.getItem("user_id");
      if (!tokenNow || !userId) {
        window.location.href = "/login?redirect=" + encodeURIComponent("/family-head");
        return;
      }
      const familyId = familySelect.value;
      if (!familyId) {
        showAlert("Chọn dòng họ.", "error");
        return;
      }
      const vis = document.getElementById("newsVisibility").value;
      if (vis === "PUBLIC_SITE" && !document.getElementById("newsPublicCategory").value) {
        showAlert("Tin công khai cần chọn tab (Thông báo / Dòng họ / …).", "error");
        return;
      }
      const editId = document.getElementById("newsEditId").value.trim();
      const payload = collectPayload(familyId, userId);

      try {
        let res;
        if (editId) {
          res = await fetch("/api/family-head/news/" + encodeURIComponent(editId), {
            method: "PUT",
            headers: authHeaders(),
            body: JSON.stringify(payload),
          });
        } else {
          res = await fetch("/api/family-head/news", {
            method: "POST",
            headers: authHeaders(),
            body: JSON.stringify(payload),
          });
        }
        if (res.status === 403) {
          showAlert("Không có quyền lưu tin.", "error");
          return;
        }
        if (!res.ok) {
          const t = await res.text();
          throw new Error(t || "Lưu thất bại.");
        }
        closeModal(modal);
        showAlert(editId ? "Đã cập nhật." : "Đã tạo bài mới.", "success");
        refreshList();
      } catch (err) {
        showAlert(err.message || "Lỗi lưu.", "error");
      }
    });

    document.addEventListener("fh:panel", function (ev) {
      if (ev.detail && ev.detail.panel === "news") refreshList();
    });

    await refreshList();
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();

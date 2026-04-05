/**
 * Quản lý tin công khai toàn site — GET/POST/PUT/DELETE /api/admin/site-news
 * Yêu cầu JWT + quyền MANAGE_SITE_NEWS.
 */
(function () {
  const TAB_LABELS = {
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

  function hasSiteNewsPermission() {
    if (localStorage.getItem("role") === "ADMIN") return true;
    try {
      const raw = localStorage.getItem("permissions");
      if (!raw) return false;
      const list = JSON.parse(raw);
      return Array.isArray(list) && list.includes("MANAGE_SITE_NEWS");
    } catch (e) {
      return false;
    }
  }

  function showAlert(elId, message, kind) {
    const el = document.getElementById(elId);
    if (!el) return;
    el.textContent = message;
    el.className = "fh-alert " + (kind || "info");
    el.style.display = "block";
  }

  function hideAlert() {
    const el = document.getElementById("snAlert");
    if (el) el.style.display = "none";
  }

  function escapeHtml(s) {
    if (s == null) return "";
    const div = document.createElement("div");
    div.textContent = s;
    return div.innerHTML;
  }

  function formatDate(v) {
    if (v == null) return "—";
    const d = new Date(v);
    return Number.isNaN(d.getTime()) ? "—" : d.toLocaleString("vi-VN");
  }

  function openModal(overlay) {
    overlay.classList.add("open");
    overlay.setAttribute("aria-hidden", "false");
  }

  function closeModal(overlay) {
    overlay.classList.remove("open");
    overlay.setAttribute("aria-hidden", "true");
  }

  async function refreshList() {
    const tbody = document.getElementById("snBody");
    const empty = document.getElementById("snEmpty");
    const search = document.getElementById("snSearch").value.trim();
    const category = document.getElementById("snCategory").value;
    const status = document.getElementById("snStatus").value;

    const params = new URLSearchParams();
    if (search) params.set("search", search);
    if (category) params.set("category", category);
    if (status) params.set("status", status);

    tbody.innerHTML = '<tr><td colspan="6" class="fh-muted">Đang tải...</td></tr>';
    if (empty) empty.style.display = "none";

    const res = await fetch("/api/admin/site-news?" + params.toString(), { headers: authHeaders() });
    if (res.status === 401 || res.status === 403) {
      showAlert("snAlert", "Không có quyền hoặc phiên đăng nhập hết hạn.", "error");
      tbody.innerHTML = '<tr><td colspan="6" class="fh-muted">Không có quyền.</td></tr>';
      return;
    }
    if (!res.ok) {
      const t = await res.text();
      showAlert("snAlert", t || "Lỗi tải danh sách.", "error");
      tbody.innerHTML = '<tr><td colspan="6" class="fh-muted">Lỗi.</td></tr>';
      return;
    }
    hideAlert();
    const rows = await res.json();
    if (!rows.length) {
      tbody.innerHTML = "";
      if (empty) empty.style.display = "block";
      return;
    }
    if (empty) empty.style.display = "none";
    tbody.innerHTML = rows
      .map((row) => {
        const cat = row.publicCategory || "";
        const vis = row.visibility || "";
        const visLabel = vis === "PUBLIC_SITE" ? "Đã đăng" : vis === "DRAFT" ? "Nháp" : vis;
        const feat = row.featured ? "Có" : "Không";
        return `
        <tr data-id="${escapeHtml(row.id)}">
          <td class="fh-title">${escapeHtml(row.title || "")}<div class="fh-muted">${escapeHtml((row.summary || "").slice(0, 72))}${(row.summary || "").length > 72 ? "…" : ""}</div></td>
          <td>${escapeHtml(TAB_LABELS[cat] || cat)}</td>
          <td>${escapeHtml(visLabel)}</td>
          <td>${feat}</td>
          <td>${formatDate(row.createdAt)}</td>
          <td class="fh-actions">
            <button type="button" class="btn btn-white btn-sm sn-toggle" data-id="${escapeHtml(row.id)}" title="Đăng / gỡ"><i class="fas fa-toggle-on"></i></button>
            <button type="button" class="btn btn-white btn-sm sn-edit" data-id="${escapeHtml(row.id)}"><i class="fas fa-pen"></i></button>
            <button type="button" class="btn btn-white btn-sm sn-del" data-id="${escapeHtml(row.id)}"><i class="fas fa-trash-alt"></i></button>
          </td>
        </tr>`;
      })
      .join("");
  }

  async function init() {
    if (!window.location.pathname.startsWith("/site-news-manage")) return;

    const token = localStorage.getItem("token");
    if (!token) {
      window.location.href = "/login?redirect=" + encodeURIComponent("/site-news-manage");
      return;
    }
    if (!hasSiteNewsPermission()) {
      try {
        const mr = await fetch("/api/auth/me", { headers: authHeaders() });
        if (mr.ok) {
          const me = await mr.json();
          if (me.permissions) {
            localStorage.setItem("permissions", JSON.stringify(me.permissions));
            if (me.role) localStorage.setItem("role", me.role);
            if (me.roles) localStorage.setItem("roles", JSON.stringify(me.roles));
          }
        }
      } catch (ignore) {}
    }
    if (!hasSiteNewsPermission()) {
      showAlert("snAlert", "Cần đăng nhập bằng tài khoản có quyền MANAGE_SITE_NEWS (ví dụ truongho@giapha.vn).", "error");
      setTimeout(() => {
        window.location.href = "/login?redirect=" + encodeURIComponent("/site-news-manage");
      }, 2500);
      return;
    }

    const modal = document.getElementById("snModal");

    document.getElementById("snReload").addEventListener("click", refreshList);
    let tmr;
    document.getElementById("snSearch").addEventListener("input", function () {
      clearTimeout(tmr);
      tmr = setTimeout(refreshList, 350);
    });
    document.getElementById("snCategory").addEventListener("change", refreshList);
    document.getElementById("snStatus").addEventListener("change", refreshList);

    document.getElementById("snAdd").addEventListener("click", function () {
      document.getElementById("snModalTitle").textContent = "Thêm bài công khai";
      document.getElementById("snEditId").value = "";
      document.getElementById("snTitle").value = "";
      document.getElementById("snSummary").value = "";
      document.getElementById("snContent").value = "";
      document.getElementById("snPubCat").value = "GENERAL";
      document.getElementById("snFeatured").value = "false";
      document.getElementById("snDraft").checked = false;
      document.getElementById("snDraftRow").style.display = "";
      openModal(modal);
    });

    document.getElementById("snModalClose").addEventListener("click", () => closeModal(modal));
    document.getElementById("snCancel").addEventListener("click", () => closeModal(modal));
    modal.addEventListener("click", (e) => {
      if (e.target === modal) closeModal(modal);
    });

    document.getElementById("snBody").addEventListener("click", async function (e) {
      const editBtn = e.target.closest(".sn-edit");
      const delBtn = e.target.closest(".sn-del");
      const tgBtn = e.target.closest(".sn-toggle");
      if (tgBtn) {
        const id = tgBtn.getAttribute("data-id");
        const res = await fetch("/api/admin/site-news/" + encodeURIComponent(id) + "/toggle-publish", {
          method: "POST",
          headers: authHeaders(),
        });
        if (!res.ok) {
          showAlert("snAlert", "Không đổi được trạng thái.", "error");
          return;
        }
        await refreshList();
        return;
      }
      if (delBtn) {
        const id = delBtn.getAttribute("data-id");
        if (!confirm("Xóa bài này?")) return;
        const res = await fetch("/api/admin/site-news/" + encodeURIComponent(id), {
          method: "DELETE",
          headers: authHeaders(),
        });
        if (res.status === 403) {
          showAlert("snAlert", "Không có quyền xóa.", "error");
          return;
        }
        if (!res.ok) {
          showAlert("snAlert", "Xóa thất bại.", "error");
          return;
        }
        await refreshList();
        return;
      }
      if (editBtn) {
        const id = editBtn.getAttribute("data-id");
        const res = await fetch("/api/admin/site-news/" + encodeURIComponent(id), { headers: authHeaders() });
        if (!res.ok) return;
        const row = await res.json();
        document.getElementById("snModalTitle").textContent = "Sửa bài";
        document.getElementById("snEditId").value = row.id || "";
        document.getElementById("snTitle").value = row.title || "";
        document.getElementById("snSummary").value = row.summary || "";
        document.getElementById("snContent").value = row.content || "";
        if (row.publicCategory) document.getElementById("snPubCat").value = row.publicCategory;
        document.getElementById("snFeatured").value = row.featured ? "true" : "false";
        document.getElementById("snDraft").checked = row.visibility === "DRAFT";
        document.getElementById("snDraftRow").style.display = "";
        openModal(modal);
      }
    });

    document.getElementById("snForm").addEventListener("submit", async function (ev) {
      ev.preventDefault();
      const editId = document.getElementById("snEditId").value.trim();
      const body = {
        title: document.getElementById("snTitle").value.trim(),
        publicCategory: document.getElementById("snPubCat").value,
        summary: document.getElementById("snSummary").value,
        content: document.getElementById("snContent").value,
        featured: document.getElementById("snFeatured").value === "true",
        draft: document.getElementById("snDraft").checked,
      };
      let res;
      if (editId) {
        body.visibility = body.draft ? "DRAFT" : "PUBLIC_SITE";
        delete body.draft;
        res = await fetch("/api/admin/site-news/" + encodeURIComponent(editId), {
          method: "PUT",
          headers: authHeaders(),
          body: JSON.stringify(body),
        });
      } else {
        res = await fetch("/api/admin/site-news", {
          method: "POST",
          headers: authHeaders(),
          body: JSON.stringify(body),
        });
      }
      if (res.status === 403) {
        showAlert("snAlert", "Không có quyền lưu.", "error");
        return;
      }
      if (!res.ok) {
        const t = await res.text();
        showAlert("snAlert", t || "Lưu thất bại.", "error");
        return;
      }
      closeModal(modal);
      await refreshList();
    });

    await refreshList();
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();

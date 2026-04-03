/**
 * CRUD dòng họ — GET/POST/PUT/DELETE /api/family-head/families
 */
(function () {
  function authHeaders() {
    const token = localStorage.getItem("token");
    const h = { "Content-Type": "application/json" };
    if (token) h["Authorization"] = "Bearer " + token;
    return h;
  }

  function showAlert(message, kind) {
    const el = document.getElementById("fhFamAlert");
    if (!el) return;
    el.textContent = message;
    el.className = "fh-alert " + (kind || "info");
    el.style.display = "block";
  }

  function escapeHtml(s) {
    if (s == null) return "";
    const div = document.createElement("div");
    div.textContent = s;
    return div.innerHTML;
  }

  function openModal(el) {
    if (!el) return;
    el.classList.add("open");
    el.setAttribute("aria-hidden", "false");
  }

  function closeModal(el) {
    if (!el) return;
    el.classList.remove("open");
    el.setAttribute("aria-hidden", "true");
  }

  let familiesCache = [];

  async function loadFamilies() {
    const token = localStorage.getItem("token");
    if (!token) {
      showAlert("Đăng nhập để quản lý dòng họ.", "info");
      return [];
    }
    const res = await fetch("/api/family-head/families", { headers: authHeaders() });
    if (res.status === 403) {
      showAlert("Không có quyền truy cập.", "error");
      return [];
    }
    if (!res.ok) throw new Error("Không tải được danh sách dòng họ.");
    familiesCache = await res.json();
    return familiesCache;
  }

  function fillParentSelect(selectEl, excludeId) {
    const opts =
      '<option value="">— Không —</option>' +
      familiesCache
        .filter(function (f) {
          return !excludeId || f.familyId !== excludeId;
        })
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
    selectEl.innerHTML = opts;
  }

  function fillFilterSelect(selectEl) {
    selectEl.innerHTML =
      '<option value="">Mọi dòng họ</option>' +
      familiesCache
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
  }

  function renderTable(tbody, rows, parentFilter) {
    let list = rows || [];
    if (parentFilter) {
      list = list.filter(function (f) {
        return (f.parentFamilyId || "") === parentFilter;
      });
    }
    if (!list.length) {
      tbody.innerHTML = '<tr><td colspan="4" class="fh-muted">Chưa có dòng họ hoặc bộ lọc trống.</td></tr>';
      return;
    }
    tbody.innerHTML = list
      .map(function (f) {
        return (
          '<tr data-id="' +
          escapeHtml(f.familyId) +
          '">' +
          "<td><strong>" +
          escapeHtml(f.familyName) +
          "</strong><div class=\"fh-muted\">" +
          escapeHtml((f.description || "").slice(0, 80)) +
          "</div></td>" +
          "<td>" +
          escapeHtml(f.privacySetting || "—") +
          "</td>" +
          "<td>" +
          escapeHtml(f.parentFamilyName || "—") +
          "</td>" +
          '<td class="fh-actions">' +
          '<button type="button" class="btn btn-white btn-sm fh-fam-edit" data-id="' +
          escapeHtml(f.familyId) +
          '"><i class="fas fa-pen"></i></button> ' +
          '<button type="button" class="btn btn-white btn-sm fh-fam-del" data-id="' +
          escapeHtml(f.familyId) +
          '"><i class="fas fa-trash-alt"></i></button>' +
          "</td>" +
          "</tr>"
        );
      })
      .join("");
  }

  async function refresh() {
    const tbody = document.getElementById("fhFamiliesBody");
    const filter = document.getElementById("fhFamParentFilter");
    if (!tbody) return;
    tbody.innerHTML = '<tr><td colspan="4" class="fh-muted">Đang tải...</td></tr>';
    try {
      await loadFamilies();
      if (filter) fillFilterSelect(filter);
      renderTable(tbody, familiesCache, filter && filter.value ? filter.value : "");
    } catch (e) {
      showAlert(e.message || "Lỗi tải.", "error");
      tbody.innerHTML = '<tr><td colspan="4" class="fh-muted">Lỗi.</td></tr>';
    }
  }

  function resetForm() {
    document.getElementById("familyEditId").value = "";
    document.getElementById("famFamilyName").value = "";
    document.getElementById("famDescription").value = "";
    document.getElementById("famPrivacy").value = "PUBLIC";
    document.getElementById("famParentId").value = "";
  }

  function init() {
    if (!window.location.pathname.startsWith("/family-head")) return;

    const tbody = document.getElementById("fhFamiliesBody");
    const modal = document.getElementById("familyFormModal");
    const filter = document.getElementById("fhFamParentFilter");

    document.addEventListener("fh:panel", function (ev) {
      if (ev.detail && ev.detail.panel === "families") refresh();
    });

    if (filter) {
      filter.addEventListener("change", function () {
        renderTable(tbody, familiesCache, filter.value || "");
      });
    }

    const btnFamAdd = document.getElementById("fhFamBtnAdd");
    if (btnFamAdd) {
      btnFamAdd.addEventListener("click", function () {
        if (!localStorage.getItem("token")) {
          window.location.href = "/login?redirect=" + encodeURIComponent("/family-head?tab=families");
          return;
        }
        document.getElementById("familyModalTitle").textContent = "Thêm dòng họ";
        resetForm();
        fillParentSelect(document.getElementById("famParentId"), null);
        openModal(modal);
      });
    }

    document.getElementById("familyModalClose").addEventListener("click", function () {
      closeModal(modal);
    });
    document.getElementById("familyModalCancel").addEventListener("click", function () {
      closeModal(modal);
    });
    modal.addEventListener("click", function (e) {
      if (e.target === modal) closeModal(modal);
    });

    tbody.addEventListener("click", async function (e) {
      const editBtn = e.target.closest(".fh-fam-edit");
      const delBtn = e.target.closest(".fh-fam-del");
      if (editBtn) {
        if (!localStorage.getItem("token")) {
          window.location.href = "/login?redirect=" + encodeURIComponent("/family-head?tab=families");
          return;
        }
        const id = editBtn.getAttribute("data-id");
        const f = familiesCache.find(function (x) {
          return x.familyId === id;
        });
        if (!f) return;
        document.getElementById("familyModalTitle").textContent = "Sửa dòng họ";
        document.getElementById("familyEditId").value = f.familyId;
        document.getElementById("famFamilyName").value = f.familyName || "";
        document.getElementById("famDescription").value = f.description || "";
        document.getElementById("famPrivacy").value = f.privacySetting || "PUBLIC";
        fillParentSelect(document.getElementById("famParentId"), f.familyId);
        document.getElementById("famParentId").value = f.parentFamilyId || "";
        openModal(modal);
        return;
      }
      if (delBtn) {
        if (!localStorage.getItem("token")) {
          window.location.href = "/login?redirect=" + encodeURIComponent("/family-head?tab=families");
          return;
        }
        const id = delBtn.getAttribute("data-id");
        if (!confirm("Xóa dòng họ này?")) return;
        try {
          const res = await fetch("/api/family-head/families/" + encodeURIComponent(id), {
            method: "DELETE",
            headers: authHeaders(),
          });
          if (!res.ok) {
            const t = await res.text();
            throw new Error(t || "Xóa thất bại.");
          }
          showAlert("Đã xóa.", "success");
          refresh();
        } catch (err) {
          showAlert(err.message || "Lỗi xóa.", "error");
        }
      }
    });

    document.getElementById("familyForm").addEventListener("submit", async function (ev) {
      ev.preventDefault();
      const editId = document.getElementById("familyEditId").value.trim();
      const body = {
        familyName: document.getElementById("famFamilyName").value.trim(),
        description: document.getElementById("famDescription").value.trim() || null,
        privacySetting: document.getElementById("famPrivacy").value,
        parentFamilyId: document.getElementById("famParentId").value || null,
      };
      try {
        let res;
        if (editId) {
          res = await fetch("/api/family-head/families/" + encodeURIComponent(editId), {
            method: "PUT",
            headers: authHeaders(),
            body: JSON.stringify(body),
          });
        } else {
          res = await fetch("/api/family-head/families", {
            method: "POST",
            headers: authHeaders(),
            body: JSON.stringify(body),
          });
        }
        if (!res.ok) {
          const t = await res.text();
          throw new Error(t || "Lưu thất bại.");
        }
        closeModal(modal);
        showAlert(editId ? "Đã cập nhật." : "Đã tạo dòng họ.", "success");
        refresh();
      } catch (err) {
        showAlert(err.message || "Lỗi lưu.", "error");
      }
    });
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();

/**
 * CRUD thành viên — /api/family-head/members
 */
(function () {
  function authHeaders() {
    const token = localStorage.getItem("token");
    const h = { "Content-Type": "application/json" };
    if (token) h["Authorization"] = "Bearer " + token;
    return h;
  }

  function showAlert(message, kind) {
    const el = document.getElementById("fhMemAlert");
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

  /** Chỉ dòng họ của tài khoản (API đã khóa phạm vi). */
  async function loadManagedFamilies(selectEl, emptyLabel) {
    const res = await fetch("/api/family-head/families", { headers: authHeaders() });
    if (!res.ok) {
      const t = await res.text();
      throw new Error(t || "Không tải được dòng họ.");
    }
    const families = await res.json();
    const head = emptyLabel
      ? '<option value="">' + escapeHtml(emptyLabel) + "</option>"
      : "";
    selectEl.innerHTML =
      head +
      families
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
    return families;
  }

  function resetForm() {
    document.getElementById("memberEditId").value = "";
    document.getElementById("memFullName").value = "";
    document.getElementById("memEmail").value = "";
    document.getElementById("memGender").value = "";
    document.getElementById("memDob").value = "";
    document.getElementById("memPhone").value = "";
    document.getElementById("memParentId").value = "";
    document.getElementById("memGeneration").value = "";
    document.getElementById("memOrder").value = "";
  }

  async function refreshList() {
    const tbody = document.getElementById("fhMembersBody");
    const familySelect = document.getElementById("fhMemFamilyFilter");
    const modalFamily = document.getElementById("memFamilyId");
    if (!tbody || !familySelect) return;

    if (!localStorage.getItem("token")) {
      tbody.innerHTML =
        '<tr><td colspan="5" class="fh-muted">Đăng nhập để xem thành viên.</td></tr>';
      return;
    }

    const fid = familySelect.value;
    if (!fid) {
      tbody.innerHTML = '<tr><td colspan="5" class="fh-muted">Chọn dòng họ.</td></tr>';
      return;
    }

    tbody.innerHTML = '<tr><td colspan="5" class="fh-muted">Đang tải...</td></tr>';
    try {
      const res = await fetch(
        "/api/family-head/members?familyId=" + encodeURIComponent(fid),
        { headers: authHeaders() }
      );
      if (!res.ok) {
        const t = await res.text();
        throw new Error(t || "Không tải được.");
      }
      const list = await res.json();
      if (!list.length) {
        tbody.innerHTML = '<tr><td colspan="5" class="fh-muted">Chưa có thành viên.</td></tr>';
        return;
      }
      tbody.innerHTML = list
        .map(function (u) {
          return (
            '<tr data-id="' +
            escapeHtml(u.userId) +
            '">' +
            "<td>" +
            escapeHtml(u.fullName || "") +
            "</td>" +
            "<td>" +
            escapeHtml(u.email || "—") +
            "</td>" +
            "<td>" +
            escapeHtml(u.familyName || "—") +
            "</td>" +
            "<td>" +
            (u.generation != null ? escapeHtml(String(u.generation)) : "—") +
            "</td>" +
            '<td class="fh-actions">' +
            '<button type="button" class="btn btn-white btn-sm fh-mem-edit" data-id="' +
            escapeHtml(u.userId) +
            '"><i class="fas fa-pen"></i></button> ' +
            '<button type="button" class="btn btn-white btn-sm fh-mem-del" data-id="' +
            escapeHtml(u.userId) +
            '"><i class="fas fa-trash-alt"></i></button>' +
            "</td>" +
            "</tr>"
          );
        })
        .join("");
    } catch (e) {
      showAlert(e.message || "Lỗi.", "error");
      tbody.innerHTML = '<tr><td colspan="5" class="fh-muted">Lỗi.</td></tr>';
    }

    if (modalFamily && modalFamily.options.length === 0) {
      await loadManagedFamilies(modalFamily, "");
    }
  }

  function init() {
    if (!window.location.pathname.startsWith("/family-head")) return;

    const tbody = document.getElementById("fhMembersBody");
    const familySelect = document.getElementById("fhMemFamilyFilter");
    const modal = document.getElementById("memberFormModal");
    const modalFamily = document.getElementById("memFamilyId");

    document.addEventListener("fh:panel", function (ev) {
      if (ev.detail && ev.detail.panel === "members") {
        (async function () {
          try {
            await loadManagedFamilies(familySelect, "Chọn dòng họ");
            const q = new URLSearchParams(window.location.search);
            const saved = localStorage.getItem("selectedFamilyId");
            const myFam = localStorage.getItem("my_family_id");
            const pre = q.get("familyId") || saved || myFam || "";
            if (pre && familySelect.querySelector('option[value="' + pre + '"]')) {
              familySelect.value = pre;
            } else {
              const firstVal = Array.prototype.slice
                .call(familySelect.options)
                .map(function (o) {
                  return o.value;
                })
                .find(function (v) {
                  return v;
                });
              if (firstVal) familySelect.value = firstVal;
            }
            await refreshList();
            await loadManagedFamilies(modalFamily, "");
          } catch (e) {
            showAlert(e.message || "Lỗi tải.", "error");
          }
        })();
      }
    });

    if (familySelect) {
      familySelect.addEventListener("change", function () {
        const v = familySelect.value;
        if (v) localStorage.setItem("selectedFamilyId", v);
        refreshList();
      });
    }

    document.getElementById("fhMemBtnAdd").addEventListener("click", async function () {
      if (!localStorage.getItem("token")) {
        window.location.href = "/login?redirect=" + encodeURIComponent("/family-head?tab=members");
        return;
      }
      const fid = familySelect.value;
      if (!fid) {
        showAlert("Chọn dòng họ ở ô lọc trước.", "error");
        return;
      }
      document.getElementById("memberModalTitle").textContent = "Thêm thành viên";
      resetForm();
      await loadManagedFamilies(modalFamily, "");
      modalFamily.value = fid;
      openModal(modal);
    });

    document.getElementById("memberModalClose").addEventListener("click", function () {
      closeModal(modal);
    });
    document.getElementById("memberModalCancel").addEventListener("click", function () {
      closeModal(modal);
    });
    modal.addEventListener("click", function (e) {
      if (e.target === modal) closeModal(modal);
    });

    tbody.addEventListener("click", async function (e) {
      const editBtn = e.target.closest(".fh-mem-edit");
      const delBtn = e.target.closest(".fh-mem-del");
      if (editBtn) {
        if (!localStorage.getItem("token")) {
          window.location.href = "/login?redirect=" + encodeURIComponent("/family-head?tab=members");
          return;
        }
        const id = editBtn.getAttribute("data-id");
        try {
          const res = await fetch("/api/family-head/members/" + encodeURIComponent(id), {
            headers: authHeaders(),
          });
          if (!res.ok) throw new Error("Không tải được hồ sơ.");
          const u = await res.json();
          document.getElementById("memberModalTitle").textContent = "Sửa thành viên";
          document.getElementById("memberEditId").value = u.userId || "";
          document.getElementById("memFullName").value = u.fullName || "";
          document.getElementById("memEmail").value = u.email || "";
          document.getElementById("memGender").value = u.gender || "";
          (function () {
            var d = u.dob;
            if (Array.isArray(d) && d.length >= 3) {
              document.getElementById("memDob").value =
                d[0] + "-" + String(d[1]).padStart(2, "0") + "-" + String(d[2]).padStart(2, "0");
            } else if (typeof d === "string") {
              document.getElementById("memDob").value = d.length >= 10 ? d.slice(0, 10) : "";
            } else {
              document.getElementById("memDob").value = "";
            }
          })();
          document.getElementById("memPhone").value = u.phoneNumber || "";
          document.getElementById("memParentId").value = u.parentId || "";
          document.getElementById("memGeneration").value =
            u.generation != null ? String(u.generation) : "";
          document.getElementById("memOrder").value =
            u.orderInFamily != null ? String(u.orderInFamily) : "";
          await loadManagedFamilies(modalFamily, "");
          if (u.familyId) modalFamily.value = u.familyId;
          openModal(modal);
        } catch (err) {
          showAlert(err.message || "Lỗi.", "error");
        }
        return;
      }
      if (delBtn) {
        if (!localStorage.getItem("token")) {
          window.location.href = "/login?redirect=" + encodeURIComponent("/family-head?tab=members");
          return;
        }
        const id = delBtn.getAttribute("data-id");
        if (!confirm("Xóa thành viên này?")) return;
        try {
          const res = await fetch("/api/family-head/members/" + encodeURIComponent(id), {
            method: "DELETE",
            headers: authHeaders(),
          });
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

    document.getElementById("memberForm").addEventListener("submit", async function (ev) {
      ev.preventDefault();
      const editId = document.getElementById("memberEditId").value.trim();
      const body = {
        fullName: document.getElementById("memFullName").value.trim(),
        email: document.getElementById("memEmail").value.trim() || null,
        familyId: document.getElementById("memFamilyId").value,
        gender: document.getElementById("memGender").value || null,
        dob: document.getElementById("memDob").value || null,
        phoneNumber: document.getElementById("memPhone").value.trim() || null,
        parentId: document.getElementById("memParentId").value.trim() || null,
        generation: document.getElementById("memGeneration").value
          ? parseInt(document.getElementById("memGeneration").value, 10)
          : null,
        orderInFamily: document.getElementById("memOrder").value
          ? parseInt(document.getElementById("memOrder").value, 10)
          : null,
      };
      if (!body.familyId) {
        showAlert("Chọn dòng họ.", "error");
        return;
      }
      try {
        let res;
        if (editId) {
          res = await fetch("/api/family-head/members/" + encodeURIComponent(editId), {
            method: "PUT",
            headers: authHeaders(),
            body: JSON.stringify(body),
          });
        } else {
          res = await fetch("/api/family-head/members", {
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
        showAlert(editId ? "Đã cập nhật." : "Đã thêm thành viên.", "success");
        refreshList();
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

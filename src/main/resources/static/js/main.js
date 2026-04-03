(function syncJwtCookieFromLocalStorage() {
  try {
    const t = localStorage.getItem("token");
    if (!t) return;
    if (document.cookie.split(";").some((c) => c.trim().startsWith("accessToken="))) return;
    document.cookie =
      "accessToken=" +
      encodeURIComponent(t) +
      "; path=/; max-age=" +
      86400 * 7 +
      "; SameSite=Lax";
  } catch (e) {
    /* ignore */
  }
})();

document.addEventListener('DOMContentLoaded', function() {
  // Logic chung cho tất cả dropdown trong navbar
  const dropdowns = document.querySelectorAll('.pub-nav-right .dropdown');

  dropdowns.forEach(dd => {
    const btn = dd.querySelector('.pub-nav-user');
    btn.addEventListener('click', function(e) {
      e.stopPropagation();
      // Đóng các dropdown khác nếu đang mở
      dropdowns.forEach(other => { if(other !== dd) other.classList.remove('open'); });
      // Toggle dropdown hiện tại
      dd.classList.toggle('open');
    });
  });

  // Click ra ngoài thì đóng hết
  document.addEventListener('click', function() {
    dropdowns.forEach(dd => dd.classList.remove('open'));
  });
});
/* =============================================
   Gia Pha System - Main JS (FE mock)
   ============================================= */

const pagePath = window.location.pathname;

/** Đọc thông báo lỗi từ response API (JSON Spring { message } / { detail } hoặc text thuần). */
async function readFetchErrorMessage(res, fallback) {
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

function canManageFamilyMembersGlobal() {
  try {
    const perms = JSON.parse(localStorage.getItem("permissions") || "[]");
    return (
      localStorage.getItem("role") === "FAMILY_HEAD" ||
      (Array.isArray(perms) && perms.indexOf("MANAGE_FAMILY_MEMBERS") >= 0)
    );
  } catch (e) {
    return false;
  }
}

function syncTreeCanvasFamilyId(familyId) {
  const el = document.getElementById("treeCanvas");
  if (el) el.dataset.familyId = familyId || "";
}

function syncTreeToolbarAddMember(familyId) {
  const btn = document.getElementById("treeMemberAddBtn");
  if (!btn) return;
  const token = localStorage.getItem("token");
  btn.hidden = !(canManageFamilyMembersGlobal() && token && familyId);
}

function updateTreePanelManageVisibility() {
  const manage = document.getElementById("treeDetailManageActions");
  const layout = document.getElementById("treeLayout");
  if (!layout) return;
  const sid = layout.dataset.selectedMemberId || "";
  const spouseSid = layout.dataset.selectedMemberSpouseId || "";
  const token = localStorage.getItem("token");
  const can = canManageFamilyMembersGlobal() && token;
  if (manage) manage.hidden = !(can && sid);

  const addChildBtn = document.getElementById("treeMemberAddChildBtn");
  const linkSpouseBtn = document.getElementById("treeMemberLinkSpouseBtn");
  const addChildHint = document.getElementById("treeAddChildHint");
  const hasSpouse = !!spouseSid;
  if (addChildBtn) {
    addChildBtn.disabled = !!(can && sid && !hasSpouse);
    addChildBtn.title =
      can && sid && !hasSpouse
        ? 'Cần dùng "Thêm vợ/chồng" tạo hồ sơ vợ/chồng trước khi thêm con.'
        : "";
  }
  if (linkSpouseBtn) {
    linkSpouseBtn.hidden = !sid || hasSpouse;
    linkSpouseBtn.disabled = false;
  }
  if (addChildHint) {
    addChildHint.hidden = !(can && sid && !hasSpouse);
  }
  const deleteBtn = document.getElementById("treeMemberDeleteBtn");
  const selGen = (layout.dataset.selectedMemberGender || "").trim();
  if (deleteBtn) {
    const blockHusbandDelete = !!(can && sid && hasSpouse && selGen !== "FEMALE");
    deleteBtn.disabled = blockHusbandDelete;
    deleteBtn.title = blockHusbandDelete
      ? "Không xóa được chồng khi còn vợ. Chỉ xóa vợ (nữ) để gỡ liên kết; hoặc xóa vợ trước."
      : "";
  }
}

function ensureTreeMemberModalBindings() {
  if (window.__treeMemberBindingsDone) return;
  window.__treeMemberBindingsDone = true;

  function treeAuthHeaders() {
    const token = localStorage.getItem("token");
    const h = { "Content-Type": "application/json" };
    if (token) h["Authorization"] = "Bearer " + token;
    return h;
  }

  let tmMembersCache = [];
  let tmDefaultMemberRoleId = "";

  function tmEscAttr(s) {
    return String(s || "").replace(/&/g, "&amp;").replace(/"/g, "&quot;").replace(/</g, "&lt;");
  }

  function tmEscText(s) {
    return String(s || "").replace(/&/g, "&amp;").replace(/</g, "&lt;");
  }

  async function ensureTreeMemberRolesLoaded() {
    const sel = document.getElementById("tmMemRoleId");
    if (!sel) return;
    if (sel.dataset.loaded === "1") return;
    const res = await fetch("/api/family-head/member-roles", { headers: treeAuthHeaders() });
    if (res.status === 403 || !res.ok) {
      sel.innerHTML = '<option value="">— Không tải được danh sách vai trò —</option>';
      sel.dataset.loaded = "1";
      tmDefaultMemberRoleId = "";
      return;
    }
    const roles = await res.json();
    sel.innerHTML = (roles || [])
      .map(function (r) {
        return (
          '<option value="' +
          tmEscAttr(r.roleId) +
          '">' +
          tmEscText(r.label || r.roleName || "") +
          "</option>"
        );
      })
      .join("");
    const mem = (roles || []).find(function (x) {
      return x.roleName === "MEMBER";
    });
    tmDefaultMemberRoleId = mem ? mem.roleId : (roles && roles[0] && roles[0].roleId) || "";
    sel.dataset.loaded = "1";
  }

  async function loadTreeManagedFamilies(selectEl) {
    const res = await fetch("/api/family-head/families", { headers: treeAuthHeaders() });
    if (!res.ok) throw new Error("Không tải được danh sách dòng họ.");
    const families = await res.json();
    selectEl.innerHTML = families
      .map(
        (f) =>
          `<option value="${String(f.familyId || "").replace(/"/g, "&quot;")}">${(f.familyName || "").replace(/</g, "&lt;")}</option>`
      )
      .join("");
    return families;
  }

  function normParentId(p) {
    const s = (p == null ? "" : String(p)).trim();
    return s || null;
  }

  function syncGenerationOrderFromParent() {
    const famSel = document.getElementById("tmMemFamilyId");
    const parSel = document.getElementById("tmMemParentId");
    const genEl = document.getElementById("tmMemGeneration");
    const ordEl = document.getElementById("tmMemOrder");
    const editId = document.getElementById("tmMemberEditId").value.trim();
    if (!famSel || !parSel || !genEl || !ordEl) return;
    const familyId = famSel.value;
    const parentId = normParentId(parSel.value);
    const self = editId ? tmMembersCache.find((m) => m.userId === editId) : null;
    const sameParent = !!(self && normParentId(self.parentId) === parentId);
    if (!familyId) {
      genEl.value = "";
      ordEl.value = "";
      return;
    }
    if (!parentId) {
      genEl.value = "1";
      if (sameParent && self.orderInFamily != null) {
        ordEl.value = String(self.orderInFamily);
      } else {
        const roots = tmMembersCache.filter((m) => !normParentId(m.parentId) && m.userId !== editId);
        const maxO = roots.reduce((a, m) => Math.max(a, m.orderInFamily != null ? m.orderInFamily : 0), 0);
        ordEl.value = String(maxO + 1);
      }
      return;
    }
    const parent = tmMembersCache.find((m) => m.userId === parentId);
    const pgen = parent && parent.generation != null ? parent.generation : 1;
    genEl.value = String(pgen + 1);
    if (sameParent && self.orderInFamily != null) {
      ordEl.value = String(self.orderInFamily);
    } else {
      const sibs = tmMembersCache.filter((m) => normParentId(m.parentId) === parentId && m.userId !== editId);
      const maxO = sibs.reduce((a, m) => Math.max(a, m.orderInFamily != null ? m.orderInFamily : 0), 0);
      ordEl.value = String(maxO + 1);
    }
  }

  async function refreshMembersAndParentSelect(familyId, excludeUserId, preferredParentId) {
    const parSel = document.getElementById("tmMemParentId");
    if (!parSel) return;
    parSel.innerHTML = "";
    const o0 = document.createElement("option");
    o0.value = "";
    o0.textContent = "— Không (đời đầu) —";
    parSel.appendChild(o0);
    if (!familyId) {
      tmMembersCache = [];
      syncGenerationOrderFromParent();
      return;
    }
    const res = await fetch(
      "/api/family-head/members?familyId=" + encodeURIComponent(familyId),
      { headers: treeAuthHeaders() }
    );
    if (!res.ok) throw new Error("Không tải danh sách thành viên.");
    tmMembersCache = await res.json();
    tmMembersCache.forEach((m) => {
      if (!m.userId || m.userId === excludeUserId) return;
      const o = document.createElement("option");
      o.value = m.userId;
      const g = m.generation != null ? m.generation : "?";
      o.textContent = (m.fullName || m.userId) + " (thế hệ " + g + ")";
      parSel.appendChild(o);
    });
    if (preferredParentId && tmMembersCache.some((x) => x.userId === preferredParentId)) {
      parSel.value = preferredParentId;
    } else {
      parSel.value = "";
    }
    syncGenerationOrderFromParent();
  }

  function resetTreeMemberForm() {
    document.getElementById("tmMemberEditId").value = "";
    document.getElementById("tmMemFullName").value = "";
    document.getElementById("tmMemEmail").value = "";
    document.getElementById("tmMemGender").value = "";
    document.getElementById("tmMemDob").value = "";
    document.getElementById("tmMemPhone").value = "";
    const parSel = document.getElementById("tmMemParentId");
    if (parSel) {
      parSel.innerHTML = "";
      const o0 = document.createElement("option");
      o0.value = "";
      o0.textContent = "— Không (đời đầu) —";
      parSel.appendChild(o0);
    }
    tmMembersCache = [];
    document.getElementById("tmMemGeneration").value = "";
    document.getElementById("tmMemOrder").value = "";
    document.getElementById("tmMemHometown").value = "";
    document.getElementById("tmMemCurrentAddress").value = "";
    document.getElementById("tmMemOccupation").value = "";
    document.getElementById("tmMemBio").value = "";
    const rSel = document.getElementById("tmMemRoleId");
    if (rSel && tmDefaultMemberRoleId) rSel.value = tmDefaultMemberRoleId;
  }

  function openTreeMemberModal() {
    const m = document.getElementById("treeMemberEditModal");
    if (!m) return;
    m.classList.add("open");
    m.setAttribute("aria-hidden", "false");
  }

  function closeTreeMemberModal() {
    const m = document.getElementById("treeMemberEditModal");
    if (!m) return;
    m.classList.remove("open");
    m.setAttribute("aria-hidden", "true");
  }

  async function openAddMemberModal(parentUserId) {
    const canvas = document.getElementById("treeCanvas");
    const fid =
      (canvas?.dataset.familyId || canvas?.dataset.clanFamilyId || "").trim();
    if (!fid) {
      alert("Chưa xác định dòng họ. Tải lại trang gia phả hoặc kiểm tra cấu hình app.clan.family-id.");
      return;
    }
    if (!localStorage.getItem("token")) {
      window.location.href = "/login?redirect=" + encodeURIComponent(window.location.pathname + window.location.search);
      return;
    }
    const famSel = document.getElementById("tmMemFamilyId");
    if (!famSel) return;
    try {
      await loadTreeManagedFamilies(famSel);
      await ensureTreeMemberRolesLoaded();
      resetTreeMemberForm();
      famSel.value = fid;
      await refreshMembersAndParentSelect(fid, "", parentUserId || null);
      document.getElementById("treeMemberModalTitle").textContent = parentUserId
        ? "Thêm thành viên (con)"
        : "Thêm thành viên";
      openTreeMemberModal();
    } catch (err) {
      alert(err.message || "Lỗi.");
    }
  }

  document.getElementById("treeMemberAddBtn")?.addEventListener("click", () => openAddMemberModal(""));

  function openTreeSpouseModal() {
    const m = document.getElementById("treeSpouseLinkModal");
    if (!m) return;
    m.classList.add("open");
    m.setAttribute("aria-hidden", "false");
  }

  function closeTreeSpouseModal() {
    const m = document.getElementById("treeSpouseLinkModal");
    if (!m) return;
    m.classList.remove("open");
    m.setAttribute("aria-hidden", "true");
  }

  document.getElementById("treeMemberLinkSpouseBtn")?.addEventListener("click", async () => {
    const layout = document.getElementById("treeLayout");
    const memberId = layout?.dataset.selectedMemberId || "";
    if (!memberId) return;
    if (!localStorage.getItem("token")) {
      window.location.href = "/login?redirect=" + encodeURIComponent(window.location.pathname + window.location.search);
      return;
    }
    try {
      const res = await fetch("/api/family-head/members/" + encodeURIComponent(memberId), {
        headers: treeAuthHeaders(),
      });
      if (res.status === 403) {
        alert("Không có quyền.");
        return;
      }
      if (!res.ok) throw new Error("Không tải được hồ sơ thành viên.");
      const u = await res.json();
      const g = (u.gender || "").trim();
      if (g !== "MALE" && g !== "FEMALE") {
        alert(
          "Cần cập nhật giới tính Nam hoặc Nữ cho thành viên này trước (dùng Sửa hồ sơ). Chỉ hỗ trợ vợ chồng nam–nữ."
        );
        return;
      }
      const spouseG = g === "MALE" ? "FEMALE" : "MALE";
      const hid = document.getElementById("tsSpouseGenderHidden");
      const lab = document.getElementById("tsSpouseGenderLabel");
      const titleEl = document.getElementById("treeSpouseLinkModalTitle");
      const intro = document.getElementById("tsSpouseIntro");
      if (hid) hid.value = spouseG;
      if (lab) lab.value = spouseG === "FEMALE" ? "Nữ" : "Nam";
      if (titleEl) titleEl.textContent = g === "MALE" ? "Thêm vợ" : "Thêm chồng";
      if (intro) {
        intro.textContent =
          "Tạo hồ sơ mới cho " +
          (spouseG === "FEMALE" ? "vợ" : "chồng") +
          " của " +
          (u.fullName || "thành viên") +
          " — cùng thế hệ, tự gắn quan hệ vợ chồng (chỉ nam với nữ).";
      }
      ["tsSpouseFullName", "tsSpouseEmail", "tsSpousePhone", "tsSpouseDob", "tsSpouseHometown", "tsSpouseAddress", "tsSpouseOccupation", "tsSpouseBio"].forEach((id) => {
        const el = document.getElementById(id);
        if (el) el.value = "";
      });
      openTreeSpouseModal();
    } catch (err) {
      alert(err.message || "Lỗi.");
    }
  });

  document.getElementById("treeMemberAddChildBtn")?.addEventListener("click", () => {
    const layout = document.getElementById("treeLayout");
    const sid = layout?.dataset.selectedMemberId || "";
    const sp = layout?.dataset.selectedMemberSpouseId || "";
    if (!sid) {
      alert("Chọn một thành viên trên cây làm cha/mẹ trước.");
      return;
    }
    if (!sp) {
      alert('Cần dùng "Thêm vợ/chồng" để tạo hồ sơ vợ/chồng trước khi thêm con.');
      return;
    }
    openAddMemberModal(sid);
  });

  document.getElementById("treeSpouseLinkModalClose")?.addEventListener("click", closeTreeSpouseModal);
  document.getElementById("treeSpouseLinkCancel")?.addEventListener("click", closeTreeSpouseModal);
  document.getElementById("treeSpouseLinkModal")?.addEventListener("click", (e) => {
    if (e.target === document.getElementById("treeSpouseLinkModal")) closeTreeSpouseModal();
  });

  document.getElementById("treeSpouseLinkForm")?.addEventListener("submit", async (ev) => {
    ev.preventDefault();
    const layout = document.getElementById("treeLayout");
    const memberId = layout?.dataset.selectedMemberId || "";
    const gender = (document.getElementById("tsSpouseGenderHidden")?.value || "").trim();
    const fullName = document.getElementById("tsSpouseFullName")?.value.trim() || "";
    if (!memberId || !gender) return;
    if (!fullName) {
      alert("Nhập họ tên vợ/chồng.");
      return;
    }
    const body = {
      fullName,
      email: document.getElementById("tsSpouseEmail")?.value.trim() || null,
      gender,
      dob: document.getElementById("tsSpouseDob")?.value || null,
      phoneNumber: document.getElementById("tsSpousePhone")?.value.trim() || null,
      hometown: document.getElementById("tsSpouseHometown")?.value.trim() || null,
      currentAddress: document.getElementById("tsSpouseAddress")?.value.trim() || null,
      occupation: document.getElementById("tsSpouseOccupation")?.value.trim() || null,
      bio: document.getElementById("tsSpouseBio")?.value.trim() || null,
    };
    try {
      const res = await fetch(
        "/api/family-head/members/" + encodeURIComponent(memberId) + "/spouse",
        {
          method: "POST",
          headers: treeAuthHeaders(),
          body: JSON.stringify(body),
        }
      );
      if (!res.ok) {
        alert(await readFetchErrorMessage(res, "Không tạo được vợ/chồng."));
        return;
      }
      closeTreeSpouseModal();
      if (typeof window.reloadFamilyTree === "function") await window.reloadFamilyTree();
    } catch (err) {
      alert(err.message || "Lỗi.");
    }
  });

  document.getElementById("treeMemberEditBtn")?.addEventListener("click", async () => {
    const selectedMemberId = document.getElementById("treeLayout")?.dataset.selectedMemberId || "";
    if (!selectedMemberId) return;
    if (!localStorage.getItem("token")) {
      window.location.href = "/login?redirect=" + encodeURIComponent(window.location.pathname + window.location.search);
      return;
    }
    const famSel = document.getElementById("tmMemFamilyId");
    if (!famSel) return;
    try {
      const res = await fetch("/api/family-head/members/" + encodeURIComponent(selectedMemberId), {
        headers: treeAuthHeaders(),
      });
      if (res.status === 403) {
        alert("Không có quyền xem/sửa thành viên này.");
        return;
      }
      if (!res.ok) throw new Error("Không tải được hồ sơ.");
      const u = await res.json();
      await loadTreeManagedFamilies(famSel);
      document.getElementById("tmMemberEditId").value = u.userId || "";
      document.getElementById("tmMemFullName").value = u.fullName || "";
      document.getElementById("tmMemEmail").value = u.email || "";
      document.getElementById("tmMemGender").value = u.gender || "";
      (function () {
        const d = u.dob;
        const el = document.getElementById("tmMemDob");
        if (!el) return;
        if (Array.isArray(d) && d.length >= 3) {
          el.value = d[0] + "-" + String(d[1]).padStart(2, "0") + "-" + String(d[2]).padStart(2, "0");
        } else if (typeof d === "string") {
          el.value = d.length >= 10 ? d.slice(0, 10) : "";
        } else {
          el.value = "";
        }
      })();
      document.getElementById("tmMemPhone").value = u.phoneNumber || "";
      document.getElementById("tmMemHometown").value = u.hometown || "";
      document.getElementById("tmMemCurrentAddress").value = u.currentAddress || "";
      document.getElementById("tmMemOccupation").value = u.occupation || "";
      document.getElementById("tmMemBio").value = u.bio || "";
      if (u.familyId) famSel.value = u.familyId;
      await refreshMembersAndParentSelect(u.familyId || "", u.userId || "", u.parentId || null);
      await ensureTreeMemberRolesLoaded();
      const rSel = document.getElementById("tmMemRoleId");
      if (rSel) {
        if (u.roleId) rSel.value = u.roleId;
        else if (tmDefaultMemberRoleId) rSel.value = tmDefaultMemberRoleId;
      }
      document.getElementById("treeMemberModalTitle").textContent = "Sửa thành viên";
      openTreeMemberModal();
    } catch (err) {
      alert(err.message || "Lỗi.");
    }
  });

  document.getElementById("treeMemberDeleteBtn")?.addEventListener("click", async () => {
    const selectedMemberId = document.getElementById("treeLayout")?.dataset.selectedMemberId || "";
    if (!selectedMemberId) return;
    if (!localStorage.getItem("token")) {
      window.location.href = "/login?redirect=" + encodeURIComponent(window.location.pathname + window.location.search);
      return;
    }
    if (
      !confirm(
        "Xóa thành viên này? Không xóa được khi còn con trực tiếp. Nếu còn vợ/chồng: chỉ xóa được vợ (nữ) để gỡ liên kết; không xóa được chồng (nam) khi còn vợ — cần xóa vợ trước."
      )
    )
      return;
    try {
      const res = await fetch("/api/family-head/members/" + encodeURIComponent(selectedMemberId), {
        method: "DELETE",
        headers: treeAuthHeaders(),
      });
      if (!res.ok) {
        alert(await readFetchErrorMessage(res, "Xóa thất bại."));
        return;
      }
      closeTreeMemberModal();
      const treeLayout = document.getElementById("treeLayout");
      if (treeLayout) {
        treeLayout.dataset.selectedMemberId = "";
        treeLayout.dataset.selectedMemberSpouseId = "";
        treeLayout.dataset.selectedMemberGender = "";
        treeLayout.classList.remove("detail-open");
        treeLayout.classList.add("detail-closed");
      }
      updateTreePanelManageVisibility();
      if (typeof window.reloadFamilyTree === "function") await window.reloadFamilyTree();
    } catch (err) {
      alert(err.message || "Lỗi xóa.");
    }
  });

  document.getElementById("treeMemberModalClose")?.addEventListener("click", closeTreeMemberModal);
  document.getElementById("treeMemberModalCancel")?.addEventListener("click", closeTreeMemberModal);
  document.getElementById("treeMemberEditModal")?.addEventListener("click", (e) => {
    if (e.target === document.getElementById("treeMemberEditModal")) closeTreeMemberModal();
  });

  document.getElementById("tmMemFamilyId")?.addEventListener("change", async function () {
    const editId = document.getElementById("tmMemberEditId").value.trim();
    try {
      await refreshMembersAndParentSelect(this.value, editId, "");
    } catch (e) {
      alert(e.message || "Lỗi tải thành viên.");
    }
  });
  document.getElementById("tmMemParentId")?.addEventListener("change", syncGenerationOrderFromParent);

  document.getElementById("treeMemberForm")?.addEventListener("submit", async (ev) => {
    ev.preventDefault();
    const editId = document.getElementById("tmMemberEditId").value.trim();
    const body = {
      fullName: document.getElementById("tmMemFullName").value.trim(),
      email: document.getElementById("tmMemEmail").value.trim() || null,
      familyId: document.getElementById("tmMemFamilyId").value,
      gender: document.getElementById("tmMemGender").value || null,
      dob: document.getElementById("tmMemDob").value || null,
      phoneNumber: document.getElementById("tmMemPhone").value.trim() || null,
      parentId: (document.getElementById("tmMemParentId")?.value || "").trim() || null,
      generation: document.getElementById("tmMemGeneration").value
        ? parseInt(document.getElementById("tmMemGeneration").value, 10)
        : null,
      orderInFamily: document.getElementById("tmMemOrder").value
        ? parseInt(document.getElementById("tmMemOrder").value, 10)
        : null,
      hometown: document.getElementById("tmMemHometown").value.trim() || null,
      currentAddress: document.getElementById("tmMemCurrentAddress").value.trim() || null,
      occupation: document.getElementById("tmMemOccupation").value.trim() || null,
      bio: document.getElementById("tmMemBio").value.trim() || null,
    };
    const rid = (document.getElementById("tmMemRoleId")?.value || "").trim();
    if (rid) body.roleId = rid;
    if (!body.fullName) {
      alert("Nhập họ tên.");
      return;
    }
    if (!body.familyId) {
      alert("Chọn dòng họ.");
      return;
    }
    try {
      const url = editId
        ? "/api/family-head/members/" + encodeURIComponent(editId)
        : "/api/family-head/members";
      const method = editId ? "PUT" : "POST";
      const res = await fetch(url, {
        method,
        headers: treeAuthHeaders(),
        body: JSON.stringify(body),
      });
      if (!res.ok) {
        alert(await readFetchErrorMessage(res, "Lưu thất bại."));
        return;
      }
      closeTreeMemberModal();
      if (typeof window.reloadFamilyTree === "function") await window.reloadFamilyTree();
    } catch (err) {
      alert(err.message || "Lỗi lưu.");
    }
  });
}

function cardMemberHtml(member) {
  const femaleCls = member.gender === "FEMALE" ? " female" : "";
  const initials = (member.name || "U").slice(0, 2).toUpperCase();
  const years = `${member.birthYear || ""} - ${member.deathYear || "nay"}`;
  return `
    <div class="pcard${femaleCls}" data-member-id="${member.id}" data-gen="${member.generation}">
      <div class="pcard-avatar"><span>${initials}</span></div>
      <div class="pcard-name">${member.name}</div>
      <div class="pcard-years"><i class="fas fa-birthday-cake" style="font-size:.6rem;"></i><span>${years}</span></div>
      ${member.occupation ? `<div class="pcard-job">${member.occupation}</div>` : ""}
    </div>
  `;
}

function getChildrenCount(parentIds, members) {
  const parentSet = new Set(parentIds.filter(Boolean));
  return members.filter((m) =>
    parentSet.has(m.fatherId) || parentSet.has(m.motherId) || parentSet.has(m.parentId)
  ).length;
}

async function loadFamilyTree() {
  const treeContent = document.getElementById("treeContent");
  if (!treeContent) return;
  ensureTreeMemberModalBindings();

  const query = new URLSearchParams(window.location.search);
  const familyFilter = document.getElementById("familyFilter");
  const familyFilterWrap = document.getElementById("familyFilterWrap");
  const queryFamilyId = query.get("familyId");
  const savedFamilyId = localStorage.getItem("selectedFamilyId");
  const myFamilyId = localStorage.getItem("my_family_id");
  const canvas = document.getElementById("treeCanvas");
  const serverClanId = (canvas?.dataset?.clanFamilyId || "").trim();

  /* Một dòng họ: ưu tiên id từ server (Thymeleaf), sau đó URL / localStorage */
  let familyId =
    serverClanId || queryFamilyId || savedFamilyId || myFamilyId || "";

  let familiesList = [];
  try {
    const familyRes = await fetch("/api/public/families");
    if (familyRes.ok) {
      familiesList = await familyRes.json();
    }
  } catch (e) {
    /* ignore */
  }

  if (familyFilterWrap) {
    familyFilterWrap.style.display = serverClanId ? "none" : "";
  }
  if (!serverClanId && familyFilter) {
    familyFilter.innerHTML =
      '<option value="">Chọn dòng họ</option>' +
      familiesList.map((f) => `<option value="${f.id}">${f.name}</option>`).join("");
    if (!familyId && familiesList.length) {
      familyId = familiesList[0].id;
    }
    if (familyId) {
      familyFilter.value = familyId;
    }
    familyFilter.addEventListener("change", function onFamilyChange() {
      const selected = this.value || "";
      if (selected) {
        localStorage.setItem("selectedFamilyId", selected);
      } else {
        localStorage.removeItem("selectedFamilyId");
      }
      const nextUrl = selected ? `/family-tree?familyId=${encodeURIComponent(selected)}` : "/family-tree";
      window.location.href = nextUrl;
    });
  }

  if (!familyId && familiesList.length) {
    familyId = familiesList[0].id;
  }

  syncTreeCanvasFamilyId(familyId);
  syncTreeToolbarAddMember(familyId);

  const apiUrl = familyId
    ? `/api/public/family-tree?familyId=${encodeURIComponent(familyId)}`
    : "/api/public/family-tree";

  const res = await fetch(apiUrl);
  if (!res.ok) {
    document.getElementById("treeEmptyState")?.style.setProperty("display", "block");
    treeContent.innerHTML = "";
    syncTreeCanvasFamilyId(familyId);
    syncTreeToolbarAddMember(familyId);
    return;
  }

  const data = await res.json();
  const rawMembers = data.members || [];
  const members = rawMembers.map((m) => ({
    ...m,
    id: m.id || m.user_id,
    parentId: m.parentId != null && String(m.parentId).trim() ? String(m.parentId).trim() : null,
    spouseId: m.spouseId != null && String(m.spouseId).trim() ? String(m.spouseId).trim() : null,
  }));
  const maxGen = data.totalGenerations || 1;

  const genFilter = document.getElementById("genFilter");
  if (genFilter) {
    genFilter.innerHTML = '<option value="">Tất cả thế hệ</option>';
    for (let g = 1; g <= maxGen; g += 1) {
      const opt = document.createElement("option");
      opt.value = String(g);
      opt.textContent = `Thế hệ ${g}`;
      genFilter.appendChild(opt);
    }
  }

  if (!members.length) {
    document.getElementById("treeEmptyState")?.style.setProperty("display", "block");
    treeContent.innerHTML = "";
    syncTreeCanvasFamilyId(familyId);
    syncTreeToolbarAddMember(familyId);
    return;
  }

  const byId = new Map(members.map((m) => [m.id, m]));

  const parentIdsOf = (member) =>
    Array.from(new Set([member.fatherId, member.motherId, member.parentId].filter(Boolean)));
  const areBloodSiblings = (a, b) => {
    if (!a || !b || a.id === b.id) return false;
    const aPs = new Set(parentIdsOf(a));
    const bPs = new Set(parentIdsOf(b));
    for (const p of aPs) {
      if (bPs.has(p)) return true;
    }
    return false;
  };

  const coParentsByMember = new Map();
  members.forEach((child) => {
    const fatherId = child.fatherId || null;
    const motherId = child.motherId || null;
    if (!fatherId || !motherId) return;
    if (!coParentsByMember.has(fatherId)) coParentsByMember.set(fatherId, new Set());
    if (!coParentsByMember.has(motherId)) coParentsByMember.set(motherId, new Set());
    coParentsByMember.get(fatherId).add(motherId);
    coParentsByMember.get(motherId).add(fatherId);
  });

  const childrenOfParents = (parentIds) => {
    const parentSet = new Set(parentIds.filter(Boolean));
    if (!parentSet.size) return [];
    return members
      .filter((m) => {
        const ps = parentIdsOf(m);
        return ps.some((pid) => parentSet.has(pid));
      })
      .sort((a, b) => (a.generation || 999) - (b.generation || 999) || (a.orderInFamily || 999) - (b.orderInFamily || 999));
  };

  const consumed = new Set();
  const resolvePartner = (member) => {
    if (!member) return null;
    const candidateIds = [];
    if (member.spouseId) candidateIds.push(member.spouseId);
    const coParents = Array.from(coParentsByMember.get(member.id) || []);
    coParents.forEach((id) => candidateIds.push(id));

    for (const candidateId of candidateIds) {
      const candidate = byId.get(candidateId);
      if (!candidate) continue;
      if (candidate.id === member.id) continue;
      if (consumed.has(candidate.id)) continue;
      if (candidate.generation !== member.generation) continue;
      // Never pair siblings as spouses (can happen with noisy co-parent data).
      if (areBloodSiblings(member, candidate)) continue;
      return candidate;
    }
    return null;
  };

  const buildUnit = (member) => {
    if (!member || consumed.has(member.id)) return null;
    const partner = resolvePartner(member);
    const unitMembers = partner ? [member, partner] : [member];
    unitMembers.forEach((m) => consumed.add(m.id));

    const unitParentIds = unitMembers.map((m) => m.id);
    const directChildren = childrenOfParents(unitParentIds);
    const childUnits = [];
    directChildren.forEach((child) => {
      const unit = buildUnit(child);
      if (unit) childUnits.push(unit);
    });
    return { unitMembers, childUnits, primaryId: member.id };
  };

  const roots = members
    .filter((m) => parentIdsOf(m).length === 0)
    .sort((a, b) => (a.generation || 999) - (b.generation || 999) || (a.orderInFamily || 999) - (b.orderInFamily || 999));

  const forest = [];
  roots.forEach((root) => {
    const unit = buildUnit(root);
    if (unit) forest.push(unit);
  });
  members
    .sort((a, b) => (a.generation || 999) - (b.generation || 999) || (a.orderInFamily || 999) - (b.orderInFamily || 999))
    .forEach((m) => {
      const unit = buildUnit(m);
      if (unit) forest.push(unit);
    });

  const renderPair = (unit, sideHint = "center") => {
    const [first, second] = unit.unitMembers;
    if (!second) {
      return cardMemberHtml(first);
    }

    const primaryIsFirst = first.id === unit.primaryId;
    const primary = primaryIsFirst ? first : second;
    const partner = primaryIsFirst ? second : first;

    let left = primary;
    let right = partner;
    // Keep blood siblings visually closer in the middle, spouses outside.
    if (sideHint === "left") {
      left = partner;
      right = primary;
    } else if (sideHint === "right") {
      left = primary;
      right = partner;
    } else {
      left = primary;
      right = partner;
    }

    return `
      <div class="tree-pair">
        ${cardMemberHtml(left)}
        <div class="tree-couple-line"></div>
        <i class="fas fa-heart tree-couple-icon"></i>
        <div class="tree-couple-line"></div>
        ${cardMemberHtml(right)}
      </div>
    `;
  };

  const renderUnit = (unit, sideHint = "center") => {
    const pairHtml = renderPair(unit, sideHint);

    const childCount = unit.childUnits.length;
    const mid = (childCount - 1) / 2;
    const childrenHtml = childCount
      ? `
        <div class="tree-children-wrap">
          <div class="tree-children-line"></div>
          <div class="tree-children">
            ${unit.childUnits.map((childUnit, idx) => {
              const hint = idx < mid ? "left" : (idx > mid ? "right" : "center");
              return renderUnit(childUnit, hint);
            }).join("")}
          </div>
        </div>
      `
      : "";

    return `
      <div class="tree-unit">
        <div class="tree-node-col">
          ${pairHtml}
          ${childCount > 0 ? `<button class="tree-expand-btn" data-expanded="true" data-count="${childCount}" aria-expanded="true"><i class="fas fa-chevron-down"></i><span>${childCount} con</span></button>` : ""}
        </div>
        ${childrenHtml}
      </div>
    `;
  };

  const bindTreeExpandButtons = (rootEl) => {
    if (!rootEl) return;
    rootEl.querySelectorAll(".tree-expand-btn").forEach((btn) => {
      btn.addEventListener("click", () => {
        const unit = btn.closest(".tree-unit");
        if (!unit) return;
        const childrenWrap = unit.querySelector(":scope > .tree-children-wrap");
        if (!childrenWrap) return;

        const current = btn.dataset.expanded !== "false";
        const next = !current;
        btn.dataset.expanded = String(next);
        btn.setAttribute("aria-expanded", String(next));
        btn.classList.toggle("collapsed", !next);
        childrenWrap.classList.toggle("collapsed", !next);

        const count = Number(btn.dataset.count || "0");
        const label = btn.querySelector("span");
        if (label) {
          label.textContent = next ? `${count} con` : `Ẩn ${count} con`;
        }
      });
    });
  };

  treeContent.innerHTML = `<div class="tree-level tree-root-level">${forest.map((unit) => renderUnit(unit)).join("")}</div>`;
  bindTreeExpandButtons(treeContent);

  const prevSelectedId = document.getElementById("treeLayout")?.dataset.selectedMemberId || "";
  let selectedMemberId =
    (prevSelectedId && byId.has(prevSelectedId) ? prevSelectedId : null) || members[0]?.id || null;
  const detailName = document.getElementById("detailName");
  const detailInitial = document.getElementById("detailInitial");
  const detailYears = document.getElementById("detailYears");
  const detailGeneration = document.getElementById("detailGeneration");
  const detailHometown = document.getElementById("detailHometown");
  const detailJob = document.getElementById("detailJob");
  const detailBio = document.getElementById("detailBio");
  const detailSpouse = document.getElementById("detailSpouse");
  const detailChildren = document.getElementById("detailChildren");
  const detailProfileLink = document.getElementById("detailProfileLink");
  const detailPanel = document.getElementById("treeDetailPanel");
  const detailClose = document.getElementById("treeDetailClose");
  const treeLayout = document.getElementById("treeLayout");
  const compareModeToggle = document.getElementById("compareModeToggle");
  const compareModal = document.getElementById("compareModal");
  const compareModalClose = document.getElementById("compareModalClose");
  const compareA = document.getElementById("compareA");
  const compareB = document.getElementById("compareB");
  const compareRelationText = document.getElementById("compareRelationText");
  const compareCommonAncestors = document.getElementById("compareCommonAncestors");
  const expandAllBtn = document.getElementById("expandAll");
  const collapseAllBtn = document.getElementById("collapseAll");
  const exportPdfBtn = document.getElementById("exportPdf");
  const treeFullscreenModal = document.getElementById("treeFullscreenModal");
  const treeFullscreenClose = document.getElementById("treeFullscreenClose");
  const treeFullscreenContent = document.getElementById("treeFullscreenContent");
  const treeCanvas = document.getElementById("treeCanvas");
  const treeFullscreenCanvas = document.querySelector(".tree-fullscreen-canvas");
  const treeDetailPopupModal = document.getElementById("treeDetailPopupModal");
  const treeDetailPopupClose = document.getElementById("treeDetailPopupClose");
  const detailPopupInitial = document.getElementById("detailPopupInitial");
  const detailPopupName = document.getElementById("detailPopupName");
  const detailPopupYears = document.getElementById("detailPopupYears");
  const detailPopupGeneration = document.getElementById("detailPopupGeneration");
  const detailPopupHometown = document.getElementById("detailPopupHometown");
  const detailPopupJob = document.getElementById("detailPopupJob");
  const detailPopupBio = document.getElementById("detailPopupBio");
  const detailPopupSpouse = document.getElementById("detailPopupSpouse");
  const detailPopupChildren = document.getElementById("detailPopupChildren");
  const detailPopupProfileLink = document.getElementById("detailPopupProfileLink");

  const sidebarDetailEls = {
    initial: detailInitial,
    name: detailName,
    years: detailYears,
    generation: detailGeneration,
    hometown: detailHometown,
    job: detailJob,
    bio: detailBio,
    spouse: detailSpouse,
    children: detailChildren,
    profileLink: detailProfileLink,
  };
  const popupDetailEls = {
    initial: detailPopupInitial,
    name: detailPopupName,
    years: detailPopupYears,
    generation: detailPopupGeneration,
    hometown: detailPopupHometown,
    job: detailPopupJob,
    bio: detailPopupBio,
    spouse: detailPopupSpouse,
    children: detailPopupChildren,
    profileLink: detailPopupProfileLink,
  };

  const centerOnFirstNode = (container, contentRoot) => {
    if (!container || !contentRoot) return;
    const firstNode = contentRoot.querySelector(".pcard");
    if (!firstNode) return;

    const nodeRect = firstNode.getBoundingClientRect();
    const contentRect = contentRoot.getBoundingClientRect();
    const relLeft = nodeRect.left - contentRect.left + contentRoot.scrollLeft;
    const relTop = nodeRect.top - contentRect.top + contentRoot.scrollTop;

    const targetLeft = Math.max(0, relLeft - (container.clientWidth / 2) + (firstNode.clientWidth / 2));
    const targetTop = Math.max(0, relTop - (container.clientHeight / 3));
    container.scrollLeft = targetLeft;
    container.scrollTop = targetTop;
  };

  const attachSpacePan = (container) => {
    if (!container) return;
    let isSpacePressed = false;
    let isPanning = false;
    let startX = 0;
    let startY = 0;
    let startScrollLeft = 0;
    let startScrollTop = 0;

    const setReadyCursor = () => {
      container.classList.toggle("space-pan-ready", isSpacePressed && !isPanning);
    };

    const onKeyDown = (e) => {
      if (e.code !== "Space") return;
      const tag = (document.activeElement?.tagName || "").toLowerCase();
      if (tag === "input" || tag === "textarea" || document.activeElement?.isContentEditable) return;
      isSpacePressed = true;
      setReadyCursor();
      if (e.target === document.body) e.preventDefault();
    };

    const onKeyUp = (e) => {
      if (e.code !== "Space") return;
      isSpacePressed = false;
      if (!isPanning) {
        container.classList.remove("space-pan-ready");
      }
    };

    const onMouseDown = (e) => {
      if (!isSpacePressed || e.button !== 0) return;
      isPanning = true;
      startX = e.clientX;
      startY = e.clientY;
      startScrollLeft = container.scrollLeft;
      startScrollTop = container.scrollTop;
      container.classList.add("space-panning");
      container.classList.remove("space-pan-ready");
      e.preventDefault();
    };

    const onMouseMove = (e) => {
      if (!isPanning) return;
      const dx = e.clientX - startX;
      const dy = e.clientY - startY;
      container.scrollLeft = startScrollLeft - dx;
      container.scrollTop = startScrollTop - dy;
    };

    const stopPan = () => {
      if (!isPanning) return;
      isPanning = false;
      container.classList.remove("space-panning");
      setReadyCursor();
    };

    document.addEventListener("keydown", onKeyDown);
    document.addEventListener("keyup", onKeyUp);
    container.addEventListener("mousedown", onMouseDown);
    window.addEventListener("mousemove", onMouseMove);
    window.addEventListener("mouseup", stopPan);
    container.addEventListener("mouseleave", stopPan);
  };

  const setDetailPanelVisible = (visible) => {
    if (!treeLayout) return;
    treeLayout.classList.toggle("detail-open", visible);
    treeLayout.classList.toggle("detail-closed", !visible);
  };

  let compareMode = false;
  let compareSelectedIds = [];

  const isTreeFullscreenOpen = () => treeFullscreenModal?.classList.contains("open");

  const closeDetailPopup = () => {
    if (!treeDetailPopupModal) return;
    treeDetailPopupModal.classList.remove("open");
    treeDetailPopupModal.setAttribute("aria-hidden", "true");
  };

  const openDetailPopup = () => {
    if (!treeDetailPopupModal) return;
    treeDetailPopupModal.classList.add("open");
    treeDetailPopupModal.setAttribute("aria-hidden", "false");
  };

  const fillDetailElements = (els, member) => {
    if (!member || !els?.initial) return;
    const spouse = member.spouseId ? byId.get(member.spouseId) : null;
    const children = members.filter(
      (m) => m.fatherId === member.id || m.motherId === member.id || m.parentId === member.id
    );

    els.initial.textContent = (member.name || "--").slice(0, 2).toUpperCase();
    els.name.textContent = member.name || "Không rõ";
    els.years.textContent = `${member.birthYear || "?"} - ${member.deathYear || "nay"}`;
    els.generation.textContent = `Thế hệ ${member.generation || "-"}`;
    els.hometown.textContent = member.branch || "Chưa cập nhật";
    els.job.textContent = member.occupation || "Chưa cập nhật";
    els.bio.textContent = `Thành viên ${member.name || ""} thuộc ${member.branch || "dòng họ"}, dữ liệu đang được quản trị viên cập nhật.`;
    els.spouse.textContent = spouse ? spouse.name : "Không có";
    els.children.textContent = children.length ? children.map((c) => c.name).join(", ") : "Không có";
    els.profileLink.href = `/member/${member.id}`;
  };

  const getMemberInitials = (name) => (name || "--").slice(0, 2).toUpperCase();
  const genderClass = (m) => m?.gender === "FEMALE" ? "female" : "male";

  const renderCompareMember = (m) => `
    <div class="compare-avatar ${genderClass(m)}">${getMemberInitials(m?.name)}</div>
    <div class="compare-name">${m?.name || "Không rõ"}</div>
    <div class="compare-years">${m?.birthYear || "?"} - ${m?.deathYear || "nay"}</div>
    <div class="compare-job">${m?.occupation || "Chưa cập nhật"}</div>
  `;

  const syncCompareCardState = () => {
    document.querySelectorAll(".pcard").forEach((card) => {
      const isPicked = compareSelectedIds.includes(card.dataset.memberId);
      card.classList.toggle("compare-picked", compareMode && isPicked);
      card.classList.toggle("compare-mode", compareMode);
    });
  };

  const openCompareModal = async () => {
    if (!compareModal || compareSelectedIds.length !== 2) return;
    const first = byId.get(compareSelectedIds[0]);
    const second = byId.get(compareSelectedIds[1]);
    if (!first || !second) return;

    compareA.innerHTML = renderCompareMember(first);
    compareB.innerHTML = renderCompareMember(second);
    compareRelationText.textContent = "Đang phân tích...";
    compareCommonAncestors.textContent = "Đang tải dữ liệu...";
    compareModal.classList.add("open");
    compareModal.setAttribute("aria-hidden", "false");

    try {
      const relationUrl = familyId
        ? `/api/public/relationship?familyId=${encodeURIComponent(familyId)}&memberAId=${encodeURIComponent(first.id)}&memberBId=${encodeURIComponent(second.id)}`
        : `/api/public/relationship?memberAId=${encodeURIComponent(first.id)}&memberBId=${encodeURIComponent(second.id)}`;
      const relationRes = await fetch(relationUrl);
      if (!relationRes.ok) {
        throw new Error(`Request failed ${relationRes.status}`);
      }
      const relationData = await relationRes.json();
      compareRelationText.textContent = relationData.relationship || "Không xác định";
      const ancestors = relationData.commonAncestors || [];
      compareCommonAncestors.textContent = ancestors.length
        ? ancestors.join(", ")
        : "Không có dữ liệu tổ tiên chung";
    } catch (error) {
      compareRelationText.textContent = "Không phân tích được";
      compareCommonAncestors.textContent = "Không thể tải dữ liệu quan hệ từ hệ thống";
    }
  };

  const closeCompareModal = () => {
  if (!compareModal) return;
  compareModal.classList.remove("open");
  compareModal.setAttribute("aria-hidden", "true");
  compareSelectedIds = [];
  syncCompareCardState();
};

  const setCompareMode = (enabled) => {
    compareMode = enabled;
    compareSelectedIds = [];
    closeCompareModal();
    syncCompareCardState();

    if (compareMode) {
      setDetailPanelVisible(false);
      closeDetailPopup();
      if (compareModeToggle) {
        compareModeToggle.classList.add("btn-primary");
        compareModeToggle.classList.remove("btn-white");
        compareModeToggle.innerHTML = '<i class="fas fa-xmark"></i> Thoát so sánh';
      }
      return;
    }

    if (compareModeToggle) {
      compareModeToggle.classList.remove("btn-primary");
      compareModeToggle.classList.add("btn-white");
      compareModeToggle.innerHTML = '<i class="fas fa-code-compare"></i> So sánh quan hệ';
    }
    setDetailPanelVisible(true);
    renderDetail(selectedMemberId);
  };

  const renderDetail = (memberId) => {
    const member = byId.get(memberId);
    if (!member) return;
    selectedMemberId = member.id;

    const layoutEl = document.getElementById("treeLayout");
    if (layoutEl) {
      layoutEl.dataset.selectedMemberId = member.id;
      layoutEl.dataset.selectedMemberSpouseId = member.spouseId || "";
      layoutEl.dataset.selectedMemberGender = member.gender || "";
    }

    fillDetailElements(sidebarDetailEls, member);
    fillDetailElements(popupDetailEls, member);

    document.querySelectorAll(".pcard").forEach((card) => {
      card.classList.toggle("selected", card.dataset.memberId === selectedMemberId);
    });

    if (isTreeFullscreenOpen()) {
      setDetailPanelVisible(false);
      openDetailPopup();
    } else {
      closeDetailPopup();
      setDetailPanelVisible(true);
    }
    updateTreePanelManageVisibility();
  };

  const bindCompareCardClicks = (rootEl) => {
    if (!rootEl) return;
    rootEl.querySelectorAll(".pcard").forEach((card) => {
      card.addEventListener("click", () => {
        const memberId = card.dataset.memberId;
        if (!compareMode) {
          renderDetail(memberId);
          return;
        }

        if (compareSelectedIds.includes(memberId)) {
          compareSelectedIds = compareSelectedIds.filter((id) => id !== memberId);
        } else if (compareSelectedIds.length < 2) {
          compareSelectedIds.push(memberId);
        } else {
          compareSelectedIds = [compareSelectedIds[1], memberId];
        }
        syncCompareCardState();

        if (compareSelectedIds.length === 2) {
          openCompareModal();
        }
      });
    });
  };
  bindCompareCardClicks(treeContent);
  
  if (detailClose && detailPanel) {
    detailClose.addEventListener("click", () => {
      setDetailPanelVisible(false);
    });
  }

  compareModeToggle?.addEventListener("click", () => {
    setCompareMode(!compareMode);
  });

  compareModalClose?.addEventListener("click", closeCompareModal);
  compareModal?.addEventListener("click", (e) => {
    if (e.target === compareModal) {
      closeCompareModal();
    }
  });

  const expandAllNodes = () => {
    treeContent.querySelectorAll(".tree-children-wrap.collapsed").forEach((wrap) => {
      wrap.classList.remove("collapsed");
    });
    treeContent.querySelectorAll(".tree-expand-btn").forEach((btn) => {
      btn.dataset.expanded = "true";
      btn.setAttribute("aria-expanded", "true");
      btn.classList.remove("collapsed");
      const count = Number(btn.dataset.count || "0");
      const label = btn.querySelector("span");
      if (label) label.textContent = `${count} con`;
    });
  };

  const openTreeFullscreen = () => {
    if (!treeFullscreenModal || !treeFullscreenContent) return;
    expandAllNodes();
    treeFullscreenContent.innerHTML = treeContent.innerHTML;
    bindTreeExpandButtons(treeFullscreenContent);
    bindCompareCardClicks(treeFullscreenContent);
    treeFullscreenModal.classList.add("open");
    treeFullscreenModal.setAttribute("aria-hidden", "false");
    requestAnimationFrame(() => centerOnFirstNode(treeFullscreenCanvas, treeFullscreenContent));
    fullscreenScale = scale;
    applyFullscreenZoom();
    if (!compareMode && selectedMemberId) {
      renderDetail(selectedMemberId);
    } else {
      setDetailPanelVisible(false);
    }
  };

  const closeTreeFullscreen = () => {
    if (!treeFullscreenModal) return;
    treeFullscreenModal.classList.remove("open");
    treeFullscreenModal.setAttribute("aria-hidden", "true");
    closeDetailPopup();
    setDetailPanelVisible(true);
    if (treeFullscreenContent) treeFullscreenContent.style.transform = "";
    applyZoom();
    if (selectedMemberId) {
      renderDetail(selectedMemberId);
    }
  };

  const exportTreeToPdf = () => {
    const sourceHtml = treeFullscreenModal?.classList.contains("open")
      ? treeFullscreenContent?.innerHTML
      : treeContent.innerHTML;
    if (!sourceHtml) return;

    const printWin = window.open("", "_blank", "width=1400,height=900");
    if (!printWin) return;
    printWin.document.write(`
      <html>
      <head>
        <title>Gia pha</title>
        <link rel="stylesheet" href="/css/style.css"/>
        <style>
          body{margin:0;padding:20px;background:#fff;}
          .tree-container{border:none;min-height:auto;overflow:visible;padding:0;}
        </style>
      </head>
      <body>
        <div class="tree-container">
          ${sourceHtml}
        </div>
      </body>
      </html>
    `);
    printWin.document.close();
    printWin.focus();
    printWin.print();
  };

  expandAllBtn?.addEventListener("click", openTreeFullscreen);
  collapseAllBtn?.addEventListener("click", closeTreeFullscreen);
  treeFullscreenClose?.addEventListener("click", closeTreeFullscreen);
  treeFullscreenModal?.addEventListener("click", (e) => {
    if (e.target === treeFullscreenModal) closeTreeFullscreen();
  });
  treeDetailPopupClose?.addEventListener("click", closeDetailPopup);
  treeDetailPopupModal?.addEventListener("click", (e) => {
    if (e.target === treeDetailPopupModal) closeDetailPopup();
  });
  exportPdfBtn?.addEventListener("click", exportTreeToPdf);

  attachSpacePan(treeCanvas);
  attachSpacePan(treeFullscreenCanvas);
  requestAnimationFrame(() => centerOnFirstNode(treeCanvas, treeContent));

  if (prevSelectedId && byId.has(prevSelectedId)) {
    renderDetail(prevSelectedId);
  } else {
    setDetailPanelVisible(false);
  }

  let scale = 1;
  let fullscreenScale = 1;
  const zoomLabel = document.getElementById("zoomLabel");
  const applyZoom = () => {
    treeContent.style.transform = `scale(${scale})`;
    treeContent.style.transformOrigin = "top center";
    if (zoomLabel && !isTreeFullscreenOpen()) zoomLabel.textContent = `${Math.round(scale * 100)}%`;
  };
  const applyFullscreenZoom = () => {
    if (!treeFullscreenContent) return;
    treeFullscreenContent.style.transform = `scale(${fullscreenScale})`;
    treeFullscreenContent.style.transformOrigin = "top center";
    if (zoomLabel && isTreeFullscreenOpen()) zoomLabel.textContent = `${Math.round(fullscreenScale * 100)}%`;
  };
  document.getElementById("zoomIn")?.addEventListener("click", () => {
    if (isTreeFullscreenOpen()) {
      fullscreenScale = Math.min(2, fullscreenScale + 0.2);
      applyFullscreenZoom();
    } else {
      scale = Math.min(2, scale + 0.2);
      applyZoom();
    }
  });
  document.getElementById("zoomOut")?.addEventListener("click", () => {
    if (isTreeFullscreenOpen()) {
      fullscreenScale = Math.max(0.3, fullscreenScale - 0.2);
      applyFullscreenZoom();
    } else {
      scale = Math.max(0.3, scale - 0.2);
      applyZoom();
    }
  });
  document.getElementById("zoomReset")?.addEventListener("click", () => {
    if (isTreeFullscreenOpen()) {
      fullscreenScale = 1;
      applyFullscreenZoom();
    } else {
      scale = 1;
      applyZoom();
    }
  });
  treeFullscreenCanvas?.addEventListener(
    "wheel",
    (e) => {
      if (!isTreeFullscreenOpen() || !e.ctrlKey) return;
      e.preventDefault();
      const step = 0.1;
      fullscreenScale += e.deltaY < 0 ? step : -step;
      fullscreenScale = Math.min(2, Math.max(0.3, fullscreenScale));
      applyFullscreenZoom();
    },
    { passive: false }
  );

  document.getElementById("treeSearch")?.addEventListener("input", function onInput() {
    const q = this.value.toLowerCase().trim();
    document.querySelectorAll(".pcard").forEach((card) => {
      const name = card.querySelector(".pcard-name")?.textContent.toLowerCase() || "";
      card.style.opacity = (!q || name.includes(q)) ? "1" : "0.25";
    });
  });

  document.getElementById("genFilter")?.addEventListener("change", function onChange() {
    const v = this.value;
    document.querySelectorAll(".pcard").forEach((card) => {
      const g = card.dataset.gen || "";
      card.style.opacity = (!v || g === v) ? "1" : "0.2";
    });
  });
}

window.reloadFamilyTree = loadFamilyTree;
if (pagePath.startsWith("/family-tree")) loadFamilyTree();

async function loadMemberDetailPage() {
  const fullNameEl = document.getElementById("memberFullName");
  if (!fullNameEl) return;

  try {
    const response = await fetch("/data/member-detail.json");
    const member = await response.json();

    const safeValue = (value) => value || "-";
    const avatarEl = document.getElementById("memberAvatar");
    const fallbackEl = document.getElementById("memberAvatarFallback");
    const renderRelations = (containerId, items, emptyText) => {
      const container = document.getElementById(containerId);
      if (!container) return;

      if (!Array.isArray(items) || !items.length) {
        container.innerHTML = `<div class="member-empty-note">${emptyText}</div>`;
        return;
      }

      container.innerHTML = items.map((item) => `
        <div class="relation-item">
          <div class="relation-item-left">
            <div class="avatar avatar-sm">${(item.name || "?").charAt(0).toUpperCase()}</div>
            <div>
              <div class="relation-item-name">${item.name || "-"}</div>
              <div class="relation-item-sub">${item.relation || ""}</div>
            </div>
          </div>
        </div>
      `).join("");
    };
    const initials = (member.fullName || "M")
      .split(" ")
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part.charAt(0).toUpperCase())
      .join("");

    fullNameEl.textContent = safeValue(member.fullName);
    document.getElementById("memberMeta").textContent = `Lien he ${safeValue(member.email)}`;
    document.getElementById("memberGender").textContent = safeValue(member.gender);
    document.getElementById("memberDateOfBirth").textContent = safeValue(member.dateOfBirth);
    document.getElementById("memberEmail").textContent = safeValue(member.email);
    document.getElementById("memberPhone").textContent = safeValue(member.phone);
    document.getElementById("memberAddress").textContent = safeValue(member.address);
    document.getElementById("memberDescription").textContent = safeValue(member.description);
    document.getElementById("memberGenderBadge").textContent = safeValue(member.gender);
    document.getElementById("memberDobBadge").textContent = safeValue(member.dateOfBirth);
    document.getElementById("memberAddressMeta").textContent = safeValue(member.address);

    renderRelations("memberParents", member.familyRelations?.parents, "Chua co thong tin cha me");
    renderRelations("memberSpouse", member.familyRelations?.spouse, "Chua co thong tin vo chong");
    renderRelations("memberChildren", member.familyRelations?.children, "Chua co thong tin con cai");

    if (member.avatar) {
      avatarEl.src = member.avatar;
      avatarEl.style.display = "block";
      fallbackEl.style.display = "none";
    } else {
      avatarEl.style.display = "none";
      fallbackEl.style.display = "flex";
      fallbackEl.textContent = initials || "M";
    }
  } catch (error) {
    console.error("Failed to load member detail", error);
    fullNameEl.textContent = "Khong tai duoc du lieu thanh vien";
  }
}

loadMemberDetailPage();

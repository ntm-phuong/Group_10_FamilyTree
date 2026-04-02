function getInitials(fullName) {
  return (fullName || "M")
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part.charAt(0).toUpperCase())
    .join("");
}

function toggleAvatar(member) {
  const avatarEl = document.getElementById("memberAvatar");
  const fallbackEl = document.getElementById("memberAvatarFallback");

  if (!avatarEl || !fallbackEl) return;

  if (member.avatar) {
    avatarEl.src = member.avatar;
    avatarEl.style.display = "block";
    fallbackEl.style.display = "none";
    return;
  }

  avatarEl.removeAttribute("src");
  avatarEl.style.display = "none";
  fallbackEl.style.display = "flex";
  fallbackEl.textContent = getInitials(member.fullName) || "M";
}

function updateHeader(member) {
  const safeValue = (value) => value || "-";

  document.getElementById("memberFullNameHeading").textContent = safeValue(member.fullName);
  document.getElementById("memberMeta").textContent = `Lien he ${safeValue(member.email)}`;
  document.getElementById("memberGenderBadge").textContent = safeValue(member.gender);
  document.getElementById("memberDobBadge").textContent = safeValue(member.dateOfBirth);
  document.getElementById("memberAddressMeta").textContent = safeValue(member.address);

  toggleAvatar(member);
}

function updateFormFields(member) {
  document.getElementById("fullName").value = member.fullName || "";
  document.getElementById("gender").value = member.gender || "";
  document.getElementById("dateOfBirth").value = member.dateOfBirth || "";
  document.getElementById("email").value = member.email || "";
  document.getElementById("phone").value = member.phone || "";
  document.getElementById("address").value = member.address || "";
  document.getElementById("description").value = member.description || "";
}

function collectFormData(member) {
  return {
    ...member,
    fullName: document.getElementById("fullName").value.trim(),
    gender: document.getElementById("gender").value,
    dateOfBirth: document.getElementById("dateOfBirth").value,
    email: document.getElementById("email").value.trim(),
    phone: document.getElementById("phone").value.trim(),
    address: document.getElementById("address").value.trim(),
    description: document.getElementById("description").value.trim()
  };
}

function bindLivePreview(memberData) {
  ["fullName", "gender", "dateOfBirth", "email", "address"].forEach((fieldId) => {
    const field = document.getElementById(fieldId);
    field?.addEventListener("input", () => {
      updateHeader(collectFormData(memberData));
    });
    field?.addEventListener("change", () => {
      updateHeader(collectFormData(memberData));
    });
  });
}

function bindAvatarUpload(memberData) {
  const input = document.getElementById("avatarUpload");
  if (!input) return;

  input.addEventListener("change", () => {
    const [file] = input.files || [];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = () => {
      memberData.avatar = typeof reader.result === "string" ? reader.result : "";
      updateHeader(collectFormData(memberData));
    };
    reader.readAsDataURL(file);
  });
}

async function loadEditMemberDetailPage() {
  const form = document.getElementById("editMemberForm");
  if (!form) return;

  let memberData = {};

  try {
    const response = await fetch("/data/member-detail.json");
    memberData = await response.json();

    updateHeader(memberData);
    updateFormFields(memberData);
  } catch (error) {
    console.error("Failed to preload member detail form", error);
    document.getElementById("memberFullNameHeading").textContent = "Khong tai duoc du lieu thanh vien";
  }

  bindLivePreview(memberData);
  bindAvatarUpload(memberData);

  form.addEventListener("submit", (event) => {
    event.preventDefault();

    const payload = collectFormData(memberData);
    console.log("Saving member detail", payload);
    updateHeader(payload);
  });
}

loadEditMemberDetailPage();

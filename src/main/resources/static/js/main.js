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
  return members.filter((m) => parentSet.has(m.fatherId) || parentSet.has(m.motherId)).length;
}

function articleCardHtml(article) {
  const categoryLabel = article.categoryLabel || article.category;
  const author = article.author?.name || "Tác giả";
  return `
    <div class="news-card">
      <div class="news-card-img">
        ${article.coverImage ? `<img src="${article.coverImage}" alt="${article.title}"/>` : `<div class="ph-icon"><i class="fas fa-newspaper"></i></div>`}
      </div>
      <div class="news-card-body">
        <div class="news-card-meta">
          <span class="badge badge-gray">${categoryLabel}</span>
          <span class="text-xs text-muted">${author}</span>
          <span class="text-xs text-muted">1 tuần trước</span>
        </div>
        <div class="news-card-title"><a href="/news/${article.slug}">${article.title}</a></div>
        <p class="news-card-excerpt">${article.summary || ""}</p>
        <div class="news-card-footer"><span></span><a href="/news/${article.slug}">Đọc thêm <i class="fas fa-arrow-right"></i></a></div>
      </div>
    </div>
  `;
}

async function loadFamilyTree() {
  const treeContent = document.getElementById("treeContent");
  if (!treeContent) return;

  const query = new URLSearchParams(window.location.search);
  const familyFilter = document.getElementById("familyFilter");
  const queryFamilyId = query.get("familyId");
  const savedFamilyId = localStorage.getItem("selectedFamilyId");
  let familyId = queryFamilyId || savedFamilyId || "";

  if (familyFilter) {
    try {
      const familyRes = await fetch("/api/public/families");
      if (familyRes.ok) {
        const families = await familyRes.json();
        familyFilter.innerHTML = '<option value="">Chọn dòng họ</option>' +
          families.map((f) => `<option value="${f.id}">${f.name}</option>`).join("");

        if (!familyId && families.length) {
          familyId = families[0].id;
        }
        if (familyId) {
          familyFilter.value = familyId;
        }
      }
    } catch (e) {
      // ignore and continue with current familyId fallback
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

  const apiUrl = familyId
    ? `/api/public/family-tree?familyId=${encodeURIComponent(familyId)}`
    : "/api/public/family-tree";

  const res = await fetch(apiUrl);
  if (!res.ok) {
    document.getElementById("treeEmptyState")?.style.setProperty("display", "block");
    treeContent.innerHTML = "";
    return;
  }

  const data = await res.json();
  const rawMembers = data.members || [];
  const members = rawMembers.map((m) => ({
    ...m,
    id: m.id || m.user_id
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
    return;
  }

  const byId = new Map(members.map((m) => [m.id, m]));

  const parentIdsOf = (member) => [member.fatherId, member.motherId].filter(Boolean);
  const areBloodSiblings = (a, b) => {
    if (!a || !b || a.id === b.id) return false;
    const aFather = a.fatherId || null;
    const aMother = a.motherId || null;
    const bFather = b.fatherId || null;
    const bMother = b.motherId || null;
    const sameFather = !!aFather && aFather === bFather;
    const sameMother = !!aMother && aMother === bMother;
    return sameFather || sameMother;
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

  let selectedMemberId = members[0]?.id || null;
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
    const children = members.filter((m) => m.fatherId === member.id || m.motherId === member.id);

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

  setDetailPanelVisible(true);
  renderDetail(selectedMemberId);

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

async function loadNewsList() {
  const newsGrid = document.getElementById("newsGrid");
  if (!newsGrid) return;

  const params = new URLSearchParams(window.location.search);
  const selectedCategory = params.get("category");
  const query = (params.get("q") || "").trim().toLowerCase();

  const res = await fetch("/data/news.json");
  const data = await res.json();
  const articles = data.articles || [];
  const categories = data.categories || [];

  const tabWrap = document.getElementById("newsCategoryTabs");
  if (tabWrap) {
    tabWrap.innerHTML = categories.map((c) => {
      const active = selectedCategory === c.value ? " active" : "";
      return `<a href="/news?category=${c.value}" class="news-cat-tab${active}">${c.label}</a>`;
    }).join("");
  }

  const filtered = articles
    .filter((a) => !selectedCategory || a.category === selectedCategory)
    .filter((a) => !query || a.title.toLowerCase().includes(query) || (a.summary || "").toLowerCase().includes(query));

  document.getElementById("newsCount").textContent = String(filtered.length);
  document.getElementById("newsEmptyState").style.display = filtered.length ? "none" : "block";
  newsGrid.innerHTML = filtered.map(articleCardHtml).join("");

  const featuredGrid = document.getElementById("featuredNewsGrid");
  if (featuredGrid) {
    const featured = selectedCategory ? [] : articles.filter((a) => a.featured).slice(0, 2);
    featuredGrid.style.display = featured.length ? "grid" : "none";
    featuredGrid.innerHTML = featured.map(articleCardHtml).join("");
  }
}

async function loadNewsDetail() {
  const titleEl = document.getElementById("newsDetailTitle");
  if (!titleEl) return;

  const slug = pagePath.split("/").filter(Boolean).pop();
  const res = await fetch("/data/news.json");
  const data = await res.json();
  const articles = data.articles || [];
  const categories = data.categories || [];
  const article = articles.find((a) => a.slug === slug);

  if (!article) {
    titleEl.textContent = "Không tìm thấy bài viết";
    return;
  }

  document.title = `${article.title} - Tin tức`;
  document.getElementById("newsDetailBreadcrumbTitle").textContent = article.title;
  titleEl.textContent = article.title;
  document.getElementById("newsDetailCategory").textContent = article.categoryLabel || article.category;
  document.getElementById("newsDetailSummary").textContent = article.summary || "";
  document.getElementById("newsDetailContent").innerHTML = article.content || "";
  document.getElementById("newsDetailViewCount").textContent = String(article.viewCount || 0);
  const authorName = article.author?.name || "Tác giả";
  document.getElementById("newsDetailAuthor").textContent = authorName;
  document.getElementById("newsDetailAuthorInitial").textContent = authorName.charAt(0).toUpperCase();
  document.getElementById("newsDetailDate").textContent = article.publishedDate || "";

  const featuredBadge = document.getElementById("newsDetailFeatured");
  featuredBadge.style.display = article.featured ? "inline-flex" : "none";

  const catTabs = document.getElementById("newsDetailCategoryTabs");
  catTabs.innerHTML = [
    `<a href="/news" class="news-cat-tab" style="font-size:.78rem;">Tất cả</a>`,
    ...categories.map((c) => `<a href="/news?category=${c.value}" class="news-cat-tab${article.category === c.value ? " active" : ""}" style="font-size:.78rem;">${c.label}</a>`)
  ].join("");

  const related = articles.filter((a) => a.category === article.category && a.slug !== article.slug).slice(0, 3);
  const relatedWrap = document.getElementById("newsRelatedList");
  relatedWrap.innerHTML = related.map((r) => `
    <div style="margin-bottom:.75rem;padding-bottom:.75rem;border-bottom:1px solid var(--border);">
      <a href="/news/${r.slug}" style="font-size:.83rem;font-weight:600;color:var(--text-900);line-height:1.4;display:block;">${r.title}</a>
    </div>
  `).join("");
}

if (pagePath.startsWith("/family-tree")) loadFamilyTree();
if (pagePath === "/news") loadNewsList();
if (pagePath.startsWith("/news/")) loadNewsDetail();

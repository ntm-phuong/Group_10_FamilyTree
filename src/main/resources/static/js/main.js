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
    <div class="pcard${femaleCls}" data-member-id="${member.id}" data-gen="${member.generation}" role="button" tabindex="0">
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
  const author = article.author?.name || "Tac gia";
  return `
    <div class="news-card">
      <div class="news-card-img">
        ${article.coverImage ? `<img src="${article.coverImage}" alt="${article.title}"/>` : `<div class="ph-icon"><i class="fas fa-newspaper"></i></div>`}
      </div>
      <div class="news-card-body">
        <div class="news-card-meta">
          <span class="badge badge-gray">${categoryLabel}</span>
          <span class="text-xs text-muted">${author}</span>
          <span class="text-xs text-muted">1 tuan truoc</span>
        </div>
        <div class="news-card-title"><a href="/news/${article.slug}">${article.title}</a></div>
        <p class="news-card-excerpt">${article.summary || ""}</p>
        <div class="news-card-footer"><span></span><a href="/news/${article.slug}">Doc them <i class="fas fa-arrow-right"></i></a></div>
      </div>
    </div>
  `;
}

async function loadFamilyTree() {
  const treeContent = document.getElementById("treeContent");
  if (!treeContent) return;

  const res = await fetch("/data/family-tree.json");
  const data = await res.json();
  const members = data.members || [];
  const maxGen = data.totalGenerations || 1;
  const membersById = new Map(members.map((member) => [member.id, member]));
  const treeLayout = document.getElementById("treeLayout");
  const treeCanvas = document.getElementById("treeCanvas");
  const infoPanel = document.getElementById("memberInfoPanel");

  const syncPanelHeight = () => {
    if (!infoPanel || infoPanel.hidden || !treeCanvas) return;
    const minHeight = treeCanvas.offsetHeight || 480;
    const card = infoPanel.querySelector(".tree-info-card");
    if (card) {
      card.style.minHeight = `${minHeight}px`;
    }
  };

  const getMemberBio = (member) => {
    if (member.bio) return member.bio;
    if (member.description) return member.description;
    const branchText = member.branch ? `thuoc ${member.branch}` : "la thanh vien trong dong ho";
    const jobText = member.occupation ? `hien lam ${member.occupation.toLowerCase()}` : "dang dong gop vao hoat dong gia dinh";
    return `${member.name} ${branchText}, ${jobText}, thuoc the he ${member.generation}.`;
  };

  const getRelations = (member) => {
    const parents = [member.fatherId, member.motherId]
      .map((id) => membersById.get(id))
      .filter(Boolean)
      .map((relative) => relative.name);
    const spouse = member.spouseId ? [membersById.get(member.spouseId)].filter(Boolean).map((relative) => relative.name) : [];
    const children = members
      .filter((relative) => relative.fatherId === member.id || relative.motherId === member.id)
      .map((relative) => relative.name);

    return { parents, spouse, children };
  };

  const renderRelationList = (elementId, items, emptyText) => {
    const element = document.getElementById(elementId);
    if (!element) return;

    if (!items.length) {
      element.innerHTML = `<li class="empty">${emptyText}</li>`;
      return;
    }

    element.innerHTML = items.map((item) => `<li>${item}</li>`).join("");
  };

  const setSelectedCard = (memberId) => {
    document.querySelectorAll(".pcard").forEach((card) => {
      card.classList.toggle("is-selected", Number(card.dataset.memberId) === memberId);
    });
  };

  const openMemberPanel = (memberId) => {
    const member = membersById.get(memberId);
    if (!member) return;

    const initials = (member.name || "M")
      .split(" ")
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part.charAt(0).toUpperCase())
      .join("");
    const relations = getRelations(member);

    document.getElementById("memberInfoAvatar").textContent = initials || "M";
    document.getElementById("memberInfoName").textContent = member.name || "-";
    document.getElementById("memberInfoBirthYear").textContent = member.birthYear || "-";
    document.getElementById("memberInfoJob").textContent = member.occupation || member.job || "-";
    document.getElementById("memberInfoBio").textContent = getMemberBio(member);
    document.getElementById("memberInfoAction").href = `/member-detail?id=${member.id}`;

    renderRelationList("memberInfoParents", relations.parents, "Chua co thong tin");
    renderRelationList("memberInfoSpouse", relations.spouse, "Chua co thong tin");
    renderRelationList("memberInfoChildren", relations.children, "Chua co thong tin");

    infoPanel.hidden = false;
    treeLayout?.classList.add("has-panel");
    setSelectedCard(member.id);
    syncPanelHeight();
  };

  const closeMemberPanel = () => {
    if (infoPanel) {
      infoPanel.hidden = true;
    }
    treeLayout?.classList.remove("has-panel");
    setSelectedCard(-1);
  };

  const genFilter = document.getElementById("genFilter");
  if (genFilter) {
    for (let g = 1; g <= maxGen; g += 1) {
      const opt = document.createElement("option");
      opt.value = String(g);
      opt.textContent = `The he ${g}`;
      genFilter.appendChild(opt);
    }
  }

  if (!members.length) {
    document.getElementById("treeEmptyState")?.style.setProperty("display", "block");
    treeContent.innerHTML = "";
    return;
  }

  let html = "";
  for (let gen = 1; gen <= maxGen; gen += 1) {
    const group = members.filter((m) => m.generation === gen);
    const byId = new Map(members.map((m) => [m.id, m]));
    const rendered = new Set();
    html += `<div class="tree-level" id="gen-${gen}">`;
    group.forEach((m) => {
      if (rendered.has(m.id)) return;

      const spouse = m.spouseId ? byId.get(m.spouseId) : null;
      const canRenderPair = spouse && spouse.generation === gen && !rendered.has(spouse.id);

      if (canRenderPair) {
        const childrenCount = getChildrenCount([m.id, spouse.id], members);
        html += `
          <div class="tree-node-col">
            <div class="tree-pair" style="display:flex;align-items:center;">
              ${cardMemberHtml(m)}
              <div class="tree-couple-line"></div>
              <i class="fas fa-heart tree-couple-icon" style="font-size:.7rem;color:#F9A8D4;"></i>
              <div class="tree-couple-line"></div>
              ${cardMemberHtml(spouse)}
            </div>
            ${childrenCount > 0 ? `<button class="tree-expand-btn"><i class="fas fa-chevron-down"></i><span>${childrenCount} con</span></button>` : ""}
          </div>
        `;
        rendered.add(m.id);
        rendered.add(spouse.id);
      } else {
        const childrenCount = getChildrenCount([m.id], members);
        html += `
          <div class="tree-node-col">
            ${cardMemberHtml(m)}
            ${childrenCount > 0 ? `<button class="tree-expand-btn"><i class="fas fa-chevron-down"></i><span>${childrenCount} con</span></button>` : ""}
          </div>
        `;
        rendered.add(m.id);
      }
    });
    html += `</div>`;
    if (gen < maxGen) {
      html += `<div style="display:flex;flex-direction:column;align-items:center;margin:0 auto;"><div class="tree-connector"></div></div>`;
    }
  }
  treeContent.innerHTML = html;
  closeMemberPanel();

  let scale = 1;
  const zoomLabel = document.getElementById("zoomLabel");
  const applyZoom = () => {
    treeContent.style.transform = `scale(${scale})`;
    treeContent.style.transformOrigin = "top center";
    if (zoomLabel) zoomLabel.textContent = `${Math.round(scale * 100)}%`;
  };
  document.getElementById("zoomIn")?.addEventListener("click", () => { scale = Math.min(2, scale + 0.2); applyZoom(); });
  document.getElementById("zoomOut")?.addEventListener("click", () => { scale = Math.max(0.3, scale - 0.2); applyZoom(); });
  document.getElementById("zoomReset")?.addEventListener("click", () => { scale = 1; applyZoom(); });

  document.getElementById("treeSearch")?.addEventListener("input", function onInput() {
    const q = this.value.toLowerCase().trim();
    document.querySelectorAll(".pcard").forEach((card) => {
      const name = card.querySelector(".pcard-name")?.textContent.toLowerCase() || "";
      card.style.opacity = (!q || name.includes(q)) ? "1" : "0.25";
    });
  });

  document.getElementById("genFilter")?.addEventListener("change", function onChange() {
    const v = this.value;
    document.querySelectorAll('[id^="gen-"]').forEach((g) => {
      g.style.display = (!v || g.id === `gen-${v}`) ? "" : "none";
    });
  });

  treeContent.addEventListener("click", (event) => {
    const card = event.target.closest(".pcard");
    if (!card) return;
    event.stopPropagation();
    openMemberPanel(Number(card.dataset.memberId));
  });

  treeContent.addEventListener("keydown", (event) => {
    const card = event.target.closest(".pcard");
    if (!card) return;
    if (event.key !== "Enter" && event.key !== " ") return;
    event.preventDefault();
    openMemberPanel(Number(card.dataset.memberId));
  });

  document.addEventListener("click", (event) => {
    const clickedCard = event.target.closest(".pcard");
    const clickedPanel = event.target.closest("#memberInfoPanel");
    if (!clickedCard && !clickedPanel) {
      closeMemberPanel();
    }
  });

  window.addEventListener("resize", syncPanelHeight);
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
    titleEl.textContent = "Khong tim thay bai viet";
    return;
  }

  document.title = `${article.title} - Tin tuc`;
  document.getElementById("newsDetailBreadcrumbTitle").textContent = article.title;
  titleEl.textContent = article.title;
  document.getElementById("newsDetailCategory").textContent = article.categoryLabel || article.category;
  document.getElementById("newsDetailSummary").textContent = article.summary || "";
  document.getElementById("newsDetailContent").innerHTML = article.content || "";
  document.getElementById("newsDetailViewCount").textContent = String(article.viewCount || 0);
  const authorName = article.author?.name || "Tac gia";
  document.getElementById("newsDetailAuthor").textContent = authorName;
  document.getElementById("newsDetailAuthorInitial").textContent = authorName.charAt(0).toUpperCase();
  document.getElementById("newsDetailDate").textContent = article.publishedDate || "";

  const featuredBadge = document.getElementById("newsDetailFeatured");
  featuredBadge.style.display = article.featured ? "inline-flex" : "none";

  const catTabs = document.getElementById("newsDetailCategoryTabs");
  catTabs.innerHTML = [
    `<a href="/news" class="news-cat-tab" style="font-size:.78rem;">Tat ca</a>`,
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

document.addEventListener("DOMContentLoaded", function() {
  const topNav = document.getElementById('topNav');
  const token = localStorage.getItem('token');
  const currentPath = window.location.pathname;

  // Elements
  const navLogo = document.getElementById('navLogo');
  const navHome = document.getElementById('navHome');
  const navAbout = document.getElementById('navAbout');
  const navFamilyTree = document.getElementById('navFamilyTree');
  const navFamilyHead = document.getElementById('navFamilyHead');
  const navNews = document.getElementById('navNews');
  const navAdminMenu = document.getElementById('navAdminMenu');
  const navLoginBtn = document.getElementById('navLoginBtn');
  const navUserDropdown = document.getElementById('navUserDropdown');
  const navFullName = document.getElementById('navFullName');
  const navAvatarInitials = document.getElementById('navAvatarInitials');
  const profileLink = document.getElementById('profileLink');
  const navToggle = document.getElementById('navToggle');
  const navMobileMenu = document.getElementById('navMobileMenu');

  function getInitials(fullName) {
    const safeName = (fullName || '').trim();
    if (!safeName) return 'G';
    const nameParts = safeName.split(' ').filter(Boolean);
    if (nameParts.length >= 2) {
      return (nameParts[nameParts.length - 2].charAt(0) + nameParts[nameParts.length - 1].charAt(0)).toUpperCase();
    }
    return safeName.charAt(0).toUpperCase();
  }

  function paintAvatar() {
    if (!navAvatarInitials) return;
    const fullName = localStorage.getItem('full_name') || '';
    const avatarUrl = (localStorage.getItem('avatar_url') || '').trim();
    const initials = getInitials(fullName);

    if (avatarUrl) {
      navAvatarInitials.textContent = '';
      navAvatarInitials.style.backgroundImage = `url('${avatarUrl}')`;
      navAvatarInitials.style.backgroundSize = 'cover';
      navAvatarInitials.style.backgroundPosition = 'center';
    } else {
      navAvatarInitials.textContent = initials;
      navAvatarInitials.style.backgroundImage = '';
      navAvatarInitials.style.backgroundSize = '';
      navAvatarInitials.style.backgroundPosition = '';
    }
  }

  function renderNavbarUserInfo() {
    const freshToken = localStorage.getItem('token') || localStorage.getItem('accessToken');
    const fullName = (localStorage.getItem('full_name') || '').trim();
    if (!freshToken || !fullName) return;

    if (navFullName) navFullName.innerText = fullName;
    paintAvatar();

    if (profileLink) {
      const userId = localStorage.getItem('user_id');
      profileLink.href = (freshToken && userId) ? `/member/${userId}` : '/profile';
    }
  }

  function bootstrapNavbarProfileFromApi() {
    const freshToken = localStorage.getItem('token') || localStorage.getItem('accessToken');
    if (!freshToken) return Promise.resolve();

    const hasName = (localStorage.getItem('full_name') || '').trim().length > 0;
    const hasAvatar = (localStorage.getItem('avatar_url') || '').trim().length > 0;
    if (hasName && hasAvatar) return Promise.resolve();

    return fetch('/api/auth/me', {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + freshToken
      }
    })
      .then(function(res) {
        if (!res.ok) return null;
        return res.json();
      })
      .then(function(me) {
        if (!me) return;
        if (typeof me.fullName === 'string' && me.fullName.trim()) {
          localStorage.setItem('full_name', me.fullName.trim());
        }
        if (typeof me.avatar === 'string' && me.avatar.trim()) {
          localStorage.setItem('avatar_url', me.avatar.trim());
        }
        renderNavbarUserInfo();
      })
      .catch(function() {
        /* ignore */
      });
  }

  // Mobile menu toggle
  if (navToggle && navMobileMenu) {
    navToggle.addEventListener('click', function() {
      navMobileMenu.classList.toggle('hidden');
      const icon = this.querySelector('span');
      if (navMobileMenu.classList.contains('hidden')) {
        icon.textContent = 'menu';
      } else {
        icon.textContent = 'close';
      }
    });
  }

  // --- LOGIC HIỂN THỊ MENU GUEST vs STANDARD ---
  const guestLinks = document.querySelectorAll('.guest-link');
  const standardLinks = document.querySelectorAll('.standard-link');

  if (currentPath === '/' && !token) {
    // Nếu đang ở trang chủ và CHƯA đăng nhập -> Hiện menu cuộn trang (Guest), ẩn menu chuẩn
    guestLinks.forEach(link => link.classList.remove('hidden'));
    standardLinks.forEach(link => link.classList.add('hidden'));
  } else {
    // Ngược lại (Đã đăng nhập hoặc ở trang khác) -> Đảm bảo menu chuẩn hiển thị
    guestLinks.forEach(link => link.classList.add('hidden'));
    // (Các menu chuẩn sẽ được kiểm tra phân quyền ở phần dưới)
  }

  // --- LOGIC HIGHLIGHT DỰA TRÊN URL (CHO CÁC TRANG KHÁC TRANG CHỦ GUEST) ---
  function setActiveNavLink() {
    // Xóa active khỏi tất cả standard links
    standardLinks.forEach(link => link.classList.remove('active'));

    let activeSection = null;
    if (currentPath === '/home' || (currentPath === '/' && token)) {
      activeSection = 'home';
    } else if (currentPath === '/about') {
      activeSection = 'about';
    } else if (currentPath === '/family-tree') {
      activeSection = 'family-tree';
    } else if (currentPath === '/family-head') {
      activeSection = 'family-head';
    } else if (currentPath.startsWith('/news')) {
      activeSection = 'news';
    }

    if (activeSection) {
      let selectorSuffix = '';
      if (activeSection === 'family-tree') {
        selectorSuffix = 'FamilyTree';
      } else if (activeSection === 'family-head') {
        selectorSuffix = 'FamilyHead';
      } else {
        selectorSuffix = activeSection.charAt(0).toUpperCase() + activeSection.slice(1);
      }

      const activeDesktop = document.getElementById(`nav${selectorSuffix}`);
      if (activeDesktop) activeDesktop.classList.add('active');

      const activeMobile = document.getElementById(`nav${selectorSuffix}Mobile`);
      if (activeMobile) activeMobile.classList.add('active');
    }
  }

  // --- SCROLL SPY LOGIC (TỰ ĐỘNG HIGHLIGHT KHI CUỘN Ở TRANG CHỦ) ---
  function initScrollSpy() {
    const sections = document.querySelectorAll('section[id]');

    // Chỉ chạy scroll spy nếu ở trang chủ và chưa đăng nhập
    if (currentPath !== '/' || token) return;

    function onScroll() {
      // Đặt mặc định là 'home' để bao trọn khu vực background trên cùng
      let currentSection = 'home';
      const scrollPosition = window.scrollY;

      sections.forEach(section => {
        const sectionTop = section.offsetTop;

        // Cập nhật section nếu người dùng cuộn qua nó (trừ hao 100px của navbar)
        if (scrollPosition >= (sectionTop - 100)) {
          currentSection = section.getAttribute('id');
        }
      });

      // BẢO HIỂM: Nếu cuộn chuột đang ở sát mép trên cùng (nhỏ hơn 50px)
      // thì chắc chắn là đang ở khu vực banner background -> Ép về 'home'
      if (scrollPosition < 50) {
        currentSection = 'home';
      }

      // Highlight menu tương ứng
      guestLinks.forEach(link => {
        link.classList.remove('active');
        if (link.getAttribute('data-scroll') === currentSection) {
          link.classList.add('active');
        }
      });
    }

    // Lắng nghe sự kiện cuộn
    window.addEventListener('scroll', onScroll);
    // Chạy 1 lần ngay khi load trang để bắt đúng vị trí background hiện tại
    onScroll();
  }

  // Chạy các hàm khởi tạo
  setActiveNavLink();
  initScrollSpy();

  // Reveal nav sau khi xử lý xong để tránh giật giao diện (flash)
  if (topNav) topNav.style.display = '';

  // --- LOGIC PHÂN QUYỀN (KHI ĐÃ ĐĂNG NHẬP) ---
  if (token && (localStorage.getItem('full_name') || '').trim()) {
    if (navLogo) navLogo.href = '/home';
    if (navHome) navHome.href = '/home';

    if (navFamilyTree) navFamilyTree.classList.remove('hidden');
    const navFamilyTreeMobile = document.getElementById('navFamilyTreeMobile');
    if (navFamilyTreeMobile) navFamilyTreeMobile.classList.remove('hidden');

    try {
      const perms = JSON.parse(localStorage.getItem('permissions') || '[]');
      const p = Array.isArray(perms) ? perms : [];
      if (p.includes('FAMILY_HEAD')) {
        if (navFamilyHead) navFamilyHead.classList.remove('hidden');
        const navFamilyHeadMobile = document.getElementById('navFamilyHeadMobile');
        if (navFamilyHeadMobile) navFamilyHeadMobile.classList.remove('hidden');
        if (navAdminMenu) navAdminMenu.classList.remove('hidden');
      }
    } catch (e) { /* ignore */ }

    if (navLoginBtn) navLoginBtn.classList.add('hidden');
    if (navUserDropdown) navUserDropdown.classList.remove('hidden');

    renderNavbarUserInfo();
  }

  bootstrapNavbarProfileFromApi();

  window.addEventListener('profile-updated', function() {
    renderNavbarUserInfo();
  });

  window.addEventListener('storage', function(e) {
    if (!e || (e.key !== 'full_name' && e.key !== 'avatar_url')) return;
    renderNavbarUserInfo();
  });
});

function logout() {
  document.cookie = 'accessToken=; path=/; max-age=0';
  localStorage.clear();
  window.location.href = '/';
}
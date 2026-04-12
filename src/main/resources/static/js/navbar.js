document.addEventListener("DOMContentLoaded", function() {
  const topNav = document.getElementById('topNav');
  const token = localStorage.getItem('token');
  const fullName = localStorage.getItem('full_name');
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

  // Set active nav link
  function setActiveNavLink() {
    // Remove active from all links
    const allLinks = document.querySelectorAll('.nav-link');
    allLinks.forEach(link => {
      link.classList.remove('active');
    });

    let activeSection = null;
    if (currentPath === '/' || currentPath === '' || currentPath === '/home') {
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
      // Determine the selector suffix
      let selectorSuffix = '';
      if (activeSection === 'family-tree') {
        selectorSuffix = 'FamilyTree';
      } else if (activeSection === 'family-head') {
        selectorSuffix = 'FamilyHead';
      } else {
        selectorSuffix = activeSection.charAt(0).toUpperCase() + activeSection.slice(1);
      }

      // Apply active to desktop and mobile links
      const activeDesktop = document.getElementById(`nav${selectorSuffix}`);
      if (activeDesktop) {
        activeDesktop.classList.add('active');
      }

      const activeMobile = document.getElementById(`nav${selectorSuffix}Mobile`);
      if (activeMobile) {
        activeMobile.classList.add('active');
      }
    }
  }

  setActiveNavLink();

  // Reveal nav after initialization to avoid flash
  if (topNav) topNav.style.display = '';

  // If logged in
  if (token && fullName) {
    // Change logo and home links to /home
    if (navLogo) navLogo.href = '/home';
    if (navHome) navHome.href = '/home';

    // Show family tree
    if (navFamilyTree) navFamilyTree.classList.remove('hidden');
    const navFamilyTreeMobile = document.getElementById('navFamilyTreeMobile');
    if (navFamilyTreeMobile) navFamilyTreeMobile.classList.remove('hidden');

    // Check permissions for family head
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

    // Hide login button, show dropdown
    if (navLoginBtn) navLoginBtn.classList.add('hidden');
    if (navUserDropdown) navUserDropdown.classList.remove('hidden');

    // Set user name
    if (navFullName) navFullName.innerText = fullName;

    // Generate initials
    const nameParts = fullName.trim().split(' ');
    let initials = '';
    if (nameParts.length >= 2) {
      initials = nameParts[nameParts.length - 2].charAt(0) + nameParts[nameParts.length - 1].charAt(0);
    } else {
      initials = fullName.charAt(0);
    }
    if (navAvatarInitials) navAvatarInitials.innerText = initials.toUpperCase();

    // Update profile link
    if (profileLink) {
      const userId = localStorage.getItem('user_id');
      profileLink.href = (token && userId) ? `/member/${userId}` : '/profile';
    }
  }
});

function logout() {
  document.cookie = 'accessToken=; path=/; max-age=0';
  localStorage.clear();
  window.location.href = '/';
}
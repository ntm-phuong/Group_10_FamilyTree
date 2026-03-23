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
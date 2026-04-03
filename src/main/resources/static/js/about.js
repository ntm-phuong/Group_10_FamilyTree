document.addEventListener("DOMContentLoaded", function() {
    // 1. Hiệu ứng hiển thị Timeline khi scroll chuột (Intersection Observer)
    const timelineItems = document.querySelectorAll('.timeline-item');

    const observerOptions = {
        threshold: 0.2
    };

    const timelineObserver = new IntersectionObserver((entries, observer) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('show');
                observer.unobserve(entry.target); // Chỉ chạy hiệu ứng 1 lần
            }
        });
    }, observerOptions);

    timelineItems.forEach(item => {
        timelineObserver.observe(item);
    });

    // 2. Log thông tin để debug (Hữu ích cho sinh viên IT)
    console.log("About page script initialized.");
});
document.addEventListener("DOMContentLoaded", function() {
    const familyId = "FAM_01"; // ID thực tế của bạn
    
    fetch(`/api/family-head/dashboard/${familyId}`)
        .then(response => {
            if (!response.ok) throw new Error("Lỗi mạng hoặc server");
            return response.json();
        })
        .then(data => {
            // Cập nhật các ID khớp với HomeResponse phẳng
            if (document.getElementById('totalMembers')) {
                document.getElementById('totalMembers').innerText = data.totalMembers;
            }
            if (document.getElementById('totalGenerations')) {
                document.getElementById('totalGenerations').innerText = data.totalGenerations;
            }
            
            // Render tin tức mới nhất
            const newsContainer = document.getElementById('newsList');
            if (newsContainer && data.latestNews) {
                newsContainer.innerHTML = data.latestNews.map(news => `
                    <div class="news-item mb-2 border-bottom pb-1">
                        <div class="fw-bold">${news.title}</div>
                        <small class="text-muted">${new Date(news.createdAt).toLocaleDateString('vi-VN')}</small>
                    </div>
                `).join('');
            }
        })
        .catch(error => {
            console.error("Dashboard Fetch Error:", error);
            // Thông báo lỗi này sẽ không hiện nếu các bước trên chuẩn
            alert("Có lỗi khi đồng bộ dữ liệu dashboard!");
        });
});
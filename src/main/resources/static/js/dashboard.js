/**
 * Dashboard Controller - Xử lý hiển thị dữ liệu thống kê
 */
document.addEventListener("DOMContentLoaded", function () {

    // 1. Hiển thị lời chào cá nhân hóa
    const fullName = localStorage.getItem('full_name');
    const welcomeEl = document.getElementById('welcomeUser');
    if (welcomeEl && fullName) {
        welcomeEl.innerText = `Xin chào, ${fullName}`;
    }

    // 2. Hàm Fetch dữ liệu từ API
    async function initDashboard() {
        try {
            const token = localStorage.getItem('token');
            if (!token) {
                console.warn("Chưa đăng nhập, chuyển hướng...");
                window.location.href = "/login";
                return;
            }

            // Gọi API (Backend tự lấy FamilyId từ SecurityContext)
            const response = await fetch('/api/family-head/dashboard', {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });

            if (response.status === 401 || response.status === 403) {
                window.location.href = "/login";
                return;
            }
            if (!response.ok) throw new Error("Không thể tải dữ liệu Dashboard");

            const data = await response.json();

            // Thực hiện đổ dữ liệu
            updateStatistics(data);
            renderGenerationProgress(data);

        } catch (error) {
            console.error("Lỗi Dashboard JS:", error);
        }
    }

    // 3. Cập nhật các con số thống kê chính
    function updateStatistics(data) {
        const elements = {
            'totalMembers': data.totalMembers || 0,
            'livingCount': `${data.livingMembers || 0} còn sống`,
            'deceasedCount': `${data.deceasedMembers || 0} đã mất`,
            'genCount': data.totalGenerations || 0,
            'newsCount': data.totalNews || 0 // Nếu backend chưa có thì mặc định 0
        };

        for (const [id, value] of Object.entries(elements)) {
            const el = document.getElementById(id);
            if (el) el.innerText = value;
        }
    }

    // 4. Vẽ danh sách phân bổ đời (Thanh Progress Bar)
    function renderGenerationProgress(data) {
        const container = document.getElementById('genDistContainer');
        if (!container || !data.generationDistribution) return;

        container.innerHTML = ""; // Xóa dữ liệu cũ
        const dist = data.generationDistribution;
        const total = data.totalMembers || 1;

        Object.entries(dist).forEach(([gen, count]) => {
            const percentage = Math.round((count / total) * 100);

            const genHtml = `
                <div class="gen-item" style="margin-bottom: 1.2rem;">
                    <div style="display: flex; justify-content: space-between; margin-bottom: 5px; font-size: 0.9rem;">
                        <span style="font-weight: 600;">Đời thứ ${gen}</span>
                        <span class="text-muted">${count} thành viên</span>
                    </div>
                    <div style="background: #e9ecef; height: 10px; border-radius: 5px; overflow: hidden;">
                        <div style="width: ${percentage}%; background: #34495e; height: 100%; transition: width 0.5s ease;"></div>
                    </div>
                </div>
            `;
            container.insertAdjacentHTML('beforeend', genHtml);
        });
    }

    // Khởi chạy
    initDashboard();
});
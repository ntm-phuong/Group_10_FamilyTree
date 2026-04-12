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
            renderRecentMembers(data.newMembers);
            renderRecentNews(data.recentNews);
            renderActivityLog(data.recentNews, data.newMembers);

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

            container.insertAdjacentHTML('beforeend', `
                <div>
                    <div class="flex justify-between mb-1 text-sm">
                        <span class="font-semibold text-slate-700">Đời thứ ${gen}</span>
                        <span class="text-slate-400">${count} thành viên</span>
                    </div>
                    <div class="h-2 rounded-full bg-slate-100 overflow-hidden">
                        <div class="h-full rounded-full bg-emerald-600 transition-all duration-500" style="width: ${percentage}%"></div>
                    </div>
                </div>
            `);
        });
    }

    // 5. Thành viên mới
    function renderRecentMembers(members) {
        const container = document.getElementById('recentMembers');
        if (!container) return;

        if (!members || members.length === 0) {
            container.innerHTML = `<p class="text-sm text-slate-400 italic py-4 text-center">Chưa có thành viên nào.</p>`;
            return;
        }

        container.innerHTML = members.map(m => {
            const isMale = m.gender === 'MALE';
            const initials = (m.fullName || '?').trim().split(' ').pop().substring(0, 2).toUpperCase();
            const avatarColor = isMale
                ? 'bg-blue-100 text-blue-700'
                : 'bg-rose-100 text-rose-600';
            const genderLabel = isMale ? 'Nam' : 'Nữ';
            const genderBadge = isMale
                ? 'bg-blue-50 text-blue-600'
                : 'bg-rose-50 text-rose-500';

            return `
                <div class="flex items-center gap-3 py-3">
                    <div class="h-10 w-10 rounded-full flex items-center justify-center font-bold text-sm shrink-0 ${avatarColor}">
                        ${initials}
                    </div>
                    <div class="flex-1 min-w-0">
                        <p class="text-sm font-semibold text-slate-800 truncate">${m.fullName || '—'}</p>
                        <p class="text-xs text-slate-400">Đời thứ ${m.generation || '?'}</p>
                    </div>
                    <span class="text-xs font-medium px-2 py-0.5 rounded-full ${genderBadge}">${genderLabel}</span>
                </div>
            `;
        }).join('');
    }

    // 6. Tin tức & Thông báo
    function renderRecentNews(newsList) {
        const container = document.getElementById('recentNews');
        if (!container) return;

        if (!newsList || newsList.length === 0) {
            container.innerHTML = `<p class="text-sm text-slate-400 italic py-4 text-center col-span-2">Chưa có bài viết nào.</p>`;
            return;
        }

        container.innerHTML = newsList.map(n => {
            const date = n.createdAt
                ? new Date(n.createdAt).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })
                : '—';
            const isPublic = n.status === 'PUBLIC_SITE';
            const badgeClass = isPublic ? 'bg-emerald-50 text-emerald-700' : 'bg-amber-50 text-amber-700';
            const badgeLabel = isPublic ? 'Công khai' : 'Nội bộ';

            return `
                <div class="rounded-2xl border border-slate-100 bg-slate-50 p-4 hover:shadow-sm transition-shadow">
                    <div class="flex items-start justify-between gap-2 mb-2">
                        <span class="text-xs font-medium px-2 py-0.5 rounded-full ${badgeClass}">${badgeLabel}</span>
                        <span class="text-xs text-slate-400 shrink-0">${date}</span>
                    </div>
                    <p class="text-sm font-semibold text-slate-800 leading-snug line-clamp-2">${n.title || '—'}</p>
                </div>
            `;
        }).join('');
    }

    // 7. Hoạt động hệ thống (tổng hợp từ tin tức + thành viên mới)
    function renderActivityLog(newsList, members) {
        const container = document.getElementById('activityList');
        if (!container) return;

        const activities = [];

        // Từ tin tức
        if (newsList && newsList.length > 0) {
            newsList.slice(0, 3).forEach(n => {
                const date = n.createdAt
                    ? new Date(n.createdAt).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })
                    : '—';
                activities.push({
                    icon: 'far fa-newspaper',
                    iconBg: 'bg-blue-50 text-blue-600',
                    text: `Bài viết <strong>"${n.title}"</strong> được đăng tải`,
                    date
                });
            });
        }

        // Từ thành viên mới
        if (members && members.length > 0) {
            members.slice(0, 2).forEach(m => {
                activities.push({
                    icon: 'fas fa-user-plus',
                    iconBg: 'bg-emerald-50 text-emerald-600',
                    text: `Thành viên <strong>${m.fullName}</strong> được thêm vào gia phả`,
                    date: ''
                });
            });
        }

        if (activities.length === 0) {
            container.innerHTML = `<p class="text-sm text-slate-400 italic py-4 text-center">Chưa có hoạt động nào.</p>`;
            return;
        }

        container.innerHTML = activities.map(a => `
            <div class="flex items-start gap-4">
                <div class="h-9 w-9 rounded-xl flex items-center justify-center shrink-0 ${a.iconBg}">
                    <i class="${a.icon} text-sm"></i>
                </div>
                <div class="flex-1 min-w-0">
                    <p class="text-sm text-slate-700 leading-relaxed">${a.text}</p>
                    ${a.date ? `<p class="text-xs text-slate-400 mt-0.5">${a.date}</p>` : ''}
                </div>
            </div>
        `).join('');
    }

    // Khởi chạy
    initDashboard();
});
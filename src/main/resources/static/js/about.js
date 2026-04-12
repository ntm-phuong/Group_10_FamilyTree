/**
 * About Controller - Hiển thị thông tin Trưởng họ và hiệu ứng Timeline
 */
document.addEventListener("DOMContentLoaded", function () {

    // --- 1. KHỞI TẠO HIỆU ỨNG TIMELINE ---
    function initScrollReveal() {
        const revealItems = document.querySelectorAll('.reveal-on-scroll, .timeline-item');
        const observerOptions = {
            threshold: 0.15,
            rootMargin: "0px 0px -50px 0px"
        };

        const scrollObserver = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    entry.target.classList.add('active');
                    if (entry.target.classList.contains('timeline-item')) {
                        entry.target.classList.add('show');
                    }
                }
            });
        }, observerOptions);

        revealItems.forEach(item => scrollObserver.observe(item));
    }

    // --- 2. HÀM FETCH DỮ LIỆU TRƯỞNG HỌ ---
    async function fetchFamilyHeadInfo() {
        const token = localStorage.getItem('token');
        const familyId = localStorage.getItem('family_id');

        if (!token) {
            renderEmpty();
            return;
        }

        try {
            // Gọi endpoint mới — chỉ cần đăng nhập, không cần quyền quản trị
            const url = familyId
                ? `/api/public/family-head-info?familyId=${familyId}`
                : `/api/public/family-head-info`;

            const response = await fetch(url, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                console.error("API trả về lỗi:", response.status);
                renderEmpty();
                return;
            }

            const leader = await response.json();

            // Kiểm tra response có dữ liệu hợp lệ không
            if (leader && leader.userId) {
                renderLeader(leader);
            } else {
                renderEmpty();
            }

        } catch (error) {
            console.error("Lỗi Fetch:", error);
            renderEmpty();
        }
    }

    // --- 3. ĐỔ DỮ LIỆU VÀO GIAO DIỆN ---
    function renderLeader(data) {
        const container = document.getElementById('leaderContainer');
        if (!container) return;

        // Xử lý Tên và Chữ đại diện (Initials)
        const nameText = data.fullName || "Trưởng Họ";
        const nameParts = nameText.trim().split(" ");
        const lastName = nameParts[nameParts.length - 1];
        const initialText = lastName.substring(0, Math.min(lastName.length, 2)).toUpperCase();

        // Xử lý thông tin phụ
        const genText = data.generation || "—";
        const bioText = data.bio ? `${data.bio}` : "Gìn giữ gia phong là trách nhiệm của mỗi người con trong dòng tộc.";
        const genderClass = (data.gender === 'MALE' || data.gender === 'Nam') ? 'avatar-male' : 'avatar-female';

        // Xử lý Avatar (Ưu tiên ảnh từ DB)
        const hasAvatar = data.avatar && data.avatar.trim() !== "";
        const avatarStyle = hasAvatar
            ? `style="background-image: url('${data.avatar}'); background-size: cover; background-position: center; color: transparent;"`
            : "";

        container.innerHTML = `
            <div class="relative mb-6">
                <div class="avatar avatar-xl ${genderClass} mx-auto shadow-lg border-4 border-white" ${avatarStyle}>
                    ${!hasAvatar ? `<span>${initialText}</span>` : ""}
                </div>
                <span class="absolute bottom-1 right-[calc(50%-55px)] h-8 w-8 bg-secondary rounded-full border-2 border-white flex items-center justify-center shadow-md">
                    <span class="material-symbols-outlined text-white text-[18px]">military_tech</span>
                </span>
            </div>
            
            <h4 class="text-2xl font-bold text-primary mb-1">${nameText}</h4>
            
            <div class="inline-block px-4 py-1 rounded-full bg-secondary-container text-secondary text-[11px] font-bold uppercase tracking-widest mb-4">
                Đời thứ ${genText} — Trưởng Họ
            </div>
            
            <p class="text-sm text-on-surface-variant italic leading-relaxed px-6 relative">
                <span class="opacity-20 text-4xl absolute -top-4 left-2 font-serif">"</span>
                ${bioText}
                <span class="opacity-20 text-4xl absolute -bottom-8 right-2 font-serif">"</span>
            </p>
        `;
    }

    // --- 4. HIỂN THỊ KHI KHÔNG CÓ DỮ LIỆU ---
    function renderEmpty() {
        const container = document.getElementById('leaderContainer');
        if (!container) return;

        container.innerHTML = `
            <div class="flex flex-col items-center text-center py-4">
                <div class="h-28 w-28 rounded-full bg-surface-container-low flex items-center justify-center mb-4">
                    <span class="material-symbols-outlined text-4xl text-on-surface-variant opacity-40">person</span>
                </div>
                <p class="text-sm text-on-surface-variant italic">Chưa có thông tin trưởng họ.</p>
            </div>
        `;
    }

    // KHỞI CHẠY
    initScrollReveal();
    fetchFamilyHeadInfo();
});
document.addEventListener("DOMContentLoaded", function() {
    const familyId = "fam-01"; // ID thực tế của bạn

    // Cập nhật tên từ localStorage (nếu có)
    const storedName = localStorage.getItem('full_name');
    if (storedName) {
        document.getElementById('welcomeUser').innerText = `Xin chào, ${storedName}`;
    }

    fetch(`/api/family-head/dashboard?familyId=${familyId}`)
        .then(res => res.json())
        .then(data => {
            // Update Stats
            document.getElementById('totalMembers').innerText = data.totalMembers || 0;
            document.getElementById('livingCount').innerText = `${data.livingMembers || 0} còn sống`;
            document.getElementById('deceasedCount').innerText = `${data.deceasedMembers || 0} đã mất`;
            document.getElementById('genCount').innerText = data.totalGenerations || 0;
            document.getElementById('newsCount').innerText = data.totalNews || 0;
            document.getElementById('publishedCount').innerText = `${data.totalNews || 0} đã đăng`;

            // Render Generation Bars
            const genContainer = document.getElementById('genDistContainer');
            const dist = data.generationDistribution || {};
            genContainer.innerHTML = Object.entries(dist).map(([gen, count]) => `
                <div class="gen-item">
                    <div class="gen-info"><span>Đời thứ ${gen}</span><span>${count} người</span></div>
                    <div class="progress-bar-bg"><div class="progress-bar-fill" style="width: ${(count/data.totalMembers)*100}%"></div></div>
                </div>
            `).join('');

            // Render New Members
            const memberContainer = document.getElementById('recentMembers');
            memberContainer.innerHTML = (data.newMembers || []).map(m => `
                <div class="list-row">
                    <div>
                        <div style="font-weight:700; font-size:0.9rem;">${m.fullName}</div>
                        <div class="text-muted" style="font-size:0.75rem;">Đời thứ ${m.generation}</div>
                    </div>
                    <span class="badge ${m.gender === 'Nam' ? 'badge-nam' : 'badge-nu'}">${m.gender}</span>
                </div>
            `).join('');

            // Render News
            const newsContainer = document.getElementById('recentNews');
            newsContainer.innerHTML = (data.recentNews || []).map(n => `
                <div class="list-row">
                    <div>
                        <div style="font-weight:700; font-size:0.9rem;">${n.title}</div>
                        <div class="text-muted" style="font-size:0.75rem;">${n.time || 'Vừa xong'}</div>
                    </div>
                    <span class="badge badge-success">Đã đăng</span>
                </div>
            `).join('');
        })
        .catch(err => console.error("Dashboard Sync Error:", err));
});
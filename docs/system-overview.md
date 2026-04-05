# Tổng quan hệ thống gia phả (family-app)

## Mục tiêu triển khai

Ứng dụng web phục vụ **một dòng họ duy nhất** trên mỗi bản cài: cây gia phả, hồ sơ thành viên, tin tức công khai và nội bộ. Không còn chế độ “nhiều dòng họ” trên giao diện công khai; `familyId` khác với dòng họ cấu hình sẽ bị từ chối ở tầng dịch vụ khi thao tác nhạy cảm.

## Cấu hình cốt lõi

| Thuộc tính | Ý nghĩa |
|------------|---------|
| `app.clan.family-id` | Mã `Family` **gốc** (tổ tông) trong DB — tin `/news` và cây công khai gắn id này; các **chi** là bản ghi `Family` con (`parent_family_id`). |
| `app.clan.display-name` | Tên hiển thị khi chưa có bản ghi trong bảng `family`. |

Lớp `AppClanProperties` đọc các giá trị trên; controller và service tham chiếu khi cần “dòng họ của site”.

## Kiến trúc MVC (Spring)

- **View (Thymeleaf):** `templates/public/*` — trang chủ, đăng nhập, `family-tree`, `news-list`, `news-detail`, fragment navbar/footer/sidebar.
- **Controller:**  
  - `@Controller` — render HTML (`PublicFamilyTreeController`, `PublicNewsController`, …).  
  - `@RestController` — JSON API (`/api/public/...`, `/api/family-head/...`, bảo mật JWT / role).
- **Service:** nghiệp vụ (`MemberService`, `SiteNewsService`, `FamilyHeadService`, …).
- **Repository:** JPA truy cập DB.

Luồng tin tức công khai: `PublicNewsController` luôn lọc theo `app.clan.family-id`; tham số `category`, `q`, `visibility` (`public` / `internal`) điều khiển danh sách. Chi tiết bài: `GET /news/{slug}`.

Luồng gia phả: trang `/family-tree` nhận model `clanFamilyId`, `clanFamilyName`; canvas có `data-clan-family-id`. JavaScript (`main.js`) ưu tiên id này trước query string / localStorage. Bộ chọn “dòng họ” trên toolbar bị ẩn khi đã có id từ server.

## API công khai liên quan một dòng họ

- `GET /api/public/families` — trả về **một** phần tử `{ id, name }` của dòng họ cấu hình.
- `GET /api/public/family-tree?familyId=` — `familyId` tùy chọn; nếu khác clan hoặc không hợp lệ, service có thể trả lỗi.
- `GET /api/public/relationship` — so sánh quan hệ trong phạm vi clan.

## Dữ liệu mẫu (seed)

`DataInitializer` tạo **một dòng họ gốc** (`app.clan.family-id`) và **ba chi phụ thuộc** (id cố định `seed-chi-phu-ke` → `seed-chi-tieu` → `seed-chi-doi-tre`, nối nhau bằng `parent_family_id`). Phả kế chính 10 thế hệ gán đúng chi; thêm **nhánh song song** (anh em cùng cha mẹ, id `seed-br-*`) để cây không chỉ một đường thẳng. **Cha–mẹ–con (PARENT_CHILD)** chỉ tới **con trai** trong từng mối quan hệ cha–mẹ–con; **con dâu** chỉ **SPOUSE** với chồng.

`MemberService.getFamilyTreeData` gom **mọi thành viên và quan hệ** thuộc gốc + mọi chi con (BFS từ `familyId` được hỏi). Trưởng họ (`FAMILY_HEAD`): `FamilyScopeService` leo lên tổ tông rồi gom toàn bộ chi con để quản trị.

Tài khoản demo (mật khẩu seed thường `123456`): `truongho@giapha.vn`, `member@giapha.vn`.

`MockNewsEventsSeeder` chỉ tạo tin cho clan đó.

**Lưu ý:** DB cũ có thể trùng email với user seed → cần dọn trùng hoặc dùng DB sạch trước khi chạy seed.

## Quy tắc tạo thành viên (BE)

`FamilyHeadService` kiểm tra khi tạo / thêm vợ chồng: **bắt buộc** giới tính, ngày sinh, số điện thoại (định dạng 10 chữ số theo logic hiện tại trong service).

## Tin tức — quản trị trên trang

`news-page-manage.js` dùng `data-page-family-id` trên `.news-page-wrap` (server set = clan id) để gắn `familyId` khi POST/PUT tin qua `/api/family-head/news`.

## Tài liệu bổ sung

- `docs/news-roles-permissions-family-filtering.txt` — mô tả quyền và phạm vi tin; phần `all=1` / lọc đa dòng họ **không còn khớp UI mới**; tham chiếu lịch sử hoặc cập nhật tay nếu cần.

## Build & chạy

Maven: `mvn spring-boot:run` (cần JDK và biến môi trường `JAVA_HOME` đúng). Cấu hình MySQL trong `application.properties` phải trỏ tới instance thực tế.

# Hệ thống đấu giá trực tuyến

Ứng dụng đấu giá trực tuyến viết bằng Java, JavaFX/FXML và PostgreSQL.

## Công nghệ

- Java 21+
- JavaFX
- PostgreSQL
- Maven
- JUnit 5
- GitHub Actions

## Cấu trúc hiện tại

Mã nguồn đang nằm trong:

- `User/src`
- `User/resources`

Maven đã được cấu hình để build trực tiếp từ hai thư mục này.

## Cấu hình database

Không commit mật khẩu thật vào repo. Ứng dụng đọc cấu hình theo thứ tự:

1. JVM system properties, ví dụ `-Ddb.url=...`
2. Environment variables, ví dụ `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
3. File local override `User/resources/userauth/database.local.properties`
4. File fallback [database.properties](User/resources/userauth/database.properties)

Biến môi trường hỗ trợ:

- `DB_URL`
- `DB_ADMIN_URL`
- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`
- `DB_SSL_MODE`
- `DB_SCHEMA`
- `DB_CREATE_DATABASE_IF_MISSING`

## Tài khoản admin mặc định

Khi database chưa có admin nào, hệ thống sẽ tự seed:

- Username: `admin`
- Password: `Admin123`

Có thể override bằng:

- `APP_ADMIN_USERNAME`
- `APP_ADMIN_PASSWORD`
- `APP_ADMIN_EMAIL`
- `APP_ADMIN_FULL_NAME`

## Chạy ứng dụng

### Cách 1: script hiện có

```powershell
powershell -ExecutionPolicy Bypass -File .\run-javafx.ps1
```

Repo hiện đã mặc định trỏ sang Supabase pooler ở `database.properties`.
Nếu cần secret cục bộ, đặt trong `database.local.properties` hoặc env vars.

Tắt scheduler nền khi demo một máy:

```powershell
powershell -ExecutionPolicy Bypass -File .\run-javafx.ps1 -DisableScheduler
```

### Cách 2: Maven

Sau khi cài Maven:

```powershell
mvn -Djavafx.platform=win test
```

## Kiểm thử

Chạy test:

```powershell
mvn -Djavafx.platform=win test
```

CI trên GitHub Actions dùng:

```bash
mvn -B -Djavafx.platform=linux test
```

## Các cải thiện đã bổ sung

- Chặn seller tự bid vào phiên của mình.
- Fix logic form auto-bid trên bidder dashboard.
- Hỗ trợ nhập lịch bắt đầu/kết thúc trên seller dashboard.
- Thêm anti-sniping: bid trong 30 giây cuối sẽ gia hạn 60 giây.
- Đóng phiên an toàn hơn với bước chuyển `OPEN/RUNNING -> FINISHED` có điều kiện trong database.
- Seed admin mặc định nếu database chưa có admin.
- Thêm index cho các bảng đấu giá/bid/auto-bid/top-up.

## Hạn chế còn lại

- Ứng dụng vẫn là JavaFX desktop kết nối database trực tiếp, chưa tách thành client-server đúng nghĩa.
- Realtime hiện vẫn là refresh định kỳ, chưa dùng socket/event push.
- Tiền vẫn đang dùng `double`; để an toàn hơn nên chuyển sang `BigDecimal`.

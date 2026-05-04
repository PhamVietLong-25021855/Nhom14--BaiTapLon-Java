# He thong dau gia truc tuyen

Ung dung dau gia viet bang Java, JavaFX/FXML va PostgreSQL. Ban hien tai da duoc tach thanh client-server toi thieu de dung yeu cau de bai:

- `userauth.Main`: JavaFX client
- `userauth.server.ServerMain`: socket server
- Supabase/PostgreSQL chi duoc truy cap o server

## Cong nghe

- Java 21+
- JavaFX
- PostgreSQL / Supabase
- Maven
- JUnit 5
- GitHub Actions

## Cau truc ma nguon

Ma nguon hien tai nam trong:

- `User/src`
- `User/resources`

Maven da duoc cau hinh de build truc tiep tu hai thu muc nay.

## Kien truc hien tai

Luong chay:

```text
JavaFX Client -> Socket TCP -> Auction Server -> DAO -> Supabase/PostgreSQL
```

Nguyen tac:

- Client chi giu UI va gui request.
- Server giu `Service`, `DAO`, `DatabaseConnection`, `DatabaseInitializer`, `AuctionScheduler`.
- Client khong truy cap database truc tiep nua.
- Du lieu van luu tren Supabase/PostgreSQL thong qua JDBC o server.

## Cau hinh database

Khong commit mat khau that vao repo. Server doc cau hinh theo thu tu:

1. JVM system properties, vi du `-Ddb.url=...`
2. Environment variables, vi du `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
3. File local override `User/resources/userauth/database.local.properties`
4. File fallback [database.properties](User/resources/userauth/database.properties)

Bien moi truong ho tro:

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
- `DB_CONNECT_TIMEOUT_SECONDS`
- `DB_SOCKET_TIMEOUT_SECONDS`
- `DB_TCP_KEEP_ALIVE`
- `DB_APPLICATION_NAME`
- `DB_MAX_POOL_CONNECTIONS`
- `DB_LOCAL_OVERRIDE_PATH`

## Tai khoan admin mac dinh

Khi database chua co admin nao, server co the tu seed:

- Username: `admin`
- Password: `Admin123`

Co the override bang:

- `APP_ADMIN_USERNAME`
- `APP_ADMIN_PASSWORD`
- `APP_ADMIN_EMAIL`
- `APP_ADMIN_FULL_NAME`

## Chay he thong

### 1. Chay server

Server la tien trinh duy nhat duoc phep truy cap Supabase/PostgreSQL.

```powershell
powershell -ExecutionPolicy Bypass -File .\run-server.ps1
```

Neu muon truyen secret Supabase truc tiep:

```powershell
powershell -ExecutionPolicy Bypass -File .\run-server.ps1 `
  -DbUrl "jdbc:postgresql://..." `
  -DbUser "postgres.xxx" `
  -DbPassword "your-password"
```

Mot so tuy chon:

- `-Port 9999`
- `-DisableDbInit`
- `-DisableScheduler`

### 2. Chay client JavaFX

Client chi can biet dia chi server:

```powershell
powershell -ExecutionPolicy Bypass -File .\run-javafx.ps1
```

Neu server chay may khac hoac cong khac:

```powershell
powershell -ExecutionPolicy Bypass -File .\run-javafx.ps1 -ServerHost 127.0.0.1 -ServerPort 9999
```

## Kiem thu

Chay test:

```powershell
mvn -Djavafx.platform=win test
```

CI tren GitHub Actions dung:

```bash
mvn -B -Djavafx.platform=linux test
```

## Pham vi client-server da hoan thanh

- Dang nhap / dang ky qua socket server
- Tai danh sach auction qua socket server
- CRUD auction qua socket server
- Bid, auto-bid, wallet, homepage announcements qua socket server
- Scheduler dong phien dat o server
- Supabase credentials chi can dat o server

## Han che con lai

- Realtime hien tai chua la push event day du; UI van can bo sung co che subscribe/event neu muon dat muc realtime cao hon.
- Money van dang dung `double`; de an toan hon nen chuyen sang `BigDecimal`.
- Protocol hien tai dung Java object serialization don gian, chua phai DTO/versioned protocol chat che.

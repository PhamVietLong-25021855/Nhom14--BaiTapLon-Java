# Huong dan Trien Khai - Auction House System

> **Version:** 1.1.0
> **Last updated:** 2026-05-16
> **Stack:** Java 25 + JavaFX 25 + Maven 3.9 + MySQL (Akamai)
> **Scripts da kiem tra & sua:** `deploy.sh`, `restart-server.sh`, `server/run-server.sh`, `scripts/deploy-to-vps.sh`, `scripts/deploy-server-to-vps.ps1`, `scripts/restart-server.ps1`

---

## Lich su sua loi (bug fixes)

| Ngay | Loi | Script | Tac hai |
|---|---|---|---|
| 2026-05-16 | Duong dan `database.properties` sai: `java/userauth/` thay vi `resources/userauth/` | `deploy.sh`, `restart-server.sh`, `server/run-server.sh`, `scripts/*.sh`, `scripts/*.ps1` | DB password khong duoc inject -> server that bai ket noi |
| 2026-05-16 | `$?` ben trong `$()` luon tra ve 0 | `deploy.sh`, `server/run-server.sh` | Exit code build khong hien thi dung |
| 2026-05-16 | JAR path `target/` thay vi `server/target/` | `restart-server.sh`, `server/run-server.sh`, `scripts/restart-server.ps1` | JAR khong tim thay khi restart |
| 2026-05-16 | `$process` chua duoc dinh nghia (chi co `$javaProcess`) | `scripts/restart-server.ps1` | Crash khi server khong khoi dong duoc |
| 2026-05-16 | `SDK_DIR="$PROJECT_ROOT/sdk"` nhung `PROJECT_ROOT` bi sai khi chay tu thu muc `server/` | `server/run-server.sh` | Java/Maven duoc tai ve thu muc sai |

---

## Muc Luc

1. [Lich su sua loi (bug fixes)](#lich-su-sua-loi-bug-fixes)
2. [Tong quan kien truc](#1-tong-quan-kien-truc)
3. [Cau truc project](#2-cau-truc-project)
4. [Chi tiet deploy.sh](#3-chi-tiet-deploysh)
5. [Lan dau len VPS](#4-lần-đầu-lên-vps)
6. [Cap nhat code tren VPS](#5-cập-nhật-code-trên-vps)
7. [Quan ly server](#6-quản-lý-server)
8. [Chay client](#7-chạy-client)
9. [Xu ly su cu](#8-xử-lý-sự-cố)

---

## 1. Tong quan kien truc

```
[JavaFX Client] <-- Socket TCP port 5050 --> [Auction Server] <-- JDBC --> [Akamai MySQL]
     (may user)                                     (VPS)                    (cloud DB)
```

He thong gom 3 thanh phan:

| Thanh phan | Mo ta | Chay tren |
|---|---|---|
| **Auction Server** (`server/`) | Socket server xu ly nghiep vu: auth, auction, auto-bid. Truy cap database. | VPS (Linux) |
| **Client** (`client/`) | JavaFX GUI goi server qua Socket. Khong truy cap database truc tiep. | May user (Windows/Linux) |
| **Database** (Akamai MySQL) | Chua users, auctions, bids, announcements. | Cloud (Akamai) |

Cu phap goi lenh trong huong dan nay:

- **Linux/macOS:** Bash shell (`.sh`)
- **Windows:** PowerShell (`.ps1`)
- **Windows (Git Bash / WSL):** Bash (`.sh`)

---

## 2. Cau truc project

```
Nhom14--BaiTapLon-Java-Long-BanGoc/
|
|-- deploy.sh                    # Script deploy chinh (chay truc tiep tren VPS)
|-- restart-server.sh            # Restart nhanh (khong build)
|
|-- scripts/
|   |-- deploy-to-vps.sh         # Upload code + deploy len VPS tu may local
|   |-- deploy-server-to-vps.ps1 # PowerShell: upload + deploy
|   |-- restart-server.ps1       # PowerShell: restart server
|
|-- server/                      # Server module (chay tren VPS)
|   |-- pom.xml
|   |-- src/main/java/userauth/
|   |   |-- server/              # AuctionServerMain, SocketServer
|   |   |-- controller/          # Request handlers
|   |   |-- service/             # Business logic
|   |   |-- dao/                 # Database access
|   |   |-- database/            # DB config, initializer, connection pool
|   |   |-- model/               # Data models
|   |   |-- event/               # Event bus
|   |   |-- exception/           # Custom exceptions
|   |   |-- api/                 # API interfaces
|   |   |-- remote/              # (khong dung tren server)
|   |   |-- network/             # Request/Response objects
|   |   |-- gui/                 # (khong dung tren server)
|   |   |-- util/
|   |   |-- validation/
|   |-- src/main/resources/userauth/
|       |-- database.properties   # Cau hinh DB (PASS TRONG NAY!)
|
|-- client/                      # Client module (chay tren may user)
|   |-- pom.xml
|   |-- src/main/java/userauth/
|   |   |-- ClientLauncher.java # Entry point
|   |   |-- ClientMain.java
|   |   |-- remote/              # Remote service (goi server qua Socket)
|   |   |-- gui/fxml/           # JavaFX controllers
|       |-- resources/userauth/gui/fxml/  # FXML files
|
|-- core-common/                 # Shared code (models, API interfaces)
|-- pom.xml                      # Root POM (multi-module)
|-- database_indexes.sql         # Chi so database (chay them)
```

### Tac vu moi file quan trong

| File | Tac vu |
|---|---|
| `server/src/main/resources/userauth/database.properties` | Cau hinh ket noi MySQL. Password de trong, se duoc script thay bang `DB_PASSWORD`. |
| `deploy.sh` | Script deploy chinh. Tu dong cai Java/Maven neu can, build, stop server cu, start server moi, verify. |
| `scripts/deploy-to-vps.sh` | Upload code tu may local len VPS qua rsync/ssh, sau do build + deploy tren VPS. |
| `logs/server.pid` | Luu PID cua server dang chay. Dung de stop server. |
| `logs/server.log` | Log chinh cua server. |
| `logs/server.err.log` |stderr cua server. |

---

## 3. Chi tiet deploy.sh

`deploy.sh` la script chinh de deploy server. No chay **truc tiep tren VPS** (sau khi da upload code).

### 3.1 Cu phap su dung

```bash
# Cu phap co ban - deploy port 5050
./deploy.sh

# Truyen DB password bang bien moi truong
DB_PASSWORD="mat_khau_cua_ban" ./deploy.sh

# Custom port va bind host
./deploy.sh 8080 0.0.0.0

# Chi restart, khong build lai
./deploy.sh --skip-build

# Chi restart, khong chay tests
./deploy.sh --skip-tests

# Chi cai dat Java/Maven, khong deploy
./deploy.sh --install-deps
```

### 3.2 Bang tham so

| Tham so/Flag | Mo ta | Mac dinh |
|---|---|---|
| `$1` (positional) | Server port | `5050` |
| `$2` (positional) | Bind host | `0.0.0.0` |
| `--skip-build` | Bo qua buoc Maven build | `false` |
| `--skip-tests` | Bo qua Maven tests khi build | `false` |
| `--install-deps` | Chi cai Java/Maven, khong deploy | `false` |
| `DB_PASSWORD` (env) | Mat khau MySQL | (khong co) |

### 3.3 Flow hoat dong (buoc cu the)

Khi chay `./deploy.sh`, cac buoc thuc hien theo thu tu:

```
BUOC 1: Kiem tra truoc khi deploy
  |- Kiem tra pom.xml co ton tai khong
  |- Kiem tra Java (toi thieu JDK 21)
  |   |- Neu chua co hoac version < 21 -> tu dong tai JDK 25
  |- Kiem tra Maven (toi thieu 3.6)
  |   |- Neu chua co -> tu dong tai Maven 3.9.9
  |- Kiem tra disk space (can it nhat 500MB)

BUOC 2: Cai dat database
  |- Doc DB_PASSWORD tu bien moi truong
  |- Neu co DB_PASSWORD -> ghi vao database.properties
  |- Neu khong co -> canh bao (server se that bai ket noi)

BUOC 3: Dung server cu (neu co)
  |- Doc PID tu logs/server.pid
  |- kill -15 (graceful, cho 10s)
  |- kill -9 (force) neu con chay
  |- Tim va kill cac tien trinh "AuctionServerMain" / "auction-server.jar" lang thang
  |- Kiem tra port con bi chiem khong -> giai phong

BUOC 4: Backup log cu
  |- Neu logs/server.log ton tai -> doi ten thanh logs/server.log.bak

BUOC 5: Build project
  |- Chay: mvn clean package -Dmaven.test.skip=true
  |- (Neu co --skip-tests: them -DskipTests)

BUOC 6: Kiem tra JAR
  |- Kiem tra server/target/auction-server.jar co ton tai
  |- Kiem tra kich thuoc > 0
  |- Kiem tra kich thuoc > 1MB (canh bao neu nho hon)

BUOC 7: Cau hinh firewall (neu chay root)
  |- Neu co firewall-cmd (CentOS/RHEL) -> mo port TCP
  |- Neu co ufw (Ubuntu/Debian) -> mo port TCP

BUOC 8: Khoi dong server
  |- Tao thu muc logs/
  |- Chay Java voi:
  |   nohup java \
  |     -Xmx512m -Xms128m \
  |     -Djava.awt.headless=true \
  |     -Dapp.server.port=5050 \
  |     -Dapp.server.bind.host=0.0.0.0 \
  |     -cp server/target/auction-server.jar \
  |     userauth.server.AuctionServerMain \
  |     >> logs/server.log 2>> logs/server.err.log &
  |- Luu PID vao logs/server.pid

BUOC 9: Cho server khoi dong
  |- Doi toi da 60 giay
  |- Kiem tra port 5050 co dang lang nghe khong
  |- Kiem tra log co "[AuctionServer] Listening on" khong
  |- Neu server chet trong luc khoi dong -> doc stderr + exit

BUOC 10: Kiem tra database
  |- Doc logs/server.log
  |- Tim "[Database] Connected to" -> Thanh cong
  |- Tim "Could not connect" / "Connection refused" -> That bai

BUOC 11: Quet loi
  |- Tim trong log: Exception, ERROR, FATAL
  |- Loc bo noise (junit, concurrent, DEBUG, Client handling error)
  |- Hien thi 10 loi cuoi cung

BUOC 12: Xuat ket qua
  |- Hien thi PID, Port, Log, DB status
  |- Huong dan lenh quan ly server
```

### 3.4 Thu tu uu tien doc cau hinh Database

`DatabaseConfig.java` doc cau hinh theo thu tu uu tien:

```
1. System property  (java -Ddb.password=xxx)
2. Environment variable  (export DB_PASSWORD=xxx)
3. File database.properties  (db.password=xxx)
```

Vi du: neu dat ca `-Ddb.password=abc` va `export DB_PASSWORD=xyz`, thi `abc` se duoc su dung.

### 3.5 Tu dong cai dat Java/Maven

Neu VPS chua co Java hoac Maven, script se tu dong tai va cai dat:

**Java JDK 25 (Liberica):**
- Thu muc: `sdk/jdk-25/`
- URL chinh: `https://download.bell-sw.com/java/25.0.2+7/bellsoft-jdk25.0.2+7-linux-amd64.tar.gz`
- URL backup: `https://github.com/bell-sw/Liberica/releases/download/25.0.2/...`

**Maven 3.9.9:**
- Thu muc: `sdk/maven/`
- URL chinh: `https://dlcdn.apache.org/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.tar.gz`
- URL backup: `https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.tar.gz`

### 3.6 Cac script khac lien quan

| Script | Su dung khi nao |
|---|---|
| `deploy.sh` | Deploy lan dau + moi lan update code. Chay **tren VPS**. |
| `restart-server.sh` | Chi restart server (da co JAR). Chay **tren VPS**. |
| `scripts/deploy-to-vps.sh` | Upload code + deploy tu may local. Chay **tren may local**. |
| `scripts/restart-server.ps1` | Restart server tu Windows. |
| `scripts/deploy-server-to-vps.ps1` | Upload + deploy nhanh tu Windows. |
| `server/run-server.sh` | Build va chay server (che do don gian, khong co verify). |
| `server/run-server.ps1` | Build va chay server tren Windows. |

---

## 4. Lan dau len VPS

Day la huong dan chi tiet cho viec deploy lan dau tien len VPS Linux.

### 4.1 Chuuan bi truoc khi deploy

**a) Yeu cau he thong**

| Thanh phan | Yeu cau |
|---|---|
| OS | Ubuntu 20.04+ / Debian 11+ / CentOS 8+ |
| RAM | Toi thieu 1GB (recommend 2GB+) |
| Disk | Toi thieu 2GB free |
| Internet | VPS co the truy cap Internet de tai JDK/Maven |
| SSH | SSH daemon dang chay, port 22 (hoac port khac) |

**b) Cau hinh SSH key (khuyen nghi)**

De tranh phai nhap mat khau nhieu lan, cau hinh SSH key:

```bash
# Tren may local (Linux/macOS)
ssh-keygen -t ed25519 -C "your_email@example.com"
ssh-copy-id -p 22 root@<VPS_IP>

# Hoac Windows PowerShell
ssh-keygen -t ed25519 -C "your_email@example.com"
# Copy noi dung ~/.ssh/id_ed25519.pub vao ~/.ssh/authorized_keys tren VPS
```

**c) Kiem tra SSH ket noi**

```bash
# Kiem tra tu may local
ssh -p 22 root@<VPS_IP> "echo 'SSH OK'"

# Neu dung port khac 22
ssh -p <PORT> root@<VPS_IP> "echo 'SSH OK'"
```

**d) Tao thu muc project tren VPS**

```bash
ssh root@<VPS_IP>
mkdir -p /root/auction-server
exit
```

### 4.2 Upload code len VPS

**Cach 1: rsync (khuyen nghi - nhanh, chi upload thay doi)**

```bash
# Tu may local (Linux/macOS)
rsync -avz --exclude 'target/' \
        --exclude '.git/' \
        --exclude '*.class' \
        --exclude '*.log' \
        --exclude 'logs/' \
        --exclude 'sdk/' \
        --exclude 'node_modules/' \
        --exclude '.idea/' \
        --exclude '*.iml' \
        --exclude '*.md' \
        ./ root@<VPS_IP>:/root/auction-server/
```

**Cach 2: scp (don gian nhung upload lai toan bo)**

```bash
scp -r ./server root@<VPS_IP>:/root/auction-server/
scp ./pom.xml root@<VPS_IP>:/root/auction-server/
scp ./deploy.sh root@<VPS_IP>:/root/auction-server/
scp ./restart-server.sh root@<VPS_IP>:/root/auction-server/
```

**Cach 3: Dung script (tu dong hoa toan bo)**

```bash
# Tren may local, cai dat rsync neu chua co
# macOS:
brew install rsync

# Linux (Ubuntu/Debian):
sudo apt install rsync

# Linux (CentOS/RHEL):
sudo yum install rsync

# Chay script deploy
chmod +x scripts/deploy-to-vps.sh
VPS_HOST=<VPS_IP> VPS_USER=root DB_PASSWORD="mat_khau_db" ./scripts/deploy-to-vps.sh
```

### 4.3 Chay deploy tren VPS

SSH vao VPS:

```bash
ssh root@<VPS_IP>
cd /root/auction-server
```

**a) Tao thu muc can thiet:**

```bash
mkdir -p logs
mkdir -p sdk
```

**b) Gan quyen thuc thi cho script:**

```bash
chmod +x deploy.sh
chmod +x restart-server.sh
```

**c) Chay deploy (lan dau - se tai JDK/Maven):**

```bash
export DB_PASSWORD="mat_khau_mysql_cua_ban"
./deploy.sh
```

**d) Theo doi qua trinh:**

Script se xuat ra man hinh theo tung buoc. Theo doi log:

```bash
# Trong cua so SSH khac, hoac sau khi deploy xong
tail -f logs/server.log
```

**e) Ket qua mong doi:**

```
[OK]     Deployment complete!
[OK]     PID:     12345
[OK]     Port:    0.0.0.0:5050
[OK]     Log:     logs/server.log
[OK]     Database: Connected
```

### 4.4 Cau hinh MySQL (Akamai)

Neu dung Akamai MySQL (nhu cau hinh hien tai), can dam bao:

**a) Kiem tra IP VPS da duoc them vao Akamai trusted sources:**

1. Dang nhap Akamai Cloud (g2a.akamaidb.net)
2. Tim database cua ban
3. Vao phan **Access Control** / **Firewall / Trusted Sources**
4. Them IP cua VPS: `<VPS_PUBLIC_IP>/32`

**b) Kiem tra ket noi MySQL tu VPS:**

```bash
# Tren VPS, cai mysql client neu chua co
apt update && apt install -y default-mysql-client

# Thu ket noi
mysql -h a463350-akamai-prod-7079948-default.g2a.akamaidb.net \
      -P 26281 \
      -u akmadmin \
      -p
```

**c) Chay script tao chi so (option - tang hieu suat):**

```bash
# Sau khi server da chay lan dau, tao cac index
mysql -h a463350-akamai-prod-7079948-default.g2a.akamaidb.net \
      -P 26281 \
      -u akmadmin \
      -p < database_indexes.sql
```

### 4.5 Cau hinh firewall tren VPS

**Ubuntu/Debian (UFW):**

```bash
# Kiem tra trang thai
ufw status

# Mo port server (5050)
ufw allow 5050/tcp

# Neu SSH dang o port khac 22, dam bao da mo
ufw allow <SSH_PORT>/tcp

# Reload
ufw reload
```

**CentOS/RHEL (firewalld):**

```bash
# Kiem tra trang thai
firewall-cmd --state

# Mo port
firewall-cmd --add-port=5050/tcp --permanent
firewall-cmd --reload

# Kiem tra
firewall-cmd --list-ports
```

### 4.6 Mo port tren Akamai (neu dung MySQL cloud)

1. Dang nhap Akamai Cloud Dashboard
2. Vao **Networking** > **Access Control** cua database
3. Them `VPS_PUBLIC_IP/32` vao trusted sources
4. Save changes (co the mat 1-2 phut de apply)

### 4.7 Kiem tra server da chay

```bash
# Kiem tra port
ss -tlpn | grep 5050

# Hoac
netstat -tlpn | grep 5050

# Kiem tra process
ps aux | grep AuctionServerMain

# Kiem tra log
tail -20 logs/server.log
```

---

## 5. Cap nhat code tren VPS

### 5.1 Quy trinh co ban (nhanh)

Khi da co code tren VPS va chi can cap nhat:

**a) Tren VPS, pull code moi (neu dung git):**

```bash
cd /root/auction-server
git pull origin main

# Hoac upload tay:
rsync -avz --exclude 'target/' --exclude '.git/' \
  ./ root@<VPS_IP>:/root/auction-server/
```

**b) Chay lai deploy:**

```bash
cd /root/auction-server
export DB_PASSWORD="mat_khau_mysql"
./deploy.sh
```

### 5.2 Quy trinh day du tu may local (Windows)

**Buoc 1: Upload code**

```powershell
# PowerShell
scp -r .\server\* root@<VPS_IP>:/root/auction-server/server/
scp .\pom.xml root@<VPS_IP>:/root/auction-server/
scp .\deploy.sh root@<VPS_IP>:/root/auction-server/
scp .\restart-server.sh root@<VPS_IP>:/root/auction-server/
```

Hoac dung script:

```powershell
.\scripts\deploy-server-to-vps.ps1 -VpsHost "<VPS_IP>" -DbPassword "<PASSWORD>"
```

**Buoc 2: SSH vao VPS va deploy**

```bash
ssh root@<VPS_IP>
cd /root/auction-server
export DB_PASSWORD="mat_khau_mysql"
./deploy.sh
```

### 5.3 Quy trinh day du tu may local (Linux/macOS)

```bash
# Upload
rsync -avz --exclude 'target/' --exclude '.git/' \
      --exclude '*.class' --exclude '*.log' \
      ./ root@<VPS_IP>:/root/auction-server/

# Deploy
ssh root@<VPS_IP> "cd /root/auction-server && export DB_PASSWORD='mat_khau_mysql' && ./deploy.sh"
```

Hoac dung script:

```bash
chmod +x scripts/deploy-to-vps.sh
DB_PASSWORD="mat_khau_mysql" ./scripts/deploy-to-vps.sh
```

### 5.4 Chi restart (khong build lai)

Neu chi can restart server voi JAR hien co:

```bash
cd /root/auction-server
export DB_PASSWORD="mat_khau_mysql"
./restart-server.sh
```

### 5.5 Tao cron job auto-restart (optional)

Neu muon server tu dong khoi dong lai sau khi VPS reboot:

```bash
# Tren VPS
crontab -e

# Them dong sau:
@reboot cd /root/auction-server && /root/auction-server/deploy.sh >> /root/auction-server/logs/cron.log 2>&1

# Hoac chi restart:
@reboot cd /root/auction-server && /root/auction-server/restart-server.sh >> /root/auction-server/logs/cron.log 2>&1
```

---

## 6. Quan ly server

### 6.1 Cac lenh quan ly thuong dung

```bash
# Xem log real-time
tail -f logs/server.log

# Xem stderr
tail -f logs/server.err.log

# Xem log cu (backup)
tail -30 logs/server.log.bak

# Stop server (graceful)
kill $(cat logs/server.pid)

# Stop server (force)
kill -9 $(cat logs/server.pid)

# Restart server
./restart-server.sh

# Kiem tra port
ss -tlpn | grep 5050

# Kiem tra process
ps aux | grep AuctionServerMain

# Xem memory/process info
top -p $(cat logs/server.pid)

# Xem disk usage cua project
du -sh /root/auction-server/
```

### 6.2 Khoi dong lai server

```bash
cd /root/auction-server

# Cach 1: Dung restart script
./restart-server.sh

# Cach 2: Stop + Start thu cong
kill $(cat logs/server.pid)
sleep 2
nohup java -Xmx512m -Xms128m \
  -Djava.awt.headless=true \
  -Dapp.server.port=5050 \
  -Dapp.server.bind.host=0.0.0.0 \
  -cp server/target/auction-server.jar \
  userauth.server.AuctionServerMain \
  >> logs/server.log 2>> logs/server.err.log &
echo $! > logs/server.pid
```

### 6.3 Chay server nhu service (systemd)

Tao file service:

```bash
# Tren VPS
cat > /etc/systemd/system/auction-server.service << 'EOF'
[Unit]
Description=Auction Server
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=/root/auction-server
Environment=DB_PASSWORD=mat_khau_mysql_cua_ban
ExecStart=/root/auction-server/sdk/jdk-25/bin/java \
    -Xmx512m -Xms128m \
    -Djava.awt.headless=true \
    -Dapp.server.port=5050 \
    -Dapp.server.bind.host=0.0.0.0 \
    -cp /root/auction-server/server/target/auction-server.jar \
    userauth.server.AuctionServerMain
Restart=always
StandardOutput=append:/root/auction-server/logs/server.log
StandardError=append:/root/auction-server/logs/server.err.log

[Install]
WantedBy=multi-user.target
EOF
```

Kich hoat service:

```bash
systemctl daemon-reload
systemctl enable auction-server
systemctl start auction-server

# Cac lenh quan ly
systemctl status auction-server
systemctl restart auction-server
systemctl stop auction-server
journalctl -u auction-server -f
```

### 6.4 Kiem tra trang thai

```bash
# Check 1: Port listening
ss -tlpn | grep 5050

# Check 2: Process running
ps aux | grep AuctionServerMain | grep -v grep

# Check 3: PID file
cat logs/server.pid
kill -0 $(cat logs/server.pid) && echo "Server is running" || echo "Server is NOT running"

# Check 4: Log co "Listening on"
grep "Listening on" logs/server.log

# Check 5: Database connected
grep "Database.*Connected" logs/server.log

# Check 6: Loi ngan chan
grep -E "ERROR|FATAL|Exception" logs/server.log | grep -v "junit\|concurrent\|DEBUG\|Client handling"
```

### 6.5 Backup du lieu

```bash
# Backup database
mysqldump -h a463350-akamai-prod-7079948-default.g2a.akamaidb.net \
           -P 26281 \
           -u akmadmin \
           -p'do_mat_khau' \
           defaultdb > backup_$(date +%Y%m%d_%H%M%S).sql

# Backup project (chi source, khong backup logs/ target/)
rsync -avz --exclude 'logs/' --exclude 'target/' \
      /root/auction-server/ /root/auction-server-backup/
```

---

## 7. Chay client

Client chay tren may cua nguoi dung, ket noi toi server tren VPS.

### 7.1 Cau hinh server host/port cho client

Client doc cau hinh theo thu tu uu tien:

```
1. System property  (-Dapp.server.host=xxx -Dapp.server.port=xxx)
2. Environment variable  (APP_SERVER_HOST, APP_SERVER_PORT)
3. Gia tri mac dinh  (172.104.50.54:5050)
```

### 7.2 Chay tren Windows

```powershell
# Mac dinh (172.104.50.54:5050)
.\client\run-client.ps1

# Server VPS cua ban
.\client\run-client.ps1 -ServerHost "<VPS_IP>" -ServerPort 5050

# Neu dung domain
.\client\run-client.ps1 -ServerHost "your-domain.com" -ServerPort 5050
```

### 7.3 Chay tren Linux/macOS

```bash
cd client
mvn javafx:run "-Dmain.class=userauth.ClientLauncher" \
  "-Dapp.server.host=<VPS_IP>" "-Dapp.server.port=5050"
```

### 7.4 Cau hinh mac dinh cua client

Trong `client/src/main/java/userauth/remote/RemoteClientConfig.java`:

```java
private static final String DEFAULT_SERVER_HOST = "172.104.50.54";
private static final int DEFAULT_SERVER_PORT = 5050;
```

Neu server VPS cua ban khac IP, sua lai gia tri nay hoac truyen qua tham so.

### 7.5 Khi doi server VPS

Neu ban doi sang VPS/IP moi:

```powershell
# Windows
.\client\run-client.ps1 -ServerHost "<NEW_VPS_IP>" -ServerPort 5050

# Hoac dat bien moi truong (tat ca shell sessions)
$env:APP_SERVER_HOST = "<NEW_VPS_IP>"
```

---

## 8. Xu ly su cu

### 8.1 Server khong khoi dong duoc

**Trieu chung:** Deploy script bao loi "Server died during startup"

**Cach kiem tra:**

```bash
# Xem stderr
cat logs/server.err.log

# Xem log
tail -50 logs/server.log

# Kiem tra Java version
java -version

# Kiem tra JAR co ton tai
ls -la server/target/auction-server.jar

# Kiem tra DB password da duoc thiet lap
grep "db.password" server/src/main/resources/userauth/database.properties
```

**Nguyen nhan thuong gap:**

| Nguyen nhan | Cach xu ly |
|---|---|
| DB_PASSWORD chua duoc dat | `export DB_PASSWORD="mat_khau"` roi chay lai |
| Database password sai | Kiem tra mat khau MySQL tren Akamai |
| VPS IP chua them vao Akamai trusted sources | Them IP vao Access Control |
| Port 5050 bi chiem | `kill` process chiem port, hoac doi port |
| Khong du RAM | Tang `-Xms`/`-Xmx` hoac kiem tra RAM VPS |
| JDK version sai | `java -version` phai la 21+ |

### 8.2 Client khong ket noi duoc server

**Trieu chung:** Client JavaFX bi treo hoac bao loi ket noi

**Cach kiem tra:**

```bash
# Tren VPS, kiem tra server co dang chay
ss -tlpn | grep 5050

# Kiem tra firewall
ufw status
# hoac
firewall-cmd --list-ports

# Thu telnet tu client (Linux)
telnet <VPS_IP> 5050

# Thu curl
curl -v telnet://<VPS_IP>:5050
```

**Nguyen nhan thuong gap:**

| Nguyen nhan | Cach xu ly |
|---|---|
| Server chua mo port 5050 tren firewall VPS | `ufw allow 5050/tcp` hoac `firewall-cmd --add-port=5050/tcp` |
| Server chua khoi dong thanh cong | Kiem tra `logs/server.log` |
| Client sai IP/port | Kiem tra `-ServerHost` va `-ServerPort` |
| VPS IP thay doi | Cap nhat IP moi tren Akamai trusted sources |
| Client khong the resolve domain | Thu IP truc tiep thay vi domain name |

### 8.3 Database connection failed

**Trieu chung:** Log bao "Could not connect", "Connection refused"

**Cach kiem tra:**

```bash
# Tu VPS, thu ket noi MySQL truc tiep
mysql -h a463350-akamai-prod-7079948-default.g2a.akamaidb.net \
      -P 26281 -u akmadmin -p

# Kiem tra da them IP vao trusted sources chua
# (Dang nhap Akamai Dashboard > Access Control)
```

**Cac buoc xu ly:**

1. **Kiem tra DB_PASSWORD**: `echo $DB_PASSWORD` - phai co gia tri
2. **Kiem tra database.properties**: `grep db.password server/src/main/resources/userauth/database.properties` - phai co mat khau
3. **Kiem tra Akamai trusted sources**: IP VPS phai co trong danh sach
4. **Kiem tra SSL**: Akamai MySQL yeu cau SSL. Kiem tra `db.sslMode=REQUIRED`
5. **Kiem tra port**: Port 26281 phai duoc phep ra ngoai

### 8.4 Server chet sau mot thoi gian

**Trieu chung:** Server chay duoc 1 luc roi tu dung

**Cach kiem tra:**

```bash
# Xem log truoc khi chet
tail -100 logs/server.log

# Theo doi resource
top
free -m
df -h
```

**Cac buoc xu ly:**

1. Kiem tra **OOM Killer** (Out of Memory): `dmesg | grep -i kill`
2. Kiem tra disk full: `df -h`
3. Tang `-Xmx` len 1G: `-Xmx1024m -Xms256m`
4. Tao swap neu RAM it: `dd if=/dev/zero of=/swapfile bs=1M count=2048 && mkswap /swapfile && swapon /swapfile`
5. Xem xet chay **systemd service** de auto-restart

### 8.5 Build that bai tren VPS

**Trieu chung:** Maven build loi

**Cach kiem tra:**

```bash
# Kiem tra Maven
mvn -version

# Kiem tra Java
java -version

# Chay build thu cong de xem loi
mvn clean package -Dmaven.test.skip=true

# Neu loi memory
export MAVEN_OPTS="-Xmx512m"
mvn clean package -Dmaven.test.skip=true
```

### 8.6 Reset hoan toan server

Neu can reset hoan toan (xoa tat ca, deploy lai tu dau):

```bash
# Dung server
kill $(cat logs/server.pid) 2>/dev/null || true

# Xoa project cu
rm -rf /root/auction-server

# Xoa logs cu
rm -rf logs/

# Xoa SDK (neu muon tai lai JDK/Maven)
rm -rf sdk/

# Tao lai thu muc
mkdir -p /root/auction-server

# Upload code moi (theo huong dan muc 4.2)
rsync -avz --exclude 'target/' --exclude '.git/' \
      ./ root@<VPS_IP>:/root/auction-server/

# Chay deploy
cd /root/auction-server
export DB_PASSWORD="mat_khau_mysql"
./deploy.sh
```

---

## Checklist nhanh

### Lan dau deploy

- [ ] SSH key da cau hinh (khong can nhap mat khau)
- [ ] VPS tao thu muc `/root/auction-server`
- [ ] Code da upload len VPS
- [ ] `DB_PASSWORD` da duoc dat
- [ ] Port 5050 da mo tren firewall VPS
- [ ] VPS IP da them vao Akamai trusted sources
- [ ] `./deploy.sh` da chay thanh cong
- [ ] Server log co "Listening on port 5050"
- [ ] Server log co "Database.*Connected"
- [ ] Client ket noi duoc server

### Moi lan update code

- [ ] Pull/upload code moi
- [ ] `export DB_PASSWORD="..."` (neu chay thu cong)
- [ ] `./deploy.sh` hoac `./restart-server.sh`
- [ ] Kiem tra log khong co loi moi
- [ ] Test client van ket noi duoc

### Monitoring hang ngay

- [ ] `ss -tlpn | grep 5050` - server con dang chay
- [ ] `tail -5 logs/server.log` - khong co loi moi
- [ ] `df -h` - disk con du space
- [ ] `free -m` - con du RAM

---

## Lưu ý quan trọng

> **Bao mat:** File `server/src/main/resources/userauth/database.properties` chua mat khau database. **TUYET DOI KHONG** commit mat khau that len Git. File nay da co `db.password=` de trong (khong co gia tri), script deploy se tu thay the.

> **HTTPS:** Server hien tai chay tren **TCP Socket thuan** (khong ma hoa). Neu can bao mat cao, dat server sau Nginx reverse proxy voi SSL, hoac su dung VPN giua client va server.

> **Backup:** Nen backup database va source code dinh ky. VPS co the chet bat cu luc nao.

> **JDK 25:** Project yeu cau JDK 25. Neu VPS chua co, script se tu tai. Neu tai that bai (vi du: VPS chan outbound download), cai dat thu cong tai https://bell-sw.com/libericajdk/.

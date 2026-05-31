# Triển khai bảo mật socket

Giao thức socket hiện đã dùng session ở phía server, kiểm tra vai trò người dùng, timeout khi đọc dữ liệu và bộ lọc giải tuần tự hóa Java. Khi triển khai, cần đóng gói và chạy `core-common`, `server` và `client` cùng một phiên bản vì phản hồi đăng nhập đã thay đổi để có thêm session token.

Kết nối TCP thường vẫn được giữ để tiện phát triển cục bộ. Trước khi mở cổng `5050` ra mạng không tin cậy, nên bật TLS để mã hóa dữ liệu truyền giữa client và server.

## Tạo chứng chỉ cho server

Với môi trường kiểm thử, có thể tạo keystore PKCS12 và xuất chứng chỉ bằng các lệnh sau:

```bash
keytool -genkeypair -alias auction-server -keyalg RSA -keysize 3072 \
  -validity 365 -storetype PKCS12 -keystore auction-server.p12 \
  -dname "CN=auction.example.com"

keytool -exportcert -alias auction-server -keystore auction-server.p12 \
  -rfc -file auction-server.crt

keytool -importcert -alias auction-server -file auction-server.crt \
  -storetype PKCS12 -keystore auction-client-truststore.p12
```

Khi triển khai thật, nên dùng chứng chỉ được cấp cho đúng domain hoặc IP public của server.

## Chạy server với TLS

Linux:

```bash
export APP_SERVER_TLS_ENABLED=true
export JAVA_TOOL_OPTIONS="-Djavax.net.ssl.keyStore=/secure/auction-server.p12 -Djavax.net.ssl.keyStorePassword=change-me"
./server/run-server.sh 5050 0.0.0.0
```

Windows PowerShell:

```powershell
.\server\run-server.ps1 -Tls `
  -KeyStore "C:\secure\auction-server.p12" `
  -KeyStorePassword "change-me"
```

## Chạy client với TLS

Windows PowerShell:

```powershell
.\client\run-client.ps1 -ServerHost "auction.example.com" -Tls `
  -TrustStore "C:\secure\auction-client-truststore.p12" `
  -TrustStorePassword "change-me"
```

Các JVM property tương đương:

```text
-Dapp.server.tls.enabled=true
-Djavax.net.ssl.trustStore=/secure/auction-client-truststore.p12
-Djavax.net.ssl.trustStorePassword=change-me
```

Không commit keystore, truststore hoặc mật khẩu vào repository.

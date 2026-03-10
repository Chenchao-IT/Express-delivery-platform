@echo off
REM 使用 JDK 17 编译（Lombok 与 Java 25 不兼容）
REM 若未安装 JDK 17，请从 https://adoptium.net/ 下载
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.13.11-hotspot"
if not exist "%JAVA_HOME%" set "JAVA_HOME=C:\Program Files\Java\jdk-17"
if not exist "%JAVA_HOME%" (
    echo [错误] 未找到 JDK 17，Lombok 与 Java 25 不兼容。
    echo 请安装 JDK 17: https://adoptium.net/temurin/releases/?version=17
    pause
    exit /b 1
)
set "PATH=%JAVA_HOME%\bin;%PATH%"

cd /d "%~dp0"
mvn clean spring-boot:run
pause

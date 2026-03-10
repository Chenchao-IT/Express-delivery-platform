# 使用 JDK 17 编译（Lombok 与 Java 25 不兼容）
$jdk17 = Get-ChildItem "C:\Program Files\Eclipse Adoptium\jdk-17*" -ErrorAction SilentlyContinue | Select-Object -First 1
$jdkPath = if (Test-Path "C:\Program Files\Eclipse Adoptium\jdk-17.0.13.11-hotspot") { "C:\Program Files\Eclipse Adoptium\jdk-17.0.13.11-hotspot" }
    elseif ($jdk17) { $jdk17.FullName }
    elseif (Test-Path "C:\Program Files\Java\jdk-17") { "C:\Program Files\Java\jdk-17" }
    else { $null }
if ($jdkPath) {
    $env:JAVA_HOME = $jdkPath
    $env:Path = "$jdkPath\bin;" + $env:Path
} else {
    Write-Host "[错误] 未找到 JDK 17，Lombok 与 Java 25 不兼容。请安装: https://adoptium.net/temurin/releases/?version=17" -ForegroundColor Red
    exit 1
}
Set-Location $PSScriptRoot
mvn clean spring-boot:run

# 检查 Java 环境脚本
Write-Host "=== Java 环境检查 ===" -ForegroundColor Cyan
Write-Host ""

# 检查 java
try {
    $javaVer = java -version 2>&1
    Write-Host "java -version:" -ForegroundColor Yellow
    $javaVer | ForEach-Object { Write-Host $_ }
} catch {
    Write-Host "java 未找到" -ForegroundColor Red
}

# 检查 javac
Write-Host ""
try {
    $javacVer = javac -version 2>&1
    Write-Host "javac -version:" -ForegroundColor Yellow
    Write-Host $javacVer
    Write-Host "JDK 已安装" -ForegroundColor Green
} catch {
    Write-Host "javac 未找到 - 当前为 JRE，需要安装 JDK" -ForegroundColor Red
}

# 检查 JAVA_HOME
Write-Host ""
Write-Host "JAVA_HOME: $env:JAVA_HOME"
if (-not $env:JAVA_HOME) {
    Write-Host "JAVA_HOME 未设置" -ForegroundColor Red
}

# 查找已安装的 JDK
Write-Host ""
Write-Host "常见 JDK 安装路径:" -ForegroundColor Yellow
$paths = @(
    "C:\Program Files\Java\jdk-17*",
    "C:\Program Files\Java\jdk-21*",
    "C:\Program Files\Eclipse Adoptium\jdk-17*",
    "C:\Program Files\Microsoft\jdk-17*",
    "C:\Program Files\OpenJDK\jdk-17*"
)
foreach ($p in $paths) {
    $found = Get-Item $p -ErrorAction SilentlyContinue
    if ($found) {
        Write-Host "  找到: $($found.FullName)" -ForegroundColor Green
        Write-Host "  设置命令: `$env:JAVA_HOME = `"$($found.FullName)`"" -ForegroundColor Gray
        $javacPath = Join-Path $found.FullName "bin\javac.exe"
        if (Test-Path $javacPath) {
            Write-Host "  (包含 javac，可正常编译)" -ForegroundColor Green
        }
    }
}

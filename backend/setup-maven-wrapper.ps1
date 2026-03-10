# 下载并设置 Maven Wrapper（无需预装 Maven）
$wrapperDir = ".mvn\wrapper"
$wrapperJar = "$wrapperDir\maven-wrapper.jar"
$wrapperUrl = "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"

Write-Host "正在下载 Maven Wrapper..." -ForegroundColor Green
New-Item -ItemType Directory -Force -Path $wrapperDir | Out-Null

try {
    Invoke-WebRequest -Uri $wrapperUrl -OutFile $wrapperJar -UseBasicParsing
    Write-Host "Maven Wrapper 已就绪。请运行: .\mvnw.cmd spring-boot:run" -ForegroundColor Green
} catch {
    Write-Host "下载失败，请手动安装 Maven: https://maven.apache.org/download.cgi" -ForegroundColor Yellow
}

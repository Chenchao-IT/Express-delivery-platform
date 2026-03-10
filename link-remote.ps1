# 将当前项目链接到 GitHub 远程仓库
# 在项目根目录 (code) 下运行: .\link-remote.ps1

$remoteUrl = "https://github.com/Chenchao-IT/Express-delivery-platform.git"

if (-not (Test-Path ".git")) {
    Write-Host "正在初始化 Git 仓库..." -ForegroundColor Cyan
    git init
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

$existing = git remote get-url origin 2>$null
if ($existing) {
    Write-Host "当前 origin 远程: $existing" -ForegroundColor Yellow
    $overwrite = Read-Host "是否改为新地址? (y/N)"
    if ($overwrite -eq 'y' -or $overwrite -eq 'Y') {
        git remote remove origin
    } else {
        Write-Host "已保留现有 origin。" -ForegroundColor Green
        exit 0
    }
}

git remote add origin $remoteUrl
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "已添加远程仓库: $remoteUrl" -ForegroundColor Green
Write-Host ""
Write-Host "后续可执行:" -ForegroundColor Cyan
Write-Host "  git add ."
Write-Host "  git commit -m \"Initial commit\""
Write-Host "  git branch -M main"
Write-Host "  git push -u origin main"

#!/usr/bin/env bash
# 本仓库 Git 提交配置（在项目根目录执行: bash git-setup.sh）

set -e
cd "$(dirname "$0")"

echo "=== Git 提交配置 ==="

# 默认分支
git config init.defaultBranch main

# 提交时自动转换换行符（与 .gitattributes 配合）
git config core.autocrlf false

# 若未设置全局 user.name / user.email，则为本仓库设置
if [ -z "$(git config user.name)" ]; then
  read -p "请输入 Git 用户名（提交者姓名）: " name
  git config user.name "$name"
  echo "  已设置 user.name"
fi

if [ -z "$(git config user.email)" ]; then
  read -p "请输入 Git 邮箱（与 GitHub 一致）: " email
  git config user.email "$email"
  echo "  已设置 user.email"
fi

echo ""
echo "当前本仓库配置："
git config --list --local | grep -E "user\.|init\.defaultBranch|core\.autocrlf" || true
echo ""
echo "配置完成。可执行: git add . && git commit -m \"Initial commit\" && git push -u origin main"

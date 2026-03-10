# 校园快递配送系统

基于 Vue.js + Java Spring Boot + MySQL 技术栈实现的校园智能物流配送系统，符合设计规范与项目方案文档。

## 技术栈

- **前端**: Vue 3 + Vite + Tailwind CSS + Pinia + Vue Router
- **后端**: Spring Boot 3.2 + Spring Security + JPA
- **数据库**: MySQL 8.0

## 功能模块

- 用户认证（登录/注册/JWT）
- 学生：我的包裹、快递查询、自助取件
- 管理员/快递员：包裹入库、配送任务管理、A* 路径规划
- 用户管理（管理员）

## 环境要求

- **JDK 17**（必须是 JDK，不能只用 JRE）
- MySQL 8.0
- Node.js 18+

> 若出现 "No compiler" 或 "JAVA_HOME is not defined" 错误，请先设置环境变量后重启终端，或直接双击 `backend/set-java-and-run.bat` 启动。

**设置 JAVA_HOME（管理员 PowerShell，永久生效）：**
```powershell
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot", "User")
```
设置后需**关闭并重新打开**终端。

## 快速启动

### 1. 数据库准备

```sql
CREATE DATABASE campus_express DEFAULT CHARACTER SET utf8mb4;
```

执行 `database/schema.sql` 或依赖应用启动时的 JPA 自动建表。

### 2. 后端配置

编辑 `backend/src/main/resources/application.yml` 中的数据库连接：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/campus_express?...
    username: root
    password: 你的密码
```

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问 http://localhost:5173

### 5. 默认账号

- 管理员: `admin` / `admin123`

## Git 提交配置

首次提交前建议完成本仓库的 Git 配置（提交者姓名与邮箱）：

```bash
bash git-setup.sh
```

已包含内容：

- **`.gitignore`**：忽略 `backend/target/`、`frontend/node_modules/`、IDE 与本地敏感配置等
- **`.gitattributes`**：统一换行符（LF），避免跨平台差异
- **本仓库配置**：默认分支 `main`、`core.autocrlf false`；脚本会提示设置 `user.name` 与 `user.email`

若已配置全局 `user.name` / `user.email`，可直接提交并推送：

```bash
git add .
git commit -m "Initial commit"
git branch -M main
git push -u origin main
```

## 项目结构

```
code/
├── backend/           # Spring Boot 后端
├── frontend/          # Vue 3 前端
├── database/          # 数据库脚本
├── 设计规范.md
└── 校园快递配送系统项目方案文档.md
```

## 设计规范

前端 UI 遵循 `设计规范.md` 中的颜色、字体、组件、布局等规范。

## 核心实现（对应项目方案文档）

| 方案章节 | 实现位置 |
|---------|----------|
| A* 路径规划算法 | `backend/.../algorithm/CampusPathPlanningService.java` |
| 虚拟货架映射 | `backend/.../entity/VirtualShelf.java`，DataInitializer 初始化 |
| 上门配送流程 | `backend/.../service/DeliveryService.java` |
| 包裹入库/取件 | `backend/.../service/PackageService.java` |
| 数据库 E-R 设计 | `database/schema.sql` |

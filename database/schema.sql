-- 校园智能物流配送系统 - 数据库初始化脚本
-- 基于项目方案文档 E-R 设计

-- 创建数据库（若不存在）
CREATE DATABASE IF NOT EXISTS campus_express DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE campus_express;

-- 用户模块
CREATE TABLE IF NOT EXISTS `user` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    real_name VARCHAR(50) COMMENT '真实姓名',
    role ENUM('STUDENT', 'COURIER', 'ADMIN') NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(50),
    college VARCHAR(50) COMMENT '学院',
    address VARCHAR(200) COMMENT '收货地址',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_role (role),
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 包裹模块
CREATE TABLE IF NOT EXISTS `package` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tracking_number VARCHAR(50) UNIQUE NOT NULL,
    student_id BIGINT NOT NULL,
    courier_id BIGINT,
    size ENUM('SMALL', 'MEDIUM', 'LARGE') NOT NULL,
    status ENUM('IN_STORAGE', 'OUT_FOR_DELIVERY', 'DELIVERED', 'PICKED_UP', 'RETURNED', 'COMPLETED') NOT NULL DEFAULT 'IN_STORAGE',
    shelf_code VARCHAR(20) COMMENT '虚拟货架编码',
    storage_time DATETIME,
    estimated_delivery_time DATETIME,
    actual_delivery_time DATETIME,
    version INT DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES `user`(id),
    FOREIGN KEY (courier_id) REFERENCES `user`(id),
    INDEX idx_tracking (tracking_number),
    INDEX idx_student_status (student_id, status),
    INDEX idx_shelf (shelf_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='包裹表';

-- 配送任务模块
CREATE TABLE IF NOT EXISTS delivery_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    package_id BIGINT NOT NULL,
    courier_id BIGINT,
    destination VARCHAR(50) NOT NULL COMMENT '目标楼栋',
    path_json TEXT COMMENT 'A*算法计算的路径坐标',
    estimated_distance DECIMAL(8,2) COMMENT '预估距离(米)',
    estimated_time DECIMAL(8,2) COMMENT '预估时间(分钟)',
    actual_distance DECIMAL(8,2),
    actual_time DECIMAL(8,2),
    status ENUM('PENDING', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'PENDING',
    priority INT DEFAULT 1 COMMENT '配送优先级',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    started_at DATETIME,
    completed_at DATETIME,
    FOREIGN KEY (package_id) REFERENCES `package`(id),
    FOREIGN KEY (courier_id) REFERENCES `user`(id),
    INDEX idx_courier_status (courier_id, status),
    INDEX idx_priority (priority, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配送任务表';

-- 虚拟货架映射表
CREATE TABLE IF NOT EXISTS virtual_shelf_mapping (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    shelf_code VARCHAR(20) UNIQUE NOT NULL,
    zone CHAR(1) NOT NULL COMMENT '区域：A-Z',
    row_num INT NOT NULL COMMENT '排号',
    level_num INT NOT NULL COMMENT '层号',
    position_num INT NOT NULL COMMENT '位号',
    grid_x INT COMMENT '对应网格坐标X',
    grid_y INT COMMENT '对应网格坐标Y',
    capacity INT DEFAULT 1 COMMENT '容量',
    current_load INT DEFAULT 0 COMMENT '当前负载',
    status ENUM('AVAILABLE', 'FULL', 'MAINTENANCE') DEFAULT 'AVAILABLE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_zone_row (zone, row_num),
    INDEX idx_grid (grid_x, grid_y),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='虚拟货架坐标映射表';

-- 限流日志表（对应项目方案：滑动窗口流量控制）
CREATE TABLE IF NOT EXISTS rate_limit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    operation_type VARCHAR(50) NOT NULL,
    request_time DATETIME(3) NOT NULL,
    allowed BOOLEAN NOT NULL,
    current_count INT NOT NULL,
    limit_count INT NOT NULL,
    window_seconds INT NOT NULL,
    peak_hour_adjusted BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE SET NULL,
    INDEX idx_user_operation (user_id, operation_type, request_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='限流日志表';

-- 初始数据由应用启动时 DataInitializer 创建

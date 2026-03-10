-- 校园快递配送系统 - 测试用户数据（可选）
-- 完整种子数据（包裹、配送任务）由应用启动时 DataInitializer 自动创建
-- 仅当需要手动添加用户时执行此脚本

USE campus_express;

-- BCrypt 哈希 (123456, cost=10)
SET @pwd = '$2a$10$8K1p/a0dL1LXMIgoEDFrwOfMQeZzZrM/YrM/YrM/YrM/YrM/YrM';

INSERT IGNORE INTO `user` (username, password, real_name, role, phone, college, address) VALUES
('courier1', @pwd, '张师傅', 'COURIER', '13800001101', NULL, NULL),
('courier2', @pwd, '李师傅', 'COURIER', '13800001102', NULL, NULL),
('courier3', @pwd, '王师傅', 'COURIER', '13800001103', NULL, NULL),
('student1', @pwd, '张三', 'STUDENT', '13900001231', '计算机学院', '宿舍1号楼201室'),
('student2', @pwd, '李四', 'STUDENT', '13900001232', '信息学院', '宿舍2号楼302室'),
('student3', @pwd, '王五', 'STUDENT', '13900001233', '电子学院', '宿舍3号楼103室'),
('student4', @pwd, '赵六', 'STUDENT', '13900001234', '机械学院', '宿舍1号楼405室'),
('student5', @pwd, '钱七', 'STUDENT', '13900001235', '经管学院', '宿舍2号楼210室'),
('student6', @pwd, '孙八', 'STUDENT', '13900001236', '外国语学院', '宿舍3号楼315室'),
('student7', @pwd, '周九', 'STUDENT', '13900001237', '计算机学院', '宿舍1号楼108室'),
('student8', @pwd, '吴十', 'STUDENT', '13900001238', '信息学院', '宿舍2号楼220室');

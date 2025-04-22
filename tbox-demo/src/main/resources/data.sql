-- 清空现有数据
TRUNCATE TABLE users;

-- 插入测试数据
INSERT INTO users (username, email, phone, create_time) VALUES
('user1', 'user1@example.com', '13800000001', NOW()),
('user2', 'user2@example.com', '13800000002', NOW()),
('user3', 'user3@example.com', '13800000003', NOW()),
('user4', 'user4@example.com', '13800000004', NOW()),
('user5', 'user5@example.com', '13800000005', NOW()),
('user6', 'user6@example.com', '13800000006', NOW()),
('user7', 'user7@example.com', '13800000007', NOW()),
('user8', 'user8@example.com', '13800000008', NOW()),
('user9', 'user9@example.com', '13800000009', NOW()),
('user10', 'user10@example.com', '13800000010', NOW()),
('user11', 'user11@example.com', '13800000011', NOW()),
('user12', 'user12@example.com', '13800000012', NOW()); 
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    create_time TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4; 
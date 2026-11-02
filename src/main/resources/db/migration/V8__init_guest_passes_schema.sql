CREATE TABLE guest_passes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    created_by_user_id INT NOT NULL,
    guest_name VARCHAR(150) NOT NULL,
    uid_link VARCHAR(255) NOT NULL UNIQUE,
    valid_from DATETIME(6) NOT NULL,
    valid_to DATETIME(6) NOT NULL,
    is_activated BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    FOREIGN KEY (created_by_user_id) REFERENCES users(id) ON DELETE CASCADE
)
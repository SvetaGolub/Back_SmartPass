CREATE TABLE access_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    checkpoint_id INT NOT NULL,
    user_id INT NULL,
    guest_pass_id INT NULL,
    direction VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    deny_reason VARCHAR(255) NULL,
    scanned_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    FOREIGN KEY (checkpoint_id) REFERENCES checkpoints(id) ON DELETE RESTRICT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (guest_pass_id) REFERENCES guest_passes(id) ON DELETE SET NULL
)
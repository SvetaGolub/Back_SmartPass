CREATE TABLE employee_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id INT NOT NULL,
    author VARCHAR(50) NOT NULL,
    message_text VARCHAR(500) NOT NULL,
    sent_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_employee_messages_user
        FOREIGN KEY (employee_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_employee_messages_employee_sent (employee_id, sent_at, id)
);

CREATE TABLE meetings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    meeting_date DATE NOT NULL,
    meeting_time TIME NOT NULL,
    note VARCHAR(1000),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_meetings_date_time (meeting_date, meeting_time, id)
);

CREATE TABLE attendance_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    attendance_date DATE NOT NULL,
    entry_time TIME NOT NULL,
    exit_time TIME,
    CONSTRAINT fk_attendance_records_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_attendance_exit_after_entry
        CHECK (exit_time IS NULL OR exit_time > entry_time),
    UNIQUE KEY uk_attendance_user_date (user_id, attendance_date),
    INDEX idx_attendance_date_user (attendance_date, user_id)
);

-- MySQL용 기본 테이블 생성 스크립트 (이미 있으면 영향 없음)

CREATE TABLE IF NOT EXISTS folder (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME,
    CONSTRAINT fk_folder_user FOREIGN KEY (user_id) REFERENCES site_user(id)
);

CREATE TABLE IF NOT EXISTS document_file (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    uploaded_at DATETIME NOT NULL,
    user_id BIGINT,
    folder_id BIGINT,
    extracted_text LONGTEXT,
    CONSTRAINT fk_document_user FOREIGN KEY (user_id) REFERENCES site_user(id),
    CONSTRAINT fk_document_folder FOREIGN KEY (folder_id) REFERENCES folder(id)
);

CREATE TABLE IF NOT EXISTS quiz_question (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    document_id BIGINT,
    folder_id BIGINT,
    number_tag INT,
    question_text TEXT,
    choices TEXT,
    multiple_choice BIT,
    answer TEXT,
    explanation TEXT,
    solved BIT,
    correct BIT,
    created_at DATETIME,
    CONSTRAINT fk_quiz_user FOREIGN KEY (user_id) REFERENCES site_user(id),
    CONSTRAINT fk_quiz_document FOREIGN KEY (document_id) REFERENCES document_file(id),
    CONSTRAINT fk_quiz_folder FOREIGN KEY (folder_id) REFERENCES folder(id)
);

CREATE TABLE IF NOT EXISTS study_group (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    join_code VARCHAR(16) NOT NULL UNIQUE,
    owner_id BIGINT NOT NULL,
    created_at DATETIME,
    CONSTRAINT fk_group_owner FOREIGN KEY (owner_id) REFERENCES site_user(id)
);

CREATE TABLE IF NOT EXISTS group_member (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(32) NOT NULL,
    joined_at DATETIME,
    CONSTRAINT fk_group_member_group FOREIGN KEY (group_id) REFERENCES study_group(id),
    CONSTRAINT fk_group_member_user FOREIGN KEY (user_id) REFERENCES site_user(id)
);

CREATE TABLE IF NOT EXISTS group_shared_question (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    shared_by BIGINT NOT NULL,
    shared_at DATETIME,
    CONSTRAINT fk_group_shared_group FOREIGN KEY (group_id) REFERENCES study_group(id),
    CONSTRAINT fk_group_shared_question FOREIGN KEY (question_id) REFERENCES quiz_question(id),
    CONSTRAINT fk_group_shared_user FOREIGN KEY (shared_by) REFERENCES site_user(id)
);

CREATE TABLE IF NOT EXISTS pending_notification (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    due_at DATETIME NOT NULL,
    delivered BIT NOT NULL DEFAULT 0,
    created_at DATETIME,
    CONSTRAINT fk_pending_user FOREIGN KEY (user_id) REFERENCES site_user(id),
    CONSTRAINT fk_pending_question FOREIGN KEY (question_id) REFERENCES quiz_question(id)
);

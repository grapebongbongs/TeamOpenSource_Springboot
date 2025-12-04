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

CREATE TABLE IF NOT EXISTS group_invite (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    from_user_id BIGINT NOT NULL,
    to_user_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at DATETIME,
    CONSTRAINT fk_group_invite_group FOREIGN KEY (group_id) REFERENCES study_group(id),
    CONSTRAINT fk_group_invite_from FOREIGN KEY (from_user_id) REFERENCES site_user(id),
    CONSTRAINT fk_group_invite_to FOREIGN KEY (to_user_id) REFERENCES site_user(id)
);

-- MySQL 5.7에서는 ADD COLUMN IF NOT EXISTS 구문을 지원하지 않으므로
-- INFORMATION_SCHEMA를 조회해 없을 때만 동적으로 컬럼을 추가한다.
SET @sql_last_solved_date := IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'site_user' AND COLUMN_NAME = 'last_solved_date') = 0,
    'ALTER TABLE site_user ADD COLUMN last_solved_date DATE',
    'SELECT 1'
);
PREPARE stmt FROM @sql_last_solved_date;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql_avatar := IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'site_user' AND COLUMN_NAME = 'avatar') = 0,
    'ALTER TABLE site_user ADD COLUMN avatar VARCHAR(16)',
    'SELECT 1'
);
PREPARE stmt FROM @sql_avatar;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql_banner := IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'site_user' AND COLUMN_NAME = 'banner') = 0,
    'ALTER TABLE site_user ADD COLUMN banner VARCHAR(32)',
    'SELECT 1'
);
PREPARE stmt FROM @sql_banner;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql_avatar_owned := IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'site_user' AND COLUMN_NAME = 'purchased_avatars') = 0,
    'ALTER TABLE site_user ADD COLUMN purchased_avatars TEXT',
    'SELECT 1'
);
PREPARE stmt FROM @sql_avatar_owned;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql_banner_owned := IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'site_user' AND COLUMN_NAME = 'purchased_banners') = 0,
    'ALTER TABLE site_user ADD COLUMN purchased_banners TEXT',
    'SELECT 1'
);
PREPARE stmt FROM @sql_banner_owned;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql_badges_owned := IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'site_user' AND COLUMN_NAME = 'purchased_badges') = 0,
    'ALTER TABLE site_user ADD COLUMN purchased_badges TEXT',
    'SELECT 1'
);
PREPARE stmt FROM @sql_badges_owned;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql_active_badge := IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'site_user' AND COLUMN_NAME = 'active_badge') = 0,
    'ALTER TABLE site_user ADD COLUMN active_badge VARCHAR(64)',
    'SELECT 1'
);
PREPARE stmt FROM @sql_active_badge;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql_shield := IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'site_user' AND COLUMN_NAME = 'shield_items') = 0,
    'ALTER TABLE site_user ADD COLUMN shield_items INT NOT NULL DEFAULT 0',
    'SELECT 1'
);
PREPARE stmt FROM @sql_shield;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql_extra_tokens := IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'site_user' AND COLUMN_NAME = 'extra_problem_tokens') = 0,
    'ALTER TABLE site_user ADD COLUMN extra_problem_tokens INT NOT NULL DEFAULT 0',
    'SELECT 1'
);
PREPARE stmt FROM @sql_extra_tokens;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql_goal := IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'site_user' AND COLUMN_NAME = 'daily_goal_minutes') = 0,
    'ALTER TABLE site_user ADD COLUMN daily_goal_minutes INT NOT NULL DEFAULT 60',
    'SELECT 1'
);
PREPARE stmt FROM @sql_goal;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS question_discussion (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME,
    CONSTRAINT fk_question_discussion_question FOREIGN KEY (question_id) REFERENCES quiz_question(id),
    CONSTRAINT fk_question_discussion_user FOREIGN KEY (user_id) REFERENCES site_user(id)
);

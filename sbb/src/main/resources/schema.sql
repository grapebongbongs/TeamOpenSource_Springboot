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

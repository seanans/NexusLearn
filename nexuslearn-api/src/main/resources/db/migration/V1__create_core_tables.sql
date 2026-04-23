-- V1__create_core_tables.sql

CREATE TABLE users (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       email VARCHAR(255) UNIQUE NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       first_name VARCHAR(100) NOT NULL,
                       last_name VARCHAR(100) NOT NULL,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE courses (
                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         title VARCHAR(255) NOT NULL,
                         description TEXT,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE course_members (
                                user_id UUID REFERENCES users(id) ON DELETE CASCADE,
                                course_id UUID REFERENCES courses(id) ON DELETE CASCADE,
                                role VARCHAR(50) NOT NULL,
                                joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                PRIMARY KEY (user_id, course_id)
);
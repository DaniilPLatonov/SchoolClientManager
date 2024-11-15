-- Очистка данных
TRUNCATE TABLE user_subjects RESTART IDENTITY CASCADE;
TRUNCATE TABLE tutors RESTART IDENTITY CASCADE;
TRUNCATE TABLE subjects RESTART IDENTITY CASCADE;
TRUNCATE TABLE users RESTART IDENTITY CASCADE;


INSERT INTO users (id, name, email, password) VALUES
                                                  ('11111111-1111-1111-1111-111111111111', 'John Doe', 'john.doe@example.com', 'password123'),
                                                  ('22222222-2222-2222-2222-222222222222', 'Jane Smith', 'jane.smith@example.com', 'password456'),
                                                  ('33333333-3333-3333-3333-333333333333', 'Alice Brown', 'alice.brown@example.com', 'password789');


INSERT INTO subjects (id, name) VALUES
                                    ('44444444-4444-4444-4444-444444444444', 'Mathematics'),
                                    ('55555555-5555-5555-5555-555555555555', 'Physics'),
                                    ('66666666-6666-6666-6666-666666666666', 'Chemistry');


INSERT INTO tutors (id, name, subject_id) VALUES
                                              ('77777777-7777-7777-7777-777777777777', 'Dr. Alan Turing', '44444444-4444-4444-4444-444444444444'),
                                              ('88888888-8888-8888-8888-888888888888', 'Dr. Marie Curie', '55555555-5555-5555-5555-555555555555'),
                                              ('99999999-9999-9999-9999-999999999999', 'Dr. Albert Einstein', '66666666-6666-6666-6666-666666666666');


INSERT INTO user_subjects (user_id, subject_id) VALUES
                                                    ('11111111-1111-1111-1111-111111111111', '44444444-4444-4444-4444-444444444444'),
                                                    ('22222222-2222-2222-2222-222222222222', '55555555-5555-5555-5555-555555555555'),
                                                    ('33333333-3333-3333-3333-333333333333', '66666666-6666-6666-6666-666666666666');

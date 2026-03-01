CREATE DATABASE IF NOT EXISTS authdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Optionnel: créer un utilisateur dédié
-- CREATE USER IF NOT EXISTS 'auth_user'@'localhost' IDENTIFIED BY 'auth_password';
-- GRANT ALL PRIVILEGES ON authdb.* TO 'auth_user'@'localhost';
-- FLUSH PRIVILEGES;

USE authdb;

-- Les tables sont créées automatiquement par Spring (hibernate ddl-auto=update)
-- Les comptes de test sont créés automatiquement au démarrage de l'app:
-- user / password123
-- admin / admin123

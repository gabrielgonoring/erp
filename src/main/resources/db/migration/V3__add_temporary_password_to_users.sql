-- Adiciona flag para indicar se a senha do usuário é temporária
ALTER TABLE users ADD COLUMN temporary_password BOOLEAN NOT NULL DEFAULT FALSE;

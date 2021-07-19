CREATE OR REPLACE USER 'shamir'@'localhost' IDENTIFIED BY 'Msiw47Ut129';
CREATE OR REPLACE USER 'shamir'@'172.17.0.1' IDENTIFIED BY 'Msiw47Ut129';
CREATE OR REPLACE DATABASE shamirs_db;
GRANT ALL ON shamirs_db.* TO 'shamir'@'localhost';
GRANT ALL ON shamirs_db.* TO 'shamir'@'172.17.0.1';
GRANT FILE ON *.* TO 'shamir'@'localhost';

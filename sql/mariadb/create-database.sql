SET @shamirs_pw="'Msiw47Ut129'";

SET @stmt=CONCAT("CREATE OR REPLACE USER 'shamir'@'localhost' IDENTIFIED BY ", @shamirs_pw, ";");
PREPARE create_user FROM @stmt;
EXECUTE create_user;
DEALLOCATE PREPARE create_user;

SET @stmt=CONCAT("CREATE OR REPLACE USER 'shamir'@'172.17.0.1' IDENTIFIED BY ", @shamirs_pw, ";");
PREPARE create_user FROM @stmt;
EXECUTE create_user;
DEALLOCATE PREPARE create_user;

CREATE OR REPLACE DATABASE shamirs_db;
GRANT ALL ON shamirs_db.* TO 'shamir'@'localhost';
GRANT ALL ON shamirs_db.* TO 'shamir'@'172.17.0.1';
GRANT FILE ON *.* TO 'shamir'@'localhost';

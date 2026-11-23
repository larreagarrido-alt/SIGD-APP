-- Incremental update V014 only. Review backup and previous migration versions before applying.
-- Solo WebSite+. Mantiene PASSWORD y BIOMETRIA; FACIAL no autoriza operaciones V011.
ALTER TABLE wp_sesiones MODIFY COLUMN metodo ENUM('PASSWORD','BIOMETRIA','FACIAL') NOT NULL;
INSERT IGNORE INTO wp_migraciones(version,descripcion) VALUES('V014','Sesiones FACIAL con identidad 1:1 y PAD local firmado; no PAD de servidor');

UPDATE wp_migraciones SET checksum='f510d1912ac76f34ca0e60c64ea799d57065cbd3cd045227cf124148156acf2a' WHERE version='V014';

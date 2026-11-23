-- Solo WebSite+. Mantiene PASSWORD y BIOMETRIA; FACIAL no autoriza operaciones V011.
ALTER TABLE wp_sesiones MODIFY COLUMN metodo ENUM('PASSWORD','BIOMETRIA','FACIAL') NOT NULL;
INSERT IGNORE INTO wp_migraciones(version,descripcion) VALUES('V014','Sesiones FACIAL con identidad 1:1 y PAD local firmado; no PAD de servidor');

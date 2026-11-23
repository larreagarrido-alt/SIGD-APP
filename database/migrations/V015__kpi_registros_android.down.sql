-- Manual rollback after exporting and reviewing rows created since V015.
-- Confirm the target name is unused. This preserves all captured data.
RENAME TABLE `wp_kpi_registros_android` TO `wp_kpi_registros_android_rollback_v015`;

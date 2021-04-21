SELECT k.id AS keystore_id, k.descriptive_name, k.modification_time, k.current_partition_id, k.version, s.id AS session_id, s.phase, s.idle_time, s.modification_time, s.expiration_time, s.version
FROM keystore k
LEFT JOIN csession s ON k.id = s.keystore_id
ORDER BY k.id, s.modification_time;
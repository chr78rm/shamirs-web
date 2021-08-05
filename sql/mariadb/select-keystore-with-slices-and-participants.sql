/**
 * Author:  Christof Reichardt
 * Created: 29.10.2020
 */

SELECT k.id AS keystore_id, k.descriptive_name, k.current_partition_id, k.creation_time, k.modification_time, s.id AS slice_id, s.processing_state, s.modification_time, s.partition_id, s.amount, p.id AS participant_id, p.preferred_name, p.`creation_time`
FROM keystore k
LEFT JOIN slice s ON s.keystore_id = k.id
LEFT JOIN participant p ON p.id = s.participant_id
ORDER BY k.id, s.creation_time;


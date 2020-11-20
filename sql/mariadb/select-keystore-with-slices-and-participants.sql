/**
 * Author:  Christof Reichardt
 * Created: 29.10.2020
 */

SELECT k.id, k.descriptive_name, k.creation_time, k.modification_time, s.id, s.processing_state, s.effective_time, p.id, p.preferred_name, p.effective_time
FROM keystore k
LEFT JOIN slice s ON s.keystore_id = k.id
LEFT JOIN participant p ON p.id = s.participant_id
WHERE k.descriptive_name = 'my-posted-keystore';


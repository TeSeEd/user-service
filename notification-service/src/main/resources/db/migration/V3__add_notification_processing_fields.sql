alter table notifications
    add column processing_started_at timestamptz,
    add column claim_token uuid;

create index idx_notifications_ready
    on notifications (created_at)
    where status = 'PENDING';

create index idx_notifications_failed
    on notifications (next_retry_at)
    where status = 'FAILED';

create index idx_notifications_processing
    on notifications (processing_started_at)
    where status = 'PROCESSING';
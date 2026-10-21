create table outbox_events (
    id uuid primary key,

    event_type varchar(30) not null,
    user_id bigint not null,
    email varchar(255) not null,

    created_at timestamp with time zone not null,
    schema_version integer not null,

    status varchar(30) not null,
    attempt_count integer not null default 0,

    next_retry_at timestamp with time zone,
    published_at timestamp with time zone,
    processing_started_at timestamp with time zone,

    last_error text,

    constraint chk_outbox_event_type
        check (event_type in ('USER_CREATED', 'USER_DELETED')),

    constraint chk_outbox_status
        check (status in ('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED')),

    constraint chk_outbox_attempt_count
        check (attempt_count >= 0),

    constraint chk_outbox_schema_version
        check (schema_version > 0)
);

create index idx_outbox_pending
    on outbox_events (created_at)
    where status = 'PENDING';

create index idx_outbox_failed_retry
    on outbox_events (next_retry_at)
    where status = 'FAILED';

create index idx_outbox_processing
    on outbox_events (processing_started_at)
    where status = 'PROCESSING';
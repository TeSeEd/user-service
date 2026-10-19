create table notifications(
    id bigserial primary key,
    event_id uuid unique not null,
    recipient_email varchar(255) not null,
    event_type varchar(30) not null check(event_type in('USER_CREATED', 'USER_DELETED')),
    status varchar(20) not null check(status in('PENDING', 'PROCESSING','SENT','FAILED')) default 'PENDING',
    attempt_count integer default 0 check (attempt_count >= 0) not null,
    created_at timestamptz not null default current_timestamp,
    sent_at timestamptz,
    next_retry_at timestamptz,
    last_error text
);
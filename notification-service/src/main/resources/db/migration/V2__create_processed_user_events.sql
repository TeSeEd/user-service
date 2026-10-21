create table processed_user_events (
    event_id uuid primary key,
    processed_at timestamptz not null default now()
);
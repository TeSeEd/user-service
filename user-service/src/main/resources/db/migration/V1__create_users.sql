create table users (
    id bigserial primary key,
    "name" varchar(255),
    email varchar(255),
    age integer,
    created_at timestamp(6) without time zone
);
create table users
(
    id  uuid default gen_random_uuid() not null
        constraint users_pk
            primary key,
    firstname varchar(20)                    not null,
    surname   varchar(30)                    not null,
    email     varchar(40)                    not null
);


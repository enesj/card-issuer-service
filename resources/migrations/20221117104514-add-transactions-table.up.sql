create type transaction_type as enum ('deposit', 'withdrawal', 'transfer');
--;;
create table transactions
(
    id uuid default gen_random_uuid(),
    type transaction_type not null default 'withdrawal',
    user_id uuid not null,
    receiver_id uuid,
    currency_code varchar(3) not null,
    amount numeric(10,2) not null,
    description varchar(100),
    created_at timestamp default now(),
    constraint transaction_pk primary key (id),
    constraint transaction_user_fk foreign key (user_id) references users (id),
    constraint transaction_receiver_fk foreign key (receiver_id) references users (id),
    constraint transaction_currency_fk foreign key (currency_code) references currency (code)
);

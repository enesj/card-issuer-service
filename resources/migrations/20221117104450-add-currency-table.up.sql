create table currency
(
    name varchar(20) not null,
    code varchar(3) not null,
    constraint currency_pk primary key (code)
);

insert into transactions (user_id, currency_code, amount, type, description)
    select id,  'USD', 100, 'deposit', 'Initial transaction' from users;
--;;

insert into transactions (user_id, currency_code, amount, type, description)
    select id,  'EUR', 100, 'deposit', 'Initial transaction' from users;

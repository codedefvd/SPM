create table if not exists users (
    id bigint primary key auto_increment,
    username varchar(64) not null unique,
    password varchar(128) not null,
    full_name varchar(128) not null,
    student_no varchar(64) unique,
    phone varchar(32),
    role varchar(32) not null,
    enabled boolean not null default true,
    created_at datetime not null,
    updated_at datetime not null,
    deleted boolean not null default false
);

create table if not exists categories (
    id bigint primary key auto_increment,
    code varchar(64) not null unique,
    name varchar(128) not null,
    enabled boolean not null default true,
    created_at datetime not null,
    updated_at datetime not null,
    deleted boolean not null default false
);

create table if not exists books (
    id bigint primary key auto_increment,
    title varchar(255) not null,
    author varchar(128) not null,
    isbn varchar(64) not null unique,
    publisher varchar(128),
    description varchar(2000),
    category_id bigint,
    shelf_status varchar(32) not null,
    created_at datetime not null,
    updated_at datetime not null,
    deleted boolean not null default false,
    constraint fk_books_category foreign key (category_id) references categories(id)
);

create table if not exists inventory (
    id bigint primary key auto_increment,
    book_id bigint not null unique,
    total_copies int not null,
    available_copies int not null,
    created_at datetime not null,
    updated_at datetime not null,
    deleted boolean not null default false,
    constraint fk_inventory_book foreign key (book_id) references books(id)
);

create table if not exists borrow_requests (
    id bigint primary key auto_increment,
    reader_id bigint not null,
    book_id bigint not null,
    status varchar(32) not null,
    request_note varchar(255),
    reject_reason varchar(255),
    processed_by bigint,
    processed_at datetime,
    created_at datetime not null,
    updated_at datetime not null,
    deleted boolean not null default false,
    constraint fk_borrow_request_reader foreign key (reader_id) references users(id),
    constraint fk_borrow_request_book foreign key (book_id) references books(id),
    constraint fk_borrow_request_operator foreign key (processed_by) references users(id)
);

create table if not exists borrow_records (
    id bigint primary key auto_increment,
    reader_id bigint not null,
    book_id bigint not null,
    borrow_request_id bigint,
    status varchar(32) not null,
    borrow_date date not null,
    due_date date not null,
    return_date date,
    created_at datetime not null,
    updated_at datetime not null,
    deleted boolean not null default false,
    constraint fk_borrow_record_reader foreign key (reader_id) references users(id),
    constraint fk_borrow_record_book foreign key (book_id) references books(id),
    constraint fk_borrow_record_request foreign key (borrow_request_id) references borrow_requests(id)
);

create table if not exists operation_logs (
    id bigint primary key auto_increment,
    module_name varchar(64) not null,
    action_name varchar(64) not null,
    operator_name varchar(64) not null,
    result_message varchar(1000),
    created_at datetime not null,
    updated_at datetime not null,
    deleted boolean not null default false
);

create table if not exists reservations (
    id bigint primary key auto_increment,
    reader_id bigint not null,
    book_id bigint not null,
    status varchar(32) not null,
    queue_no int not null,
    created_at datetime not null,
    updated_at datetime not null,
    deleted boolean not null default false,
    constraint fk_reservation_reader foreign key (reader_id) references users(id),
    constraint fk_reservation_book foreign key (book_id) references books(id)
);

create table if not exists fines (
    id bigint primary key auto_increment,
    reader_id bigint not null,
    borrow_record_id bigint not null,
    amount decimal(10,2) not null,
    status varchar(32) not null,
    created_at datetime not null,
    updated_at datetime not null,
    deleted boolean not null default false,
    constraint fk_fine_reader foreign key (reader_id) references users(id),
    constraint fk_fine_borrow_record foreign key (borrow_record_id) references borrow_records(id)
);

create table if not exists data_dictionaries (
    id bigint primary key auto_increment,
    dict_type varchar(64) not null,
    dict_code varchar(64) not null,
    dict_name varchar(128) not null,
    business_value varchar(255),
    sort_order int not null default 0,
    enabled boolean not null default true,
    description varchar(500),
    created_at datetime not null,
    updated_at datetime not null,
    deleted boolean not null default false,
    unique key uk_dictionary_type_code_deleted (dict_type, dict_code, deleted)
);

insert into categories(code, name, enabled, created_at, updated_at, deleted)
select 'CS', 'Computer Science', true, now(), now(), false
where not exists (select 1 from categories where code = 'CS');

insert into categories(code, name, enabled, created_at, updated_at, deleted)
select 'LIT', 'Literature', true, now(), now(), false
where not exists (select 1 from categories where code = 'LIT');

insert into data_dictionaries(dict_type, dict_code, dict_name, business_value, sort_order, enabled, description, created_at, updated_at, deleted)
select 'CATEGORY', 'CS', 'Computer Science', 'BOOK_CATEGORY', 10, true, 'Seeded category dictionary entry.', now(), now(), false
where not exists (
    select 1 from data_dictionaries where dict_type = 'CATEGORY' and dict_code = 'CS' and deleted = false
);

insert into data_dictionaries(dict_type, dict_code, dict_name, business_value, sort_order, enabled, description, created_at, updated_at, deleted)
select 'CATEGORY', 'LIT', 'Literature', 'BOOK_CATEGORY', 20, true, 'Seeded category dictionary entry.', now(), now(), false
where not exists (
    select 1 from data_dictionaries where dict_type = 'CATEGORY' and dict_code = 'LIT' and deleted = false
);

insert into data_dictionaries(dict_type, dict_code, dict_name, business_value, sort_order, enabled, description, created_at, updated_at, deleted)
select 'STATUS', 'ON_SHELF', 'On Shelf', 'BOOK_SHELF_STATUS', 10, true, 'Book can be discovered and borrowed.', now(), now(), false
where not exists (
    select 1 from data_dictionaries where dict_type = 'STATUS' and dict_code = 'ON_SHELF' and deleted = false
);

insert into data_dictionaries(dict_type, dict_code, dict_name, business_value, sort_order, enabled, description, created_at, updated_at, deleted)
select 'STATUS', 'OFF_SHELF', 'Off Shelf', 'BOOK_SHELF_STATUS', 20, true, 'Book cannot be borrowed temporarily.', now(), now(), false
where not exists (
    select 1 from data_dictionaries where dict_type = 'STATUS' and dict_code = 'OFF_SHELF' and deleted = false
);

insert into data_dictionaries(dict_type, dict_code, dict_name, business_value, sort_order, enabled, description, created_at, updated_at, deleted)
select 'STATUS', 'PENDING', 'Pending', 'BORROW_REQUEST_STATUS', 30, true, 'Request is waiting for processing.', now(), now(), false
where not exists (
    select 1 from data_dictionaries where dict_type = 'STATUS' and dict_code = 'PENDING' and deleted = false
);

insert into data_dictionaries(dict_type, dict_code, dict_name, business_value, sort_order, enabled, description, created_at, updated_at, deleted)
select 'STATUS', 'APPROVED', 'Approved', 'BORROW_REQUEST_STATUS', 40, true, 'Request is approved.', now(), now(), false
where not exists (
    select 1 from data_dictionaries where dict_type = 'STATUS' and dict_code = 'APPROVED' and deleted = false
);

insert into data_dictionaries(dict_type, dict_code, dict_name, business_value, sort_order, enabled, description, created_at, updated_at, deleted)
select 'STATUS', 'REJECTED', 'Rejected', 'BORROW_REQUEST_STATUS', 50, true, 'Request is rejected.', now(), now(), false
where not exists (
    select 1 from data_dictionaries where dict_type = 'STATUS' and dict_code = 'REJECTED' and deleted = false
);

insert into data_dictionaries(dict_type, dict_code, dict_name, business_value, sort_order, enabled, description, created_at, updated_at, deleted)
select 'BUSINESS_CODE', 'FINE_OVERDUE_DAILY', 'Overdue Fine Per Day', '1.50', 10, true, 'Default overdue fine amount per day.', now(), now(), false
where not exists (
    select 1 from data_dictionaries where dict_type = 'BUSINESS_CODE' and dict_code = 'FINE_OVERDUE_DAILY' and deleted = false
);

insert into data_dictionaries(dict_type, dict_code, dict_name, business_value, sort_order, enabled, description, created_at, updated_at, deleted)
select 'BUSINESS_CODE', 'RESERVATION_EXPIRE_DAYS', 'Reservation Expire Days', '3', 20, true, 'How many days a reservation is kept before expiration.', now(), now(), false
where not exists (
    select 1 from data_dictionaries where dict_type = 'BUSINESS_CODE' and dict_code = 'RESERVATION_EXPIRE_DAYS' and deleted = false
);

insert into users(username, password, full_name, student_no, phone, role, enabled, created_at, updated_at, deleted)
select 'reader', '123456', 'Demo Reader', 'R2026001', '13800000001', 'READER', true, now(), now(), false
where not exists (select 1 from users where username = 'reader');

insert into users(username, password, full_name, student_no, phone, role, enabled, created_at, updated_at, deleted)
select 'librarian', '123456', 'Demo Librarian', 'L2026001', '13800000002', 'LIBRARIAN', true, now(), now(), false
where not exists (select 1 from users where username = 'librarian');

insert into users(username, password, full_name, student_no, phone, role, enabled, created_at, updated_at, deleted)
select 'admin', '123456', 'Demo Admin', 'A2026001', '13800000003', 'ADMIN', true, now(), now(), false
where not exists (select 1 from users where username = 'admin');

insert into books(title, author, isbn, publisher, description, category_id, shelf_status, created_at, updated_at, deleted)
select 'Clean Architecture', 'Robert C. Martin', 'DEMO-ISBN-001', 'Prentice Hall', 'Seeded demo book for borrow workflow.', c.id, 'ON_SHELF', now(), now(), false
from categories c
where c.code = 'CS'
  and not exists (select 1 from books where isbn = 'DEMO-ISBN-001');

insert into inventory(book_id, total_copies, available_copies, created_at, updated_at, deleted)
select b.id, 5, 5, now(), now(), false
from books b
where b.isbn = 'DEMO-ISBN-001'
  and not exists (select 1 from inventory where book_id = b.id);

insert into reservations(reader_id, book_id, status, queue_no, created_at, updated_at, deleted)
select u.id, b.id, 'PENDING', 1, now(), now(), false
from users u
join books b on b.isbn = 'DEMO-ISBN-001'
where u.username = 'reader'
  and not exists (
      select 1 from reservations r
      where r.reader_id = u.id and r.book_id = b.id and r.status = 'PENDING' and r.deleted = false
  );

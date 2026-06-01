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
    barcode varchar(64) unique,
    publisher varchar(128),
    description varchar(2000),
    category_id bigint,
    shelf_status varchar(32) not null,
    created_at datetime not null,
    updated_at datetime not null,
    deleted boolean not null default false,
    constraint fk_books_category foreign key (category_id) references categories(id)
);

alter table books add column barcode varchar(64) unique after isbn;
alter table books add column thumbnail_url varchar(1024);
alter table books add column published_date varchar(32);

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
    book_copy_id bigint,
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

alter table borrow_records add column book_copy_id bigint after borrow_request_id;

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

create table if not exists role_permissions (
    id bigint primary key auto_increment,
    role varchar(32) not null unique,
    permission_scope varchar(2000) not null,
    created_at datetime not null,
    updated_at datetime not null,
    deleted boolean not null default false
);

create table if not exists system_configs (
    id bigint primary key auto_increment,
    config_key varchar(128) not null unique,
    config_value varchar(255) not null,
    description varchar(255),
    created_at datetime not null,
    updated_at datetime not null,
    deleted boolean not null default false
);

create table if not exists status_codes (
    id bigint primary key auto_increment,
    code_type varchar(64) not null,
    code_value varchar(64) not null,
    display_name varchar(128) not null,
    description varchar(255),
    enabled boolean not null default true,
    created_at datetime not null,
    updated_at datetime not null,
    deleted boolean not null default false,
    constraint uq_status_code unique (code_type, code_value)
);

create table if not exists backup_records (
    id bigint primary key auto_increment,
    backup_name varchar(128) not null,
    file_path varchar(512) not null,
    status varchar(32) not null,
    summary varchar(2000),
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
    constraint fk_fine_borrow_record foreign key (borrow_record_id) references borrow_records(id),
    constraint uq_fine_borrow_record unique (borrow_record_id)
);

create table if not exists book_favorites (
    id bigint primary key auto_increment,
    reader_id bigint not null,
    book_id bigint not null,
    created_at datetime not null,
    updated_at datetime not null,
    deleted boolean not null default false,
    constraint uq_book_favorite unique (reader_id, book_id),
    constraint fk_book_favorite_reader foreign key (reader_id) references users(id),
    constraint fk_book_favorite_book foreign key (book_id) references books(id)
);

create table if not exists book_reviews (
    id bigint primary key auto_increment,
    reader_id bigint not null,
    book_id bigint not null,
    rating_score int not null,
    review_content varchar(2000) not null,
    created_at datetime not null,
    updated_at datetime not null,
    deleted boolean not null default false,
    constraint fk_book_review_reader foreign key (reader_id) references users(id),
    constraint fk_book_review_book foreign key (book_id) references books(id)
);

alter table book_reviews drop index uq_book_review;

alter table books add column storage_location varchar(255);
alter table book_copies add column storage_location varchar(255);
alter table borrow_records add column renewal_count int not null default 0;

create table if not exists review_likes (
    id bigint primary key auto_increment,
    review_id bigint not null,
    reader_id bigint not null,
    created_at datetime not null,
    constraint uq_review_like unique (review_id, reader_id),
    constraint fk_review_like_review foreign key (review_id) references book_reviews(id),
    constraint fk_review_like_reader foreign key (reader_id) references users(id)
);

create table if not exists review_replies (
    id bigint primary key auto_increment,
    review_id bigint not null,
    reader_id bigint not null,
    reply_content varchar(1000) not null,
    created_at datetime not null,
    updated_at datetime not null,
    deleted boolean not null default false,
    constraint fk_review_reply_review foreign key (review_id) references book_reviews(id),
    constraint fk_review_reply_reader foreign key (reader_id) references users(id)
);

create table if not exists feedback_conversations (
    id bigint primary key auto_increment,
    reader_id bigint not null,
    subject varchar(255) not null,
    status varchar(32) not null,
    created_at datetime not null,
    updated_at datetime not null,
    deleted boolean not null default false,
    constraint fk_feedback_conv_reader foreign key (reader_id) references users(id)
);

create table if not exists feedback_messages (
    id bigint primary key auto_increment,
    conversation_id bigint not null,
    sender_id bigint not null,
    content varchar(2000) not null,
    read_at datetime,
    created_at datetime not null,
    updated_at datetime not null,
    deleted boolean not null default false,
    constraint fk_feedback_msg_conv foreign key (conversation_id) references feedback_conversations(id),
    constraint fk_feedback_msg_sender foreign key (sender_id) references users(id)
);

create table if not exists book_copies (
    id bigint primary key auto_increment,
    book_id bigint not null,
    copy_no int not null,
    barcode varchar(64) not null unique,
    created_at datetime not null,
    updated_at datetime not null,
    deleted boolean not null default false,
    constraint uq_book_copy_no unique (book_id, copy_no),
    constraint fk_book_copy_book foreign key (book_id) references books(id)
);

insert into categories(code, name, enabled, created_at, updated_at, deleted)
select 'CS', 'Computer Science', true, now(), now(), false
where not exists (select 1 from categories where code = 'CS');

insert into categories(code, name, enabled, created_at, updated_at, deleted)
select 'LIT', 'Literature', true, now(), now(), false
where not exists (select 1 from categories where code = 'LIT');

insert into users(username, password, full_name, student_no, phone, role, enabled, created_at, updated_at, deleted)
select 'reader', '123456', 'Demo Reader', 'R2026001', '13800000001', 'READER', true, now(), now(), false
where not exists (select 1 from users where username = 'reader');

insert into users(username, password, full_name, student_no, phone, role, enabled, created_at, updated_at, deleted)
select 'librarian', '123456', 'Demo Librarian', 'L2026001', '13800000002', 'LIBRARIAN', true, now(), now(), false
where not exists (select 1 from users where username = 'librarian');

insert into users(username, password, full_name, student_no, phone, role, enabled, created_at, updated_at, deleted)
select 'admin', '123456', 'Demo Admin', 'A2026001', '13800000003', 'ADMIN', true, now(), now(), false
where not exists (select 1 from users where username = 'admin');

insert into role_permissions(role, permission_scope, created_at, updated_at, deleted)
select 'READER', 'BOOK_SEARCH,BOOK_VIEW,BORROW_REQUEST,RETURN_REQUEST,RESERVATION,FEEDBACK', now(), now(), false
where not exists (select 1 from role_permissions where role = 'READER');

insert into role_permissions(role, permission_scope, created_at, updated_at, deleted)
select 'LIBRARIAN', 'BOOK_MANAGE,INVENTORY_MANAGE,REQUEST_PROCESS,RESERVATION_PROCESS,FINE_MANAGE,FEEDBACK_MANAGE', now(), now(), false
where not exists (select 1 from role_permissions where role = 'LIBRARIAN');

insert into role_permissions(role, permission_scope, created_at, updated_at, deleted)
select 'ADMIN', 'USER_MANAGE,ROLE_MANAGE,SYSTEM_CONFIG,LOG_VIEW,BACKUP_RESTORE,REPORT_VIEW', now(), now(), false
where not exists (select 1 from role_permissions where role = 'ADMIN');

insert into system_configs(config_key, config_value, description, created_at, updated_at, deleted)
select 'BORROW_PERIOD_DAYS', '30', 'Default borrowing period in days', now(), now(), false
where not exists (select 1 from system_configs where config_key = 'BORROW_PERIOD_DAYS');

insert into system_configs(config_key, config_value, description, created_at, updated_at, deleted)
select 'BORROW_LIMIT', '5', 'Maximum number of borrowed books', now(), now(), false
where not exists (select 1 from system_configs where config_key = 'BORROW_LIMIT');

insert into system_configs(config_key, config_value, description, created_at, updated_at, deleted)
select 'OVERDUE_FINE_PER_DAY', '1.00', 'Overdue fine charged per day', now(), now(), false
where not exists (select 1 from system_configs where config_key = 'OVERDUE_FINE_PER_DAY');

insert into system_configs(config_key, config_value, description, created_at, updated_at, deleted)
select 'MAX_RENEWALS', '2', 'Maximum renewals per borrow record', now(), now(), false
where not exists (select 1 from system_configs where config_key = 'MAX_RENEWALS');

insert into status_codes(code_type, code_value, display_name, description, enabled, created_at, updated_at, deleted)
select 'BOOK_SHELF_STATUS', 'ON_SHELF', 'On Shelf', 'Book can be shown in catalog', true, now(), now(), false
where not exists (select 1 from status_codes where code_type = 'BOOK_SHELF_STATUS' and code_value = 'ON_SHELF');

insert into status_codes(code_type, code_value, display_name, description, enabled, created_at, updated_at, deleted)
select 'BOOK_SHELF_STATUS', 'OFF_SHELF', 'Off Shelf', 'Book is hidden from borrowing', true, now(), now(), false
where not exists (select 1 from status_codes where code_type = 'BOOK_SHELF_STATUS' and code_value = 'OFF_SHELF');

insert into status_codes(code_type, code_value, display_name, description, enabled, created_at, updated_at, deleted)
select 'BOOK_SHELF_STATUS', 'DAMAGED', 'Damaged', 'Book copy is damaged or unavailable', true, now(), now(), false
where not exists (select 1 from status_codes where code_type = 'BOOK_SHELF_STATUS' and code_value = 'DAMAGED');

insert into status_codes(code_type, code_value, display_name, description, enabled, created_at, updated_at, deleted)
select 'BORROW_RECORD_STATUS', 'BORROWED', 'Borrowed', 'Book is currently borrowed', true, now(), now(), false
where not exists (select 1 from status_codes where code_type = 'BORROW_RECORD_STATUS' and code_value = 'BORROWED');

insert into status_codes(code_type, code_value, display_name, description, enabled, created_at, updated_at, deleted)
select 'BORROW_RECORD_STATUS', 'RETURN_PENDING', 'Return Pending', 'Reader has submitted a return request', true, now(), now(), false
where not exists (select 1 from status_codes where code_type = 'BORROW_RECORD_STATUS' and code_value = 'RETURN_PENDING');

insert into status_codes(code_type, code_value, display_name, description, enabled, created_at, updated_at, deleted)
select 'BORROW_RECORD_STATUS', 'RETURNED', 'Returned', 'Borrowing is completed', true, now(), now(), false
where not exists (select 1 from status_codes where code_type = 'BORROW_RECORD_STATUS' and code_value = 'RETURNED');

insert into status_codes(code_type, code_value, display_name, description, enabled, created_at, updated_at, deleted)
select 'BORROW_RECORD_STATUS', 'OVERDUE', 'Overdue', 'Borrowing is overdue', true, now(), now(), false
where not exists (select 1 from status_codes where code_type = 'BORROW_RECORD_STATUS' and code_value = 'OVERDUE');

insert into books(title, author, isbn, barcode, publisher, description, category_id, shelf_status, created_at, updated_at, deleted)
select 'Clean Architecture', 'Robert C. Martin', 'DEMO-ISBN-001', 'LIB-DEMO-000001', 'Prentice Hall', 'Seeded demo book for borrow workflow.', c.id, 'ON_SHELF', now(), now(), false
from categories c
where c.code = 'CS'
  and not exists (select 1 from books where isbn = 'DEMO-ISBN-001');

insert into inventory(book_id, total_copies, available_copies, created_at, updated_at, deleted)
select b.id, 5, 5, now(), now(), false
from books b
where b.isbn = 'DEMO-ISBN-001'
  and not exists (select 1 from inventory where book_id = b.id);

insert into books(title, author, isbn, barcode, publisher, description, category_id, shelf_status, created_at, updated_at, deleted)
select 'Designing Data-Intensive Applications', 'Martin Kleppmann', 'DEMO-ISBN-002', 'LIB-DEMO-000002', 'O''Reilly', 'Seeded unavailable demo book for reservation workflow.', c.id, 'ON_SHELF', now(), now(), false
from categories c
where c.code = 'CS'
  and not exists (select 1 from books where isbn = 'DEMO-ISBN-002');

insert into inventory(book_id, total_copies, available_copies, created_at, updated_at, deleted)
select b.id, 2, 0, now(), now(), false
from books b
where b.isbn = 'DEMO-ISBN-002'
  and not exists (select 1 from inventory where book_id = b.id);

insert into reservations(reader_id, book_id, status, queue_no, created_at, updated_at, deleted)
select u.id, b.id, 'PENDING', 1, now(), now(), false
from users u
join books b on b.isbn = 'DEMO-ISBN-002'
where u.username = 'reader'
  and not exists (
      select 1 from reservations r
      where r.reader_id = u.id and r.book_id = b.id and r.status = 'PENDING' and r.deleted = false
  );

update books
set barcode = concat('LIB-', lpad(id, 8, '0'))
where (barcode is null or barcode = '')
  and deleted = false;

update books
set storage_location = '3F-A区-计算机书架'
where isbn = 'DEMO-ISBN-001' and (storage_location is null or storage_location = '');

update books
set storage_location = '3F-B区-技术书架'
where isbn = 'DEMO-ISBN-002' and (storage_location is null or storage_location = '');

insert into book_copies(book_id, copy_no, barcode, created_at, updated_at, deleted)
select b.id, seq.copy_no, concat('LIB-', lpad(b.id, 6, '0'), '-', lpad(seq.copy_no, 3, '0')), now(), now(), false
from books b
join (
    select 1 as copy_no union all
    select 2 union all
    select 3 union all
    select 4 union all
    select 5 union all
    select 6 union all
    select 7 union all
    select 8 union all
    select 9 union all
    select 10 union all
    select 11 union all
    select 12 union all
    select 13 union all
    select 14 union all
    select 15 union all
    select 16 union all
    select 17 union all
    select 18 union all
    select 19 union all
    select 20
) seq
join inventory i on i.book_id = b.id and i.deleted = false
where seq.copy_no <= i.total_copies
  and b.deleted = false
  and not exists (
      select 1 from book_copies bc
      where bc.book_id = b.id and bc.copy_no = seq.copy_no
  );

-- =====================================================================
-- V1 — Baseline schema for Klink
-- =====================================================================
-- IMPORTANT: This baseline is generated from the JPA entities. On the
-- EXISTING (live) database it is NEVER executed — spring.flyway.baseline-on-migrate
-- records version 1 as the starting point and only V2+ run. It exists so a
-- FRESH database can be built from scratch by Flyway.
--
-- Before enabling `HIBERNATE_DDL_AUTO=validate` or doing a fresh production
-- deploy, diff this file against a real dump of the live schema:
--     pg_dump --schema-only --no-owner "$DB_URL" > live_schema.sql
-- and reconcile any differences. Column types/lengths, nullability and
-- constraint details here follow the entities as read, not a live dump.
-- =====================================================================

-- ---------- Core tenant + identity ----------

create table churches (
    id             uuid primary key,
    church_name    varchar(255) not null,
    location       varchar(255),
    denomination   varchar(255),
    church_code    varchar(255) not null unique,
    welfare_amount numeric(19,2),
    contact_phone  varchar(255),
    contact_email  varchar(255),
    photo_url      varchar(255),
    created_at     timestamp,
    updated_at     timestamp,
    deleted_at     timestamp
);

create table members (
    id                                   uuid primary key,
    church_id                            uuid not null,
    full_name                            varchar(255) not null,
    phone                                varchar(255),
    email                                varchar(255) unique,
    password                             varchar(255),
    role                                 varchar(255) not null,
    category                             varchar(255),
    has_smartphone                       boolean not null default false,
    qr_code_value                        varchar(255) unique,
    date_of_birth                        date,
    address                              varchar(255),
    baptism_date                         date,
    membership_date                      date,
    status                               varchar(255) not null,
    deactivated_by                       uuid,
    deactivated_at                       timestamp,
    registered_by                        uuid,
    auth_user_id                         uuid,
    fcm_token                            varchar(255),
    photo_url                            varchar(255),
    email_verified                       boolean default false,
    phone_number                         varchar(255) unique,
    phone_verified                       boolean default false,
    phone_verification_code_hash         varchar(255),
    phone_verification_code_expires_at   timestamp,
    phone_verification_attempts          integer not null default 0,
    last_phone_verification_attempt_at   timestamp,
    password_changed_at                  timestamp,
    failed_login_attempts                integer not null default 0,
    last_failed_at                       timestamp,
    locked_until                         timestamp,
    token_version                        integer not null default 0,
    created_at                           timestamp,
    updated_at                           timestamp
);
create index idx_members_church_id on members (church_id);
create index idx_members_status on members (status);
create index idx_members_church_status on members (church_id, status);
create index idx_members_date_of_birth on members (date_of_birth);

create table refresh_tokens (
    id         uuid primary key,
    member_id  uuid not null,
    family_id  uuid not null,
    token_hash varchar(255) not null unique,
    expires_at timestamp not null,
    revoked    boolean not null default false,
    created_at timestamp
);
create index idx_refresh_tokens_member_id on refresh_tokens (member_id);
create index idx_refresh_tokens_family_id on refresh_tokens (family_id);

create table verification_tokens (
    id         uuid primary key,
    email      varchar(255) not null,
    code       varchar(255) not null,
    type       varchar(255) not null,
    expires_at timestamp not null,
    used       boolean not null default false,
    created_at timestamp
);
create index idx_verification_tokens_email_type_used on verification_tokens (email, type, used);

-- ---------- Attendance ----------

create table attendance (
    id           uuid primary key,
    church_id    uuid not null,
    member_id    uuid not null,
    service_name varchar(255) not null,
    service_date date not null,
    time_of_scan timestamp,
    marked_by    uuid,
    method       varchar(255),
    status       varchar(255),
    created_at   timestamp,
    updated_at   timestamp,
    constraint uq_attendance_member_service unique (member_id, service_date, service_name)
);
create index idx_attendance_church_id on attendance (church_id);
create index idx_attendance_member_id on attendance (member_id);
create index idx_attendance_service_date on attendance (service_date);

create table attendance_sessions (
    id           uuid primary key,
    church_id    uuid not null,
    service_name varchar(255) not null,
    service_date date not null,
    expires_at   timestamp not null,
    processed    boolean not null default false,
    created_at   timestamp,
    updated_at   timestamp
);
create index idx_attendance_sessions_church_id on attendance_sessions (church_id);

-- ---------- Finance ----------

create table payments (
    id            uuid primary key,
    church_id     uuid not null,
    member_id     uuid,
    group_id      uuid,
    payment_type  varchar(255) not null,
    amount        numeric(19,2) not null,
    payment_month varchar(255),
    payment_date  date,
    status        varchar(255),
    momo_reference varchar(255),
    recorded_by   uuid,
    created_at    timestamp,
    updated_at    timestamp
);
create index idx_payments_church_id on payments (church_id);
create index idx_payments_member_id on payments (member_id);
create index idx_payments_church_type_month on payments (church_id, payment_type, payment_month);
create index idx_payments_group_id on payments (group_id);

create table pledges (
    id          uuid primary key,
    church_id   uuid not null,
    member_id   uuid not null,
    description varchar(255),
    amount      numeric(19,2) not null,
    amount_paid numeric(19,2) default 0,
    paid_at     date,
    status      varchar(255) not null,
    recorded_by uuid,
    created_at  timestamp,
    updated_at  timestamp
);
create index idx_pledges_church_id on pledges (church_id);
create index idx_pledges_member_id on pledges (member_id);

create table pledge_payments (
    id           uuid primary key,
    church_id    uuid not null,
    pledge_id    uuid not null,
    member_id    uuid not null,
    amount       numeric(19,2) not null,
    payment_date date not null,
    recorded_by  uuid,
    created_at   timestamp
);
create index idx_pledge_payments_church_id on pledge_payments (church_id);
create index idx_pledge_payments_pledge_id on pledge_payments (pledge_id);

-- ---------- Groups ----------

create table groups (
    id              uuid primary key,
    church_id       uuid not null,
    group_name      varchar(255) not null,
    description     text,
    dues_amount     numeric(19,2),
    photo_url       text,
    group_admin_id  uuid,
    group_fin_sec_id uuid,
    status          varchar(255) not null,
    created_by      uuid,
    created_at      timestamp,
    updated_at      timestamp
);
create index idx_groups_church_id on groups (church_id);
create index idx_groups_church_status on groups (church_id, status);

create table group_members (
    id        uuid primary key,
    church_id uuid not null,
    group_id  uuid not null,
    member_id uuid not null,
    joined_at timestamp,
    created_at timestamp,
    constraint uq_group_member unique (group_id, member_id)
);
create index idx_group_members_church_id on group_members (church_id);
create index idx_group_members_member_id on group_members (member_id);

create table group_messages (
    id        uuid primary key,
    church_id uuid not null,
    group_id  uuid not null,
    content   text not null,
    posted_by uuid,
    created_at timestamp
);
create index idx_group_messages_group_id on group_messages (group_id);

-- ---------- Announcements ----------

create table announcements (
    id               uuid primary key,
    church_id        uuid not null,
    title            varchar(255) not null,
    body             text not null,
    flyer_url        varchar(255),
    posted_by        uuid,
    target_type      varchar(255),
    target_roles     jsonb,
    target_group_ids jsonb,
    target_member_ids jsonb,
    is_targeted      boolean,
    recipient_count  integer,
    created_at       timestamp,
    updated_at       timestamp
);
create index idx_announcements_church_id on announcements (church_id);

-- ---------- Content ----------

create table events (
    id            uuid primary key,
    church_id     uuid not null,
    title         varchar(255) not null,
    description   text,
    location      varchar(255),
    category      varchar(255),
    event_date    timestamp not null,
    reminder_sent boolean not null default false,
    created_by    uuid,
    created_at    timestamp,
    updated_at    timestamp
);
create index idx_events_church_id on events (church_id);

create table sermons (
    id           uuid primary key,
    church_id    uuid not null,
    preacher     varchar(255) not null,
    title        varchar(255) not null,
    memory_verse varchar(255),
    scripture    varchar(255),
    sermon_date  date not null,
    audio_url    varchar(255),
    notes        text,
    posted_by    uuid,
    created_at   timestamp,
    updated_at   timestamp
);
create index idx_sermons_church_id on sermons (church_id);

create table devotionals (
    id              uuid primary key,
    church_id       uuid not null,
    title           varchar(255) not null,
    content         text not null,
    devotional_date date not null,
    posted_by       uuid,
    created_at      timestamp,
    updated_at      timestamp
);
create index idx_devotionals_church_id on devotionals (church_id);

create table gallery (
    id          uuid primary key,
    church_id   uuid not null,
    photo_url   varchar(255) not null,
    caption     varchar(255),
    uploaded_by uuid,
    uploaded_at timestamp,
    updated_at  timestamp
);
create index idx_gallery_church_id on gallery (church_id);

create table church_files (
    id          uuid primary key,
    church_id   uuid not null,
    title       varchar(255) not null,
    category    varchar(255) not null,
    language    varchar(255),
    file_url    varchar(255) not null,
    uploaded_by uuid,
    uploaded_at timestamp,
    updated_at  timestamp
);
create index idx_church_files_church_id on church_files (church_id);

create table polls (
    id         uuid primary key,
    church_id  uuid not null,
    question   varchar(255) not null,
    options    jsonb not null,
    closes_at  timestamp,
    created_by uuid,
    created_at timestamp,
    updated_at timestamp
);
create index idx_polls_church_id on polls (church_id);

create table poll_votes (
    id              uuid primary key,
    church_id       uuid not null,
    poll_id         uuid not null,
    member_id       uuid not null,
    selected_option varchar(255) not null,
    voted_at        timestamp,
    created_at      timestamp,
    constraint uq_poll_vote_member unique (poll_id, member_id)
);
create index idx_poll_votes_church_id on poll_votes (church_id);

create table hall_of_fame (
    id          uuid primary key,
    church_id   uuid not null,
    member_id   uuid,
    title       varchar(255) not null,
    description text,
    photo_url   varchar(255),
    posted_by   uuid,
    created_at  timestamp,
    updated_at  timestamp
);
create index idx_hall_of_fame_church_id on hall_of_fame (church_id);

create table prayer_requests (
    id              uuid primary key,
    church_id       uuid not null,
    member_id       uuid not null,
    title           varchar(255) not null,
    content         text not null,
    visibility      varchar(255) not null,
    status          varchar(255) not null,
    leader_response text,
    answered_by     uuid,
    answered_at     timestamp,
    created_at      timestamp,
    updated_at      timestamp,
    deleted_at      timestamp
);
create index idx_prayer_church on prayer_requests (church_id);
create index idx_prayer_member on prayer_requests (member_id);

-- ---------- Store ----------

create table store_items (
    id          uuid primary key,
    church_id   uuid not null,
    name        varchar(255) not null,
    description text,
    price       numeric(19,2) not null,
    quantity    integer not null,
    category    varchar(255),
    photo_url   varchar(255),
    photo_urls  jsonb,
    status      varchar(255) not null,
    created_by  uuid,
    created_at  timestamp,
    updated_at  timestamp
);
create index idx_store_items_church_id on store_items (church_id);

create table store_payments (
    id                uuid primary key,
    church_id         uuid not null,
    member_id         uuid not null,
    item_id           uuid not null,
    amount            numeric(19,2) not null,
    date_paid         date,
    collection_status varchar(255) not null,
    momo_reference    varchar(255),
    collected_by      uuid,
    collected_at      timestamp,
    created_at        timestamp,
    updated_at        timestamp
);
create index idx_store_payments_church_id on store_payments (church_id);
create index idx_store_payments_member_id on store_payments (member_id);

-- ---------- Facilities ----------

create table facilities (
    id               uuid primary key,
    church_id        uuid not null,
    name             varchar(200) not null,
    description      text,
    facility_type    varchar(30) not null,
    address          varchar(500),
    capacity         integer,
    year_acquired    integer,
    estimated_value  numeric(19,2),
    currency         varchar(10),
    condition_status varchar(20) not null,
    is_active        boolean not null default true,
    notes            text,
    created_by       uuid,
    created_at       timestamp,
    updated_at       timestamp,
    deleted_at       timestamp
);
create index idx_facilities_church_id on facilities (church_id);
create index idx_facilities_type on facilities (facility_type);
create index idx_facilities_deleted_at on facilities (deleted_at);
create index idx_facilities_created_at on facilities (created_at);

create table facility_images (
    id          uuid primary key,
    facility_id uuid not null,
    church_id   uuid not null,
    image_url   varchar(2000) not null,
    caption     varchar(500),
    is_primary  boolean not null default false,
    uploaded_by uuid,
    uploaded_at timestamp,
    sort_order  integer
);
create index idx_facility_images_facility_id on facility_images (facility_id);
create index idx_facility_images_church_id on facility_images (church_id);
create index idx_facility_images_uploaded_at on facility_images (uploaded_at);

-- ---------- Projects ----------

create table church_projects (
    id                uuid primary key,
    church_id         uuid not null,
    title             varchar(300) not null,
    description       text not null,
    project_type      varchar(30) not null,
    status            varchar(20) not null,
    target_amount     numeric(19,2) not null,
    amount_raised     numeric(19,2) not null,
    currency          varchar(10),
    start_date        date,
    expected_end_date date,
    actual_end_date   date,
    location          varchar(500),
    contractor        varchar(300),
    facility_id       uuid,
    created_by        uuid not null,
    approved_by       uuid,
    approved_at       timestamp,
    is_public         boolean not null default false,
    created_at        timestamp,
    updated_at        timestamp,
    deleted_at        timestamp
);
create index idx_projects_church_id on church_projects (church_id);
create index idx_projects_status on church_projects (status);
create index idx_projects_deleted_at on church_projects (deleted_at);
create index idx_projects_created_at on church_projects (created_at);

create table project_updates (
    id         uuid primary key,
    project_id uuid not null,
    church_id  uuid not null,
    title      varchar(300) not null,
    content    text not null,
    posted_by  uuid not null,
    posted_at  timestamp,
    updated_at timestamp
);
create index idx_project_updates_project_id on project_updates (project_id);
create index idx_project_updates_church_id on project_updates (church_id);
create index idx_project_updates_posted_at on project_updates (posted_at);

create table project_images (
    id          uuid primary key,
    project_id  uuid not null,
    update_id   uuid,
    church_id   uuid not null,
    image_url   varchar(2000) not null,
    caption     varchar(500),
    is_primary  boolean not null default false,
    uploaded_by uuid,
    uploaded_at timestamp,
    sort_order  integer,
    phase       varchar(100)
);
create index idx_project_images_project_id on project_images (project_id);
create index idx_project_images_church_id on project_images (church_id);
create index idx_project_images_uploaded_at on project_images (uploaded_at);

create table project_contributions (
    id                uuid primary key,
    project_id        uuid not null,
    member_id         uuid not null,
    church_id         uuid not null,
    amount            numeric(19,2) not null,
    currency          varchar(10),
    contribution_date date not null,
    payment_method    varchar(20) not null,
    recorded_by       uuid not null,
    notes             text,
    created_at        timestamp
);
create index idx_contributions_project_id on project_contributions (project_id);
create index idx_contributions_member_id on project_contributions (member_id);
create index idx_contributions_church_id on project_contributions (church_id);
create index idx_contributions_created_at on project_contributions (created_at);
create index idx_contributions_date on project_contributions (contribution_date);

-- ---------- Paystack ----------

create table paystack_transactions (
    id                          uuid primary key,
    church_id                   uuid not null,
    member_id                   uuid not null,
    amount                      numeric(19,2) not null,
    currency                    varchar(255) not null,
    payment_type                varchar(255) not null,
    status                      varchar(255) not null,
    paystack_reference          varchar(255) not null unique,
    paystack_transaction_id     varchar(255),
    paystack_authorization_code varchar(255),
    channel                     varchar(255),
    customer_email              varchar(255),
    description                 varchar(255),
    project_id                  uuid,
    is_recorded                 boolean not null default false,
    paid_at                     timestamp,
    created_at                  timestamp,
    updated_at                  timestamp,
    deleted_at                  timestamp
);
create index idx_paytx_church on paystack_transactions (church_id);
create index idx_paytx_member on paystack_transactions (member_id);

-- ---------- Foreign keys (added after all tables exist; order-independent) ----------

alter table members            add constraint fk_members_church            foreign key (church_id) references churches (id);

alter table attendance         add constraint fk_attendance_church         foreign key (church_id) references churches (id);
alter table attendance         add constraint fk_attendance_member         foreign key (member_id) references members (id);
alter table attendance_sessions add constraint fk_att_sessions_church      foreign key (church_id) references churches (id);

alter table payments           add constraint fk_payments_church           foreign key (church_id) references churches (id);
alter table payments           add constraint fk_payments_member           foreign key (member_id) references members (id);
alter table payments           add constraint fk_payments_group            foreign key (group_id)  references groups (id);

alter table pledges            add constraint fk_pledges_church            foreign key (church_id) references churches (id);
alter table pledges            add constraint fk_pledges_member            foreign key (member_id) references members (id);
alter table pledge_payments    add constraint fk_pledge_payments_church    foreign key (church_id) references churches (id);
alter table pledge_payments    add constraint fk_pledge_payments_pledge    foreign key (pledge_id) references pledges (id);
alter table pledge_payments    add constraint fk_pledge_payments_member    foreign key (member_id) references members (id);

alter table groups             add constraint fk_groups_church             foreign key (church_id)        references churches (id);
alter table groups             add constraint fk_groups_admin              foreign key (group_admin_id)   references members (id);
alter table groups             add constraint fk_groups_finsec             foreign key (group_fin_sec_id) references members (id);
alter table group_members      add constraint fk_group_members_church      foreign key (church_id) references churches (id);
alter table group_members      add constraint fk_group_members_group       foreign key (group_id)  references groups (id);
alter table group_members      add constraint fk_group_members_member      foreign key (member_id) references members (id);
alter table group_messages     add constraint fk_group_messages_church     foreign key (church_id) references churches (id);
alter table group_messages     add constraint fk_group_messages_group      foreign key (group_id)  references groups (id);

alter table announcements      add constraint fk_announcements_church      foreign key (church_id) references churches (id);

alter table events             add constraint fk_events_church             foreign key (church_id) references churches (id);
alter table sermons            add constraint fk_sermons_church            foreign key (church_id) references churches (id);
alter table devotionals        add constraint fk_devotionals_church        foreign key (church_id) references churches (id);
alter table gallery            add constraint fk_gallery_church            foreign key (church_id) references churches (id);
alter table church_files       add constraint fk_church_files_church       foreign key (church_id) references churches (id);

alter table polls              add constraint fk_polls_church              foreign key (church_id) references churches (id);
alter table poll_votes         add constraint fk_poll_votes_church         foreign key (church_id) references churches (id);
alter table poll_votes         add constraint fk_poll_votes_poll           foreign key (poll_id)   references polls (id);
alter table poll_votes         add constraint fk_poll_votes_member         foreign key (member_id) references members (id);

alter table hall_of_fame       add constraint fk_hall_of_fame_church       foreign key (church_id) references churches (id);
alter table hall_of_fame       add constraint fk_hall_of_fame_member       foreign key (member_id) references members (id);

alter table prayer_requests    add constraint fk_prayer_requests_church    foreign key (church_id) references churches (id);

alter table store_items        add constraint fk_store_items_church        foreign key (church_id) references churches (id);
alter table store_payments     add constraint fk_store_payments_church     foreign key (church_id) references churches (id);
alter table store_payments     add constraint fk_store_payments_member     foreign key (member_id) references members (id);
alter table store_payments     add constraint fk_store_payments_item       foreign key (item_id)   references store_items (id);

alter table facilities         add constraint fk_facilities_church         foreign key (church_id)   references churches (id);
alter table facility_images    add constraint fk_facility_images_facility  foreign key (facility_id) references facilities (id);
alter table facility_images    add constraint fk_facility_images_church    foreign key (church_id)   references churches (id);

alter table church_projects        add constraint fk_projects_church        foreign key (church_id)  references churches (id);
alter table project_updates        add constraint fk_project_updates_project foreign key (project_id) references church_projects (id);
alter table project_updates        add constraint fk_project_updates_church  foreign key (church_id)  references churches (id);
alter table project_images         add constraint fk_project_images_project  foreign key (project_id) references church_projects (id);
alter table project_images         add constraint fk_project_images_church   foreign key (church_id)  references churches (id);
alter table project_contributions  add constraint fk_contributions_project   foreign key (project_id) references church_projects (id);
alter table project_contributions  add constraint fk_contributions_member    foreign key (member_id)  references members (id);
alter table project_contributions  add constraint fk_contributions_church    foreign key (church_id)  references churches (id);

alter table paystack_transactions  add constraint fk_paytx_church            foreign key (church_id)  references churches (id);

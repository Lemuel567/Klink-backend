-- =====================================================================
-- V2 — Per-member announcement read receipts
-- =====================================================================
-- New table backing the notifications inbox read-state. Runs on both the
-- existing database (after the V1 baseline is recorded) and fresh databases.
-- =====================================================================

create table announcement_reads (
    id              uuid primary key,
    church_id       uuid not null,
    announcement_id uuid not null,
    member_id       uuid not null,
    read_at         timestamp,
    constraint uq_announcement_read unique (announcement_id, member_id)
);

create index idx_announcement_reads_member on announcement_reads (church_id, member_id);

alter table announcement_reads
    add constraint fk_announcement_reads_church
    foreign key (church_id) references churches (id);

alter table announcement_reads
    add constraint fk_announcement_reads_announcement
    foreign key (announcement_id) references announcements (id);

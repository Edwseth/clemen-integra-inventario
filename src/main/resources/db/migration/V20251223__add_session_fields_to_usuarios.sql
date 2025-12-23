alter table usuarios
    add column session_version bigint not null default 0,
    add column ultima_actividad datetime(6) null;

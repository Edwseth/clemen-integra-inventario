create table roles (
    id bigint not null auto_increment,
    codigo varchar(80) not null,
    nombre varchar(120) null,
    activo bit(1) not null default b'1',
    primary key (id),
    unique key uk_roles_codigo (codigo)
) ENGINE=InnoDB;

create table permisos (
    id bigint not null auto_increment,
    codigo varchar(120) not null,
    modulo varchar(40) not null,
    accion varchar(40) not null,
    descripcion varchar(255) null,
    activo bit(1) not null default b'1',
    primary key (id),
    unique key uk_permisos_codigo (codigo)
) ENGINE=InnoDB;

create table roles_permisos (
    rol_id bigint not null,
    permiso_id bigint not null,
    primary key (rol_id, permiso_id),
    constraint fk_roles_permisos_rol
        foreign key (rol_id) references roles (id)
        on delete cascade,
    constraint fk_roles_permisos_permiso
        foreign key (permiso_id) references permisos (id)
        on delete cascade
) ENGINE=InnoDB;

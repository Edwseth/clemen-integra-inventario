alter table formula_producto add column version_major int null;
alter table formula_producto add column version_minor int null;

update formula_producto
set version_major = case
    when version is null or trim(version) = '' then null
    else cast(substring_index(replace(upper(version), 'V', ''), '.', 1) as unsigned)
end,
version_minor = case
    when version is null or trim(version) = '' then null
    when locate('.', replace(upper(version), 'V', '')) > 0
        then cast(substring_index(replace(upper(version), 'V', ''), '.', -1) as unsigned)
    else 0
end
where version is not null and (version_major is null or version_minor is null);

create index idx_formula_producto_version on formula_producto (producto_id, version_major, version_minor);

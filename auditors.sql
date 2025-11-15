create table auditors(
	auditor_id BIGSERIAL primary key,
	name varchar(100) not null,
	home_lat double precision,
	home_lon double precision,
	availability_status varchar(20) not null,
	workload_capacity_hours integer,
	current_assigned_hours integer
);

select * from auditors;

drop table auditors;

create table store(
id bigserial primary key,
name varchar(255) not null,
address text,
location_lat double precision not null,
location_lon double precision not null,
store_status varchar(20) not null
);

select * from store;

select * from audit_plan;



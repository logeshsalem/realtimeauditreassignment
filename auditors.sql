create table auditors(
	auditor_id BIGSERIAL primary key,
	name varchar(100) not null,
	home_lat double precision,
	home_lon double precision,
	availability_status varchar(20) not null,
	workload_capacity_hours double precision,
	current_assigned_hours double precision
);

select * from auditors;

drop table auditors;

create table store(
store_id bigserial primary key,
name varchar(255) not null,
address text,
location_lat double precision not null,
location_lon double precision not null,
store_status varchar(20) not null
);

select * from store;

drop table store;

CREATE TABLE audit_plan (
    audit_id BIGSERIAL PRIMARY KEY,    
    auditor_id BIGINT REFERENCES auditors(auditor_id) ON DELETE SET NULL,
    store_id BIGINT REFERENCES store(store_id) ON DELETE CASCADE,   
    audit_priority VARCHAR(20) NOT NULL, -- High / Medium / Low  
    audit_status VARCHAR(20) NOT NULL 
);

select * from audit_plan;

drop table audit_plan;

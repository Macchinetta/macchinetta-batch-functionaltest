CREATE TABLE IF NOT EXISTS branch_mst (
    branch_id   varchar(10) NOT NULL,
    branch_name varchar(50) NOT NULL,
    branch_address  varchar(200) NOT NULL,
    branch_tel  varchar(11) NOT NULL,
    create_date timestamp NOT NULL,
    update_date timestamp ,
    CONSTRAINT pk_branch_mst PRIMARY KEY (branch_id)
);

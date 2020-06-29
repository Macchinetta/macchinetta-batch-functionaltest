CREATE TABLE IF NOT EXISTS sales_plan_summary (
    branch_id   varchar(6) NOT NULL,
    year    integer ,
    month   integer ,
    amount  numeric(10) NOT NULL,
    CONSTRAINT pk_salples_plan_summary PRIMARY KEY (branch_id, year, month)
);

CREATE TABLE IF NOT EXISTS sales_plan_detail (
    branch_id   varchar(6) NOT NULL,
    year    integer ,
    month   integer ,
    customer_id varchar(10),
    amount  numeric(10) NOT NULL,
    CONSTRAINT pk_salples_plan_detail PRIMARY KEY (branch_id, year, month, customer_id)
);

CREATE TABLE IF NOT EXISTS sales_performance_summary (
    branch_id   varchar(6) NOT NULL,
    year    integer ,
    month   integer ,
    amount  numeric(10) NOT NULL,
    CONSTRAINT pk_salples_performance_summary PRIMARY KEY (branch_id, year, month)
);

CREATE TABLE IF NOT EXISTS sales_performance_detail (
    branch_id   varchar(6) NOT NULL,
    year    integer ,
    month   integer ,
    customer_id varchar(10),
    amount  numeric(10) NOT NULL,
    CONSTRAINT pk_salples_performance_detail PRIMARY KEY (branch_id, year, month, customer_id)
);

CREATE TABLE IF NOT EXISTS invoice (
    invoice_no  varchar(20) NOT NULL,
    invoice_date    date  NOT NULL,
    invoice_amount  numeric(8)  NOT NULL,
    customer_id varchar(10)  NOT NULL,
    CONSTRAINT pk_invoice PRIMARY KEY (invoice_no)
);

CREATE TABLE IF NOT EXISTS customer_mst (
    customer_id varchar(10) NOT NULL,
    customer_name   varchar(50) NOT NULL,
    customer_address    varchar(200) NOT NULL,
    customer_tel    varchar(11) NOT NULL,
    charge_branch_id    varchar(6) NOT NULL,
    create_date timestamp NOT NULL,
    update_date timestamp ,
    CONSTRAINT pk_customer_mst PRIMARY KEY (customer_id)
);

CREATE TABLE IF NOT EXISTS branch_mst (
    branch_id   varchar(10) NOT NULL,
    branch_name varchar(50) NOT NULL,
    branch_address  varchar(200) NOT NULL,
    branch_tel  varchar(11) NOT NULL,
    create_date timestamp NOT NULL,
    update_date timestamp ,
    CONSTRAINT pk_branch_mst PRIMARY KEY (branch_id)
);

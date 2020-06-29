dbunit {
    admin {
        driver = 'oracle.jdbc.driver.OracleDriver'
        url = 'jdbc:oracle:thin:@//172.17.1.28:1521/MACCHINETTADB'
        user = 'admin'
        pass = 'macchinetta'
        schema = 'ADMIN'
        tables = ['BATCH_JOB_REQUEST', 'CREATE TABLE BATCH_JOB_INSTANCE', 'CREATE TABLE BATCH_JOB_EXECUTION',
                  'CREATE TABLE BATCH_JOB_EXECUTION_PARAMS', 'CREATE TABLE BATCH_STEP_EXECUTION',
                  'CREATE TABLE BATCH_STEP_EXECUTION_CONTEXT', 'CREATE TABLE BATCH_JOB_EXECUTION_CONTEXT']

        dropSqlFilePaths = ['org/springframework/batch/core/schema-drop-oracle10g.sql',
                            'org/terasoluna/batch/async/db/schema-drop-oracle12c.sql']
        createSqlFilePaths = ['schema-oracle10g.sql',
                              'org/terasoluna/batch/async/db/schema-oracle12c.sql']
        ch04 {
            // for Ch04-AsyncJobWithDB, define customized job-request table ddl script.
            customedDropSqlsFilePaths = ['ch04/asyncjobwithdb/schema-drop-oracle.sql']
            customedCreateSqlFilePaths = ['ch04/asyncjobwithdb/schema-oracle.sql']
            defaultDropSqlsFilePaths = ['org/terasoluna/batch/async/db/schema-drop-oracle12c.sql']
            defaultCreateSqlFilePaths = ['org/terasoluna/batch/async/db/schema-oracle12c.sql']
        }
        ch05 {
            // for Ch05-Transaction, define customized job-request table ddl script.
            customedDropSqlsFilePaths = ['ch05/transaction/schema-drop-oracle.sql']
            customedCreateSqlFilePaths = ['ch05/transaction/schema-oracle.sql']
        }
    }
    job {
        driver = 'oracle.jdbc.driver.OracleDriver'
        url = 'jdbc:oracle:thin:@//172.17.1.28:1521/MACCHINETTADB'
        user = 'macchinetta'
        pass = 'macchinetta'
        schema = 'MACCHINETTA'
        tables = ['SALES_PLAN_SUMMARY', 'SALES_PLAN_DETAIL', 'SALES_PERFORMANCE_SUMMARY', 'SALES_PERFORMANCE_DETAIL',
                  'INVOICE', 'CUSTOMER_MST', 'BRANCH_MST']
        dropSqlFilePaths = ['job-schema-drop-oracle.sql']
        createSqlFilePaths = ['job-schema-oracle.sql']
    }
}

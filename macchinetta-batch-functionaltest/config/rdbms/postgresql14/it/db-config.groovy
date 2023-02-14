dbunit {
    admin {
        driver = 'org.postgresql.Driver'
        url = 'jdbc:postgresql://postgresql:5432/admin'
        user = 'postgres'
        pass = 'postgres'
        schema = null
        tables = ['batch_job_request', 'create table batch_job_instance', 'create table batch_job_execution',
                  'create table batch_job_execution_params', 'create table batch_step_execution',
                  'create table batch_step_execution_context', 'create table batch_job_execution_context']

        dropSqlFilePaths = ['org/springframework/batch/core/schema-drop-postgresql.sql',
                            'org/terasoluna/batch/async/db/schema-drop-postgresql.sql']
        createSqlFilePaths = ['org/springframework/batch/core/schema-postgresql.sql',
                              'org/terasoluna/batch/async/db/schema-postgresql.sql']
        ch04 {
            // for Ch04-AsyncJobWithDB, define customized job-request table ddl script.
            customedDropSqlsFilePaths = ['ch04/asyncjobwithdb/schema-drop-postgresql.sql']
            customedCreateSqlFilePaths = ['ch04/asyncjobwithdb/schema-postgresql.sql']
            defaultDropSqlsFilePaths = ['org/terasoluna/batch/async/db/schema-drop-postgresql.sql']
            defaultCreateSqlFilePaths = ['org/terasoluna/batch/async/db/schema-postgresql.sql']
        }
        ch05 {
            // for Ch05-Transaction, define customized job-request table ddl script.
            customedDropSqlsFilePaths = ['ch05/transaction/schema-drop-postgresql.sql']
            customedCreateSqlFilePaths = ['ch05/transaction/schema-postgresql.sql']
        }
    }
    job {
        driver = 'org.postgresql.Driver'
        url = 'jdbc:postgresql://postgresql:5432/postgres'
        user = 'postgres'
        pass = 'postgres'
        schema = null
        tables = ['sales_plan_summary', 'sales_plan_detail', 'sales_performance_summary', 'sales_performance_detail',
                  'invoice', 'customer_mst', 'branch_mst']
        dropSqlFilePaths = ['job-schema-drop-postgresql.sql']
        createSqlFilePaths = ['job-schema-postgresql.sql']
    }
}

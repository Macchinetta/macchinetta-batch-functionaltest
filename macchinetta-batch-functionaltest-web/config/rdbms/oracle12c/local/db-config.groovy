dbunit {
    admin {
        driver = 'oracle.jdbc.driver.OracleDriver'
        url = 'jdbc:oracle:thin:@//localhost:1521/MACCHINETTADB.localdomain'
        user = 'admin'
        pass = 'macchinetta'
        schema = 'ADMIN'
        tables = ['batch_job_request', 'create table batch_job_instance', 'create table batch_job_execution',
                  'create table batch_job_execution_params', 'create table batch_step_execution',
                  'create table batch_step_execution_context', 'create table batch_job_execution_context']
        dropSqlFilePaths = ['org/springframework/batch/core/schema-drop-oracle10g.sql',
                            'org/terasoluna/batch/async/db/schema-drop-oracle12c.sql']
        createSqlFilePaths = ['schema-oracle10g.sql',
                              'org/terasoluna/batch/async/db/schema-oracle12c.sql']
    }
    job {
        driver = 'oracle.jdbc.driver.OracleDriver'
        url = 'jdbc:oracle:thin:@//localhost:1521/MACCHINETTADB.localdomain'
        user = 'macchinetta'
        pass = 'macchinetta'
        schema = 'MACCHINETTA'
        tables = ['sales_plan_summary', 'sales_plan_detail', 'sales_performance_summary', 'sales_performance_detail',
                  'invoice', 'customer_mst', 'branch_mst']
        dropSqlFilePaths = ['job-schema-drop-oracle.sql']
        createSqlFilePaths = ['job-schema-oracle.sql']
    }
}

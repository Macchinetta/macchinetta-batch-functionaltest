/*
 * Copyright (C) 2017 NTT Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05

import org.junit.Rule
import org.junit.rules.TestName
import org.slf4j.LoggerFactory
import org.springframework.batch.item.file.FlatFileParseException
import org.springframework.batch.item.file.transform.IncorrectLineLengthException
import org.springframework.oxm.UnmarshallingFailureException
import org.springframework.validation.BindException
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common.LoggingItemReaderListener
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.module.LoggingHeaderRecordItemProcessor
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.plan.UseDateSalesPlanDetailFieldSetMapper
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.DBUnitUtil
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.JobLauncher
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.JobRequest
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.LogCondition
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.MongoUtil
import spock.lang.Narrative
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files
import java.util.concurrent.TimeUnit
/**
 * Function test of file access.
 *
 * @since 5.0.0
 */
@Narrative("""
A list of test cases is shown below.
1. Test of variable length records.
1.1. Test of variable length records input.
1.1.1 CSV (support RFC-4180). Space at the end of record. File tail record has no line break.
1.1.2 CSV (support RFC-4180). Record ends with a comma.
1.1.3 CSV (support RFC-4180). Enclosure is double quotes, Fields contain newlines, delimiters, and enclosure.
1.1.4 TSV (including pattern of 1.1.1 to 1.1.3). Enclosure is single quotes. Fields contain newlines, delimiters, and enclosure.
1.1.5 CSV (support RFC-4180). Use FieldSetMapper. Use Japanese Calendar and Comma-containing numbers.
1.1.6 CSV (support RFC-4180). Error. Use BeanWrapperFieldSetMapper. Use Japanese Calendar.
1.1.7 CSV (support RFC-4180). Error. Use BeanWrapperFieldSetMapper. Use Comma-containing numbers.
1.2. Test of variable length records output.
1.2.1 CSV (support RFC-4180). Space at the end of record.
1.2.2 CSV (support RFC-4180). Enclosure is double quotes, Fields contain newlines, delimiters, and enclosure, Specify false for allEnclosing.
1.2.3 CSV (support RFC-4180). Enclosure is double quotes, Fields contain newlines, delimiters, and enclosure, Specify true for allEnclosing.
1.2.4 TSV (including pattern of 1.2.1 to 1.2.3)
2. Test of fixed length records.
2.1. Test of fixed length records input.
2.1.1 Record is separated by newlines. Encoding = MS932. Including surrogate pair.
2.1.2 No record delimiter. Encoding = MS932. Including surrogate pair.
2.1.3 No record delimiter. Error. Including surrogate pair and line break.
2.2. Test of fixed length records output.
2.2.1 Records separated by newlines. Single-byte double-byte mixed. Encoding = MS932. Including surrogate pair. Format using FiedlExtactor and FiedlExtactor.
2.2.2 Records are not separated. Single-byte double-byte mixed. Encoding = MS932. Including surrogate pair. Format using FiedlExtactor and FiedlExtactor.
2.2.3. Define CRLF line separator obviously in FlatFileItemWriter.
3. Test of single string record.
3.1 Read single string record using PassThroughLineMapper.
3.2 Write single string record using PassThroughLineAggregator.
4. Test of header and footer.
4.1 Skipping header records, CSV file.
4.2 Skipping header records, Other than CSV file.
4.3 Skip header and footer by OS command.
4.4 Acquire header in reader and store in executionContext, and referenced at the ItemProcessor.
4.5 Write CSV file including header record (header is not CSV format)
4.6 Read and write of footer records using FlatFileFooterCallback.
5. Test of multiple files.
5.1 Read multiple files of the same record format using MultiResourceItemReader.
5.2 Write to multiple files per fixed number of cases using MultiResourceItemWriter (Read 21 records from DB, chunk-interval=5, itemCountLimitPerResource=4).
6. Test of XML file.
6.1 Input using JAXB.
6.2 Output using JAXB.
6.3 Output header and footer.
6.4 Schema validation.
6.5 Error, Schema validation.
7. Test of multi layout file.
7.1 Multi layout file input, Record pattern : (Header->Data->Trailer)*N->Footer, Each record has a different format.
7.2 Multi layout file output, Record pattern : (Header->Data->Trailer)*N->Footer, Each record has a different format.
8. Control break.
""")
class FileAccessSpec extends Specification {

    @Rule
    TestName testName = new TestName()

    @Shared
            log = LoggerFactory.getLogger(FileAccessSpec)

    @Shared
            jobLauncher = new JobLauncher()

    @Shared
            adminDBUnitUtil = new DBUnitUtil("admin")

    @Shared
            jobDBUnitUtil = new DBUnitUtil()

    @Shared
            mongoUtil = new MongoUtil()

    def setup() {
        log.debug("### Spec case of [{}]", testName.methodName)
        adminDBUnitUtil.dropAndCreateTable()
        jobDBUnitUtil.dropAndCreateTable()
        mongoUtil.deleteAll()
        if (!new File(outputDir).deleteDir()) {
            throw new IllegalStateException("#deleteAll at setup-method failed.")
        }
        if (!new File(outputDir).mkdirs()) {
            throw new IllegalStateException("#mkdirs at setup-method failed.")
        }
    }

    def cleanupSpec() {
        adminDBUnitUtil.close()
        jobDBUnitUtil.close()
        mongoUtil.close()
    }

    def outputDir = "files/test/output/ch05/fileaccess"

    def simpleInitCustomerMstDataSet = DBUnitUtil.createDataSet {
        customer_mst {
            customer_id | customer_name | customer_address | customer_tel | charge_branch_id | create_date | update_date
            "001" | "CustomerName001" | "CustomerAddress001" | "11111111111" | "001" | "[now]" | "[now]"
            "002" | "CustomerName002" | "CustomerAddress002" | "11111111111" | "002" | "[now]" | "[now]"
            "003" | "CustomerName003" | "CustomerAddress003" | "11111111111" | "003" | "[now]" | "[now]"
        }
    }

    // 1.1.1
    def "Reading CSV file supporting RFC - 4180. Ending of record have space. Ending of last record in the file not have line break."() {
        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/common/jobSalesPlan01.xml',
                jobName: 'jobSalesPlan01',
                jobParameter: "inputFile=files/test/input/ch05/fileaccess/sales_plan_detail_01.csv"))

        then:
        exitCode == 0

        def cursorFind = mongoUtil.find(
                new LogCondition(
                        logger: LoggingItemReaderListener.class.name,
                        level: 'INFO'
                ))
        cursorFind.size() == 3

        cursorFind.any { it.message == "Read item: SalesPlanDetail{branchId='000001', year=2016, month=1, customerId='0000000001', amount=1000000000}" }
        cursorFind.any { it.message == "Read item: SalesPlanDetail{branchId='000002', year=2017, month=2, customerId='0000000002', amount=2000000000}" }
        cursorFind.any { it.message == "Read item: SalesPlanDetail{branchId='000003', year=2018, month=3, customerId='0000000003', amount=3000000000}" }
    }

    // 1.1.2
    def "Reading CSV file supporting RFC - 4180. Ending of record is comma."() {
        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/fileaccess/jobReadCsvNullField.xml',
                jobName: 'jobReadCsvNullField',
                jobParameter: "inputFile=files/test/input/ch05/fileaccess/sales_plan_detail_02.csv"))

        then:
        exitCode == 0

        def cursorFind = mongoUtil.find(
                new LogCondition(
                        logger: LoggingItemReaderListener.class.name,
                        level: 'INFO'
                ))
        cursorFind.size() == 3

        cursorFind.any{ it.message == "Read item: SalesPlanDetail{branchId='000001', year=2016, month=1, customerId='0000000001', amount=null}" }
        cursorFind.any{ it.message == "Read item: SalesPlanDetail{branchId='000002', year=2017, month=2, customerId='0000000002', amount=null}" }
        cursorFind.any{ it.message == "Read item: SalesPlanDetail{branchId='000003', year=2018, month=3, customerId='0000000003', amount=null}" }
    }

    // 1.1.3
    def "Reading CSV file supporting RFC - 4180. Fields are enclosed in double quotes and symbols are included."() {
        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/fileaccess/jobReadCsvEnclosedFields.xml',
                jobName: 'jobReadCsvEnclosedFields',
                jobParameter: "inputFile=files/test/input/ch05/fileaccess/sales_plan_detail_03.csv"))

        then:
        exitCode == 0

        def cursorFind = mongoUtil.find(
                new LogCondition(
                        logger: LoggingItemReaderListener.class.name,
                        level: 'INFO'
                ))
        cursorFind.size() == 3

        cursorFind.any{ it.message == "Read item: SalesPlanDetail{branchId='000001', year=2016, month=1, customerId='0000000001', amount=1000000000}" }
        cursorFind.any{ it.message == """Read item: SalesPlanDetail{branchId='00,002', year=2017, month=2, customerId='000
000002', amount=2000000000}""" }
        cursorFind.any{ it.message == "Read item: SalesPlanDetail{branchId='000003', year=2018, month=3, customerId='0000\"00003', amount=3000000000}" }
    }

    // 1.1.4
    def "Reading TSV file that satisfies the input pattern of the test of CSV. Fields are enclosed in single quart."() {
        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/fileaccess/jobReadTsv.xml',
                jobName: 'jobReadTsv',
                jobParameter: "inputFile=files/test/input/ch05/fileaccess/sales_plan_detail_04.tsv"))

        then:
        exitCode == 0

        def cursorFind = mongoUtil.find(
                new LogCondition(
                        logger: LoggingItemReaderListener.class.name,
                        level: 'INFO'
                ))
        cursorFind.size() == 3

        cursorFind.any{ it.message == "Read item: SalesPlanDetail{branchId='000001', year=2016, month=1, customerId='0000000001', amount=1000000000}" }
        cursorFind.any{ it.message == """Read item: SalesPlanDetail{branchId='00\t0,2', year=2017, month=2, customerId='000
000002', amount=2000000000}""" }
        cursorFind.any{ it.message == "Read item: SalesPlanDetail{branchId='000003', year=2018, month=3, customerId='0000'00003', amount=3000000000}" }
    }

    // 1.1.5
    def "Reading CSV file supporting RFC - 4180. Implement FieldSetMapper and handle Japanese calendars and numbers with commas."() {
        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/fileaccess/jobReadCsvJpnCalAndCommaNum.xml',
                jobName: 'jobReadCsvJpnCalAndCommaNum',
                jobParameter: "inputFile=files/test/input/ch05/fileaccess/sales_plan_detail_05.csv"))

        then:
        exitCode == 0

        def cursorFind1 = mongoUtil.find(
                new LogCondition(
                        logger: UseDateSalesPlanDetailFieldSetMapper.class.name,
                        level: 'INFO'
                ))
        cursorFind1.size() == 3

        cursorFind1.any{ it.message == "FieldSet : {branchId='000001', date='平成28年1月1日', customerId='0000000001', amount='1,000,000,000'}" }
        cursorFind1.any{ it.message == """FieldSet : {branchId='00,002', date='平成29年2月2日', customerId='000
000002', amount='2,000,000,000'}""" }
        cursorFind1.any{ it.message == "FieldSet : {branchId='000003', date='平成30年3月3日', customerId='0000\"00003', amount='3,000,000,000'}" }

        def cursorFind2 = mongoUtil.find(
                new LogCondition(
                        logger: LoggingItemReaderListener.class.name,
                        level: 'INFO'
                ))
        cursorFind2.size() == 3

        cursorFind2.any{ it.message == "Read item: SalesPlanDetail{branchId='000001', date=2016-01-01, customerId='0000000001', amount=1000000000}" }
        cursorFind2.any{ it.message == """Read item: SalesPlanDetail{branchId='00,002', date=2017-02-02, customerId='000
000002', amount=2000000000}""" }
        cursorFind2.any{ it.message == "Read item: SalesPlanDetail{branchId='000003', date=2018-03-03, customerId='0000\"00003', amount=3000000000}" }
    }

    // 1.1.6
    def "Reading CSV file supporting RFC - 4180. Make sure BeanWrapperFieldSetMapper can not handle Japanese calendar."() {
        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/fileaccess/jobReadCsvJpnCalByBeanWrapper.xml',
                jobName: 'jobReadCsvJpnCalByBeanWrapper',
                jobParameter: "inputFile=files/test/input/ch05/fileaccess/sales_plan_detail_06.csv"))

        then:
        exitCode == 255

        def throwableCursor = mongoUtil.findOne(
                new LogCondition(
                        logger: LoggingItemReaderListener.class.name,
                        level: 'ERROR'
                ))

        throwableCursor.message == "Exception occurred while reading."

        throwableCursor.throwable._class == FlatFileParseException
        throwableCursor.throwable.message == "Parsing error at line: 1 in resource=[URL [file:files/test/input/ch05/fileaccess/sales_plan_detail_06.csv]], input=[000001,\"平成28年1月1日\",\"0000000001\",\"1000000000\"]"
        throwableCursor.throwable.stackTrace.size() > 0

        throwableCursor.throwable.cause._class == BindException
        throwableCursor.throwable.cause.message.contains("Cannot convert value of type 'java.lang.String' to required type 'java.util.Date' for property 'date'")
    }

    // 1.1.7
    def "Reading CSV file supporting RFC - 4180. Make sure BeanWrapperFieldSetMapper can not handle numbers with commas."() {
        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/fileaccess/jobReadCsvCommaNumByBeanWrapper.xml',
                jobName: 'jobReadCsvCommaNumByBeanWrapper',
                jobParameter: "inputFile=files/test/input/ch05/fileaccess/sales_plan_detail_07.csv"))

        then:
        exitCode == 255

        def throwableCursor = mongoUtil.findOne(
                new LogCondition(
                        logger: LoggingItemReaderListener.class.name,
                        level: 'ERROR'
                ))

        throwableCursor.message == "Exception occurred while reading."

        throwableCursor.throwable._class == FlatFileParseException
        throwableCursor.throwable.message == "Parsing error at line: 1 in resource=[URL [file:files/test/input/ch05/fileaccess/sales_plan_detail_07.csv]], input=[000001,\"2016\",\"1\",\"0000000001\",\"1,000,000,000\"]"
        throwableCursor.throwable.stackTrace.size() > 0

        throwableCursor.throwable.cause._class == BindException
        throwableCursor.throwable.cause.message.contains("Failed to convert property value of type 'java.lang.String' to required type 'int' for property 'amount'")
    }

    // 1.2.1
    def "Writing CSV file supporting RFC - 4180. Ending of record have space."() {
        setup:
        // charge_branch_id has a space at the end 
        def initCustomerMstDataSet = DBUnitUtil.createDataSet {
            customer_mst {
                customer_id | customer_name | customer_address | customer_tel | charge_branch_id | create_date | update_date
                "001" | "CustomerName001" | "CustomerAddress001" | "11111111111" | "001 " | "[now]" | "[now]"
                "002" | "CustomerName002" | "CustomerAddress002" | "11111111111" | "002 " | "[now]" | "[now]"
                "003" | "CustomerName003" | "CustomerAddress003" | "11111111111" | "003 " | "[now]" | "[now]"
            }
        }
        jobDBUnitUtil.insert(initCustomerMstDataSet)

        def outputPath = outputDir + "/customer_list.csv"

        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/common/jobCustomerList01.xml',
                jobName: 'jobCustomerList01',
                jobParameter: "outputFile=" + outputPath))

        then:
        exitCode == 0

        def actual = new File(outputPath).readLines()
        def expect = new File("files/expect/output/ch05/fileaccess/customer_list_01.csv").readLines()
        actual == expect
    }

    // 1.2.2
    def "Writing CSV file supporting RFC - 4180. Fields are enclosed in double quotes if fields include symbols."() {
        setup:
        def outputPath = outputDir + "/sales_plan_detail.csv"

        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/fileaccess/jobWriteCsvEnclosedFields.xml',
                jobName: 'jobWriteCsvEnclosedFields',
                jobParameter: "inputFile=files/test/input/ch05/fileaccess/sales_plan_detail_03.csv " +
                        "outputFile=" + outputPath))

        then:
        exitCode == 0

        def actual = new File(outputPath).readLines()
        def expect = new File("files/expect/output/ch05/fileaccess/sales_plan_detail_01.csv").readLines()
        actual == expect
    }

    // 1.2.3
    def "Writing CSV file supporting RFC - 4180. All fields are enclosed in double quotes."() {
        setup:
        def outputPath = outputDir + "/sales_plan_detail.csv"

        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/fileaccess/jobWriteCsvEnclosedAllFields.xml',
                jobName: 'jobWriteCsvEnclosedAllFields',
                jobParameter: "inputFile=files/test/input/ch05/fileaccess/sales_plan_detail_03.csv " +
                        "outputFile=" + outputPath))

        then:
        exitCode == 0

        def actual = new File(outputPath).readLines()
        def expect = new File("files/expect/output/ch05/fileaccess/sales_plan_detail_02.csv").readLines()
        actual == expect
    }

    // 1.2.4
    def "Writing  TSV file. Fields are enclosed in single quart."() {
        setup:
        def outputPath = outputDir + "/sales_plan_detail.tsv"

        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/fileaccess/jobWriteTsv.xml',
                jobName: 'jobWriteTsv',
                jobParameter: "inputFile=files/test/input/ch05/fileaccess/sales_plan_detail_04.tsv " +
                        "outputFile=" + outputPath))

        then:
        exitCode == 0

        def actual = new File(outputPath).readLines()
        def expect = new File("files/expect/output/ch05/fileaccess/sales_plan_detail_03.tsv").readLines()
        actual == expect
    }

    // 2.1.1
    def "Reading fixed byte length file. Line endings is linebreak."() {
        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/fileaccess/jobReadFixedLengthSeparateLineBreaks.xml',
                jobName: 'jobReadFixedLengthSeparateLineBreaks',
                jobParameter: "inputFile=files/test/input/ch05/fileaccess/sales_plan_detail_08.txt"))

        then:
        exitCode == 0

        def cursorFind = mongoUtil.find(
                new LogCondition(
                        logger: LoggingItemReaderListener.class.name,
                        level: 'INFO'
                ))
        cursorFind.size() == 3

        cursorFind.any{ it.message == "Read item: SalesPlanDetail{branchId='売上01', year=2016, month=1, customerId='0000001', amount=1000000000}" }
        cursorFind.any{ it.message == "Read item: SalesPlanDetail{branchId='売上02', year=2017, month=2, customerId='0000002', amount=2000000000}" }
        cursorFind.any{ it.message == "Read item: SalesPlanDetail{branchId='売上03', year=2018, month=3, customerId='0000003', amount=3000000000}" }
    }

    // 2.1.2
    def "Reading fixed byte length file. Line endings specified in fixed bytes."() {
        when:
        def charsetName = "MS932"
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/fileaccess/jobReadFixedLengthSeparateFixedLength.xml',
                jobName: 'jobReadFixedLengthSeparateFixedLength',
                jobParameter: "inputFile=files/test/input/ch05/fileaccess/sales_plan_detail_09.txt " +
                        "readCharsetName=" + charsetName + " tokenizeCharsetName=" + charsetName))

        then:
        exitCode == 0

        def cursorFind = mongoUtil.find(
                new LogCondition(
                        logger: LoggingItemReaderListener.class.name,
                        level: 'INFO'
                ))
        cursorFind.size() == 3

        cursorFind.any{ it.message == "Read item: SalesPlanDetail{branchId='売上01', year=2016, month=1, customerId='0000001', amount=1000000000}" }
        cursorFind.any{ it.message == "Read item: SalesPlanDetail{branchId='売上02', year=2017, month=2, customerId='0000002', amount=2000000000}" }
        cursorFind.any{ it.message == "Read item: SalesPlanDetail{branchId='売上03', year=2018, month=3, customerId='0000003', amount=3000000000}" }
    }

    // 2.1.3
    def "Reading fixed byte length file. Incorrect encoding specification."() {
        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/fileaccess/jobReadFixedLengthSeparateFixedLengthWithLinebreaks.xml',
                jobName: 'jobReadFixedLengthSeparateFixedLengthWithLinebreaks',
                jobParameter: "inputFile=files/test/input/ch05/fileaccess/sales_plan_detail_18.txt " +
                        "readCharsetName=UTF-8 tokenizeCharsetName=MS932"))

        then:
        exitCode == 0

        def cursorFind = mongoUtil.find(
                new LogCondition(
                        logger: LoggingItemReaderListener.class.name,
                        level: 'INFO'
                ))
        cursorFind.size() == 3

        cursorFind.any{ it.message == "Read item: SalesPlanDetail{branchId='SALE01', year=2016, month=1, customerId='0000001', amount=1000000000}" }
        cursorFind.every{ it.message != "Read item: SalesPlanDetail{branchId='売上02', year=2017, month=2, customerId='0000002', amount=2000000000}" }
        cursorFind.any{ it.message == "Read item: SalesPlanDetail{branchId='S\nLE03', year=2018, month=3, customerId='0000003', amount=3000000000}" }
    }

    // 2.1.4
    def "Reading fixed byte length file. Data including surrogate pair."() {
        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/fileaccess/jobReadFixedLengthWithSurrogatePair.xml',
                jobName: 'jobReadFixedLengthWithSurrogatePair',
                jobParameter: "inputFile=files/test/input/ch05/fileaccess/sales_plan_detail_19.txt"))

        then:
        exitCode == 255

        def throwableCursor = mongoUtil.findOne(
                new LogCondition(
                        logger: LoggingItemReaderListener.class.name,
                        level: 'ERROR'
                ))

        throwableCursor.message == "Exception occurred while reading."

        throwableCursor.throwable._class == FlatFileParseException
        throwableCursor.throwable.message == "Parsing error at line: 1 in resource=[URL [file:files/test/input/ch05/fileaccess/sales_plan_detail_19.txt]], input=[𠮷012016 1   00000011000000000]"
        throwableCursor.throwable.cause._class == IncorrectLineLengthException
        throwableCursor.throwable.cause.message == "Line is longer than max range 29"
    }

    // 2.2.1
    def "Writing fixed byte length file. Line endings is linebreak."() {
        setup:
        def outputPath = outputDir + "/customer_list.txt"

        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/fileaccess/jobWriteFixedLengthSeparateLineBreaks.xml',
                jobName: 'jobWriteFixedLengthSeparateLineBreaks',
                jobParameter: "inputFile=files/test/input/ch05/fileaccess/sales_plan_detail_10.csv " +
                        "outputFile=" + outputPath))

        then:
        exitCode == 0

        def actual = new File(outputPath).readLines()
        def expect = new File("files/expect/output/ch05/fileaccess/customer_list_02.txt").readLines()
        actual == expect
    }

    // 2.2.2
    def "Writing fixed byte length file. Line endings specified in fixed bytes."() {
        setup:
        def outputPath = outputDir + "/customer_list.txt"

        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/fileaccess/jobWriteFixedLengthSeparateFixedLength.xml',
                jobName: 'jobWriteFixedLengthSeparateFixedLength',
                jobParameter: "inputFile=files/test/input/ch05/fileaccess/sales_plan_detail_10.csv " +
                        "outputFile=" + outputPath))

        then:
        exitCode == 0

        def actual = new File(outputPath).readLines()
        def expect = new File("files/expect/output/ch05/fileaccess/customer_list_03.txt").readLines()
        actual == expect
    }

    // 2.2.3
    def "Define CRLF line separator obviously in FlatFileItemWriter."() {
        setup:
        def outputPath = outputDir + "/customer_list_with_crlf.txt"

        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/fileaccess/jobWriteFixedLengthWithLineSeparator.xml',
                jobName: 'jobWriteFixedLengthWithLineSeparator',
                jobParameter: "inputFile=files/test/input/ch05/fileaccess/sales_plan_detail_10.csv " +
                        "outputFile=" + outputPath))

        then:
        exitCode == 0

        def wroteBytes = new byte[34]
        def length = -1

        new File(outputPath).withInputStream { inputStream ->
            while ((length = inputStream.read(wroteBytes, 0, 34)) > -1) {
                // must be fixed byte length.
                assert length == 34
                // must be ended CRLF.
                assert wroteBytes[32] == '\r'.bytes[0]
                assert wroteBytes[33] == '\n'.bytes[0]
            }
            true
        }
    }

    // 3.1
    def "Reading single string record."() {
        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/fileaccess/jobReadSingleStringRecord.xml',
                jobName: 'jobReadSingleStringRecord',
                jobParameter: "inputFile=files/test/input/ch05/fileaccess/sales_plan_detail_01.csv"))

        then:
        exitCode == 0

        def cursorFind = mongoUtil.find(
                new LogCondition(
                        logger: LoggingItemReaderListener.class.name,
                        level: 'INFO'
                ))
        cursorFind.size() == 3

        cursorFind.any{ it.message == "Read item: 000001,2016,1,0000000001,1000000000 " }
        cursorFind.any{ it.message == "Read item: 000002,2017,2,0000000002,2000000000 " }
        cursorFind.any{ it.message == "Read item: 000003,2018,3,0000000003,3000000000 " }
    }

    // 3.2
    def "Writing single string record."() {
        setup:
        // charge_branch_id has a space at the end 
        def initSalesPlanDetailDataSet = DBUnitUtil.createDataSet {
            sales_plan_detail {
                branch_id | year | month | customer_id | amount
                "000001" | 2016 | 1 | "0000000001" | 1000000000
                "000002" | 2017 | 2 | "0000000002" | 2000000000
                "000003" | 2018 | 3 | "0000000003" | 3000000000
            }
        }
        jobDBUnitUtil.insert(initSalesPlanDetailDataSet)

        def outputPath = outputDir + "/sales_plan_detail.txt"

        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/fileaccess/jobWriteSingleStringRecord.xml',
                jobName: 'jobWriteSingleStringRecord',
                jobParameter: "outputFile=" + outputPath))

        then:
        exitCode == 0

        def actual = new File(outputPath).readLines()
        def expect = new File("files/expect/output/ch05/fileaccess/sales_plan_detail_04.txt").readLines()
        actual == expect
    }

    // 4.1
    def "Read CSV file skipping header record."() {
        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/fileaccess/jobReadCsvSkipHeader.xml',
                jobName: 'jobReadCsvSkipHeader',
                jobParameter: "inputFile=files/test/input/ch05/fileaccess/sales_plan_detail_11.csv"))

        then:
        exitCode == 0

        def cursorFind = mongoUtil.find(
                new LogCondition(
                        logger: LoggingItemReaderListener.class.name,
                        level: 'INFO'
                ))
        cursorFind.size() == 3

        cursorFind.any{ it.message == "Read item: SalesPlanDetail{branchId='000001', year=2016, month=1, customerId='0000000001', amount=1000000000}" }
        cursorFind.any{ it.message == "Read item: SalesPlanDetail{branchId='000002', year=2017, month=2, customerId='0000000002', amount=2000000000}" }
        cursorFind.any{ it.message == "Read item: SalesPlanDetail{branchId='000003', year=2018, month=3, customerId='0000000003', amount=3000000000}" }
    }

    // 4.2
    def "Read fixed byte length file skipping header record. (not CSV file)"() {
        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/fileaccess/jobReadFixedLengthSkipHeader.xml',
                jobName: 'jobReadFixedLengthSkipHeader',
                jobParameter: "inputFile=files/test/input/ch05/fileaccess/sales_plan_detail_12.txt"))

        then:
        exitCode == 0

        def cursorFind = mongoUtil.find(
                new LogCondition(
                        logger: LoggingItemReaderListener.class.name,
                        level: 'INFO'
                ))
        cursorFind.size() == 3

        cursorFind.any{ it.message == "Read item: SalesPlanDetail{branchId='000001', year=2016, month=1, customerId='0000000001', amount=1000000000}" }
        cursorFind.any{ it.message == "Read item: SalesPlanDetail{branchId='000002', year=2017, month=2, customerId='0000000002', amount=2000000000}" }
        cursorFind.any{ it.message == "Read item: SalesPlanDetail{branchId='000003', year=2018, month=3, customerId='0000000003', amount=3000000000}" }
    }

    // 4.3
    def "Read CSV file skipping header and footer record using shell script."() {
        setup:
        def script = "scripts/ch05/fileaccess/delete_header_and_footer.sh"
        def originInputPath = "files/test/input/ch05/fileaccess/sales_plan_detail_13.csv"
        def editedInputPath = outputDir + "/sales_plan_detail.csv"
        def headerRecordNum = 2
        def footerRecordNum = 2
        def command = "sh ${script} ${originInputPath} ${editedInputPath} ${headerRecordNum} ${footerRecordNum}"

        when:
        Process p = jobLauncher.executeProcess(command)
        p.waitForOrKill(TimeUnit.MILLISECONDS.toMillis(10 * 1000L))
        int exitCode1 = p.exitValue()

        then:
        exitCode1 == 0

        def actual = new File(editedInputPath).readLines()
        def expect = new File("files/expect/output/ch05/fileaccess/sales_plan_detail_05.csv").readLines()
        actual == expect

        when:
        int exitCode2 = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/common/jobSalesPlan01.xml',
                jobName: 'jobSalesPlan01',
                jobParameter: "inputFile=" + editedInputPath))

        then:
        exitCode2 == 0

        def cursorFind = mongoUtil.find(
                new LogCondition(
                        logger: LoggingItemReaderListener.class.name,
                        level: 'INFO'
                ))
        cursorFind.size() == 3

        cursorFind.any{ it.message == "Read item: SalesPlanDetail{branchId='000001', year=2016, month=1, customerId='0000000001', amount=1000000000}" }
        cursorFind.any{ it.message == "Read item: SalesPlanDetail{branchId='000002', year=2017, month=2, customerId='0000000002', amount=2000000000}" }
        cursorFind.any{ it.message == "Read item: SalesPlanDetail{branchId='000003', year=2018, month=3, customerId='0000000003', amount=3000000000}" }
    }

    // 4.4
    def "Read CSV file. ItemProcessor refer to header record."() {
        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/fileaccess/jobReadCsvSkipAndReferHeader.xml',
                jobName: 'jobReadCsvSkipAndReferHeader',
                jobParameter: "inputFile=files/test/input/ch05/fileaccess/sales_plan_detail_14.csv"))

        then:
        exitCode == 0

        def planDetailLog = mongoUtil.find(
                new LogCondition(
                        logger: LoggingHeaderRecordItemProcessor.class.name,
                        level: 'INFO',
                        message: "Header recoed : Sales plan detail"
                ))
        planDetailLog.size() == 3
    }

    // 4.5
    def "Writing CSV file with header record."() {
        setup:
        jobDBUnitUtil.insert(simpleInitCustomerMstDataSet)

        def outputPath = outputDir + "/customer_list.csv"

        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/fileaccess/jobWriteCsvWithHeader.xml',
                jobName: 'jobWriteCsvWithHeader',
                jobParameter: "outputFile=" + outputPath))

        then:
        exitCode == 0

        def actual = new File(outputPath).readLines()
        def expect = new File("files/expect/output/ch05/fileaccess/customer_list_04.csv").readLines()
        actual == expect
    }

    // 4.6
    def "Reading and writing CSV file with footer record."() {
        setup:
        def script = "scripts/ch05/fileaccess/split_footer_and_other.sh"
        def originInputPath = "files/test/input/ch05/fileaccess/sales_plan_detail_15.csv"
        def dataInputPath = outputDir + "/sales_plan_detail_data.csv"
        def footerInputPath = outputDir + "/sales_plan_detail_footer.csv"
        def footerRecordNum = 2
        def command = "sh ${script} ${originInputPath} ${dataInputPath} ${footerInputPath} ${footerRecordNum}"

        when:
        Process p = jobLauncher.executeProcess(command)
        p.waitForOrKill(TimeUnit.MILLISECONDS.toMillis(10 * 1000L))
        int exitCode1 = p.exitValue()

        then:
        exitCode1 == 0

        def actualDataInput = new File(dataInputPath).readLines()
        def expectDataInput = new File("files/expect/output/ch05/fileaccess/sales_plan_detail_data_01.csv").readLines()
        actualDataInput == expectDataInput

        def actualFooterInput = new File(footerInputPath).readLines()
        def expectFooterInput = new File("files/expect/output/ch05/fileaccess/sales_plan_detail_footer_01.csv").readLines()
        actualFooterInput == expectFooterInput

        when:
        def outputPath = outputDir + "/sales_plan_detail.csv"
        int exitCode2 = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/fileaccess/jobReadAndWriteCsvWithFooter.xml',
                jobName: 'jobReadAndWriteCsvWithFooter',
                jobParameter: "dataInputFile=" + dataInputPath + " footerInputFile=" + footerInputPath
                        + " outputFile=" + outputPath))

        then:
        exitCode2 == 0

        def actualOutput = new File(outputPath).readLines()
        def expectOutput = new File("files/expect/output/ch05/fileaccess/sales_plan_detail_06.csv").readLines()
        actualOutput == expectOutput
    }

    // 5.1
    def "Reading multiple CSV files."() {
        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/fileaccess/jobReadMultipleCsv.xml',
                jobName: 'jobReadMultipleCsv',
                jobParameter: "inputFiles=files/test/input/ch05/fileaccess/sales_plan_detail_for_multi_*.csv"))

        then:
        exitCode == 0

        def cursorFind = mongoUtil.find(
                new LogCondition(
                        logger: LoggingItemReaderListener.class.name,
                        level: 'INFO'
                ))
        cursorFind.size() == 9

        def messages = new ArrayList()
        cursorFind.forEach { logCursor -> messages.add(logCursor.message) }
        messages.contains("Read item: SalesPlanDetail{branchId='000001', year=2016, month=1, customerId='0000000001', amount=1000000000}")
        messages.contains("Read item: SalesPlanDetail{branchId='000002', year=2017, month=2, customerId='0000000002', amount=2000000000}")
        messages.contains("Read item: SalesPlanDetail{branchId='000003', year=2018, month=3, customerId='0000000003', amount=3000000000}")
        messages.contains("Read item: SalesPlanDetail{branchId='000004', year=2019, month=4, customerId='0000000004', amount=4000000000}")
        messages.contains("Read item: SalesPlanDetail{branchId='000005', year=2020, month=5, customerId='0000000005', amount=5000000000}")
        messages.contains("Read item: SalesPlanDetail{branchId='000006', year=2021, month=6, customerId='0000000006', amount=6000000000}")
        messages.contains("Read item: SalesPlanDetail{branchId='000007', year=2022, month=7, customerId='0000000007', amount=7000000000}")
        messages.contains("Read item: SalesPlanDetail{branchId='000008', year=2023, month=8, customerId='0000000008', amount=8000000000}")
        messages.contains("Read item: SalesPlanDetail{branchId='000009', year=2024, month=9, customerId='0000000009', amount=9000000000}")
    }

    // 5.2
    def "Writing multiple CSV files."() {
        setup:
        def initCustomerMstDataSet = DBUnitUtil.createDataSet {
            customer_mst {
                customer_id | customer_name | customer_address | customer_tel | charge_branch_id | create_date | update_date
                "001" | "CustomerName001" | "CustomerAddress001" | "11111111111" | "001" | "[now]" | "[now]"
                "002" | "CustomerName002" | "CustomerAddress002" | "11111111111" | "002" | "[now]" | "[now]"
                "003" | "CustomerName003" | "CustomerAddress003" | "11111111111" | "003" | "[now]" | "[now]"
                "004" | "CustomerName004" | "CustomerAddress004" | "11111111111" | "004" | "[now]" | "[now]"
                "005" | "CustomerName005" | "CustomerAddress005" | "11111111111" | "005" | "[now]" | "[now]"
                "006" | "CustomerName006" | "CustomerAddress006" | "11111111111" | "006" | "[now]" | "[now]"
                "007" | "CustomerName007" | "CustomerAddress007" | "11111111111" | "007" | "[now]" | "[now]"
                "008" | "CustomerName008" | "CustomerAddress008" | "11111111111" | "008" | "[now]" | "[now]"
                "009" | "CustomerName009" | "CustomerAddress009" | "11111111111" | "009" | "[now]" | "[now]"
                "010" | "CustomerName010" | "CustomerAddress010" | "11111111111" | "010" | "[now]" | "[now]"
                "011" | "CustomerName011" | "CustomerAddress011" | "11111111111" | "011" | "[now]" | "[now]"
                "012" | "CustomerName012" | "CustomerAddress012" | "11111111111" | "012" | "[now]" | "[now]"
                "013" | "CustomerName013" | "CustomerAddress013" | "11111111111" | "013" | "[now]" | "[now]"
                "014" | "CustomerName014" | "CustomerAddress014" | "11111111111" | "014" | "[now]" | "[now]"
                "015" | "CustomerName015" | "CustomerAddress015" | "11111111111" | "015" | "[now]" | "[now]"
                "016" | "CustomerName016" | "CustomerAddress016" | "11111111111" | "016" | "[now]" | "[now]"
                "017" | "CustomerName017" | "CustomerAddress017" | "11111111111" | "017" | "[now]" | "[now]"
                "018" | "CustomerName018" | "CustomerAddress018" | "11111111111" | "018" | "[now]" | "[now]"
                "019" | "CustomerName019" | "CustomerAddress019" | "11111111111" | "019" | "[now]" | "[now]"
                "020" | "CustomerName020" | "CustomerAddress020" | "11111111111" | "020" | "[now]" | "[now]"
                "021" | "CustomerName021" | "CustomerAddress021" | "11111111111" | "021" | "[now]" | "[now]"
            }
        }
        jobDBUnitUtil.insert(initCustomerMstDataSet)

        def outputPath = outputDir + "/customer_list_"

        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/fileaccess/jobWriteMultipleCsv.xml',
                jobName: 'jobWriteMultipleCsv',
                jobParameter: "outputDir=" + outputPath))

        then:
        exitCode == 0

        def actual1 = new File(outputPath + "01.csv").readLines()
        def expect1 = new File("files/expect/output/ch05/fileaccess/customer_list_05.csv").readLines()
        actual1 == expect1
        def actual2 = new File(outputPath + "02.csv").readLines()
        def expect2 = new File("files/expect/output/ch05/fileaccess/customer_list_06.csv").readLines()
        actual2 == expect2
        def actual3 = new File(outputPath + "03.csv").readLines()
        def expect3 = new File("files/expect/output/ch05/fileaccess/customer_list_07.csv").readLines()
        actual3 == expect3
        def actual4 = new File(outputPath + "04.csv").readLines()
        def expect4 = new File("files/expect/output/ch05/fileaccess/customer_list_08.csv").readLines()
        actual4 == expect4
        def actual5 = new File(outputPath + "05.csv").readLines()
        def expect5 = new File("files/expect/output/ch05/fileaccess/customer_list_09.csv").readLines()
        actual5 == expect5
    }

    // 6.1
    def "Reading XML file using JAXB."() {
        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/fileaccess/jobReadXml.xml',
                jobName: 'jobReadXml',
                jobParameter: "inputFile=files/test/input/ch05/fileaccess/sales_plan_detail_16.xml"))

        then:
        exitCode == 0

        def cursorFind = mongoUtil.find(
                new LogCondition(
                        logger: LoggingItemReaderListener.class.name,
                        level: 'INFO'
                ))
        cursorFind.size() == 3

        cursorFind.any{ it.message == "Read item: SalesPlanDetail{branchId='000001', year=2016, month=1, customerId='0000000001', amount=1000000000}" }
        cursorFind.any{ it.message == "Read item: SalesPlanDetail{branchId='000002', year=2017, month=2, customerId='0000000002', amount=2000000000}" }
        cursorFind.any{ it.message == "Read item: SalesPlanDetail{branchId='000003', year=2018, month=3, customerId='0000000003', amount=3000000000}" }
    }

    // 6.2
    def "Writing XML file using JAXB."() {
        setup:
        jobDBUnitUtil.insert(simpleInitCustomerMstDataSet)

        def outputPath = outputDir + "/customer_list.xml"

        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/fileaccess/jobWriteXml.xml',
                jobName: 'jobWriteXml',
                jobParameter: "outputFile=" + outputPath))

        then:
        exitCode == 0

        def actual = new File(outputPath).readLines()
        def expect = new File("files/expect/output/ch05/fileaccess/customer_list_10.xml").readLines()
        actual == expect
    }

    // 6.3
    def "Writing XML file using JAXB with header and footer."() {
        setup:
        jobDBUnitUtil.insert(simpleInitCustomerMstDataSet)

        def outputPath = outputDir + "/customer_list.xml"

        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/fileaccess/jobWriteXmlWithHeaderAndFooter.xml',
                jobName: 'jobWriteXmlWithHeaderAndFooter',
                jobParameter: "outputFile=" + outputPath))

        then:
        exitCode == 0

        def actual = new File(outputPath).readLines()
        def expect = new File("files/expect/output/ch05/fileaccess/customer_list_11.xml").readLines()
        actual == expect
    }

    // 6.4
    def "Reading XML file with schema validation using JAXB."() {
        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/fileaccess/jobReadXmlWithSchemaValidation.xml',
                jobName: 'jobReadXmlWithSchemaValidation',
                jobParameter: "inputFile=files/test/input/ch05/fileaccess/customer_01.xml"))

        then:
        exitCode == 0

        def cursorFind = mongoUtil.find(
                new LogCondition(
                        logger: LoggingItemReaderListener.class.name,
                        level: 'INFO'
                ))

        cursorFind.size() == 3

        cursorFind.any {
            it.message == "Read item: Customer{name='Data Taro', phoneNumbers='[01234567890]'}"
        }
        cursorFind.any {
            it.message == "Read item: Customer{name='Data Jiro', phoneNumbers='[01234567891, 01234567892]'}"
        }
        cursorFind.any {
            it.message == "Read item: Customer{name='Data Kazuo', phoneNumbers='[01234567893, 01234567894]'}"
        }
    }

    // 6.5
    def "Reading Error XML file with schema validation using JAXB."() {
        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/fileaccess/jobReadXmlWithSchemaValidation.xml',
                jobName: 'jobReadXmlWithSchemaValidation',
                jobParameter: "inputFile=files/test/input/ch05/fileaccess/customer_02.xml"))

        then:
        exitCode == 255

        def throwableCursor = mongoUtil.findOne(
                new LogCondition(
                        logger: LoggingItemReaderListener.class.name,
                        level: 'ERROR'
                ))

        throwableCursor.message == "Exception occurred while reading."

        throwableCursor.throwable._class == UnmarshallingFailureException
        throwableCursor.throwable.message.contains("Value 'Data Hanako' with length = '11' is not facet-valid with respect to maxLength '10' for type 'stringMaxSize'.")
        throwableCursor.throwable.stackTrace.size() > 0
    }

    // 7.1
    def "Reading multi layout CSV file."() {
        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/fileaccess/jobReadMultiLayoutCsv.xml',
                jobName: 'jobReadMultiLayoutCsv',
                jobParameter: "inputFile=files/test/input/ch05/fileaccess/sales_plan_detail_17.csv"))

        then:
        exitCode == 0

        def cursorFind = mongoUtil.find(
                new LogCondition(
                        logger: LoggingItemReaderListener.class.name,
                        level: 'INFO'
                ))
        cursorFind.size() == 16

        cursorFind.any{ it.message == "Read item: SalesPlanDetailHeader{record='H', description='Sales_plan_detail header No.1'}" }
        cursorFind.any{ it.message == "Read item: SalesPlanDetailData{record='D', branchId='000001', year=2016, month=1, customerId='0000000001', amount=100000000}" }
        cursorFind.any{ it.message == "Read item: SalesPlanDetailData{record='D', branchId='000001', year=2016, month=1, customerId='0000000002', amount=200000000}" }
        cursorFind.any{ it.message == "Read item: SalesPlanDetailData{record='D', branchId='000001', year=2016, month=1, customerId='0000000003', amount=300000000}" }
        cursorFind.any{ it.message == "Read item: SalesPlanDetailTrailer{record='T', branchId='000001', number=3, total=600000000}" }

        cursorFind.any{ it.message == "Read item: SalesPlanDetailHeader{record='H', description='Sales_plan_detail header No.2'}" }
        cursorFind.any{ it.message == "Read item: SalesPlanDetailData{record='D', branchId='00002', year=2016, month=1, customerId='0000000004', amount=400000000}" }
        cursorFind.any{ it.message == "Read item: SalesPlanDetailData{record='D', branchId='00002', year=2016, month=1, customerId='0000000005', amount=500000000}" }
        cursorFind.any{ it.message == "Read item: SalesPlanDetailData{record='D', branchId='00002', year=2016, month=1, customerId='0000000006', amount=600000000}" }
        cursorFind.any{ it.message == "Read item: SalesPlanDetailTrailer{record='T', branchId='00002', number=3, total=1500000000}" }

        cursorFind.any{ it.message == "Read item: SalesPlanDetailHeader{record='H', description='Sales_plan_detail header No.3'}" }
        cursorFind.any{ it.message == "Read item: SalesPlanDetailData{record='D', branchId='00003', year=2016, month=1, customerId='0000000007', amount=700000000}" }
        cursorFind.any{ it.message == "Read item: SalesPlanDetailData{record='D', branchId='00003', year=2016, month=1, customerId='0000000008', amount=800000000}" }
        cursorFind.any{ it.message == "Read item: SalesPlanDetailData{record='D', branchId='00003', year=2016, month=1, customerId='0000000009', amount=900000000}" }
        cursorFind.any{ it.message == "Read item: SalesPlanDetailTrailer{record='T', branchId='00003', number=3, total=2400000000}" }

        cursorFind.any{ it.message == "Read item: SalesPlanDetailEnd{record='E', headNum=3, trailerNum=9, total=4500000000}" }
    }

    // 7.2
    def "Writing multi layout CSV file."() {
        setup:
        def outputPath = outputDir + "/sales_plan_detail.csv"

        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/fileaccess/jobWriteMultiLayoutCsv.xml',
                jobName: 'jobWriteMultiLayoutCsv',
                jobParameter: "inputFile=files/test/input/ch05/fileaccess/sales_plan_detail_17.csv " +
                        "outputFile=" + outputPath))

        then:
        exitCode == 0

        def actual = new File(outputPath).readLines()
        def expect = new File("files/expect/output/ch05/fileaccess/sales_plan_detail_07.csv").readLines()
        actual == expect
    }
    
    // 8
    def "Control break using SingleItemPeekableItemReader"() {
        setup:
        def inputFileName = "./files/test/input/ch05/fileaccess/sales_performance_detail_01.csv"
        def outputFileName = "./files/test/output/ch05/fileaccess/sales_performance_detail_control_break_01.csv"
        def expectFileName = "./files/expect/output/ch05/fileaccess/sales_performance_detail_control_break_01.csv"

        def outputFile = new File(outputFileName)
        def expectFile = new File(expectFileName)

        Files.deleteIfExists(outputFile.toPath())

        when:
        def exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/fileaccess/jobControlBreak.xml',
                jobName: 'jobControlBreak',
                jobParameter: "inputFile=${inputFileName} outputFile=${outputFileName}"
        ))

        then:
        exitCode == 0
        outputFile.readLines() == expectFile.readLines()

        cleanup:
        Files.deleteIfExists(outputFile.toPath())

    }
}

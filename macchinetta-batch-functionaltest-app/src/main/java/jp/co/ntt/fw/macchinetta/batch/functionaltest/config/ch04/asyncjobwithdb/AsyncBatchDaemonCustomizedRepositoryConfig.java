/*
 * Copyright (C) 2023 NTT Corporation
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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.config.ch04.asyncjobwithdb;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.config.AsyncBatchDaemonConfig;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.asyncjobwithdb.repository.CustomizedBatchJobRequestRepository;

/**
 * Customized Acync Batch Daemon Configuration.
 * (customized job request table)
 *
 * @since 2.4.0
 */
@Configuration
@Import(AsyncBatchDaemonConfig.class)
public class AsyncBatchDaemonCustomizedRepositoryConfig {

    @Bean
    public MapperFactoryBean<CustomizedBatchJobRequestRepository> batchJobRequestRepository(
            @Qualifier("adminSqlSessionFactory") SqlSessionFactory adminSqlSessionFactory) {
        final MapperFactoryBean<CustomizedBatchJobRequestRepository> mapperFactoryBean = new MapperFactoryBean<>();
        mapperFactoryBean.setMapperInterface(
                CustomizedBatchJobRequestRepository.class);
        mapperFactoryBean.setSqlSessionFactory(adminSqlSessionFactory);
        return mapperFactoryBean;
    }
}

/*
 * Copyright (C) 2024 NTT Corporation
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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.config.web;

import java.io.IOException;
import java.util.List;

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.ApplicationContextFactory;
import org.springframework.batch.core.configuration.support.AutomaticJobRegistrar;
import org.springframework.batch.core.configuration.support.DefaultJobLoader;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperFactoryBean;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.config.LaunchContextConfig;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.config.helper.ApplicationContextFactoryHelper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;

/**
 * Spring MVC Rest Configuration.
 * @since 2.6.0
 */
@Configuration
@Import(LaunchContextConfig.class)
@EnableAspectJAutoProxy
@ComponentScan("jp.co.ntt.fw.macchinetta.batch.functionaltest.api")
@EnableWebMvc
public class SpringMvcRestConfig implements WebMvcConfigurer {

    @Override
    public void configureMessageConverters(
            List<HttpMessageConverter<?>> converters) {
        converters.add(jsonMessageConverter(objectMapper()));
    }

    @Bean("jsonMessageConverter")
    public MappingJackson2HttpMessageConverter jsonMessageConverter(
            ObjectMapper objectMapper) {
        MappingJackson2HttpMessageConverter bean = new MappingJackson2HttpMessageConverter(
                objectMapper);
        return bean;
    }

    @Bean("objectMapper")
    public ObjectMapper objectMapper() {
        Jackson2ObjectMapperFactoryBean bean = new Jackson2ObjectMapperFactoryBean();
        bean.setDateFormat(stdDateFormat());
        bean.afterPropertiesSet();
        return bean.getObject();
    }

    @Bean
    public StdDateFormat stdDateFormat() {
        return new StdDateFormat();
    }

    @Bean
    public AutomaticJobRegistrar automaticJobRegistrar(JobRegistry jobRegistry,
                                                       ApplicationContextFactory[] applicationContextFactories) throws Exception {
        final AutomaticJobRegistrar automaticJobRegistrar = new AutomaticJobRegistrar();
        final DefaultJobLoader defaultJobLoader = new DefaultJobLoader();
        defaultJobLoader.setJobRegistry(jobRegistry);
        automaticJobRegistrar.setApplicationContextFactories(
                applicationContextFactories);
        automaticJobRegistrar.setJobLoader(defaultJobLoader);
        automaticJobRegistrar.afterPropertiesSet();
        return automaticJobRegistrar;
    }

    @Bean
    public ApplicationContextFactory[] applicationContextFactories(
            final ApplicationContext ctx) throws IOException {
        return new ApplicationContextFactoryHelper(ctx).load(
                "classpath:jp/co/ntt/fw/macchinetta/batch/functionaltest/jobs/common/*.class",
                "classpath:jp/co/ntt/fw/macchinetta/batch/functionaltest/jobs/ch04/asyncjobwithweb/*.class");
    }

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(3);
        taskExecutor.setMaxPoolSize(3);
        taskExecutor.setQueueCapacity(10);
        return taskExecutor;
    }

    @Bean
    public JobLauncher jobLauncher(JobRepository jobRepository,
                                   TaskExecutor taskExecutor) {
        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.setTaskExecutor(taskExecutor);
        return jobLauncher;
    }
}

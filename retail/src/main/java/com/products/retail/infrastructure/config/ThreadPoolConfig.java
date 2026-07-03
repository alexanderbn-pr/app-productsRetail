package com.products.retail.infrastructure.config;

import com.products.retail.infrastructure.config.ThreadPoolProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ThreadPoolConfig {

    @Bean(name = "productDetailExecutor")
    public ExecutorService productDetailExecutor(ThreadPoolProperties props) {
        return new ThreadPoolExecutor(
                props.coreSize(), props.maxSize(),
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(props.queueCapacity()),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }
}

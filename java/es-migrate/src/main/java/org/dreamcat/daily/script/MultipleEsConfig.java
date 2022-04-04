package org.dreamcat.daily.script;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.extern.slf4j.Slf4j;
import org.dreamcat.jwrap.elasticsearch.util.RestClientUtil;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Create by tuke on 2021/3/22
 */
@Slf4j
@Configuration
public class MultipleEsConfig {

    @Value("${elastic.source.host}")
    private String sourceHost;

    @Value("${elastic.source.port:9200}")
    private int sourcePort;

    @Value("${elastic.source.username:}")
    private String sourceUsername;

    @Value("${elastic.source.password:}")
    private String sourcePassword;

    @Value("${elastic.target.host}")
    private String targetHost;

    @Value("${elastic.target.port:9200}")
    private int targetPort;

    @Value("${elastic.target.username:}")
    private String targetUsername;

    @Value("${elastic.target.password:}")
    private String targetPassword;

    @Bean
    public ElasticsearchClient sourceClient() {
        RestClientBuilder restClientBuilder = RestClientUtil.restClientBuilder(
                sourceHost, sourcePort, sourceUsername, sourcePassword);
        return RestClientUtil.elasticsearchClient(restClientBuilder);
    }

    @Bean
    public ElasticsearchClient targetClient() {
        RestClientBuilder restClientBuilder = RestClientUtil.restClientBuilder(
                targetHost, targetPort, targetUsername, targetPassword);
        return RestClientUtil.elasticsearchClient(restClientBuilder);
    }
}


package org.dreamcat.daily.script;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import javax.annotation.Resource;
import org.dreamcat.jwrap.elasticsearch.EsDocClient;
import org.dreamcat.jwrap.elasticsearch.EsIndexClient;
import org.dreamcat.jwrap.elasticsearch.EsQueryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Create by tuke on 2021/3/22
 */
@Configuration
public class AppConfig {

    @Resource(name = "sourceClient")
    private ElasticsearchClient sourceClient;

    @Resource(name = "targetClient")
    private ElasticsearchClient targetClient;

    @Bean
    public EsIndexClient sourceEsIndexClient() {
        return new EsIndexClient(sourceClient);
    }

    @Bean
    public EsQueryClient sourceEsQueryClient() {
        return new EsQueryClient(sourceClient);
    }

    @Bean
    public EsIndexClient targetEsIndexClient() {
        return new EsIndexClient(targetClient);
    }

    @Bean
    public EsDocClient targetEsDocClient() {
        return new EsDocClient(targetClient);
    }
}

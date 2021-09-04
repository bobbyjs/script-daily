package org.dreamcat.daily.script;

import javax.annotation.Resource;
import org.dreamcat.jwrap.elasticsearch.EsDocumentComponent;
import org.dreamcat.jwrap.elasticsearch.EsIndexComponent;
import org.dreamcat.jwrap.elasticsearch.EsSearchComponent;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Create by tuke on 2021/3/22
 */
@Configuration
public class AppConfig {

    @Resource(name = "sourceRestHighLevelClient")
    private RestHighLevelClient sourceRestHighLevelClient;

    @Resource(name = "targetRestHighLevelClient")
    private RestHighLevelClient targetRestHighLevelClient;

    @Bean(name = "sourceEsIndexComponent")
    public EsIndexComponent sourceEsIndexComponent() {
        return new EsIndexComponent(sourceRestHighLevelClient);
    }

    @Bean(name = "sourceEsSearchComponent")
    public EsSearchComponent sourceEsSearchComponent() {
        return new EsSearchComponent(sourceRestHighLevelClient);
    }

    @Bean(name = "targetEsIndexComponent")
    public EsIndexComponent targetEsIndexComponent() {
        return new EsIndexComponent(targetRestHighLevelClient);
    }

    @Bean(name = "targetEsDocumentComponent")
    public EsDocumentComponent targetEsDocumentComponent() {
        return new EsDocumentComponent(targetRestHighLevelClient);
    }
}

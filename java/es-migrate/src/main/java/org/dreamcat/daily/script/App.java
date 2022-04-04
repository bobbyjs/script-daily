package org.dreamcat.daily.script;

import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.dreamcat.daily.script.common.SchemaHandler;
import org.dreamcat.jwrap.elasticsearch.EsDocClient;
import org.dreamcat.jwrap.elasticsearch.EsIndexClient;
import org.dreamcat.jwrap.elasticsearch.EsQueryClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;

/**
 * Create by tuke on 2021/3/22
 */
@Slf4j
@SpringBootApplication(exclude = {
        ElasticsearchRepositoriesAutoConfiguration.class,
        ElasticsearchRestClientAutoConfiguration.class,
        ElasticsearchDataAutoConfiguration.class
})
public class App implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(App.class, args).close();
    }

    @Value("${elastic.index.converter:}")
    private String indexConverter;
    @Value("${elastic.index.settings}")
    private String indexSettings;

    @Resource(name = "sourceEsIndexClient")
    private EsIndexClient sourceEsIndexClient;
    @Resource(name = "sourceEsQueryClient")
    private EsQueryClient sourceEsQueryClient;
    @Resource(name = "targetEsIndexClient")
    private EsIndexClient targetEsIndexClient;
    @Resource(name = "targetEsDocClient")
    private EsDocClient targetEsDocClient;

    @Override
    public void run(String... args) {
        SchemaHandler<?> schemaHandler = EsSchemaMigrateHandler.builder()
                .indexSettings(indexSettings)
                .indexConverter(indexConverter)
                .sourceEsIndexClient(sourceEsIndexClient)
                .sourceEsQueryClient(sourceEsQueryClient)
                .targetEsIndexClient(targetEsIndexClient)
                .targetEsDocClient(targetEsDocClient)
                .build();
        schemaHandler.run(args);
    }
}

package org.dreamcat.daily.script;

import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.dreamcat.daily.script.common.SchemaHandler;
import org.dreamcat.jwrap.elasticsearch.EsDocumentComponent;
import org.dreamcat.jwrap.elasticsearch.EsIndexComponent;
import org.dreamcat.jwrap.elasticsearch.EsSearchComponent;
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

    @Resource(name = "sourceEsIndexComponent")
    private EsIndexComponent sourceEsIndexComponent;
    @Resource(name = "sourceEsSearchComponent")
    private EsSearchComponent sourceEsSearchComponent;
    @Resource(name = "targetEsIndexComponent")
    private EsIndexComponent targetEsIndexComponent;
    @Resource(name = "targetEsDocumentComponent")
    private EsDocumentComponent targetEsDocumentComponent;

    @Override
    public void run(String... args) {
        SchemaHandler<?> schemaHandler = ElasticsearchSchemaMigrateHandler.builder()
                .indexSettings(indexSettings)
                .indexConverter(indexConverter)
                .sourceEsIndexComponent(sourceEsIndexComponent)
                .sourceEsSearchComponent(sourceEsSearchComponent)
                .targetEsIndexComponent(targetEsIndexComponent)
                .targetEsDocumentComponent(targetEsDocumentComponent)
                .build();
        schemaHandler.run(args);
    }
}

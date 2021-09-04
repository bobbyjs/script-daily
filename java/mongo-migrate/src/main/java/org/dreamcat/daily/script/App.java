package org.dreamcat.daily.script;

import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.dreamcat.daily.script.common.SchemaHandler;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Create by tuke on 2021/3/22
 */
@Slf4j
@SpringBootApplication(exclude = {
        MongoAutoConfiguration.class,
        MongoDataAutoConfiguration.class
})
public class App implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Resource(name = "sourceMongoTemplate")
    private MongoTemplate sourceMongoTemplate;
    @Resource(name = "targetMongoTemplate")
    private MongoTemplate targetMongoTemplate;

    @Override
    public void run(String... args) {
        SchemaHandler<?> schemaHandler = MongoSchemaMigrateHandler.builder()
                .sourceMongoTemplate(sourceMongoTemplate)
                .targetMongoTemplate(targetMongoTemplate)
                .build();
        schemaHandler.run(args);
    }
}

package org.dreamcat.daily.script;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * Create by tuke on 2021/3/22
 */
@EnableMongoRepositories()
@Configuration
public class MultipleMongoConfig {

    @Value("${mongodb.source.url}")
    private String sourceUrl;

    @Value("${mongodb.target.url}")
    private String targetUrl;

    @Bean(name = "sourceMongoTemplate")
    public MongoTemplate sourceMongoTemplate() {
        MongoDatabaseFactory mongoDatabaseFactory = new SimpleMongoClientDatabaseFactory(sourceUrl);
        return new MongoTemplate(mongoDatabaseFactory);
    }

    @Bean(name = "targetMongoTemplate")
    public MongoTemplate targetMongoTemplate() {
        MongoDatabaseFactory mongoDatabaseFactory = new SimpleMongoClientDatabaseFactory(targetUrl);
        return new MongoTemplate(mongoDatabaseFactory);
    }
}

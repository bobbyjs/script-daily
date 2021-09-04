package org.dreamcat.daily.script;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.dreamcat.common.x.jackson.JacksonUtil;
import org.dreamcat.daily.script.common.SchemaHandler;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Create by tuke on 2021/3/22
 */
@Slf4j
@Builder
@AllArgsConstructor
@SuppressWarnings({"rawtypes"})
public class MongoSchemaMigrateHandler extends SchemaHandler<String> {

    private final MongoTemplate sourceMongoTemplate;
    private final MongoTemplate targetMongoTemplate;

    @Override
    public String getSchemaKeyword() {
        return "collection";
    }

    @Override
    public Iterable<String> getSchemas() {
        return sourceMongoTemplate.getCollectionNames();
    }


    @Override
    public void handle(String collectionName) {
        if (targetMongoTemplate.collectionExists(collectionName)) {
            if (force) {
                log.warn("drop target collection {}", collectionName);
                if (effect) {
                    targetMongoTemplate.dropCollection(collectionName);
                }
            } else {
                log.warn("collection {} already exists in target source", collectionName);
                return;
            }
        }

        List<Map> list = sourceMongoTemplate.findAll(Map.class, collectionName);
        log.info("migrate collection {}, find {} records", collectionName, list.size());
        for (Map map : list) {
            if (verbose) {
                log.info("migrate collection {}, record: {}",
                        collectionName, JacksonUtil.toJson(map));
            }
            if (effect) {
                targetMongoTemplate.save(map, collectionName);
            }
        }
    }
}

package org.dreamcat.daily.script;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.script.ScriptException;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.dreamcat.common.Pair;
import org.dreamcat.common.script.DelegateScriptEngine;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.json.JsonUtil;
import org.dreamcat.daily.script.common.SchemaMigrateHandler;
import org.dreamcat.jwrap.elasticsearch.EsDocClient;
import org.dreamcat.jwrap.elasticsearch.EsIndexClient;
import org.dreamcat.jwrap.elasticsearch.EsQueryClient;
import org.dreamcat.jwrap.elasticsearch.EsQueryClient.ScrollIter;

/**
 * Create by tuke on 2021/3/22
 */
@Slf4j
public class EsSchemaMigrateHandler extends SchemaMigrateHandler<String> {

    private final String indexSettings;
    private final String indexConverter;
    private final EsIndexClient sourceEsIndexClient;
    private final EsQueryClient sourceEsQueryClient;
    private final EsIndexClient targetEsIndexClient;
    private final EsDocClient targetEsDocClient;
    private final DelegateScriptEngine scriptEngine;

    @Builder
    public EsSchemaMigrateHandler(
            String indexSettings,
            String indexConverter,
            EsIndexClient sourceEsIndexClient,
            EsQueryClient sourceEsQueryClient,
            EsIndexClient targetEsIndexClient,
            EsDocClient targetEsDocClient) {
        this.indexSettings = indexSettings;
        this.indexConverter = indexConverter;
        this.sourceEsIndexClient = sourceEsIndexClient;
        this.sourceEsQueryClient = sourceEsQueryClient;
        this.targetEsIndexClient = targetEsIndexClient;
        this.targetEsDocClient = targetEsDocClient;
        this.scriptEngine = new DelegateScriptEngine();
    }

    @Override
    public String getSchemaKeyword() {
        return "index";
    }

    @Override
    protected Iterable<String> getSchemas() {
        return sourceEsIndexClient.getAllIndex();
    }

    @Override
    protected String getTargetSchema(String sourceSchema) {
        if (ObjectUtil.isNotBlank(indexConverter)) {
            try {
                return scriptEngine.evalWith(indexConverter, sourceSchema);
            } catch (ScriptException e) {
                String message = String.format("failed to format schema %s with converter `%s`, %s",
                        sourceSchema, indexConverter, e.getMessage());
                throw new RuntimeException(message, e);
            }
        }
        return super.getTargetSchema(sourceSchema);
    }

    @Override
    protected boolean existsSchema(String schema) {
        return targetEsIndexClient.existsIndex(schema);
    }

    @Override
    protected void deleteSchema(String schema) {
        if (verbose) {
            log.warn("delete {} {}", getSchemaKeyword(), schema);
        }
        if (effect) {
            targetEsIndexClient.deleteIndex(schema);
        }
    }

    @Override
    protected void createSchema(String sourceSchema, String schema) {
        Pair<String, String> pair = sourceEsIndexClient.getIndex(sourceSchema);
        String mappings = pair.first();
        if (verbose) {
            log.info("create index {}, mappings={}, settings={}",
                    schema, mappings, indexSettings);
        }
        if (effect) {
            targetEsIndexClient.createIndex(schema, mappings, indexSettings);
        }
    }

    @Override
    protected void migrateSchema(String sourceSchema, String targetSchema) {
        long total = sourceEsQueryClient.count(sourceSchema);
        log.info("migrate index {} to {}, find {} records", sourceSchema, targetSchema, total);
        if (total == 0) return;

        // huge index, then
        try (ScrollIter<Map<String, Object>> scrollIter = sourceEsQueryClient.scrollIter(
                sourceSchema, DEFAULT_SIZE, DEFAULT_KEEP_ALIVE)) {
            while (scrollIter.hasNext()) {
                List<Map<String, Object>> list = scrollIter.next();
                if (list.isEmpty()) continue;

                Map<String, String> idJsonMap = list.stream().collect(Collectors.toMap(
                        it -> it.get("id").toString(), JsonUtil::toJson, (a, b) -> a));
                if (verbose) {
                    log.info("migrate index {} to {}, bulk records: {}",
                            sourceSchema, targetSchema, idJsonMap);
                }
                if (effect) {
                    targetEsDocClient.bulkSave(targetSchema, idJsonMap);
                }
            }
        }
    }

    private static final int DEFAULT_SIZE = 1024;
    private static final int DEFAULT_KEEP_ALIVE = 60; // 60s
}

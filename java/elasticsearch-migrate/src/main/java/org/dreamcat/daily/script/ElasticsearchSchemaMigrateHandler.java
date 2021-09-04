package org.dreamcat.daily.script;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.script.ScriptException;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.dreamcat.common.core.Pair;
import org.dreamcat.common.script.DelegateScriptEngine;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.x.jackson.JacksonUtil;
import org.dreamcat.daily.script.common.SchemaMigrateHandler;
import org.dreamcat.jwrap.elasticsearch.EsDocumentComponent;
import org.dreamcat.jwrap.elasticsearch.EsIndexComponent;
import org.dreamcat.jwrap.elasticsearch.EsSearchComponent;
import org.dreamcat.jwrap.elasticsearch.EsSearchComponent.ScrollMapIter;
import org.elasticsearch.common.unit.TimeValue;

/**
 * Create by tuke on 2021/3/22
 */
@Slf4j
public class ElasticsearchSchemaMigrateHandler extends SchemaMigrateHandler<String> {

    private final String indexSettings;
    private final String indexConverter;
    private final EsIndexComponent sourceEsIndexComponent;
    private final EsSearchComponent sourceEsSearchComponent;
    private final EsIndexComponent targetEsIndexComponent;
    private final EsDocumentComponent targetEsDocumentComponent;
    private final DelegateScriptEngine scriptEngine;

    @Builder
    public ElasticsearchSchemaMigrateHandler(
            String indexSettings,
            String indexConverter,
            EsIndexComponent sourceEsIndexComponent,
            EsSearchComponent sourceEsSearchComponent,
            EsIndexComponent targetEsIndexComponent,
            EsDocumentComponent targetEsDocumentComponent) {
        this.indexSettings = indexSettings;
        this.indexConverter = indexConverter;
        this.sourceEsIndexComponent = sourceEsIndexComponent;
        this.sourceEsSearchComponent = sourceEsSearchComponent;
        this.targetEsIndexComponent = targetEsIndexComponent;
        this.targetEsDocumentComponent = targetEsDocumentComponent;
        this.scriptEngine = new DelegateScriptEngine();
    }

    @Override
    public String getSchemaKeyword() {
        return "index";
    }

    @Override
    protected Iterable<String> getSchemas() {
        return sourceEsIndexComponent.getAllIndex();
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
        return targetEsIndexComponent.existsIndex(schema);
    }

    @Override
    protected void deleteSchema(String schema) {
        if (verbose) {
            log.warn("delete {} {}", getSchemaKeyword(), schema);
        }
        if (effect) {
            targetEsIndexComponent.deleteIndex(schema);
        }
    }

    @Override
    protected void createSchema(String sourceSchema, String schema) {
        Pair<String, String> pair = sourceEsIndexComponent.getIndex(sourceSchema);
        String mappings = pair.first();
        if (verbose) {
            log.info("create index {}, mappings={}, settings={}",
                    schema, mappings, indexSettings);
        }
        if (effect) {
            targetEsIndexComponent.createIndex(schema, mappings, indexSettings);
        }
    }

    @Override
    protected void migrateSchema(String sourceSchema, String targetSchema) {
        long total = sourceEsSearchComponent.count(sourceSchema);
        log.info("migrate index {} to {}, find {} records", sourceSchema, targetSchema, total);
        if (total == 0) return;

        // huge index, then
        try (ScrollMapIter scrollIter = sourceEsSearchComponent.scrollMapIter(
                sourceSchema, DEFAULT_SIZE, TimeValue.timeValueMillis(DEFAULT_KEEP_ALIVE))) {
            while (scrollIter.hasNext()) {
                List<Map<String, Object>> list = scrollIter.next();
                if (list.isEmpty()) continue;

                Map<String, String> idJsonMap = list.stream().collect(Collectors.toMap(
                        it -> it.get("id").toString(), JacksonUtil::toJson, (a, b) -> a));
                if (verbose) {
                    log.info("migrate index {} to {}, bulk records: {}",
                            sourceSchema, targetSchema, idJsonMap);
                }
                if (effect) {
                    targetEsDocumentComponent.bulkSave(targetSchema, idJsonMap);
                }
            }
        }
    }

    private static final int DEFAULT_SIZE = 1024;
    private static final long DEFAULT_KEEP_ALIVE = 60L * 1000;
}

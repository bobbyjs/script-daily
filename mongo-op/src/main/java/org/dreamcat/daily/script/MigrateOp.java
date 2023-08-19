package org.dreamcat.daily.script;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.print.Doc;
import javax.script.ScriptException;
import org.bson.Document;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.json.JsonUtil;
import org.dreamcat.common.script.DelegateScriptEngine;
import org.dreamcat.common.text.PatternUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.daily.script.common.BaseHandler;
import org.dreamcat.daily.script.common.CliUtil;

/**
 * Create by tuke on 2021/3/22
 */
public class MigrateOp extends BaseHandler {

    @ArgParserField({"c", "cc"})
    private String collectionConverter;
    private String sourceDatabase;
    private String targetDatabase;
    private boolean exclude;
    @ArgParserField(firstChar = true)
    private List<Pattern> patterns;
    @ArgParserField(firstChar = true)
    private boolean merge;
    @ArgParserField(firstChar = true)
    private boolean force;
    private boolean metaOnly;
    private boolean abort;
    @ArgParserField({"y", "yes"})
    private boolean effect; // actually modify or not
    @ArgParserField("V")
    private boolean verbose;
    @ArgParserField(firstChar = true)
    private int batchSize = 1024;

    @ArgParserField(nested = true, nestedPrefix = "source.")
    private MongoModule source;
    @ArgParserField(nested = true, nestedPrefix = "target.")
    private MongoModule target;

    private final DelegateScriptEngine scriptEngine = new DelegateScriptEngine();

    @Override
    protected void afterPropertySet() throws Exception {
        super.afterPropertySet();
        source.afterPropertySet();
        target.afterPropertySet();
        CliUtil.checkParameter(sourceDatabase, "--source-database");
        if (ObjectUtil.isBlank(targetDatabase)) {
            targetDatabase = sourceDatabase;
        }
    }

    @Override
    public void run() throws Exception {
        MongoDatabase sourceDb = source.client.getDatabase(sourceDatabase);
        MongoDatabase targetDb = target.client.getDatabase(targetDatabase);

        for (String collection : sourceDb.listCollectionNames()) {
            if (!PatternUtil.match(collection, patterns, exclude)) {
                if (verbose) {
                    System.out.printf("skip unmatched collection %s%n", collection);
                }
                continue;
            }
            try {
                handle(collection, sourceDb, targetDb);
            } catch (Exception e) {
                System.err.printf("handle collection %s, error occurred: %s%n",
                        collection, e.getMessage());
                if (verbose) e.printStackTrace();
                if (abort) {
                    System.out.println("operation is aborted by exception");
                    break;
                }
            }
        }
    }

    public void handle(String sourceCollection, MongoDatabase sourceDb, MongoDatabase targetDb) {
        String targetCollection = getTargetSchema(sourceCollection);

        MongoCollection<Document> targetCol = null;
        try {
            targetCol = targetDb.getCollection(targetCollection);
        } catch (Exception e) {
            if (verbose) {
                System.err.println("target has no such collection " + targetCollection);
                e.printStackTrace();
            }
        }
        if (targetCol != null) {
            if (force) {
                System.out.printf("drop target collection %s%n", targetCollection);
                if (effect) {
                    targetCol.drop();
                }
            } else {
                System.out.printf("collection %s already exists in the target%n", targetCollection);
                // ignore when no-merge for existing schemas
                if (!merge) return;
            }
        } else {
            if (metaOnly) {
                System.out.printf("migrate collection %s to %s%n",
                        sourceCollection, targetCollection);
                return;
            } else {
                System.out.printf("create collection %s%n", targetCollection);
                targetDb.createCollection(targetCollection);
                targetCol = targetDb.getCollection(targetCollection);
            }
        }

        MongoCollection<Document> sourceCol = sourceDb.getCollection(sourceCollection);
        long total = sourceCol.countDocuments();
        System.out.printf("migrate collection %s to %s, find %d records%n",
                sourceCollection, targetCollection, total);

        List<Document> docments = new ArrayList<>();
        for (Document document : sourceCol.find()) {
            docments.add(document);
            if (docments.size() < batchSize) continue;
            saveToTarget(docments, targetCol, sourceCollection, targetCollection);
        }
        if (!docments.isEmpty()) {
            saveToTarget(docments, targetCol, sourceCollection, targetCollection);
        }
    }

    private void saveToTarget(List<Document> docments, MongoCollection<Document> targetCol,
            String sourceCollection, String targetCollection) {
        if (verbose) {
            System.out.printf("migrate collection %s to %s, insert %d records, "
                            + "last record: %s%n",
                    sourceCollection, targetCollection, docments.size(),
                    docments.get(docments.size() - 1).toJson());
        } else {
            System.out.printf("migrate collection %s to %s, insert %d records%n",
                    sourceCollection, targetCollection, docments.size());
        }
        if (effect) {
            targetCol.insertMany(docments);
        }
        docments.clear();
    }

    private String getTargetSchema(String sourceSchema) {
        if (ObjectUtil.isNotBlank(collectionConverter)) {
            try {
                return scriptEngine.evalWith(collectionConverter, sourceSchema);
            } catch (ScriptException e) {
                String message = String.format(
                        "failed to format schema %s with converter `%s`, %s",
                        sourceSchema, collectionConverter, e.getMessage());
                throw new RuntimeException(message, e);
            }
        }
        return sourceSchema;
    }
}

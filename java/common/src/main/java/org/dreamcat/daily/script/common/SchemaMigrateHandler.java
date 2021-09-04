package org.dreamcat.daily.script.common;

import org.dreamcat.common.text.argparse.ArgParser;

/**
 * Create by tuke on 2021/3/22
 */
public abstract class SchemaMigrateHandler<T> extends SchemaHandler<T> {

    protected boolean merge;
    protected boolean metaOnly;

    protected abstract boolean existsSchema(T schema);

    protected abstract void deleteSchema(T schema);

    protected abstract void createSchema(T sourceSchema, T schema);

    protected abstract void migrateSchema(T sourceSchema, T targetSchema) throws Exception;

    protected T getTargetSchema(T sourceSchema) {
        return sourceSchema;
    }

    @Override
    protected final void handle(T sourceSchema) throws Exception {
        String schemaKeyword = this.getSchemaKeyword();
        T targetSchema = this.getTargetSchema(sourceSchema);
        if (targetSchema == null) return;

        String targetSchemaName = this.formatSchema(targetSchema);
        if (targetSchema.equals(sourceSchema)) {
            log.warn("skip same {} {}", schemaKeyword, targetSchemaName);
        }

        if (this.existsSchema(targetSchema)) {
            // schema already exist
            if (force) {
                // force delete existing schemas
                log.warn("drop target {} {}", schemaKeyword, targetSchemaName);
                this.deleteSchema(targetSchema);
            } else {
                log.warn("{} {} already exists in the target",
                        schemaKeyword, targetSchemaName);
                // ignore when no-merge for existing schemas
                if (!merge) return;
            }
        } else {
            // schema doesn't exist, so create it
            this.createSchema(sourceSchema, targetSchema);
        }

        if (metaOnly) {
            String sourceSchemaName = this.formatSchema(sourceSchema);
            log.info("migrate {} {} to {}, skip by meta-only mode",
                    schemaKeyword, sourceSchemaName, targetSchemaName);
            return;
        }

        this.migrateSchema(sourceSchema, targetSchema);
    }

    @Override
    protected ArgParser defineArgs() {
        ArgParser argParser = super.defineArgs();
        argParser.addBool("merge", "m", "merge");
        argParser.addBool("meta-only", "mo", "meta-only");
        return argParser;
    }

    @Override
    protected ArgParser parseArgs(String... args) {
        ArgParser argParser = super.parseArgs(args);
        this.merge = argParser.getBool("merge");
        this.metaOnly = argParser.getBool("meta-only");
        return argParser;
    }

}

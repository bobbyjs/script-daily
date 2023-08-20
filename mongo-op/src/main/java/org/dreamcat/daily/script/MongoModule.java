package org.dreamcat.daily.script;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;

/**
 * @author Jerry Will
 * @version 2023-08-20
 */
@ArgParserType(allProperties = true)
public class MongoModule {

    String url;
    @ArgParserField("db")
    String database;

    private transient MongoClient client;

    protected void afterPropertySet() throws Exception {
        client = MongoClients.create(url);
    }

    public MongoDatabase getDatabase() {
        return client.getDatabase(database);
    }
}

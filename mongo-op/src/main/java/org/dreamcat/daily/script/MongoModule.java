package org.dreamcat.daily.script;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.dreamcat.common.argparse.ArgParserType;

/**
 * @author Jerry Will
 * @version 2023-08-20
 */
@ArgParserType(allProperties = true)
public class MongoModule {

    private String url;

    transient MongoClient client;

    protected void afterPropertySet() throws Exception {
        client = MongoClients.create(url);
    }
}

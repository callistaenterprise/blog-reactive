package se.callista.springmvc.asynch.pattern.aggregator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by magnus on 20/07/14.
 */
public class DbLookup {
    private final int dbLookupMs;
    private final int dbHits;
    private static final Logger logger = LoggerFactory.getLogger(DbLookup.class);

    public DbLookup(int dbLookupMs, int dbHits) {
        this.dbLookupMs = dbLookupMs;
        this.dbHits = dbHits;
    }

    /**
     * @deprecated use lookupUrlsInDb
     * @return
     */
    public int executeDbLookup() {

        // Start of blocking dbLookup
        int hits = simulateDbLookup();
        // Processing of blocking dbLookup done

        return hits;
    }

    public List<String> lookupUrlsInDb(String baseUrl, int minMs, int maxMs) {

        logger.debug("Lookup Db urls");
        // Start of blocking db-lookup
        List<String> urls = new ArrayList<>();

        // Simulate a blocking db-lookup by putting the current thread to sleep for a while...
        try {
            Thread.sleep(dbLookupMs);
        } catch (InterruptedException ignore) {}

        for (int i = 0; i < dbHits; i++) {
            // Use one and the same address for all returned URL's
            urls.add(baseUrl + "?minMs=" + minMs + "&maxMs=" + maxMs);
        }

        // Processing of blocking db-lookup done

        return urls;
    }


    // Simulate a blocking db-lookup by putting the current thread to sleep for a while...
    protected int simulateDbLookup(){

        try {
            Thread.sleep(dbLookupMs);
        } catch (InterruptedException ignore) {}

        return dbHits;
    }
}

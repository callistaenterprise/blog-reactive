package util;

import java.util.ArrayList;
import java.util.List;

public class DbLookup {
    private final int dbLookupMs;
    private final int dbHits;

    public DbLookup(int dbLookupMs, int dbHits) {
        this.dbLookupMs = dbLookupMs;
        this.dbHits = dbHits;
    }

    public int executeDbLookup() {

        // Start of blocking dbLookup
        int hits = simulateDbLookup();
        // Processing of blocking dbLookup done

        return hits;
    }

    public List<String> lookupUrlsInDb(String baseUrl, int minMs, int maxMs) {

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

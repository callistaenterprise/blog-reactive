package se.callista.springmvc.asynch.pattern.aggregator;

/**
 * Created by magnus on 20/07/14.
 */
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

    // Simulate a blocking db-lookup by putting the current thread to sleep for a while...
    protected int simulateDbLookup(){

        try {
            Thread.sleep(dbLookupMs);
        } catch (InterruptedException e) {}

        return dbHits;
    }
}

package com.minisocial.config;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Routes database connections based on transaction type.
 * 
 * - Read-only transactions (@Transactional(readOnly = true)) → read datasource (replica)
 * - Write transactions (@Transactional or @Transactional(readOnly = false)) → write datasource (primary)
 * 
 * This provides automatic routing without code changes in services.
 */
public class RoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        // Check if current transaction is read-only
        boolean isReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
        
        // Route to read datasource for read-only transactions, write datasource otherwise
        String dataSourceKey = isReadOnly ? "read" : "write";
        
        // Log routing decision (can be removed in production)
        logger.debug("Routing to " + dataSourceKey + " datasource (readOnly=" + isReadOnly + ")");
        
        return dataSourceKey;
    }
}

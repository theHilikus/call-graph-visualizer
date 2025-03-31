package com.github.thehilikus.call_graph.db;

import org.apache.commons.lang3.time.StopWatch;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;


/**
 * Database abstraction
 */
public class GraphDatabase {
    private static final Logger LOG = LoggerFactory.getLogger(GraphDatabase.class);

    private final Path directory;
    private final String databaseName;
    private GraphDatabaseService databaseService;
    private DatabaseManagementService managementService;

    public GraphDatabase(Path directory, String databaseName) {
        this.directory = directory;
        this.databaseName = databaseName;
    }

    public void initialize() {
        LOG.info("Initializing Neo4j database '{}' at {}", databaseName, directory);
        StopWatch stopWatch = StopWatch.createStarted();
        managementService = new DatabaseManagementServiceBuilder(directory.resolve(databaseName)).build();
        databaseService = managementService.database(GraphDatabaseSettings.DEFAULT_DATABASE_NAME);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Neo4j database '{}' initialized in {} ms", databaseName, stopWatch.getTime(TimeUnit.MILLISECONDS));
        }

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    public void shutdown() {
        if (managementService != null) {
            LOG.info("Shutting down Neo4j database '{}'", databaseName);
            managementService.shutdown();
            managementService = null;
        }
    }

    public GraphTransaction startTransaction() {
        return new GraphTransaction(databaseService.beginTx());
    }
}

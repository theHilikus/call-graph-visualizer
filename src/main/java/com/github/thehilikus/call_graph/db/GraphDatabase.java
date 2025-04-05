package com.github.thehilikus.call_graph.db;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.configuration.connectors.BoltConnector;
import org.neo4j.configuration.helpers.SocketAddress;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.dbms.api.Neo4jDatabaseManagementServiceBuilder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
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
        initialize(null);
    }

    public void initialize(@Nullable Integer boltPort) {
        LOG.info("Initializing Neo4j database '{}' at {}", databaseName, directory);
        StopWatch stopWatch = StopWatch.createStarted();
        Neo4jDatabaseManagementServiceBuilder serviceBuilder = new DatabaseManagementServiceBuilder(directory.resolve(databaseName));
        if (boltPort != null) {
            serviceBuilder.setConfig(BoltConnector.enabled, true);
            serviceBuilder.setConfig(BoltConnector.listen_address, new SocketAddress("localhost", boltPort));
        }
        managementService = serviceBuilder.build();

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
        if (databaseService == null) {
            databaseService = managementService.database(GraphDatabaseSettings.DEFAULT_DATABASE_NAME);
        }
        return new GraphTransaction(databaseService.beginTx());
    }

    public void truncate() {
        LOG.info("Truncating Neo4j database '{}' in {}", databaseName, directory);
        try {
            FileUtils.deleteDirectory(directory.resolve(databaseName).toFile());
        } catch (IOException e) {
            throw new GraphDatabaseException("Error truncating database", e);
        }
    }
}

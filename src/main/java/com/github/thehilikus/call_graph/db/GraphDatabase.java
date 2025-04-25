package com.github.thehilikus.call_graph.db;

import com.github.thehilikus.call_graph.run.PerfTracker;
import org.apache.commons.io.FileUtils;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.configuration.connectors.BoltConnector;
import org.neo4j.configuration.connectors.HttpConnector;
import org.neo4j.configuration.helpers.SocketAddress;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.dbms.api.Neo4jDatabaseManagementServiceBuilder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;


/**
 * Database abstraction
 */
public class GraphDatabase {
    private static final Logger LOG = LoggerFactory.getLogger(GraphDatabase.class);

    private final Path directory;
    private final String databaseName;
    private GraphDatabaseService databaseService;
    private DatabaseManagementService managementService;
    private int boltPort = -1;
    private int httpPort = -1;

    public GraphDatabase(Path directory, String databaseName) {
        this.directory = directory;
        this.databaseName = databaseName;
    }

    public void enableBrowser(int boltPort, int httpPort) {
        this.boltPort = boltPort;
        this.httpPort = httpPort;
    }

    public void initialize() {
        LOG.info("Initializing Neo4j database '{}' at {}", databaseName, directory);
        PerfTracker perfTracker = PerfTracker.createStarted("Neo4j database initialization");
        Neo4jDatabaseManagementServiceBuilder serviceBuilder = new DatabaseManagementServiceBuilder(directory.resolve(databaseName));
        if (boltPort != -1) {
            LOG.info("Enabling Bolt and HTTP connectors in ports {} and {}", boltPort, httpPort);
            serviceBuilder.setConfig(BoltConnector.enabled, true);
            serviceBuilder.setConfig(BoltConnector.listen_address, new SocketAddress("localhost", boltPort));
            serviceBuilder.setConfig(HttpConnector.enabled, true);
            serviceBuilder.setConfig(HttpConnector.listen_address, new SocketAddress("localhost", httpPort));
        }
        managementService = serviceBuilder.build();

        perfTracker.finish();

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

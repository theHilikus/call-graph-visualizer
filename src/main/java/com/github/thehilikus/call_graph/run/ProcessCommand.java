package com.github.thehilikus.call_graph.run;

import com.github.thehilikus.call_graph.analysis.AnalysisFilter;
import com.github.thehilikus.call_graph.analysis.call_graph.CallGraphCreator;
import com.github.thehilikus.call_graph.analysis.type_hierarchy.TypeHierarchyCreator;
import com.github.thehilikus.call_graph.db.GraphDatabase;
import com.github.thehilikus.call_graph.db.GraphTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.nio.file.Path;
import java.util.Set;

/**
 * Command to process a jar
 */
@Command(name = "process", description = "Process a jar to be able to query it later")
public class ProcessCommand implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(ProcessCommand.class);
    @Parameters(description = "the jars to process", arity = "1..*")
    private Set<Path> jarPaths;

    @Option(names = "--dry-run", description = "Don't write to the database")
    private boolean dryRun = false;

    @Option(names="--truncate", description = "Deletes the existing database before creating a new one")
    private boolean truncate = false;

    @Option(names ="--include", description = "Comma separated list of packages/classes to include. If omitted, include everything")
    private Set<String> include = null;

    @Option(names ="--exclude", description = "Comma separated list of packages/classes to exclude. If omitted, exclude JDK classes")
    private Set<String> exclude = Set.of("java.", "javax.", "sun.", "com.sun.", "jdk.", "org.w3c.dom.", "org.xml.sax.");

    @Option(names ="--exclude-extra", description = "Comma separated list of packages/classes to exclude on top of the default excludes")
    private Set<String> excludeExtra = Set.of();

    @ParentCommand
    private Main main;

    @Override
    public void run() {
        PerfTracker perfTracker = PerfTracker.createStarted("Complete process");
        GraphDatabase db = prepareGraphDb();
        try (GraphTransaction graphTransaction = db.startTransaction()) {
            if (dryRun) {
                LOG.warn("Running in dry-run mode. Changes won't be committed to the database");
            }

            AnalysisFilter analysisFilter = new AnalysisFilter(include, exclude, excludeExtra);

            TypeHierarchyCreator typeHierarchyCreator = new TypeHierarchyCreator(jarPaths, analysisFilter);
            typeHierarchyCreator.run(graphTransaction);

            CallGraphCreator callGraphCreator = new CallGraphCreator(jarPaths, analysisFilter);
            callGraphCreator.run(graphTransaction);

            if (!dryRun) {
                graphTransaction.commit();
            } else {
                graphTransaction.rollback();
            }
        } finally {
            db.shutdown();
            perfTracker.finish();
        }
    }

    private GraphDatabase prepareGraphDb() {
        GraphDatabase db = new GraphDatabase(main.databaseFolder, main.databaseName);
        if (truncate) {
            if (dryRun) {
                LOG.info("Not truncating database in dry-run mode");
            } else {
                db.truncate();
            }
        }
        db.initialize();

        return db;
    }
}

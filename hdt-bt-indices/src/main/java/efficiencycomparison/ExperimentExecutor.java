package efficiencycomparison;

import queryenginestubs.*;
import queryenginestubs.QueryEngineStub;
import btindices.statisticalquerygeneration.QueryModel;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.time.StopWatch;

import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Console application in order to execute queries with a given query engine and save the corresponding
 * results to a CSV file.
 */
public class ExperimentExecutor {

    public static final StopWatch swGlobal = new StopWatch();
    public static final StopWatch sw = new StopWatch();

    public static void main(String[] args) {
        ExperimentExecutor experimentExecutor = new ExperimentExecutor();
        experimentExecutor.run(args);
    }

    String queryModelsDir;
    QueryExecutorChoice queryType;
    int numQueryTypes; // #filters or #joins
    int numLevels;
    QueryEngineStub qeStub;
    String stubName;


    private void run(String[] args) {
        CommandLine cl = parseArguments(args);

        if (!cl.hasOption("queryModelsDir")
        || !cl.hasOption("queryType")) {
            printAppHelp();
            return;
        }

        queryModelsDir = cl.getOptionValue("queryModelsDir");

        String[] queryTypeArgs = cl.getOptionValues("queryType");
        numQueryTypes = Integer.parseInt(queryTypeArgs[1]);
        numLevels = Integer.parseInt(queryTypeArgs[2]);
        int queryTypeInt = Integer.parseInt(queryTypeArgs[0]);

        switch (queryTypeInt) {
            case 1:
                queryType = QueryExecutorChoice.Verbose;
                break;
            case 2:
                queryType = QueryExecutorChoice.RC;
                break;
            case 3:
                queryType = QueryExecutorChoice.CP;
                break;
            case 4:
                queryType = QueryExecutorChoice.ApplyFilters;
                break;
            case 5:
                queryType = QueryExecutorChoice.AvailableFilters;
                break;
            case 6:
                queryType = QueryExecutorChoice.Hybrid;
                break;
            default:
                queryType = QueryExecutorChoice.Count;
                break;
        }

        if (cl.hasOption("hdtJena")) {
            qeStub = new HDTJenaStub(cl.getOptionValue("hdtJena"));
            stubName = "HDTJena";
        } else if (cl.hasOption("ctcIndices")) {
            String[] btiArgs = cl.getOptionValues("ctcIndices");
            qeStub = new CtCIndicesStub(btiArgs[0], btiArgs[1]);
            stubName = "CtCIndices";
        } else if (cl.hasOption("plainHDT")) {
            qeStub = new PlainHDTStub(cl.getOptionValue("plainHDT"));
            stubName = "PlainHDT";
        } else if (cl.hasOption("pvIndices")) {
            String[] fsArgs = cl.getOptionValues("pvIndices");
            qeStub = new PVIndicesStub(fsArgs[0], fsArgs[1]);
            stubName = "PVIndices";
        } else if (cl.hasOption("hybrid")) {
            String[] hybridArgs = cl.getOptionValues("hybrid");
            qeStub = new CtCPVHybridStub(hybridArgs[0], hybridArgs[1], hybridArgs[2]);
            stubName = "HybridIndices";
        }

        experiment();

    }

    public void experiment() {
        String queryPrefix = "q";

        // initial queries (no joins) as warm up
        String path = Paths.get(queryModelsDir, queryPrefix + "0").toString();

        //ArrayList<BTIndices.StatisticalQueryExtraction.QueryModel> warmUpQueries = QueryModelManager.loadQueryModelsFromFile(path);
        ArrayList<QueryModel> warmUpQueries = null; // no specific warm up


        String resultsPath = null;

        for (int i = 0; i <= numQueryTypes; i++) {

            if (i == 0) {
                // initial query -> no joins, no levels of difficulty, and no predicates
                path = Paths.get(queryModelsDir, queryPrefix + i).toString();
                resultsPath = path  + "_" + queryType.toString() + "_" + stubName;
                runExperiment(qeStub, path, resultsPath, warmUpQueries, queryType);
                continue;
            }

            for (int k = 1; k <= numLevels; k++) {

                path = Paths.get(queryModelsDir, queryPrefix + i + "_" + k).toString();
                resultsPath = path  + "_" + queryType.toString() + "_" + stubName;
                System.out.println("Run experiment: " + path.toString());
                runExperiment(qeStub, path, resultsPath, warmUpQueries, queryType);
            }

        }
        System.exit(0);
    }

    public static void runExperiment(QueryEngineStub qeStub, String queryPath, String resultsPath, ArrayList<QueryModel> warmUpQueries, QueryExecutorChoice queryType) {
        ExperimentManager experiment = new ExperimentManager(queryPath, resultsPath, qeStub, 2, queryType, warmUpQueries);
        experiment.runExperiment();
    }

    private CommandLine parseArguments(String[] args) {

        Options options = getOptions();
        CommandLine line = null;

        CommandLineParser parser = new DefaultParser();

        try {
            line = parser.parse(options, args);

        } catch (Exception ex) {

            System.err.println("Failed to parse command line arguments");
            System.err.println(ex.toString());
            printAppHelp();

            System.exit(1);
        }

        return line;
    }

    private void printAppHelp() {

        Options options = getOptions();

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("JavaStatsEx", options, true);
    }

    private Options getOptions() {

        Options options = new Options();

        // Query Engines
        options.addOption(Option.builder("plainHDT")
                .numberOfArgs(1)
                .desc("Uses the plain HDT query engine")
                .argName("HDT-PATH")
                .build());

        options.addOption(Option.builder("hdtJena")
                .numberOfArgs(1)
                .desc("Uses the Jena ARQ query engine")
                .argName("HDT-PATH")
                .build());

        options.addOption(Option.builder("rdfox")
                .numberOfArgs(1)
                .desc("Uses the RDFox query engine")
                .argName("RDF-TTL-PATH")
                .build());

        options.addOption(Option.builder("ctcIndices")
                .numberOfArgs(2)
                .desc("Uses the Category Indices query engine")
                .argName("HDT-PATH> <CATEGORY-INDICES-DIRECTORY")
                .build());

        // Query Models
        options.addOption(Option.builder("queryModelsDir")
                .desc("Directory with all query models for the experiment")
                .hasArg()
                .argName("DIRECTORY")
                .required()
                .build());
        options.addOption(Option.builder("queryType")
                .desc("Choose between 'Count-Queries' = 0, 'String-Queries' = 1, 'Reachable Categories ' = 2," +
                        "'Connected Predicates' = 3, 'Apply Facets' = 4 and 'Available Facets' = 5")
                .numberOfArgs(3)
                .required()
                .argName("QUERY-TYPE> <NUM-FILTERS-OR-JOINS> <NUM-LEVELS")
                .build());

        options.addOption(Option.builder("pvIndices")
                .desc("Uses the PV Indices query engine.")
                .numberOfArgs(2)
                .argName("HDT-PATH> <PV-INDICES-DIRECTORY")
                .build());

        options.addOption(Option.builder("hybrid")
                .desc("Uses the PV indices and CtC indices to resolve hybrid queries.")
                .numberOfArgs(3)
                .argName("HDT-PATH> <CTC-INDICES-DIRECTORY> <PV-INDICES-DIRECTORY")
                .build());

        return options;
    }


}




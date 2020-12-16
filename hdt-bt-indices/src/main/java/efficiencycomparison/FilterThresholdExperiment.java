package efficiencycomparison;

import btindices.indicesmanager.FilterExplorationManager;
import queryenginestubs.PVIndicesStub;
import queryenginestubs.PlainHDTStub;
import queryenginestubs.QueryEngineStub;

import java.nio.file.Paths;
import java.util.Arrays;

public class FilterThresholdExperiment {

    String hdtPath;
    String pvIndicesDir;
    String pvQueriesDir;
    PVIndicesStub PVIndicesStub;
    PlainHDTStub plainHDTStub;
    long[] thresholds;

    public static void main(String[] args) {

        /*
            <HDT-PATH> <FILTER-INDICES-DIRECTORY> <FILTER-QUERIES-DIRECTORY> [<THRESHOLD-1>, <THRESHOLD-2>, ...]
         */
        FilterThresholdExperiment fse = new FilterThresholdExperiment();
        fse.hdtPath = args[0];
        fse.pvIndicesDir = args[1];
        fse.pvQueriesDir = args[2];

        fse.PVIndicesStub = new PVIndicesStub(fse.hdtPath, fse.pvIndicesDir);
        fse.plainHDTStub = new PlainHDTStub(fse.hdtPath);

        // parse thresholds:
        String[] thresholdsStr = Arrays.copyOfRange(args, 3, args.length);
        fse.thresholds = new long[thresholdsStr.length];
        for (int i = 0; i < fse.thresholds.length; i++) {
            fse.thresholds[i] = Long.parseLong(thresholdsStr[i]);
        }

        fse.executeExperiments(QueryExecutorChoice.AvailableFilters);
        fse.executeExperiments(QueryExecutorChoice.ApplyFilters);
    }

    public void executeExperiments(QueryExecutorChoice experimentType) {
        executeExperimentsForQEStub(PVIndicesStub, PVIndicesStub.fsManager, "pvIndices", experimentType);
        executeExperimentsForQEStub(plainHDTStub, plainHDTStub.fsManager, "plainHDT", experimentType);
    }

    public void executeExperimentsForQEStub(QueryEngineStub stub, FilterExplorationManager fsManager, String qeStubName, QueryExecutorChoice experimentType) {

        String queryTypeStr;

        switch (experimentType) {
            case AvailableFilters:
                queryTypeStr = "AF";
                break;
            default:
                queryTypeStr = "ApplyFilters";
                break;
        }

        if (experimentType == QueryExecutorChoice.AvailableFilters) {

            for (int i = 0; i < thresholds.length; i++) {
                fsManager.thresAvailableFilters = thresholds[i];
                // name convention: q<NUM-JOINS/FILTERS>_<DIFFICULTY-LEVEL>_<QUERY-TYPE>_<QE-STUB-NAME>
                String thresholdString = queryTypeStr + "_" + qeStubName + "_" + thresholds[i];

                int numFilters = 1; // queries are only used for initialization

                for (int k = 1; k <= 3; k++) {
                    // number of difficulties
                    String queryString = "q" + numFilters + "_" + k;
                    String queryPath = Paths.get(pvQueriesDir, queryString).toString();
                    String filePath = Paths.get(pvQueriesDir, queryString + "_" + thresholdString).toString();
                    ExperimentManager expManager = new ExperimentManager(queryPath, filePath, stub, 2, experimentType, null);
                    expManager.runExperiment();
                }
            }

        } else {

            for (int i = 0; i < thresholds.length; i++) {
                fsManager.thresApplyFilter = thresholds[i];
                // name convention: q<NUM-JOINS/FILTERS>_<DIFFICULTY-LEVEL>_<QUERY-TYPE>_<QE-STUB-NAME>
                String thresholdString = queryTypeStr + "_" + qeStubName + "_" + thresholds[i];
                for (int j = 1; j <= 4; j++) {
                    // number of applied filters
                    for (int k = 1; k <= 3; k++) {
                        // number of difficulties
                        String queryString = "q" + j + "_" + k;
                        String queryPath = Paths.get(pvQueriesDir, queryString).toString();
                        String filePath = Paths.get(pvQueriesDir, queryString + "_" + thresholdString).toString();
                        ExperimentManager expManager = new ExperimentManager(queryPath, filePath, stub, 2, experimentType, null);
                        expManager.runExperiment();
                    }
                }
            }
        }

    }
}

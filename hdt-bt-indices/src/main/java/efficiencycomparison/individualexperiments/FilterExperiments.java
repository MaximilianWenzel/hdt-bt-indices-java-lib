package efficiencycomparison.individualexperiments;

import efficiencycomparison.ExperimentManager;
import efficiencycomparison.QueryExecutorChoice;
import queryenginestubs.*;

import java.nio.file.Paths;

public class FilterExperiments {

    String queryModelsDir;
    String rdfDumpFilePath;
    String hdtFilePath;
    String pvIndicesDir;

    QueryEngineStub plainHDT;
    QueryEngineStub pvIndices;
    QueryEngineStub hdtJena;

    public FilterExperiments(String rdfDumpFilePath, String hdtFilePath, String pvIndicesDir, String queryModelsDir) {
        this.rdfDumpFilePath = rdfDumpFilePath;
        this.hdtFilePath = hdtFilePath;
        this.pvIndicesDir = pvIndicesDir;
        this.queryModelsDir = queryModelsDir;
    }

    public static void main(String[] args) {

        // <TTL-FILE> <HDT-FILE> <FILTER-INDICES-DIR> <FILTER-QUERY-MODELS-DIR>
        String rdfDumpFilePath = args[0];
        String hdtFilePath = args[1];
        String filterIndicesDir = args[2];
        String filterQueryModelsDir = args[3];
        FilterExperiments experiment = new FilterExperiments(rdfDumpFilePath, hdtFilePath, filterIndicesDir, filterQueryModelsDir);
        experiment.runAvailableFiltersExperiments();
        experiment.runApplyFiltersExperiments();

    }

    private void runApplyFiltersExperiments() {
        pvIndices = new PVIndicesStub(hdtFilePath, pvIndicesDir);
        runApplyFiltersExperiment(pvIndices, "PVIndices");
        pvIndices = null;
        System.gc();

        plainHDT = new PlainHDTStub(hdtFilePath);
        runApplyFiltersExperiment(plainHDT, "PlainHDT");
        plainHDT = null;
        System.gc();

        hdtJena = new HDTJenaStub(hdtFilePath);
        runApplyFiltersExperiment(hdtJena, "HDTJena");
        hdtJena = null;
        System.gc();
        System.exit(0);
    }

    private void runAvailableFiltersExperiments() {
        pvIndices = new PVIndicesStub(hdtFilePath, pvIndicesDir);
        runAvailableFiltersExperiment(pvIndices, "PVIndices");
        pvIndices = null;
        System.gc();

        plainHDT = new PlainHDTStub(hdtFilePath);
        runAvailableFiltersExperiment(plainHDT, "PlainHDT");
        plainHDT = null;
        System.gc();
    }

    public void runApplyFiltersExperiment(QueryEngineStub qeStub, String stubName) {
        // 3 levels of difficulty, maximum 4 applied filters
        int numFilters = 4;
        int levels = 3;
        int rounds = 2;
        ExperimentManager em;

        String queryPath;
        String resultsPath;
        String queryName;
        QueryExecutorChoice queryType = QueryExecutorChoice.ApplyFilters;

        for (int i = 1; i <= numFilters; i++) {
            for (int k = 1; k <= levels; k++) {
                queryName = getQueryName(i, k);
                queryPath = Paths.get(queryModelsDir, queryName).toString();
                resultsPath = queryPath + "_" + queryType.toString() + "_" + stubName;
                System.out.println("Run experiment: " + queryPath.toString());
                em = new ExperimentManager(queryPath, resultsPath, qeStub, rounds, queryType, null);
                em.runExperiment();
            }
        }
    }

    public void runAvailableFiltersExperiment(QueryEngineStub qeStub, String stubName) {
        int joins = 2;
        int levels = 3;
        int rounds = 2;
        ExperimentManager em;

        String queryPath;
        String resultsPath;
        String queryName;
        QueryExecutorChoice queryType = QueryExecutorChoice.AvailableFilters;

        for (int i = 1; i < joins; i++) {
            for (int k = 1; k <= levels; k++) {
                queryName = getQueryName(i, k);
                queryPath = Paths.get(queryModelsDir, queryName).toString();
                resultsPath = queryPath + "_" + queryType.toString() + "_" + stubName;
                System.out.println("Run experiment: " + queryPath.toString());
                em = new ExperimentManager(queryPath, resultsPath, qeStub, rounds,queryType, null);
                em.runExperiment();
            }
        }
    }

    private String getQueryName(int numJoinsFilters, int level) {
        return "q" + numJoinsFilters + "_" + level;
    }

}

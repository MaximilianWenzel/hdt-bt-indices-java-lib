package efficiencycomparison.individualexperiments;

import efficiencycomparison.ExperimentManager;
import efficiencycomparison.QueryExecutorChoice;
import queryenginestubs.*;

import java.nio.file.Paths;

public class CategoryExperiments {
    String queryModelsDir;
    String rdfDumpFilePath;
    String hdtFilePath;
    String ctcIndicesDir;

    QueryEngineStub plainHDT;
    QueryEngineStub ctcIndices;
    QueryEngineStub hdtJena;

    public CategoryExperiments(String rdfDumpFilePath, String hdtFilePath, String ctcIndicesDir, String queryModelsDir) {
        this.rdfDumpFilePath = rdfDumpFilePath;
        this.hdtFilePath = hdtFilePath;
        this.ctcIndicesDir = ctcIndicesDir;
        this.queryModelsDir = queryModelsDir;
    }

    public static void main(String[] args) {

        // <TTL-FILE> <HDT-FILE> <CtC-INDICES-DIR> <CATEGORY-QUERY-MODELS-DIR>
        String rdfDumpFilePath = args[0];
        String hdtFilePath = args[1];
        String filterIndicesDir = args[2];
        String filterQueryModelsDir = args[3];
        CategoryExperiments experiment = new CategoryExperiments(rdfDumpFilePath, hdtFilePath, filterIndicesDir, filterQueryModelsDir);
        experiment.runReachableCategoriesExperiments();
        experiment.runIncrementalJoinsExperiments();
    }

    private void runIncrementalJoinsExperiments() {
        ctcIndices = new CtCIndicesStub(hdtFilePath, ctcIndicesDir);
        runIncrementalJoinExperiment(ctcIndices, "CtCIndices");
        ctcIndices = null;
        System.gc();

        plainHDT = new PlainHDTStub(hdtFilePath);
        runIncrementalJoinExperiment(plainHDT, "PlainHDT");
        plainHDT = null;
        System.gc();

        hdtJena = new HDTJenaStub(hdtFilePath);
        runIncrementalJoinExperiment(hdtJena, "HDTJena");
        hdtJena = null;
        System.gc();
        System.exit(0);
    }

    private void runReachableCategoriesExperiments() {
        ctcIndices = new CtCIndicesStub(hdtFilePath, ctcIndicesDir);
        runReachableCategoriesExperiment(ctcIndices, "CtCIndices");
        ctcIndices = null;
        System.gc();

        plainHDT = new PlainHDTStub(hdtFilePath);
        runReachableCategoriesExperiment(plainHDT, "PlainHDT");
        plainHDT = null;
        System.gc();
    }

    public void runIncrementalJoinExperiment(QueryEngineStub qeStub, String stubName) {
        // 3 levels of difficulty, maximum 4 joins
        int numJoins = 4;
        int levels = 3;
        int rounds = 2;
        ExperimentManager em;

        String queryPath;
        String resultsPath;
        String queryName;
        QueryExecutorChoice queryType = QueryExecutorChoice.Verbose;

        for (int i = 1; i <= numJoins; i++) {
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

    public void runReachableCategoriesExperiment(QueryEngineStub qeStub, String stubName) {
        int joins = 1; // queries are only used to initialize exploration - therefore 1 join sufficient
        int levels = 3;
        int rounds = 2;
        ExperimentManager em;

        String queryPath;
        String resultsPath;
        String queryName;
        QueryExecutorChoice queryType = QueryExecutorChoice.CP;

        for (int k = 1; k <= levels; k++) {
            queryName = getQueryName(joins, k);
            queryPath = Paths.get(queryModelsDir, queryName).toString();
            resultsPath = queryPath + "_" + queryType.toString() + "_" + stubName;
            System.out.println("Run experiment: " + queryPath.toString());
            em = new ExperimentManager(queryPath, resultsPath, qeStub, rounds, queryType, null);
            em.runExperiment();
        }
    }

    private String getQueryName(int numJoinsFilters, int level) {
        return "q" + numJoinsFilters + "_" + level;
    }
}

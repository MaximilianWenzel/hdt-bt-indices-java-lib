package efficiencycomparison.individualexperiments;

import efficiencycomparison.ExperimentManager;
import efficiencycomparison.QueryExecutorChoice;
import queryenginestubs.*;

import java.nio.file.Paths;

public class HybridExperiments {

    String queryModelsDir;
    String rdfDumpFilePath;
    String hdtFilePath;
    String ctcIndicesDir;
    String pvIndicesDir;

    QueryEngineStub plainHDT;
    QueryEngineStub hybrid;
    QueryEngineStub hdtJena;

    public HybridExperiments(String rdfDumpFilePath, String hdtFilePath, String ctcIndicesDir, String pvIndicesDir, String queryModelsDir) {
        this.rdfDumpFilePath = rdfDumpFilePath;
        this.hdtFilePath = hdtFilePath;
        this.pvIndicesDir = pvIndicesDir;
        this.ctcIndicesDir = ctcIndicesDir;
        this.queryModelsDir = queryModelsDir;
    }

    public static void main(String[] args) {

        // <TTL-FILE> <HDT-FILE> <CTC-INDICES-DIR> <FILTER-INDICES-DIR> <HYBRID-QUERY-MODELS-DIR>
        String rdfDumpFilePath = args[0];
        String hdtFilePath = args[1];
        String ctcIndicesDir = args[2];
        String pvIndicesDir = args[3];
        String hybridQueryModelsDir = args[4];
        HybridExperiments experiment = new HybridExperiments(rdfDumpFilePath, hdtFilePath, ctcIndicesDir, pvIndicesDir, hybridQueryModelsDir);
        experiment.runHybridQueryExperiments();

    }

    public void runHybridQueryExperiments() {
        hybrid = new CtCPVHybridStub(hdtFilePath, ctcIndicesDir, pvIndicesDir);
        runHybridQueryExperiment(hybrid, "Hybrid");
        hybrid = null;
        System.gc();

        plainHDT = new PlainHDTStub(hdtFilePath);
        runHybridQueryExperiment(plainHDT, "PlainHDT");
        plainHDT = null;
        System.gc();

        hdtJena = new HDTJenaStub(hdtFilePath);
        runHybridQueryExperiment(hdtJena, "HDTJena");
        hdtJena = null;
        System.gc();

        System.exit(0);
    }

    private void runHybridQueryExperiment(QueryEngineStub qeStub, String stubName) {
        // 3 levels of difficulty, maximum 4 applied filters
        int numJoins = 4;
        int levels = 3;
        int rounds = 2;
        ExperimentManager em;

        String queryPath;
        String resultsPath;
        String queryName;
        QueryExecutorChoice queryType = QueryExecutorChoice.Hybrid;

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

    private String getQueryName(int numJoinsFilters, int level) {
        return "q" + numJoinsFilters + "_" + level;
    }

}

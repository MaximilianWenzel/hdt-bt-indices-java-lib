package btindices.statisticalquerygeneration.querysets;

import btindices.statisticalquerygeneration.*;
import org.rdfhdt.hdt.util.StopWatch;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * Generate all required category, filter and hybrid queries for a given RDF data set at once and reduce
 * them to the specified size.
 */
public class ExperimentQueriesGenerator {

    private static final StopWatch sw = new StopWatch();

    enum QueryType {
        CategoryQuery,
        FilterQuery,
        HybridQuery
    }

    String hdtPath;
    String ctcIndicesDir;
    String pvIndicesDir;

    String categoryQueriesDir;
    String filterQueriesDir;
    String hybridQueriesDir;

    String categoryQueriesDirFull;
    String filterQueriesDirFull;
    String hybridQueriesDirFull;

    FilterQueryGenerator fqEx;
    CategoryQueryGenerator cqEx;
    HybridQueryGenerator hqEx;

    // category queries
    int numCatJoins = 4;
    int numCatLevels = 3;
    int numCatQueriesPerLevel = 10;

    // filter queries
    int numFilters = 3;
    int numFilterLevels = 3;
    int numFilterQueriesPerLevel = 10;

    // hybrid queries
    int numHybridJoins = 4;
    int numHybridLevels = 3;
    int numHybridQueriesPerLevel = 10;

    public ExperimentQueriesGenerator(String hdtPath, String ctcIndicesDir, String pvIndicesDir) {
        this.hdtPath = hdtPath;
        this.ctcIndicesDir = ctcIndicesDir;
        this.pvIndicesDir = pvIndicesDir;

        categoryQueriesDir = "categoryQueries";
        filterQueriesDir = "filterQueries";
        hybridQueriesDir = "hybridQueries";

        categoryQueriesDirFull = categoryQueriesDir + "Full";
        filterQueriesDirFull = filterQueriesDir + "Full";
        hybridQueriesDirFull = hybridQueriesDir + "Full";
    }

    public static void main(String[] args) {
        // <HDT-PATH> <CTC-INDICES-DIR> <PV-INDICES-DIR>
        String hdtPath = args[0];
        String ctcIndicesDir = args[1];
        String pvIndicesDir = args[2];
        ExperimentQueriesGenerator qs = new ExperimentQueriesGenerator(hdtPath, ctcIndicesDir, pvIndicesDir);
        qs.runGeneration();

    }

    public void runGeneration() {
        System.out.println("Start query generation...");
        startGeneration();

        System.out.println("Reduce query size...");
        startQuerySizeReduction();
    }

    private void startGeneration() {
        startCategoryQueryGeneration();
        startFilterQueryGeneration();
        startHybridQueryGeneration();
    }

    private void startQuerySizeReduction() {
        // category queries
        QueryModelManager.reduceNumberOfQueries(categoryQueriesDirFull, categoryQueriesDir,
                numCatQueriesPerLevel, numCatLevels, numCatJoins);
        QueryModelManager.queryModelToString(categoryQueriesDir, numCatJoins, numCatLevels);

        // filter queries
        QueryModelManager.reduceNumberOfQueries(filterQueriesDirFull, filterQueriesDir,
                numFilterQueriesPerLevel, numFilterLevels, numFilters);
        QueryModelManager.queryModelToString(filterQueriesDir, numFilters, numFilterLevels);

        // hybrid queries
        QueryModelManager.reduceNumberOfQueries(hybridQueriesDirFull, hybridQueriesDir,
                numHybridQueriesPerLevel, numHybridLevels, numHybridJoins);
        QueryModelManager.queryModelToString(hybridQueriesDir, numHybridJoins, numHybridLevels);
    }

    private void startCategoryQueryGeneration() {
        cqEx = new CategoryQueryGenerator(hdtPath, ctcIndicesDir, categoryQueriesDirFull);
        sw.reset();
        cqEx.extractAllQueries(numCatJoins);
        writeRequiredExecutionTimeToFile(categoryQueriesDirFull, "categoryQueriesGenerationTime",
                sw.stopAndShow(), QueryType.CategoryQuery);
        cqEx = null;
        System.gc();
    }

    private void startFilterQueryGeneration() {
        fqEx = new FilterQueryGenerator(hdtPath, pvIndicesDir, filterQueriesDirFull, numFilters);
        sw.reset();
        fqEx.extract3LevelsOfDifficulty();
        writeRequiredExecutionTimeToFile(filterQueriesDirFull, "filterQueriesGenerationTime",
                sw.stopAndShow(), QueryType.FilterQuery);
        QueryModelManager.queryModelToString(filterQueriesDirFull, numFilters, numFilterLevels);
        fqEx = null;
        System.gc();
    }

    private void startHybridQueryGeneration() {
        hqEx = new HybridQueryGenerator(hdtPath, ctcIndicesDir, pvIndicesDir, categoryQueriesDirFull, filterQueriesDirFull, hybridQueriesDirFull);
        sw.reset();
        hqEx.startExtraction();
        writeRequiredExecutionTimeToFile(hybridQueriesDirFull, "hybridQueriesGenerationTime",
                sw.stopAndShow(), QueryType.HybridQuery);
        hqEx = null;
        System.gc();
    }

    private void writeRequiredExecutionTimeToFile(String path, String fileName, String time, QueryType qt) {

        String queryName;
        switch (qt) {
            case CategoryQuery:
                queryName = "category queries";
                break;
            case FilterQuery:
                queryName = "filter queries";
                break;
            default:
                queryName = "hybrid queries";
                break;
        }

        FileWriter fw = null;
        try {
            File f = new File(Paths.get(path, fileName + ".txt").toString());
            f.createNewFile();
            fw = new FileWriter(f);
            fw.write("Time required to extract " + queryName + ": "  + time);
            fw.flush();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}

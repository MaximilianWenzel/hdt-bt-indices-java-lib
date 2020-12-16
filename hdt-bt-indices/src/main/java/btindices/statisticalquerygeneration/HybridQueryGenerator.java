package btindices.statisticalquerygeneration;

import btindexmodels.facetedsearchmodels.SemanticAnnotationResults;
import btindices.indicesmanager.CatExplorationManager;
import btindices.indicesmanager.FilterExplorationManagerIndices;
import org.rdfhdt.hdt.dictionary.Dictionary;
import org.rdfhdt.hdt.util.StopWatch;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a class in order to generate representative hybrid queries (= combination of a filter and category query)
 * for a given RDF data set.
 */
public class HybridQueryGenerator {

    private static final StopWatch sw = new StopWatch();
    CatExplorationManager catManager;
    FilterExplorationManagerIndices filterManager;
    String categoryQueriesDir;
    String filterQueriesDir;
    String targetDir;
    PropertyValueCollection pvCollector;
    ArrayList<SemanticAnnotationResults> top100PropertyValues;
    double[] quantile;
    private Dictionary dic;

    public HybridQueryGenerator(String hdtPath, String ctcIndicesDir, String pvIndicesDir, String categoryQueriesDir, String filterQueriesDir, String targetDir) {
        catManager = new CatExplorationManager(hdtPath, ctcIndicesDir);
        filterManager = new FilterExplorationManagerIndices(catManager.hdt, pvIndicesDir);
        this.categoryQueriesDir = categoryQueriesDir;
        this.filterQueriesDir = filterQueriesDir;
        pvCollector = new PropertyValueCollection(catManager.hdt, filterManager.indicesManager);
        this.targetDir = targetDir;
        quantile = new double[]{0.3, 0.8, 1.0}; // quantile for 3 levels of difficulty
        this.dic = catManager.hdt.getDictionary();
    }

    public static void main(String[] args) {
        // <HDT-PATH> <CTC-INDICES-DIR> <PV-INDICES-DIR> <CATEGORY-QUERIES-DIR> <FILTER-QUERIES-DIR> <TARGET-DIR>
        String hdtPath = args[0];
        String ctcIndicesDir = args[1];
        String pvIndicesDir = args[2];
        String categoryQueriesDir = args[3];
        String filterQueriesDir = args[4];
        String targetDir = args[5];
        HybridQueryGenerator qe = new HybridQueryGenerator(hdtPath, ctcIndicesDir, pvIndicesDir,
                categoryQueriesDir, filterQueriesDir, targetDir);

        sw.reset();
        qe.startExtraction();

        FileWriter fw = null;
        try {
            fw = new FileWriter("hybridQueryExtractionDuration.txt");
            fw.write("Time required to extract hybrid queries: " + sw.stopAndShow() + "\r\n");
            fw.flush();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // print queries
        QueryModelManager.queryModelToString(targetDir, 4, 3);
    }

    public void startExtraction() {
        // get top 100 property values
        System.out.println("Collecting top 100 property value pairs...");
        top100PropertyValues = pvCollector.getPropertyValuePairs(100);

        // create target directory if it does not exist
        File directory = new File(targetDir);
        if (!directory.exists()) {
            directory.mkdir();
        }

        System.out.println("Start query extraction...");
        // for each existing query, apply filters and extract resulting queries
        int maxJoins = 4;
        int maxLevel = 3;
        String queryName;

        for (int i = 1; i <= maxJoins; i++) {
            for (int level = 1; level <= maxLevel; level++) {
                // extract query for each level
                ArrayList<QueryModel> results = new ArrayList<>();
                queryName = getQueryString(i, level);
                System.out.println("Current queries: " + queryName);
                ArrayList<QueryModel> catQMs = QueryModelManager.loadQueryModelsFromFile(Paths.get(categoryQueriesDir, queryName).toString());

                catQMs = QueryModelManager.extractNumQueriesFromList(catQMs, 50);

                // update exploration state to qm
                for (int k = 0; k < catQMs.size(); k++) {
                    QueryModel catQM = catQMs.get(k);
                    ArrayList<QueryModel> filterQueries = extractFilterQuery(catQM, level);
                    if (filterQueries != null) {
                        results.addAll(filterQueries);
                    }
                }

                // save queries to target directory
                QueryModelManager.saveQueryModelsToFile(Paths.get(targetDir, queryName).toString(), results);
            }
        }
    }

    private ArrayList<QueryModel> extractFilterQuery(QueryModel catQM, int currLevel) {
        ArrayList<QueryModel> results = new ArrayList<>();

        // apply as much filters as levels to the data set
        int numFilters = currLevel;

        catManager.updateExplorationState(catQM);
        String rdfClass = catQM.types.get(catQM.types.size() - 1).toString();

        // update filter manager to exploration state
        filterManager.resetExplorationState();
        filterManager.updateToInitialRDFClass(rdfClass);
        filterManager.center = catManager.hsSubject;

        for (int appliedFilters = 0; appliedFilters < numFilters; appliedFilters++) {
            List<SemanticAnnotationResults> afs = new ArrayList<>(
                    FilterQueryGenerator.getAllAvailableFacetsSortedByResults(filterManager, top100PropertyValues));

            if (afs.size() == 0) {
                // no available filters -> consider next query model
                return new ArrayList<>();
            }

            if (appliedFilters == numFilters - 1) {
                // add all currently available filters to the result
                catQM.addFilterToType(catQM.types.size() - 1, filterManager.appliedFacets);
                afs.forEach(s -> {
                    QueryModel qm = catQM.getCopy();
                    qm.addFilterToType(catQM.types.size() - 1, s.getSemanticAnnotation(dic));
                    results.add(qm);
                });
                return results;

            } else {
                // get quantile from available filters
                SemanticAnnotationResults filter = afs.get((int) ((afs.size() - 1) * quantile[currLevel - 1]));
                // apply filter
                filterManager.applyFacet(true, filter.facetID, filter.facetValue);
            }
        }

        return results;
    }

    private String getQueryString(int numJoins, int level) {
        return "q" + numJoins + "_" + level;
    }
}

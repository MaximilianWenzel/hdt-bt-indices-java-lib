package btindices.statisticalquerygeneration;

import btindexmodels.LabeledResults;
import btindexmodels.facetedsearchmodels.SemanticAnnotation;
import btindexmodels.facetedsearchmodels.SemanticAnnotationResults;
import btindices.HDTUtil;
import btindices.indicesmanager.FilterExplorationManager;
import btindices.indicesmanager.FilterExplorationManagerIndices;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.rdfhdt.hdt.dictionary.Dictionary;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.util.StopWatch;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.*;

/**
 * Represents a class in order to generate representative filter queries for a given RDF data set.
 */
public class FilterQueryGenerator {

    public long thresholdPropertyValues;
    String outputDir;
    int maxNumFilters;
    FilterExplorationManager fsManager;
    URI[] types;
    HDT hdt;
    Dictionary dic;
    ArrayList<SemanticAnnotationResults> topNPropertyValuePairs;
    PropertyValueCollection pvc;
    int topNPropertyValues;


    public FilterQueryGenerator(FilterExplorationManager fsManager, String outputDir, int maxNumFilters) {
        init(fsManager, outputDir, maxNumFilters);
    }

    public FilterQueryGenerator(String hdtPath, String pvIndicesDir, String outputDir, int maxNumFilters) {
        FilterExplorationManager fsManager = new FilterExplorationManagerIndices(hdtPath, pvIndicesDir);
        init(fsManager, outputDir, maxNumFilters);
    }

    public static void main(String[] args) {
        // <HDT-PATH> <FS-INDICES-DIR> <EXTRACTED-QUERIES-DIR> <NUM-FILTERS>
        try {

            String hdtPath = args[0];
            String pvIndicesDir = args[1];
            String queryDir = args[2];
            int numFilters = Integer.parseInt(args[3]);
            StopWatch sw = new StopWatch();

            sw.reset(); // stop time
            FilterQueryGenerator extractor = new FilterQueryGenerator(hdtPath, pvIndicesDir, queryDir, numFilters);
            extractor.extract3LevelsOfDifficulty();

            FileWriter fw = new FileWriter("filterQueryExtractionDuration.txt");
            fw.write("Time required to extract queries with " + numFilters + " filters: " + sw.stopAndShow());
            fw.flush();
            fw.close();
            // print queries
            QueryModelManager.queryModelToString(queryDir, numFilters, 3);

        } catch (IOException e) {
            System.out.println("Arguments: <HDT-PATH> <FS-INDICES-DIR> <EXTRACTED-QUERIES-DIR> <NUM-FILTERS>");
            e.printStackTrace();
        }
    }

    public static List<SemanticAnnotationResults> getAllAvailableFacetsSortedByResults(FilterExplorationManager fsManager, ArrayList<SemanticAnnotationResults> topNPropertyValuePairs) {
        List<SemanticAnnotationResults> results = new ArrayList<>();

        for (int i = 0; i < topNPropertyValuePairs.size(); i++) {
            UnifiedSet<Long> centerClone = fsManager.center.clone();
            SemanticAnnotationResults sa = topNPropertyValuePairs.get(i);
            centerClone = fsManager.getSubsetOfHashSet(centerClone, sa.facetID, sa.facetValue, sa.facetValueIsObject);

            if (centerClone.size() == fsManager.center.size()) {
                // if number of resources stays same after filtering - discard filter
                continue;
            }

            if (centerClone.size() > 0) {
                // add objID + numResults to resulting array
                SemanticAnnotationResults resultsForPredObj = new SemanticAnnotationResults();
                resultsForPredObj.facetID = sa.facetID;
                resultsForPredObj.facetValue = sa.facetValue;
                resultsForPredObj.facetValueIsObject = sa.facetValueIsObject;
                resultsForPredObj.numAnnotatedResults = (long) centerClone.size();
                results.add(resultsForPredObj);
            }
        }

        Collections.sort(results);

        return results;
    }

    private void init(FilterExplorationManager fsManager, String outputDir, int maxNumFilters) {
        this.fsManager = fsManager;
        types = fsManager.typeURIs;
        hdt = fsManager.hdt;
        dic = fsManager.dic;
        this.outputDir = outputDir;
        this.maxNumFilters = maxNumFilters;
        topNPropertyValues = 100;

        pvc = new PropertyValueCollection(hdt, fsManager.indicesManager);

        // Create directory if it does not exist
        File directory = new File(outputDir);
        if (!directory.exists()) {
            directory.mkdir();
        }

    }

    public void saveQueries(ArrayList<QueryModel> qms, String fileName) {
        String filePath = Paths.get(outputDir, fileName).toString();
        QueryModelManager.saveQueryModelsToFile(filePath, qms);
    }

    public String getNameForQueries(int numAppliedFilters, int difficulty) {
        if (numAppliedFilters == 0) {
            return "q0";
        }
        return "q" + numAppliedFilters + "_" + difficulty;
    }


    /*
    public void extractQueriesRecursively(int numFiltersToApply) {
        if (numFiltersToApply == 0) {
            return;
        }

        // save variables before recursive calls
        ArrayList<AvailableFacet> availableFacets = fsManager.availableFacets;
        ArrayList<SemanticAnnotation> appliedFacets = fsManager.appliedFacets;
        UnifiedSet<Long> center = fsManager.center;
        URI currentType = fsManager.currentType;

    }
    */

    public void extract3LevelsOfDifficulty() {

        double[] semanticAnnotationQuantile = {0.3, 0.6, 1.0};
        int lvls = 3;
        ArrayList<ArrayList<ArrayList<QueryModel>>> queriesLvlNumFilters = new ArrayList<>(); // queries[numLevels][numFilters]
        for (int i = 0; i < lvls; i++) {
            queriesLvlNumFilters.add(new ArrayList<>());
            for (int k = 0; k < maxNumFilters; k++) {
                queriesLvlNumFilters.get(i).add(new ArrayList<>());
            }
        }


        ArrayList<LabeledResults> typesSortedByResults = HDTUtil.getAllTypesSortedByResults(types, hdt);
        ArrayList<LabeledResults> typesOfLvl;

        System.out.println("Get property value pairs...");
        topNPropertyValuePairs = pvc.getPropertyValuePairs(topNPropertyValues);

        System.out.println("Start query extraction...");
        for (int lvlIt = 0; lvlIt < lvls; lvlIt++) {

            System.out.println("Level: (" + (lvlIt + 1) + "/" + lvls + ")");
            typesOfLvl = get3PartsOfSortedArray(typesSortedByResults, (lvlIt + 1));


            for (int typesIt = 0; typesIt < typesOfLvl.size(); typesIt++) {
                System.out.println("Type: (" + (typesIt + 1) + "/" + typesOfLvl.size() + ") " +
                        typesOfLvl.get(typesIt).label);

                // step 1: update exploration state
                fsManager.resetExplorationState();
                fsManager.updateToInitialRDFClass(typesOfLvl.get(typesIt).label);
                fsManager.updateAvailableFacets();

                // step 2: get available facets and extract queries
                List<SemanticAnnotationResults> afs = getAllAvailableFacetsSortedByResults(fsManager, topNPropertyValuePairs);
                QueryModel currentQuery = new QueryModel();
                currentQuery.types.add(URI.create(typesOfLvl.get(typesIt).label));
                queriesLvlNumFilters.get(lvlIt).get(0).addAll(getQueryModels(afs, currentQuery));


                // for every next filter:
                for (int numFilterIt = 1; numFilterIt < maxNumFilters; numFilterIt++) {

                    if (afs.size() == 0)
                        break;

                    // step 1
                    int quantileIndex = (int) ((afs.size() - 1) * semanticAnnotationQuantile[lvlIt]);
                    SemanticAnnotationResults sa = afs.get(quantileIndex);
                    while (currentQuery.filters.size() > 0
                            && currentQuery.filters.get(0).contains(sa.getSemanticAnnotation(dic))) {
                        if (quantileIndex > 0) {
                            quantileIndex--;
                        } else {
                            break;
                        }
                        sa = afs.get(quantileIndex);
                    }

                    currentQuery.addFilterToType(0, sa.getSemanticAnnotation(dic));
                    fsManager.applyFacet(true, sa.facetID, sa.facetValue);
                    fsManager.updateAvailableFacets();

                    // step 2
                    afs = getAllAvailableFacetsSortedByResults(fsManager, topNPropertyValuePairs);
                    queriesLvlNumFilters.get(lvlIt).get(numFilterIt).addAll(getQueryModels(afs, currentQuery));
                }
            }
        } // end: for each level

        // save queries
        for (int i = 1; i <= lvls; i++) {
            for (int k = 1; k <= maxNumFilters; k++) {
                saveQueries(queriesLvlNumFilters.get(i - 1).get(k - 1), getNameForQueries(k, i));
            }
        }

    }


    /**
     * Splits the array into 3 parts and returns the part which is passed to the method.
     *
     * @param part Choose part 1, 2 or 3.
     */
    public ArrayList<LabeledResults> get3PartsOfSortedArray(ArrayList<LabeledResults> typesSorted, int part) {
        if (part > 3 || part < 1)
            return null;


        int lengthOnePart = ((typesSorted.size() - 1) / 3);
        int startIndex = (part - 1) * lengthOnePart;
        int endIndex = part * lengthOnePart;

        ArrayList<LabeledResults> results = new ArrayList<>();

        for (int i = startIndex; i <= endIndex; i++) {
            results.add(typesSorted.get(i));
        }
        return results;
    }

    public ArrayList<Long> getMostOccuringObjects(int topNObjects) {
        TreeMap<Long, ArrayList<Long>> objectIDToResult = new TreeMap<>();
        long numObjs = dic.getNobjects();

        // if object is an RDF type - discard it
        UnifiedSet<Long> typeIDs = new UnifiedSet<>();
        for (int i = 0; i < types.length; i++) {
            long typeID = dic.stringToId(types[i].toString(), TripleComponentRole.OBJECT);
            typeIDs.add(typeID);
        }

        TripleID tID = new TripleID(0, 0, 0);
        IteratorTripleID itID;
        for (long i = 1; i <= numObjs; i++) {
            if (typeIDs.contains(i)) {
                continue;
            }
            // get results for object
            tID.setObject(i);
            itID = hdt.getTriples().search(tID);

            long resultsEst = itID.estimatedNumResults();
            ArrayList<Long> objIDs = objectIDToResult.get(resultsEst);
            if (objIDs == null) {
                objIDs = new ArrayList<>();
                objIDs.add(i);
                objectIDToResult.put(resultsEst, objIDs);
            } else {
                objIDs.add(i);
            }
        }

        // get top N objects
        Iterator<ArrayList<Long>> objIDs = objectIDToResult.descendingMap().values().iterator();
        ArrayList<Long> results = new ArrayList<>();
        long count = 0;
        ArrayList<Long> objsArray = null;
        while (count < topNObjects && objIDs.hasNext()) {
            objsArray = objIDs.next();
            for (int i = 0; i < objsArray.size(); i++) {
                results.add(objsArray.get(i));
                count++;
                if (count == topNObjects)
                    return results;
            }
        }

        return results;

    }

    public ArrayList<QueryModel> getQueryModels(List<SemanticAnnotationResults> afs, QueryModel currentQuery) {
        ArrayList<QueryModel> qms = new ArrayList<>();
        afs.forEach(r -> {
            QueryModel qm = currentQuery.getCopy();
            SemanticAnnotation s = r.getSemanticAnnotation(dic);
            qm.addFilterToType(0, s);
            qms.add(qm);
        });

        return qms;
    }


}

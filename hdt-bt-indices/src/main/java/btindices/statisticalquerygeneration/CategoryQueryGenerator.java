package btindices.statisticalquerygeneration;

import btindexmodels.categoryexplorationmodels.SingleJoinModel;
import btindices.indicesmanager.CatExplorationManager;
import org.rdfhdt.hdt.util.StopWatch;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Represents a class which can be used to generate representative queries for a given RDF data set.
 */
public class CategoryQueryGenerator {

    public static final StopWatch sw = new StopWatch();
    private CatExplorationManager em;
    private String outputDir;
    private boolean onlyCount;
    private boolean predicates;
    private double quantile;
    private long lowerBound;
    private long upperBound;
    private int numOfQueriesToExtract;

    public CategoryQueryGenerator(String hdtPath, String btIndicesPath, String outputDir) {
        System.out.println("Initialize exploration manager...");
        this.em = new CatExplorationManager(hdtPath, btIndicesPath);
        System.out.println("Exploration manager initialized.");
        this.outputDir = outputDir;

        // Create directory if it does not exist
        File directory = new File(outputDir);
        if (!directory.exists()) {
            directory.mkdir();
        }

        this.predicates = true;
    }

    public static void main(String[] args) {

        // <HDT-PATH> <BT-INDICES-PATH> <EXTRACTED-QUERIES-DIRECTORY> <NUM-JOINS>
        try {
            CategoryQueryGenerator qe = new CategoryQueryGenerator(args[0], args[1], args[2]);
            qe.numOfQueriesToExtract = Integer.MAX_VALUE;
            int numJoins = Integer.parseInt(args[3]);

            String queriesDir = args[2];

            sw.reset();
            qe.extractAllQueries(numJoins);

            FileWriter fw = new FileWriter("catQueryExtractionDuration.txt");
            fw.write("Time required to extract queries with " + queriesDir + " joins: " + sw.stopAndShow());
            fw.flush();
            fw.close();

            QueryModelManager.queryModelToString(queriesDir, numJoins, 3);

        } catch (IOException e) {
            System.out.println("Arguments: <HDT-PATH> <BT-INDICES-PATH> <EXTRACTED-QUERIES-DIRECTORY> <NUM-JOINS>");
            e.printStackTrace();
        }


    }

    private static void printSPARQLQueries(ArrayList<QueryModel> qmList) {
        for (int i = 0; i < qmList.size(); i++) {
            System.out.println(QueryModelFormatter.getSparqlQuery(qmList.get(i)));
        }
    }

    /**
     * Extracts 3 levels of difficulty up to and including the given number of joins from the data set.
     */
    public void extractAllQueries(int numJoins) {
        extractInitialQueries();
        for (int i = 1; i <= numJoins; i++) {
            System.out.println("Extract queries with " + i + " joins...");
            extract3QueryClasses(i);
        }
        System.out.println("Queries successfully extracted.");
    }

    public void extract3QueryClasses(int numJoins) {

        // 3 different query classes
        this.lowerBound = 0;
        this.upperBound = Long.MAX_VALUE;

        // needed if upper bound has been set too high since no queries could be extracted
        long manualUpperBound = 0;

        double[] typeQuantile = {0.3, 0.6, 1.0};
        double[] quantileJoins = {0, 0.5, 1.0};
        this.quantile = quantileJoins[0];
        SingleJoinModel type;
        int numLevels = 3;

        for (int i = 0; i < numLevels; i++) {

            System.out.println("Level of difficulty: " + (i + 1));
            QueryModelGenerator qmg = new QueryModelGenerator(em, predicates, onlyCount, numOfQueriesToExtract);

            // set upper bound
            long maxConnectionResults = qmg.getMaxPercentileOfInitialIncomingOutgoingCons(em.btIndexManager.typeURIs,
                    typeQuantile[i], predicates);
            long typeResults = qmg.getPercentileOfTypes(typeQuantile[i]).getResults();

            // set upper bound
            this.upperBound = Math.max(maxConnectionResults, typeResults);
            this.upperBound = Math.max(this.upperBound, manualUpperBound); // if upper bound was to low

            // set output
            Path out = getOutputFilePath("q" + numJoins, (i + 1), this.predicates);
            qmg.outputFile = out.toString();

            System.out.println("LowerBound = " + this.lowerBound + ", UpperBound = " + this.upperBound);
            System.out.println("Percentile = " + quantile);
            ArrayList<QueryModel> extractedQMs = qmg.generateJoinQueries(numJoins, quantile, this.lowerBound, this.upperBound);
            if (extractedQMs.size() > 0) {

                QueryModelManager.saveQueryModelsToFile(out.toString(), extractedQMs);
            } else {
                // upper bound was set too low - try again with higher upper bound
                System.out.println("Upper bound was set to low! Try again with higher upper bound...");
                i--;
                manualUpperBound = Math.max(this.upperBound * 2, manualUpperBound * 2);

                if (manualUpperBound > qmg.em.hdt.getTriples().getNumberOfElements()) {
                    this.quantile += 0.1;
                    this.quantile = Math.min(1.0, this.quantile);

                    if (this.quantile >= 1.0) {
                        System.out.println("No queries could be extracted. Number of joins: " + numJoins + ", difficulty: " + (i + 1));
                        break;
                    }
                }
                continue;
            }

            // update variables
            //this.lowerBound = this.upperBound;
            if (i < numLevels - 1) {
                this.quantile = quantileJoins[i + 1];
            }
        }
    }

    public void extractInitialQueries() {
        QueryModelGenerator qmg = new QueryModelGenerator(em, this.predicates, onlyCount, numOfQueriesToExtract);
        qmg.outputFile = Paths.get(outputDir, "q0").toString();
        ArrayList<QueryModel> extractedQMs = qmg.generateJoinQueries(0, quantile, this.lowerBound, Long.MAX_VALUE);
        QueryModelManager.saveQueryModelsToFile(qmg.outputFile, extractedQMs);
    }

    public Path getOutputFilePath(String fileName) {
        String percentileAndBounds = ((int) (100 * this.quantile)) + "p_" + this.lowerBound + "_" + this.upperBound;
        if (predicates) {
            return Paths.get(outputDir, fileName + "preds_" + percentileAndBounds);
        }
        return Paths.get(outputDir, fileName + "_" + percentileAndBounds);
    }

    public Path getOutputFilePath(String fileName, int level, boolean predicates) {
        /*
        if (predicates) {
            return Paths.get(outputDir, fileName + "preds_" + level);
        }

         */
        return Paths.get(outputDir, fileName + "_" + level);
    }

}

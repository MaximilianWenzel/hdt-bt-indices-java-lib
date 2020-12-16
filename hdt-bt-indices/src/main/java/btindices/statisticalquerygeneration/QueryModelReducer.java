package btindices.statisticalquerygeneration;

/**
 * Represents a console application which reduces the number of queries (randomly) for all query models
 * of a given directory.
 */
public class QueryModelReducer {

    public static void main(String[] args) {

        int numLevels = 0;
        int maxNumJoins = 0;
        int numQueries = 0;
        try {
            maxNumJoins = Integer.parseInt(args[2]);
            numLevels = Integer.parseInt(args[3]);
            numQueries = Integer.parseInt(args[4]);

        } catch (Exception e) {
            System.out.println("Could not parse arguments. Supported syntax:");
            System.out.println("QueryModelReducer.jar <QUERY-DIRECTORY> <REDUCED-QUERIES-DIRECTORY> <MAX-NUM-JOINS> <NUM-LEVELS> <REDUCE-QUERIES-TO>");
        }

        System.out.println("Start reduction of queries...");
        QueryModelManager.reduceNumberOfQueries(args[0], args[1], numQueries, numLevels, maxNumJoins);
        System.out.println("Queries have been successfully reduced.");
    }
}

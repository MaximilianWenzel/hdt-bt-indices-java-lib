package btindices.statisticalquerygeneration;

/**
 * A console app which reads all query models from the specified directory and writes them as SPARQL queries to a file.
 */
public class QueryModelPrinter {

    public static void main(String[] args) {

        // <QUERY-DIRECTORY> <NUM-LEVELS> <NUM-JOINS>
        try {
            int numJoins = Integer.parseInt(args[1]);
            int numLevels = Integer.parseInt(args[2]);
            //QueryModelManager.queryModelToString(args[0], numJoins, numLevels, false);
            QueryModelManager.queryModelToString(args[0], numJoins, numLevels);

        } catch (NumberFormatException e) {
            System.out.println("Could not parse arguments: " + e.getMessage());
            System.out.println("Syntax:");
            System.out.println("QueryModelPrinter.jar <QUERY-DIRECTORY> <NUM-JOINS> <NUM-LEVELS>");
            e.printStackTrace();
        }

    }

}

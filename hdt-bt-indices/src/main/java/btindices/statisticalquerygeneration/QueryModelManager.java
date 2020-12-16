package btindices.statisticalquerygeneration;

import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

/**
 * Represents a class which provides utility methods for Query Model objects.
 */
public class QueryModelManager {


    public static void printCountOfQueryModelLists(String directory, int numJoins, int numLevels) {
        for (int i = 1; i <= numJoins; i++) {
            String query = "q" + i;
            for (int k = 1; k <= numLevels; k++) {

                String fileName = getQueryFilePath(directory, query, k).toString();
                ArrayList<QueryModel> qmList = QueryModelManager.loadQueryModelsFromFile(fileName);

                System.out.println(fileName + ": " + qmList.size());
            }
        }
    }

    /**
     * Generates for all extracted query model files of the current directory a text file which contains the query
     * models as SPARQL queries.
     */
    public static void queryModelToString(String directory, int queryTypesPerLvl, int numLevels) {
        try {
            String fileName;
            ArrayList<QueryModel> qmList;
            File queryTextFile;
            PrintStream ps;

            for (int i = 0; i <= queryTypesPerLvl; i++) {
                String query = "q" + i;
                for (int k = 1; k <= numLevels; k++) {


                    if (i == 0) {
                        // print queries without joins separately
                        fileName = Paths.get(directory, "q0").toString();
                    } else {

                        fileName = getQueryFilePath(directory, query, k).toString();
                    }

                    qmList = QueryModelManager.loadQueryModelsFromFile(fileName);

                    queryTextFile = new File(fileName + ".txt");
                    ps = new PrintStream(queryTextFile);

                    ps.println("Number of queries: " + qmList.size());
                    ps.println();
                    for (int j = 0; j < qmList.size(); j++) {

                        String queryStr;
                        queryStr = QueryModelFormatter.getSparqlQuery(qmList.get(j));
                        ps.println(queryStr);
                        ps.println();
                        ps.println();
                    }
                    ps.close();

                    if (i == 0) {
                        // queries without joins have only 1 level of difficulty
                        break;
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    public static void saveQueryModelsToFile(String filePath, ArrayList<QueryModel> qm) {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath));
            oos.writeObject(qm);
            oos.close();
            return;

        } catch (FileNotFoundException e) {
            e.printStackTrace();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<QueryModel> loadQueryModelsFromFile(String filePath) {
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath));

            @SuppressWarnings("unchecked")
            ArrayList<QueryModel> loadedObj = (ArrayList<QueryModel>) ois.readObject();
            ois.close();
            return loadedObj;

        } catch (FileNotFoundException e) {
            return new ArrayList<>();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {

            e.printStackTrace();
        }

        return new ArrayList<>();
    }

    public static void reduceNumberOfQueries(String sourceDir, String targetDir, int numQueries, int numLevels, int numJoins) {

        File directory = new File(targetDir);

        if (!directory.exists()) {
            directory.mkdir();
        }
        reduceInitialQueryModelList(sourceDir, targetDir, numQueries);

        reduceJoinQueryModelList(sourceDir, targetDir, numQueries, numLevels, numJoins);

    }

    private static void reduceJoinQueryModelList(String sourceDir, String targetDir, int numQueries, int numLevels, int numJoins) {
        for (int i = 1; i <= numJoins; i++) {

            String query = "q" + i;
            for (int k = 1; k <= numLevels; k++) {

                String fileName = getQueryFilePath(sourceDir, query, k).toString();
                ArrayList<QueryModel> qmList = QueryModelManager.loadQueryModelsFromFile(fileName);

                ArrayList<QueryModel> result = extractNumQueriesFromList(qmList, numQueries);

                if (result.size() == 0)
                    continue;

                // write extracted list of queries to specified directory
                fileName = getQueryFilePath(targetDir, query, k).toString();
                QueryModelManager.saveQueryModelsToFile(fileName, result);
            }
        }
    }

    private static void reduceInitialQueryModelList(String sourceDir, String targetDir, int numQueries) {
        String fileName = Paths.get(sourceDir, "q0").toString();

        ArrayList<QueryModel> qmList = QueryModelManager.loadQueryModelsFromFile(fileName);

        ArrayList<QueryModel> result = extractNumQueriesFromList(qmList, numQueries);
        if (result.size() == 0) {
            return;
        }

        // write extracted list of queries to specified directory
        fileName = Paths.get(targetDir, "q0").toString();
        QueryModelManager.saveQueryModelsToFile(fileName, result);
    }

    public static ArrayList<QueryModel> extractNumQueriesFromList(ArrayList<QueryModel> qmList, int numQueries) {

        ArrayList<Integer> indexArray = new ArrayList<Integer>();
        for (int j = 0; j < qmList.size(); j++) {
            indexArray.add(j);
        }

        Collections.shuffle(indexArray);

        ArrayList<QueryModel> result = new ArrayList<QueryModel>();

        for (int j = 0; j < numQueries && j < qmList.size(); j++) {
            result.add(qmList.get(indexArray.get(j)));
        }

        return result;
    }

    public static Path getQueryFilePath(String directory, String queryName, int level) {
        return Paths.get(directory, queryName + "_" + level);
    }

    private UnifiedSet<Long> copyHashSet(UnifiedSet<Long> toCopy) {
        Iterator<Long> itLong = toCopy.iterator();

        UnifiedSet<Long> result = new UnifiedSet<Long>();

        while (itLong.hasNext()) {
            result.add(itLong.next().longValue());
        }

        return result;
    }
}

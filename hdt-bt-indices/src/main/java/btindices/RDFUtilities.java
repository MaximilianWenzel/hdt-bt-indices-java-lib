package btindices;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Represents a class which provides utility methods for RDF specific problems.
 */
public class RDFUtilities {
    public final static String rdfType = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    public final static ArrayList<String> undesiredCategories = new ArrayList<>(Arrays.asList(
            "http://www.w3.org/2000/01/rdf-schema#Class",
            "http://www.w3.org/2000/01/rdf-schema#Resource",
            "http://www.w3.org/2002/07/owl#Thing",
            "http://www.w3.org/2002/07/owl#Class",
            "http://www.w3.org/2002/07/owl#NamedIndividual"
    ));
}

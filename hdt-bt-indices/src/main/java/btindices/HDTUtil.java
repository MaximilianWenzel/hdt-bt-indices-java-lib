package btindices;

import btindexmodels.LabeledResults;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.rdfhdt.hdt.dictionary.Dictionary;
import org.rdfhdt.hdt.enums.TripleComponentOrder;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManagerImpl;
import org.rdfhdt.hdt.hdt.impl.HDTImpl;
import org.rdfhdt.hdt.iterator.DictionaryTranslateIterator;
import org.rdfhdt.hdt.listener.ProgressOut;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.IteratorTripleString;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.triples.TripleString;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

/**
 * Represents a class with useful methods and variables in order to manage,
 * query and explore an HDT file.
 * 
 * @author Maximilian Wenzel
 *
 */
public class HDTUtil {


	/**
	 * Loads a HDT file from the specified file location.
	 */
	public static HDTImpl loadHDTFile(String filePath) {
		HDTManagerImpl manager = new HDTManagerImpl();
		HDTImpl hdt;

		ProgressOut pOut = new ProgressOut();

		try {
			hdt = (HDTImpl) manager.doLoadIndexedHDT(filePath, pOut);
			return hdt;

		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Executes a string query on a specified HDT file. The value of "null" acts as
	 * a wildcard.
	 * 
	 * @return Returns the results as an iterator.
	 */
	public static IteratorTripleString executeQuery(HDT hdt, CharSequence s, CharSequence p, CharSequence o) {

		try {
			IteratorTripleString its = hdt.search(s, p, o);
			return its;

		} catch (NotFoundException e) {
			return (IteratorTripleString) new ArrayList<TripleString>().iterator();
		}

	}

	/**
	 * Executes a TripleString query on a specified HDT file.
	 * 
	 * @return Returns the results as an iterator.
	 */
	public static IteratorTripleString executeQuery(HDT hdt, TripleString tripleStr) {

		String s = tripleStr.getSubject() != null ? tripleStr.getSubject().toString() : null;
		String p = tripleStr.getPredicate() != null ? tripleStr.getPredicate().toString() : null;
		String o = tripleStr.getObject() != null ? tripleStr.getObject().toString() : null;

		return executeQuery(hdt, s, p, o);
	}

	/**
	 * Executes a triple ID query on a specified HDT file. The value of "0" acts as
	 * a wildcard.
	 * 
	 * @return Returns the results as an iterator.
	 */
	public static IteratorTripleID executeQuery(HDT hdt, TripleID tiID) {

		IteratorTripleID itID = hdt.getTriples().search(tiID);
		return itID;
	}

	/**
	 * Prints all results of an IteratorTripleID as IDs to the standard output.
	 */
	public static void printIteratorResultsAsID(IteratorTripleID itID) {
		System.out.println("Iterator results: ");
		TripleID t;

		while (itID.hasNext()) {
			t = itID.next();
			System.out.println(t.toString());
		}

		itID.goToStart();
	}

	/**
	 * Prints all results of an IteratorTripleID as strings to the standard output.
	 */
	public static void printIteratorResultsAsStrings(Dictionary dic, IteratorTripleID itID) {
		System.out.println("Iterator results: ");
		TripleID t;

		while (itID.hasNext()) {
			t = itID.next();
			System.out.println(tripleIDToString(dic, t));
		}

		itID.goToStart();
	}

	/**
	 * Prints all results of an IteratorTripleString containing only the URI
	 * fragments to the standard output.
	 */
	public static void printIteratorResultsAsURIFragments(IteratorTripleString itString) {
		System.out.println("Iterator results: ");
		TripleString t;

		while (itString.hasNext()) {
			t = itString.next();
			printURIFragmentsForTripleString(t);
		}

		itString.goToStart();
	}
	
	/**
	 * Prints all results of an IteratorTripleID object, containing only the URI
	 * fragments, to the standard output.
	 */
	public static void printIteratorResultsAsURIFragments(Dictionary dic, IteratorTripleID itID) {

		printIteratorResultsAsURIFragments(new DictionaryTranslateIterator(itID, dic));
	}

	/**
	 * Converts a TripleID object to a corresponding string representation.
	 */
	public static CharSequence tripleIDToString(Dictionary dic, TripleID tID) {
		CharSequence sbj;
		CharSequence pred;
		CharSequence obj;
		sbj = dic.idToString(tID.getSubject(), TripleComponentRole.SUBJECT);
		pred = dic.idToString(tID.getPredicate(), TripleComponentRole.PREDICATE);
		obj = dic.idToString(tID.getObject(), TripleComponentRole.OBJECT);

		return sbj + " " + pred + " " + obj;
	}

	/**
	 * Converts a TripleID object to its string representation and prints only the
	 * URI fragments to the standard output. In case of literals, the whole resource
	 * is printed.
	 */
	public static void printURIFragmentsForTripleID(Dictionary dic, TripleID tID) {
		CharSequence sbj;
		CharSequence pred;
		CharSequence obj;
		sbj = dic.idToString(tID.getSubject(), TripleComponentRole.SUBJECT);
		pred = dic.idToString(tID.getPredicate(), TripleComponentRole.PREDICATE);
		obj = dic.idToString(tID.getObject(), TripleComponentRole.OBJECT);

		printURIFragmentsForTripleString(sbj.toString(), pred.toString(), obj.toString());

	}

	/**
	 * Converts a TripleID object to its string representation and prints only the
	 * URI fragments to the standard output. In case of literals, the whole resource
	 * is printed.
	 */
	public static void printURIFragmentsForTripleString(String sbj, String pred, String obj) {

		try {
			URI sbjURI = new URI(sbj);
			URI predURI = new URI(pred);
			URI objURI = new URI(obj);

			sbj = sbjURI.getFragment() != null ? sbjURI.getFragment() : getLastPathSegment(sbjURI.getPath());
			pred = predURI.getFragment() != null ? predURI.getFragment() : getLastPathSegment(predURI.getPath());
			obj = objURI.getFragment() != null ? objURI.getFragment() : getLastPathSegment(objURI.getPath());

			System.out.printf("%-50.50s  %-50.50s %-50.50s%n", sbj, pred, obj);

		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Converts a TripleID object to its string representation and prints only the
	 * URI fragments to the standard output. In case of literals, the whole resource
	 * is printed.
	 */
	public static void printURIFragmentsForTripleString(TripleString tripleStr) {

		String sbj = tripleStr.getSubject().toString();
		String pred = tripleStr.getPredicate().toString();
		String obj = tripleStr.getObject().toString();

		try {
			URI sbjURI = new URI(sbj);
			URI predURI = new URI(pred);
			URI objURI = new URI(obj);

			sbj = sbjURI.getFragment() != null ? sbjURI.getFragment() : getLastPathSegment(sbjURI.getPath());
			pred = predURI.getFragment() != null ? predURI.getFragment() : getLastPathSegment(predURI.getPath());
			obj = objURI.getFragment() != null ? objURI.getFragment() : getLastPathSegment(objURI.getPath());

			System.out.printf("%-50.50s  %-50.50s %-50.50s%n", sbj, pred, obj);

		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Returns the number of resources which are annotated by a specific RDF class in ascending order.
	 */
	public static ArrayList<LabeledResults> getAllTypesSortedByResults(URI[] types, HDT hdt) {
		Dictionary dic = hdt.getDictionary();
		ArrayList<LabeledResults> results = new ArrayList<>();

		TripleID tID = new TripleID(0, 0, 0);
		for (int i = 0; i < types.length; i++) {
			long rdfClassID = dic.stringToId(types[i].toString(), TripleComponentRole.OBJECT);
			tID.setObject(rdfClassID);
			IteratorTripleID itID = hdt.getTriples().search(tID);
			LabeledResults r = new LabeledResults();
			r.results = itID.estimatedNumResults();
			r.label = types[i].toString();
			results.add(r);
		}

		Collections.sort(results);
		return results;
	}

	/**
	 * Returns the last path segment for a given sequence of path segments.
	 */
	public static String getLastPathSegment(String path) {
		String[] segments = path.split("/");
		return segments[segments.length - 1];
	}

	/**
	 * Generates a hash set with all IDs of the specified triple component role
	 * (i.e. save all subjects, predicates or objects).
	 *
	 */
	public static UnifiedSet<Long> generateHashSetForIterator(IteratorTripleID itID, TripleComponentRole role) {

		UnifiedSet<Long> hs = new UnifiedSet<Long>((int) itID.estimatedNumResults());

		while (itID.hasNext()) {
			if (role == TripleComponentRole.SUBJECT) {
				// save subject
				hs.add(itID.next().getSubject());

			} else if (role == TripleComponentRole.OBJECT) {
				// save object
				hs.add(itID.next().getObject());

			} else if (role == TripleComponentRole.PREDICATE) {

				// save predicate
				hs.add(itID.next().getPredicate());
			}
		}

		itID.goToStart();
		return hs;
	}

	/**
	 * Returns all RDF classes of a given RDF data set in a hash set.
	 */
	public static UnifiedSet<Long> getRDFTypeHashSet(HDT hdt) {
		long p = hdt.getDictionary().stringToId(RDFUtilities.rdfType, TripleComponentRole.PREDICATE);

		IteratorTripleID itID = HDTUtil.executeQuery(hdt, new TripleID(0, p, 0));

		UnifiedSet<Long> rdfTypesSet = new UnifiedSet<Long>();

		while (itID.hasNext()) {

			rdfTypesSet.add(itID.next().getObject());
		}

		return rdfTypesSet;
	}


	/**
	 * Obtains all RDF classes from a given HDT file and returns them in an array as URIs.
	 */
	public static URI[] getRDFTypesAsArray(HDT hdt) {
		UnifiedSet<Long> rdfTypes = getRDFTypeHashSet(hdt);
		Iterator<Long> it = rdfTypes.iterator();
		String rdfType;
		ArrayList<URI> result = new ArrayList<>();
		while (it.hasNext()) {
			rdfType = hdt.getDictionary().idToString(it.next(), TripleComponentRole.OBJECT).toString();
			if (RDFUtilities.undesiredCategories.contains(rdfType)) {
				continue;
			}
			result.add(URI.create(rdfType));
		}
		URI[] types = new URI[result.size()];
		return result.toArray(types);
	}

	/**
	 * Generates a hash set with all IDs of the specified triple component role in
	 * correlation to their IDs in the dictionary. (i.e. save all subjects,
	 * predicates or objects).
	 *
	 */
	public static UnifiedSet<Long> generateIDHashSetForIteratorTripleStr(IteratorTripleString itString,
			TripleComponentRole role, Dictionary dic, TripleComponentRole dictionaryIDRole) {

		UnifiedSet<Long> hs = new UnifiedSet<Long>((int) itString.estimatedNumResults());

		long resource;

		while (itString.hasNext()) {
			if (role == TripleComponentRole.SUBJECT) {
				// save subject
				resource = dic.stringToId(itString.next().getSubject(), dictionaryIDRole);

			} else if (role == TripleComponentRole.OBJECT) {
				// save object
				resource = dic.stringToId(itString.next().getObject(), dictionaryIDRole);

			} else {

				// save predicate
				resource = dic.stringToId(itString.next().getPredicate(), dictionaryIDRole);
			}

			hs.add(resource);
		}

		itString.goToStart();
		return hs;
	}

	/**
	 * Converts a string hash set into a ID hash set in correlation to the
	 * corresponding ID role in the dictionary. (i.e. save all subjects, predicates
	 * or objects).
	 *
	 */
	public static UnifiedSet<Long> convertStringHSIntoIDHS(UnifiedSet<String> hashSetStr, Dictionary dic,
			TripleComponentRole dictionaryIDRole) {

		UnifiedSet<Long> idHashSet = new UnifiedSet<Long>((int) hashSetStr.size());

		long resource;

		Iterator<String> itStr = hashSetStr.iterator();

		while (itStr.hasNext()) {
			if (dictionaryIDRole == TripleComponentRole.SUBJECT) {
				// save subject
				resource = dic.stringToId(itStr.next(), dictionaryIDRole);

			} else if (dictionaryIDRole == TripleComponentRole.OBJECT) {
				// save object
				resource = dic.stringToId(itStr.next(), dictionaryIDRole);

			} else {

				// save predicate
				resource = dic.stringToId(itStr.next(), dictionaryIDRole);
			}

			idHashSet.add(resource);
		}

		return idHashSet;
	}

	/**
	 * Generates a hash set with all strings of the specified triple component role
	 * (i.e. save all subjects, predicates or objects).
	 *
	 */
	public static UnifiedSet<String> generateHashSetForIterator(IteratorTripleString itString,
			TripleComponentRole role) {

		UnifiedSet<String> hs = new UnifiedSet<String>((int) itString.estimatedNumResults());

		while (itString.hasNext()) {
			if (role == TripleComponentRole.SUBJECT) {
				// save subject
				hs.add(itString.next().getSubject().toString());

			} else if (role == TripleComponentRole.OBJECT) {
				// save object
				hs.add(itString.next().getObject().toString());

			} else if (role == TripleComponentRole.PREDICATE) {

				// save predicate
				hs.add(itString.next().getPredicate().toString());
			}
		}

		itString.goToStart();
		return hs;
	}

	/**
	 * Writes all elements of an Iterator<? extends CharSequence> object to the
	 * standard output.
	 */
	public static void printIteratorCharsequence(Iterator<? extends CharSequence> itString) {

		while (itString.hasNext()) {
			System.out.println(itString.next());
		}

	}

	/**
	 * Writes all elements of an IteratorTripleString to the standard output.
	 */
	public static void printIteratorTripleString(IteratorTripleString itString) {
		if (itString.estimatedNumResults() == 0) {
			System.out.println("No results.");
		}

		while (itString.hasNext()) {
			System.out.println(itString.next().toString());
		}

		itString.goToStart();
	}

	/**
	 * Adds quotes to the string if it is a literal (because apache command line
	 * removes all quotes...)
	 */
	public static String addQuotesIfNecessary(String s) {

		if (s != null && !s.startsWith("http") && !s.startsWith("\"")) {
			return "\"" + s + "\"";
		}

		return s;
	}

	/**
	 * Returns a TripleID object for a given subject, predicate and object in
	 * regarding their IDs in the passed dictionary.
	 */
	public static TripleID getTripleIDFromStrings(Dictionary dic, String s, String p, String o) {
		TripleID pattern = new TripleID();

		pattern.setSubject(dic.stringToId(s, TripleComponentRole.SUBJECT));
		pattern.setPredicate(dic.stringToId(p, TripleComponentRole.PREDICATE));
		pattern.setObject(dic.stringToId(o, TripleComponentRole.OBJECT));

		return pattern;
	}

	/**
	 * Returns a TripleID object for a TripleString, predicate and object in
	 * regarding their IDs in the passed dictionary.
	 */
	public static TripleID convertTripleStrToTripleID(Dictionary dic, TripleString tripleStr) {

		String s = tripleStr.getSubject() != null ? tripleStr.getSubject().toString() : null;
		String p = tripleStr.getPredicate() != null ? tripleStr.getPredicate().toString() : null;
		String o = tripleStr.getObject() != null ? tripleStr.getObject().toString() : null;

		return getTripleIDFromStrings(dic, s, p, o);
	}

	/**
	 * Converts a given TripleString into a TripleID object.
	 */
	public static TripleID convertTripleIDToTripleString(Dictionary dic, TripleString tripleStr) {
		String s = tripleStr.getSubject().toString();
		String p = tripleStr.getPredicate().toString();
		String o = tripleStr.getObject().toString();
		return getTripleIDFromStrings(dic, s, p, o);
	}
	
	public static UnifiedSet<Long> getSharedIDsFromHashSet(Dictionary dic, UnifiedSet<Long> hs) {
		
		long numberOfShared = dic.getNshared();
		
		UnifiedSet<Long> resultHs = new UnifiedSet<Long>();
		Iterator<Long> itIDSubjects = hs.iterator();

		long resourceID;
		while (itIDSubjects.hasNext()) {
			resourceID = itIDSubjects.next();
			if (resourceID <= numberOfShared) {
				resultHs.add(resourceID);
			}
		}
		
		return resultHs;
	}

	/**
	 * Removes the undesired categories defined in the HDT Util class from the corresponding hash set.
	 */
	public static UnifiedSet<Long> removeUndesiredCategories(Dictionary dic, UnifiedSet<Long> hs) {

		Long undesiredCatID = null;
		for(String undesiredCat : RDFUtilities.undesiredCategories) {
			undesiredCatID = dic.stringToId(undesiredCat, TripleComponentRole.OBJECT);
			hs.remove(undesiredCatID);
		}

		return hs;
	}

	/**
	 * Returns a SPARQL query where all resources at the subject position are from RDF class 1 and all resources
	 * at object position are from RDF class 2.
	 */
	public static String getSPARQLClassQuery(String rdfClass1, String rdfClass2, TripleComponentOrder order) {
		String query = "SELECT ?s ?p ?o\r\n" + "WHERE {\r\n"
				+ "?s <" + RDFUtilities.rdfType + "> <" + rdfClass1 + "> . \r\n"
				+ "?o <" + RDFUtilities.rdfType + "> <" + rdfClass2 + "> .\r\n"
				+ "?s ?p ?o .\r\n" + "}";


		if (order.equals(TripleComponentOrder.SPO)) {
			query += "\r\n" + "order by ?s ?p ?o";
		} else {
			// pso index
			query += "\r\n" + "order by ?p ?s ?o";
		}
		return query;
	}

	/**
	 * Returns a SPARQL query which fetches all resources where all subject are of the given RDF class.
	 */
	public static String getOutgoingFSSparqlQuery(String rdfClass, TripleComponentOrder order) {
		String query = "SELECT *\r\n" + "WHERE {\r\n"
				+ "?s <" + RDFUtilities.rdfType + "> <" + rdfClass + "> . \r\n"
				+ "?s ?p ?o .\r\n"
				+ "}";

		switch (order) {
			case PSO:
				query += "\r\n" + "order by ?p ?s ?o";
				break;
			default:
				// POS
				query += "\r\n" + "order by ?p ?o ?s";
				break;
		}

		return query;
	}

	/**
	 * Returns a SPARQL query which fetches all resources where all objects are of the given RDF class.
	 */
	public static String getIncomingFSSparqlQuery(String rdfClass, TripleComponentOrder order) {
		String query = "SELECT *\r\n" + "WHERE {\r\n"
				+ "?o <" + RDFUtilities.rdfType + "> <" + rdfClass + "> . \r\n"
				+ "?s ?p ?o .\r\n" + "}";

		switch (order) {
			case PSO:
				query += "\r\n" + "order by ?p ?s ?o";
				break;
			default:
				// POS
				query += "\r\n" + "order by ?p ?o ?s";
		}

		return query;
	}
	
}

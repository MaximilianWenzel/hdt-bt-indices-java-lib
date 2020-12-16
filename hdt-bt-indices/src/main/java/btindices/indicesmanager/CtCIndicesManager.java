package btindices.indicesmanager;

import btindexmodels.BTIndex;
import btindexmodels.categoryexplorationmodels.ConnectedPredicates;
import btindexmodels.categoryexplorationmodels.ReachableCategories;
import btindexmodels.categoryexplorationmodels.SingleJoinModel;
import btindices.HDTUtil;
import btindices.RDFUtilities;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.rdfhdt.hdt.dictionary.Dictionary;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.options.ControlInfo;
import org.rdfhdt.hdt.options.ControlInformation;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.IteratorTripleString;
import org.rdfhdt.hdt.util.io.CountInputStream;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

/**
 * This class provides methods in order to load BT Indices from a file and make
 * use of its inherent functionalities for the exploration of an HDT file.
 * 
 * @author Maximilian Wenzel
 *
 */
public abstract class CtCIndicesManager extends BTIndicesManager {


	/**
	 * Represents an array of all RDF predicate URIs that exist as a predicate
	 * connection between each category i and j.
	 */
	URI[][][] predicateURIs;

	/**
	 * Represents an array of all RDF predicate IDs that exist as a predicate
	 * connection between each category i and j.
	 */
	long[][][] predicateIDs;

	/**
	 * The 2-dimensional BTIndex array represents all RDF class combinations. The
	 * index of the corresponding class can be retrieved with the "classURIs"
	 * variable.
	 */
	public BTIndex[][] btIndices;

	/**
	 * Variable which is needed in order to load and save BitmapTriples.
	 */
	ControlInformation ci;

	/**
	 * Represents the path to the directory with all the indices.
	 */
	String directoryPath;

	public HDT hdt;
	public Dictionary dic;

	public CtCIndicesManager(String directoryPath, HDT hdt, boolean readPredicates) {

		// initialize necessary variables
		ci = new ControlInformation();
		ci.clear();
		ci.setType(ControlInfo.Type.TRIPLES);
		ci.setFormat(HDTVocabulary.TRIPLES_TYPE_BITMAP);

		this.directoryPath = directoryPath;

		this.hdt = hdt;
		this.dic = hdt.getDictionary();

		// read all index files from the specified directory into main memory
		typeURIs = readClassURIs(directoryPath + "/rdfClassURIs");
		btIndices = new BTIndex[typeURIs.length][typeURIs.length];
		readBTIndices();

		// make loading of predicates optional, because predicates retrieval itself
		// needs the BTIndexManager
		if (readPredicates) {
			readPredicateURIs(directoryPath + "/rdfPredicateURIs");
		}
	}

	/**
	 * Parses the file of the BTIndices which contains all the predicate URIs of
	 * every class URI.
	 * 
	 * @param path
	 */
	private void readPredicateURIs(String path) {

		ArrayList<ArrayList<ArrayList<URI>>> predicateURIsList = new ArrayList<ArrayList<ArrayList<URI>>>();

		for (int i = 0; i < typeURIs.length; i++) {

			predicateURIsList.add(new ArrayList<ArrayList<URI>>());

			for (int k = 0; k < typeURIs.length; k++) {

				predicateURIsList.get(i).add(new ArrayList<URI>());
			}
		}

		try (CSVParser csvParser = new CSVParser(new FileReader(path),
				CSVFormat.DEFAULT.withHeader("classFrom", "classTo", "predicate").withFirstRecordAsHeader())) {

			csvParser.forEach(record -> {
				int classFromIndex = -1;
				int classToIndex = -1;

				try {

					classToIndex = ArrayUtils.indexOf(typeURIs, new URI(record.get("classTo")));
					classFromIndex = ArrayUtils.indexOf(typeURIs, new URI(record.get("classFrom")));

				} catch (URISyntaxException e1) {
					e1.printStackTrace();
				}
				URI predicateURI = null;
				try {
					predicateURI = new URI(record.get("predicate"));
				} catch (URISyntaxException e) {
					e.printStackTrace();
				}
				predicateURIsList.get(classFromIndex).get(classToIndex).add(predicateURI);
			});

			predicateURIs = new URI[typeURIs.length][typeURIs.length][];

			for (int i = 0; i < predicateURIs.length; i++) {
				for (int k = 0; k < predicateURIs.length; k++) {

					predicateURIs[i][k] = new URI[predicateURIsList.get(i).get(k).size()];
					for (int j = 0; j < predicateURIsList.get(i).get(k).size(); j++) {

						predicateURIs[i][k][j] = predicateURIsList.get(i).get(k).get(j);
					}
				}
			}

			// convert predicate URIs to dictionary IDs

			predicateIDs = new long[typeURIs.length][typeURIs.length][];
			for (int i = 0; i < predicateIDs.length; i++) {
				for (int k = 0; k < predicateIDs.length; k++) {

					predicateIDs[i][k] = new long[predicateURIsList.get(i).get(k).size()];
					for (int j = 0; j < predicateURIs[i][k].length; j++) {
						long predID = dic.stringToId(predicateURIs[i][k][j].toString(), TripleComponentRole.PREDICATE);
						predicateIDs[i][k][j] = predID;
					}
				}
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Reads all files of the specified BTIndices directory into the data structures
	 * of this class.
	 */
	private void readBTIndices() {

		String filePath;
		File f;

		for (int i = 0; i < typeURIs.length; i++) {
			for (int k = 0; k < typeURIs.length; k++) {

				// generate file path
				// name convention: uriClass1_uriClass2
				filePath = i + "_" + k;

				f = new File(this.directoryPath, filePath);

				try (InputStream input = new CountInputStream(new BufferedInputStream(new FileInputStream(f)))) {

					btIndices[i][k] = new BTIndex();

					ci.clear();
					ci.load(input);
					btIndices[i][k].load(input, ci, null);

				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		}
	}

	/**
	 * Executes a query on the specified RDF Class-Class index. Returns the results
	 * as an iterator. Null values in strings act as wildcards.
	 */
	public abstract IteratorTripleString executeQueryItString(URI classURI1, URI classURI2, String s, String p,
			String o);

	/**
	 * Executes a query on the specified RDF Class-Class index. Returns the results
	 * as an iterator. '0' values act as wildcards.
	 */
	public abstract IteratorTripleID executeQuery(URI classURI1, URI classURI2, long s, long p, long o);

	/**
	 * Returns all reachable categories (i.e. RDF classes) for a given RDF class.
	 */
	public abstract ReachableCategories getReachableCategories(URI center);

	/**
	 * Returns all incoming categories for an initial center.
	 */
	public abstract ArrayList<SingleJoinModel> getIncomingCategories(URI center);

	/**
	 * Returns all outgoing categories for an initial center.
	 */
	public abstract ArrayList<SingleJoinModel> getOutgoingCategories(URI center);

	/**
	 * Returns all reachable categories (i.e. RDF types) for a given subset of a
	 * center. The hash set contains a subset of resources of the corresponding RDF
	 * type, i.e. depending on the incoming or outgoing classes this subset of
	 * resources acts as a set of subjects or objects.
	 * 
	 * @param hsSubject IDs act as subjects - remember: subject ID could be the same
	 *                  as object ID !.
	 * @param hsObject  IDs act as objects.
	 */
	public abstract ReachableCategories getReachableCategories(UnifiedSet<Long> hsSubject, UnifiedSet<Long> hsObject,
			URI center);

	/**
	 * Returns all reachable outgoing categories for a set of resources of a
	 * specific RDF type.
	 */
	public abstract ArrayList<SingleJoinModel> getOutgoingCategories(UnifiedSet<Long> hs, URI center);

	/**
	 * Returns all reachable incoming categories for a set of resources of a
	 * specific RDF type.
	 */
	public abstract ArrayList<SingleJoinModel> getIncomingCategories(UnifiedSet<Long> hs, URI center);

	/**
	 * Returns all predicates which exist as a connection between two RDF classes.
	 */
	public abstract ConnectedPredicates getConnectedPredicates(URI center, URI outsider);


	/**
	 * Returns all predicates which exist as a connection between a subset of a RDF
	 * class and a complete RDF class. The hash set contains a subset of resources
	 * of the corresponding RDF class, i.e. depending on the incoming or outgoing
	 * classes this subset of resources acts as a set of subjects or objects.
	 * 
	 */
	public abstract ConnectedPredicates getConnectedPredicates(UnifiedSet<Long> hsSubject, UnifiedSet<Long> hsObject,
			URI center, URI outsider);


	/**
	 * Returns the index, i.e. the position in the typeURIs array for a given URI.
	 * 
	 * @return
	 */
	public int getTypePositionForURI(URI typeURI) {

		return ArrayUtils.indexOf(typeURIs, typeURI);
	}

	/**
	 * Filters the triple IDs of an iterator by the resources contained in the
	 * passed hash set, e.g. if the triple component role is set to subject, then it
	 * returns all triples which have a subject ID that is contained in the hash
	 * set.
	 * 
	 * @param hs       The hash set which contains the subject or object IDs.
	 * @param itString Contains triple strings from which a subset of resources
	 *                 should be extracted.
	 * @param role     Defines whether the hash set contains subjects or objects.
	 */
	public abstract IteratorTripleString getSubsetFromIterator(UnifiedSet<String> hs, TripleComponentRole role,
			IteratorTripleString itString);

	/**
	 * Filters the triple IDs of an iterator by the resources contained in the
	 * passed hash set, e.g. if the triple component role is set to subject, then it
	 * returns all triples which have a subject ID that is contained in the hash
	 * set.
	 * 
	 * @param hs   The hash set which contains the subject or object IDs.
	 * @param itID Contains triple IDs from which a subset of resources should be
	 *             extracted.
	 * @param role Defines whether the hash set contains subjects or objects.
	 */
	public abstract IteratorTripleID getSubsetFromIterator(UnifiedSet<Long> hs, TripleComponentRole role,
			IteratorTripleID itID);

	/**
	 * Executes a query on type1-type2 and removes afterwards the triples from the
	 * result set whose subject resource are not hash set.
	 * 
	 * @param hs       The hash set which contains the subject which should appear
	 *                 in the resulting subset.
	 * @param typeURI1
	 * @param typeURI2
	 * @param s        Subject query parameter
	 * @param p        Predicate query parameter
	 * @param o        Object query parameter
	 * @param role     Defines whether the hash set contains subject or object IDs.
	 * @return Returns the resulting subset as an array list.
	 */
	public abstract IteratorTripleString executeSubsetQueryString(UnifiedSet<String> hs, TripleComponentRole role,
			URI typeURI1, URI typeURI2, String s, String p, String o);

	/**
	 * Executes a query on type1-type2 and removes afterwards the triples from the
	 * result set whose subject resource are not hash set.
	 * 
	 * @param hs       The hash set which contains the subject which should appear
	 *                 in the resulting subset.
	 * @param typeURI1
	 * @param typeURI2
	 * @param s        Subject query parameter
	 * @param p        Predicate query parameter
	 * @param o        Object query parameter
	 * @param role     Defines whether the hash set contains subject or object IDs.
	 * @return Returns the resulting subset as an array list.
	 */
	public abstract IteratorTripleID executeSubsetQuery(UnifiedSet<Long> hs, TripleComponentRole role, URI typeURI1,
			URI typeURI2, long s, long p, long o);

	/**
	 * Returns a subset of the given hash set which resources are of the specified
	 * RDF type.
	 */
	public abstract IteratorTripleString getSubsetOfRDFClass(UnifiedSet<String> hs, URI typeURI);

	/**
	 * Prints all RDF type URIs to the standard output stream.
	 * 
	 * @throws NotFoundException
	 */
	public abstract void printTypeURIs();

	/**
	 * Returns all triples of the HDT file which are of the given RDF type.
	 */
	public IteratorTripleString getTriplesForRDFType(URI typeURI) {
		return HDTUtil.executeQuery(hdt, null, RDFUtilities.rdfType, typeURI.toString());
	}

}

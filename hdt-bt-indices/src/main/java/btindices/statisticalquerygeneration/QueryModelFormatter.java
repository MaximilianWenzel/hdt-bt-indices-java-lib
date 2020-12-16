package btindices.statisticalquerygeneration;

import btindexmodels.categoryexplorationmodels.CatConnection;
import btindexmodels.facetedsearchmodels.SemanticAnnotation;
import btindices.RDFUtilities;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Map;

/**
 * Converts query models into SPARQL or Cypher queries.
 */
public class QueryModelFormatter {

	private static final String arrowIn = " <- [] - ";
	private static final String arrowOut = " - [] -> ";

	private static final String arrowIn0 = " <- [:";
	private static final String arrowIn1 = "] - ";

	private static final String arrowOut0 = " - [:";
	private static final String arrowOut1 = "] -> ";

	public static void main(String[] args) {

		ArrayList<CatConnection> test = new ArrayList<CatConnection>();
		test.add(CatConnection.IN);
		test.add(CatConnection.OUT);
		test.add(CatConnection.IN);
		test.add(CatConnection.OUT);

		String result = getSparqlConnectionStatements(test);
		System.out.println(result);

		ArrayList<URI> types = new ArrayList<URI>();

		try {
			types.add(new URI("http://www.blabla.org/type#1"));
			types.add(new URI("http://www.blabla.org/type#2"));
			types.add(new URI("http://www.blabla.org/type#3"));
			types.add(new URI("http://www.blabla.org/type#4"));
			types.add(new URI("http://www.blabla.org/type#5"));
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		System.out.println(getSparqlTypeStatements(types));

		//System.out.println(getSparqlQuery(types, test, true));

	}

	public static String getSparqlQuery(QueryModel q) {

		StringBuilder sb = new StringBuilder();

		String relevantResults = " ?x" + (q.types.size() - 1);

		if (q.onlyCount) {
			sb.append("select (count(distinct " + relevantResults + " ) as ?numResults) ");
		} else {

			sb.append("select distinct " + relevantResults + " ");
		}

		sb.append("where { \r\n");
		sb.append(getSparqlTypeStatements(q.types));

		if (q.predicates == null || q.predicates.size() == 0) {

			sb.append(getSparqlConnectionStatements(q.catConnections));
		} else {
			sb.append(getSparqlConnectionStatements(q.catConnections, q.predicates));
		}
		sb.append(getSparqlFilterStatements(q.filters));
		sb.append("} ");

		return sb.toString();
	}

	private static String getSparqlConnectionStatements(ArrayList<CatConnection> catConnections) {

		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < catConnections.size(); i++) {
			if (catConnections.get(i).equals(CatConnection.OUT)) {
				sb.append("?x" + i + " ?p" + i + " ?x" + (i + 1) + " . \r\n");
			} else {
				sb.append("?x" + (i + 1) + " ?p" + i + " ?x" + i + " . \r\n");
			}
		}

		return sb.toString();

	}

	private static String getSparqlConnectionStatements(ArrayList<CatConnection> catConnections,
			ArrayList<URI> predicates) {

		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < catConnections.size(); i++) {
			if (catConnections.get(i).equals(CatConnection.OUT)) {
				sb.append("?x" + i + " <" + predicates.get(i) + ">" + " ?x" + (i + 1) + " . \r\n");
			} else {
				sb.append("?x" + (i + 1) + " <" + predicates.get(i) + ">" + " ?x" + i + " . \r\n");
			}
		}

		return sb.toString();

	}

	private static String getSparqlTypeStatements(ArrayList<URI> types) {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < types.size(); i++) {
			sb.append("?x" + i + " <" + RDFUtilities.rdfType + "> " + "<" + types.get(i).toString() + "> .\r\n");
		}

		return sb.toString();
	}

	private static String getSparqlFilterStatements(ArrayList<ArrayList<SemanticAnnotation>> sas) {
		StringBuilder sb = new StringBuilder();
		String pred;
		String obj;
		for (int i = 0; i < sas.size(); i++) {
			for (int k = 0; k < sas.get(i).size(); k++) {
				pred = "<" + sas.get(i).get(k).facet + ">";
				obj = sas.get(i).get(k).facetValue;
				if (!obj.startsWith("\"")) {
					// obj is a URI
					obj = "<" + obj + ">";
				}

				sb.append("?x" + i + " " + pred + " " + obj + " .\r\n");
			}
		}
		return sb.toString();
	}


	public static String getCypherQuery(QueryModel qm, Map<String, Object> namespaces) {

		return getCypherQuery(qm.types, qm.catConnections, qm.predicates, qm.onlyCount, namespaces);
	}
	
	public static String getCypherQuery(ArrayList<URI> types, ArrayList<CatConnection> catConnections,
			boolean onlyCount, Map<String, Object> namespaces) {

		return getCypherQuery(types, catConnections, null, onlyCount, namespaces);
	}

	public static String getCypherQuery(ArrayList<URI> types, ArrayList<CatConnection> catConnections,
			ArrayList<URI> predicates, boolean onlyCount, Map<String, Object> namespaces) {

		StringBuilder sb = new StringBuilder();

		sb.append("match ");

		String convertedName = getFullCypherResource(namespaces, types.get(0));

		sb.append("(n0:" + convertedName + ")");

		for (int i = 1; i < types.size(); i++) {

			if (predicates == null || predicates.size() == 0) {

				addEmptyCypherCon(catConnections.get(i - 1), sb);
			} else {

				addPredicateCypherCon(catConnections.get(i - 1), predicates.get(i - 1), sb, namespaces);
			}

			sb.append("(n" + i + ":" + getFullCypherResource(namespaces, types.get(i)) + ")");
		}

		if (onlyCount) {

			sb.append(" return count(distinct n" + (types.size() - 1) + ")");
		} else {

			sb.append(" return distinct n" + (types.size() - 1));
		}

		return sb.toString();
	}

	private static String getFullCypherResource(Map<String, Object> namespaces, URI type) {
		String typeStr = type.toString();
		String prefix = (String) namespaces.get(typeStr.split("#")[0] + "#");
		if (prefix == null) {
			int separator = typeStr.lastIndexOf("/");
			String suffix = typeStr.substring(separator + 1);
			prefix = (String) namespaces.get(typeStr.substring(0,  separator + 1));
			return prefix + "__" + suffix;
		}
		return prefix + "__" + type.getFragment();
	}

	private static void addPredicateCypherCon(CatConnection con, URI predicate, StringBuilder sb, Map<String, Object> namespaces) {

		if (con.equals(CatConnection.OUT)) {

			sb.append(arrowOut0);
			sb.append(getFullCypherResource(namespaces, predicate));
			sb.append(arrowOut1);
		} else {
			sb.append(arrowIn0);
			sb.append(getFullCypherResource(namespaces, predicate));
			sb.append(arrowIn1);
		}
	}

	private static void addEmptyCypherCon(CatConnection con, StringBuilder sb) {
		if (con.equals(CatConnection.OUT)) {

			sb.append(arrowOut);
		} else {
			sb.append(arrowIn);
		}

	}

}

package btindexmodels.categoryexplorationmodels;

import java.net.URI;
import java.util.ArrayList;

/**
 * Represents a data structure that contains two array lists of all incoming and
 * outgoing predicate connections.
 * 
 * @author Maximilian Wenzel
 *
 */
public class ConnectedPredicates {

	public ArrayList<SingleJoinModel> outgoingPreds;
	public ArrayList<SingleJoinModel> incomingPreds;

	public ConnectedPredicates() {
		outgoingPreds = new ArrayList<SingleJoinModel>();
		incomingPreds = new ArrayList<SingleJoinModel>();
	}

	public void printReachableCategories() {
		URI predicateURI;
		URI typeURI;
		long results;

		String con;
		String arrow;
		String format = "%-30.30s %-3.3s %-50.50s %-3.3s %-50.50s%n";

		for (int i = 0; i < outgoingPreds.size(); i++) {
			results = outgoingPreds.get(i).getResults();
			predicateURI = outgoingPreds.get(i).getPredicate();
			typeURI = outgoingPreds.get(i).getOutsider();

			con = "[" + i + "] Outgoing" + "  (" + results + ") ";
			System.out.printf(format, con, "---", predicateURI, "-->", typeURI);
		}

		for (int i = 0; i < incomingPreds.size(); i++) {
			results = incomingPreds.get(i).getResults();
			predicateURI = incomingPreds.get(i).getPredicate();
			typeURI = incomingPreds.get(i).getOutsider();

			con = "[" + i + "] Incoming" + "  (" + results + ") ";
			arrow = "<-- " + predicateURI + " ---";
			System.out.printf(format, con, "<--", predicateURI, "---", typeURI);
		}
	}
}

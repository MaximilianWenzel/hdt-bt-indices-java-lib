package btindexmodels.categoryexplorationmodels;

import java.net.URI;
import java.util.ArrayList;

/**
 * Represents a datastructure which contains two array lists of all incoming and
 * outgoing category connections.
 * 
 * @author Maximilian Wenzel
 *
 */
public class ReachableCategories {

	public ArrayList<SingleJoinModel> outgoingCategories;

	public ArrayList<SingleJoinModel> incomingCategories;

	public ReachableCategories() {
		outgoingCategories = new ArrayList<SingleJoinModel>();
		incomingCategories = new ArrayList<SingleJoinModel>();
	}

	public void printReachableCategories() {
		URI classURI;
		long results;

		String con;
		String arrowOut = " ---->";
		String arrowIn = " <----";
		String format = "%-30.30s %-75.75s%n";


		for (int i = 0; i < outgoingCategories.size(); i++) {
			results = outgoingCategories.get(i).getResults();
			classURI = outgoingCategories.get(i).getOutsider();

			con = "[" + i + "] Outgoing" + arrowOut + "  (" + results + ") ";
			System.out.printf(format, con, classURI);
		}

		for (int i = 0; i < incomingCategories.size(); i++) {
			results = incomingCategories.get(i).getResults();
			classURI = incomingCategories.get(i).getOutsider();

			con = "[" + i + "] Incoming" + arrowIn + "  (" + results + ") ";
			System.out.printf(format, con, classURI);
		}
	}
}

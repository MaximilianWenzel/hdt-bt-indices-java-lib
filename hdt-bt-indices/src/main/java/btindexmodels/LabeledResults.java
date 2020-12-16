package btindexmodels;


/**
 * Represents a data structure in order to add a label to a numeric long value.
 * @author Maximilian Wenzel
 *
 */
public class LabeledResults implements Comparable<LabeledResults> {

	public String label;
	public long results;

	@Override
	public int compareTo(LabeledResults o) {
		int compare = Long.compare(results, o.results);
		if (compare == 0) {
			compare = label.compareTo(o.label);
		}
		return compare;
	}
}

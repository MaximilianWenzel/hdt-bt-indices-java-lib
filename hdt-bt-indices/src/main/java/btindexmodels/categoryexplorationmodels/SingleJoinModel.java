package btindexmodels.categoryexplorationmodels;

import java.net.URI;
import java.util.Comparator;

;

public class SingleJoinModel implements Comparator<SingleJoinModel> {

	protected URI center;
	protected URI outsider;

	protected URI predicate;

	CatConnection connection;

	protected long results;
	public SingleJoinModel() {

	}

	public SingleJoinModel(URI center, long results) {
		this.center = center;
		this.results = results;
	}

	public URI getCenter() {
		return center;
	}

	public void setCenter(URI center) {
		this.center = center;
	}
	public long getResults() {
		return results;
	}
	public void setResults(long results) {
		this.results = results;
	}

	public URI getOutsider() {
		return outsider;
	}

	public void setOutsider(URI outsider) {
		this.outsider = outsider;
	}

	public URI getPredicate() {
		return predicate;
	}

	public void setPredicate(URI predicate) {
		this.predicate = predicate;
	}

	public CatConnection getConnection() {
		return connection;
	}

	public void setConnection(CatConnection connection) {
		this.connection = connection;
	}

	@Override
	public int compare(SingleJoinModel o1, SingleJoinModel o2) {
		String outsider1 = o1.getOutsider() == null ? "" : o1.getOutsider().toString();
		String outsider2 = o2.getOutsider() == null ? "" : o2.getOutsider().toString();
		String predicate1 = o1.getPredicate() == null ? "" : o1.getPredicate().toString();
		String predicate2 = o2.getPredicate() == null ? "" : o2.getPredicate().toString();
		boolean outsider1IsGreater = outsider1.compareTo(outsider2) > 0;
		boolean predicate1IsGreater = predicate1.compareTo(predicate2) > 0;

		if (o1.getResults() == o2.getResults()
			&& outsider1.compareTo(outsider2) == 0
			&& predicate1.compareTo(predicate2) == 0) {
			return 0;
		} else if (o1.getResults() > o2.getResults()
			|| (o1.getResults() == o2.getResults()
				&& outsider1IsGreater)
			|| (o1.getResults() == o2.getResults() &&
				outsider1.compareTo(outsider2) == 0
				&& predicate1IsGreater)) {

			return 1;
		} else {
			return -1;
		}
	}

	public SingleJoinModel clone() {
		SingleJoinModel clone = new SingleJoinModel();

		try {
			clone.setCenter(URI.create(this.center.toString()));

		} catch (Exception e) {
			clone.setCenter(null);
		}
		try {
			clone.setOutsider(URI.create(this.outsider.toString()));

		} catch (Exception e) {
			clone.setOutsider(null);
		}
		try {
			clone.setPredicate(URI.create(this.outsider.toString()));

		} catch (Exception e) {
			clone.setPredicate(null);
		}

		clone.setResults(this.results);
		clone.setConnection(this.connection);

		return clone;
	}
}

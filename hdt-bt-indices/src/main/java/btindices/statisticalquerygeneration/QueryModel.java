package btindices.statisticalquerygeneration;

import btindexmodels.categoryexplorationmodels.CatConnection;
import btindexmodels.facetedsearchmodels.SemanticAnnotation;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;

public class QueryModel implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 *  Corresponds to the position in the type array list.
	 */
	private int center;
	public ArrayList<URI> types;
	public ArrayList<ArrayList<SemanticAnnotation>> filters;

	public ArrayList<URI> predicates;
	public ArrayList<CatConnection> catConnections;
	public boolean onlyCount;

	public QueryModel() {
		types = new ArrayList<>();
		predicates = null;
		catConnections = new ArrayList<>();
		filters = new ArrayList<>();
		center = -1;
	}

	public QueryModel(ArrayList<URI> types, ArrayList<URI> predicates, ArrayList<CatConnection> catConnections, boolean onlyCount) {
		this(types, predicates, catConnections, -1, onlyCount);
	}

	public QueryModel(ArrayList<URI> types, ArrayList<URI> predicates, ArrayList<CatConnection> catConnections, int center, boolean onlyCount) {
		this.types = types;
		this.predicates = predicates;
		this.catConnections = catConnections;
		this.onlyCount = onlyCount;
		this.filters = new ArrayList<>();
		this.center = center;
	}

	public boolean addFilterToType(int typePos, SemanticAnnotation s) {
		if (typePos < 0 || typePos > types.size() - 1) {
			return false;
		}
		while (filters.size() < (typePos + 1)) {
			filters.add(new ArrayList<>());
		}
		filters.get(typePos).add(s);
		return true;
	}

	public boolean addFilterToType(int typePos, ArrayList<SemanticAnnotation> s) {
		if (typePos < 0 || typePos > types.size() - 1) {
			return false;
		}
		while (filters.size() < (typePos + 1)) {
			filters.add(new ArrayList<>());
		}
		filters.get(typePos).addAll(s);
		return true;
	}
	
	@SuppressWarnings("unchecked")
	public QueryModel getCopy() {

		ArrayList<URI> typesCopy = null;
		if (types != null) {

			typesCopy  = (ArrayList<URI>) types.clone();
		}

		ArrayList<URI> predicatesCopy = null;
		if (predicates != null) {
			predicatesCopy = (ArrayList<URI>) predicates.clone();
		}

		ArrayList<ArrayList<SemanticAnnotation>> filtersCopy = new ArrayList<>();
		for (int i = 0; i < filters.size(); i++) {
			filtersCopy.add(new ArrayList<>());
			for (int k = 0; k < filters.get(i).size(); k++) {
				filtersCopy.get(i).add(filters.get(i).get(k).clone());
			}
		}

		ArrayList<CatConnection> catConnectionsCopy = (ArrayList<CatConnection>) catConnections.clone();

		QueryModel copy = new QueryModel(typesCopy, predicatesCopy, catConnectionsCopy, center, onlyCount);
		copy.filters = filtersCopy;

		return copy;
	}

	public void setCenter(int typePos) {
		center = typePos;
	}

	public int getCenter() {
		if (center == -1) {
			return types.size() - 1;
		}
		return center;
	}

}

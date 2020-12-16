package btindices.indicesmanager;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;

import btindexmodels.categoryexplorationmodels.CatConnection;
import btindexmodels.categoryexplorationmodels.ConnectedPredicates;
import btindexmodels.categoryexplorationmodels.ReachableCategories;
import btindexmodels.categoryexplorationmodels.SingleJoinModel;
import btindexmodels.iterators.BitmapTriplesIteratorExact;
import btindexmodels.iterators.IteratorTripleIDExact;
import btindexmodels.iterators.IteratorTripleStringImpl;
import btindexmodels.BTIndex;
import btindices.HDTUtil;
import btindices.RDFUtilities;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.iterator.DictionaryTranslateIterator;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.IteratorTripleString;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.triples.TripleString;

public class CtCIndicesManagerImpl extends CtCIndicesManager {

	public CtCIndicesManagerImpl(String directoryPath, HDT hdt, boolean readPredicates) {
		super(directoryPath, hdt, readPredicates);
	}

	@Override
	public IteratorTripleString executeQueryItString(URI classURI1, URI classURI2, String s, String p, String o) {
		TripleID pattern = HDTUtil.getTripleIDFromStrings(this.dic, s, p, o);

		int posURI1 = getTypePositionForURI(classURI1);
		int posURI2 = getTypePositionForURI(classURI2);

		BTIndex btIndex = btIndices[posURI1][posURI2];

		return new DictionaryTranslateIterator(btIndex.search(pattern), this.dic, s, p, o);
	}

	@Override
	public IteratorTripleID executeQuery(URI classURI1, URI classURI2, long s, long p, long o) {
		TripleID pattern = new TripleID(s, p, o);

		int posURI1 = getTypePositionForURI(classURI1);
		int posURI2 = getTypePositionForURI(classURI2);

		BTIndex btIndex = btIndices[posURI1][posURI2];

		return new BitmapTriplesIteratorExact(btIndex.search(pattern));
	}

	@Override
	public ReachableCategories getReachableCategories(URI center) {

		ReachableCategories rc = new ReachableCategories();

		rc.incomingCategories = getIncomingCategories(center);
		rc.outgoingCategories = getOutgoingCategories(center);

		return rc;
	}

	@Override
	public ArrayList<SingleJoinModel> getIncomingCategories(URI center) {
		ArrayList<SingleJoinModel> incomingCategories = new ArrayList<SingleJoinModel>();

		for (int i = 0; i < typeURIs.length; i++) {
			// incoming "<--"
			IteratorTripleID itIDIn = executeQuery(typeURIs[i], center, 0, 0, 0);

			if (itIDIn.estimatedNumResults() > 0) {
				SingleJoinModel sj = new SingleJoinModel();
				sj.setResults(itIDIn.estimatedNumResults());
				sj.setCenter(center);
				sj.setOutsider(typeURIs[i]);
				sj.setConnection(CatConnection.IN);

				incomingCategories.add(sj);
			}
		}

		return incomingCategories;
	}

	@Override
	public ArrayList<SingleJoinModel> getOutgoingCategories(URI center) {
		ArrayList<SingleJoinModel> outgoingCategories = new ArrayList<SingleJoinModel>();

		for (int i = 0; i < typeURIs.length; i++) {
			// outgoing "-->"
			IteratorTripleID itIDOut = executeQuery(center, typeURIs[i], 0, 0, 0);

			if (itIDOut.estimatedNumResults() > 0) {
				SingleJoinModel sj = new SingleJoinModel();
				sj.setResults(itIDOut.estimatedNumResults());
				sj.setCenter(center);
				sj.setOutsider(typeURIs[i]);
				sj.setConnection(CatConnection.OUT);

				outgoingCategories.add(sj);
			}
		}

		return outgoingCategories;
	}

	@Override
	public ReachableCategories getReachableCategories(UnifiedSet<Long> hsSubject, UnifiedSet<Long> hsObject,
			URI center) {
		ReachableCategories rc = new ReachableCategories();

		// incoming "<--"
		rc.incomingCategories = getIncomingCategories(hsObject, center);

		// outgoing "-->"
		rc.outgoingCategories = getOutgoingCategories(hsSubject, center);

		return rc;
	}

	@Override
	public ArrayList<SingleJoinModel> getOutgoingCategories(UnifiedSet<Long> hs, URI center) {
		ArrayList<SingleJoinModel> outgoingCategories = new ArrayList<SingleJoinModel>();

		for (int i = 0; i < typeURIs.length; i++) {
			// outgoing "-->"
			IteratorTripleID itIDOut = executeSubsetQuery(hs, TripleComponentRole.SUBJECT, center, typeURIs[i], 0,
					0, 0);

			if (itIDOut.estimatedNumResults() > 0) {
				SingleJoinModel sj = new SingleJoinModel();
				sj.setCenter(center);
				sj.setResults(itIDOut.estimatedNumResults());
				sj.setOutsider(typeURIs[i]);
				sj.setConnection(CatConnection.OUT);

				outgoingCategories.add(sj);
			}
		}

		return outgoingCategories;

	}

	@Override
	public ArrayList<SingleJoinModel> getIncomingCategories(UnifiedSet<Long> hs, URI center) {
		ArrayList<SingleJoinModel> incomingCategories = new ArrayList<SingleJoinModel>();

		for (int i = 0; i < typeURIs.length; i++) {
			// incoming "<--"
			IteratorTripleID itIDIn = executeSubsetQuery(hs, TripleComponentRole.OBJECT, typeURIs[i], center, 0,
					0, 0);

			if (itIDIn.estimatedNumResults() > 0) {
				SingleJoinModel sj = new SingleJoinModel();
				sj.setCenter(center);
				sj.setResults(itIDIn.estimatedNumResults());
				sj.setOutsider(typeURIs[i]);
				sj.setConnection(CatConnection.IN);

				incomingCategories.add(sj);
			}
		}

		return incomingCategories;

	}

	@Override
	public ConnectedPredicates getConnectedPredicates(URI center, URI outsider) {

		ConnectedPredicates connectedPreds = new ConnectedPredicates();
		connectedPreds.outgoingPreds = getPredicateCons(center, outsider, CatConnection.OUT);
		connectedPreds.incomingPreds = getPredicateCons(center, outsider, CatConnection.IN);

		return connectedPreds;

	}

	public ArrayList<SingleJoinModel> getPredicateCons(URI center, URI outsider, CatConnection catCon) {

		return getPredicateCons(center, outsider, catCon, null, null);

	}

	public ArrayList<SingleJoinModel> getPredicateCons(URI center, URI outsider, CatConnection catCon, UnifiedSet<Long> hs, TripleComponentRole role) {
		ArrayList<SingleJoinModel> predicateCons = new ArrayList<SingleJoinModel>();

		int positionCenter = getTypePositionForURI(center);
		int positionOutsider = getTypePositionForURI(outsider);

		// count the occurrences of each predicate connection in a corresponding BT index
		IteratorTripleID itID;
		long [] predIDs;

		if (catCon.equals(CatConnection.OUT)) {
			itID = executeQuery(center, outsider, 0, 0, 0);
		} else {
			itID = executeQuery(outsider, center, 0, 0, 0);
		}

		HashMap<Long, Long> countResultsPreds = new HashMap<Long, Long>();


		if (hs == null) {
			while (itID.hasNext()) {
				TripleID tID = itID.next();
				long pred = tID.getPredicate();
				incrementPredicateCountHashMap(pred, countResultsPreds);
			}

		} else {
			// check if resource is in center hash set
			if (role.equals(TripleComponentRole.SUBJECT)) {

				while (itID.hasNext()) {
					TripleID tID = itID.next();
					if (!hs.contains(tID.getSubject())) {
						continue;
					}
					long pred = tID.getPredicate();
					incrementPredicateCountHashMap(pred, countResultsPreds);
				}

			} else {
				// object role
				while (itID.hasNext()) {
					TripleID tID = itID.next();
					if (!hs.contains(tID.getObject())) {
						continue;
					}
					long pred = tID.getPredicate();
					incrementPredicateCountHashMap(pred, countResultsPreds);
				}
			}
		}

		countResultsPreds.forEach((pred, count) -> {
			SingleJoinModel sj = new SingleJoinModel();
			sj.setResults(count);
			String predStr = dic.idToString(pred, TripleComponentRole.PREDICATE).toString();
			sj.setPredicate(URI.create(predStr));
			sj.setCenter(URI.create(center.toString()));
			sj.setOutsider(URI.create(outsider.toString()));
			sj.setConnection(catCon);
			predicateCons.add(sj);
		});

		return predicateCons;
	}

	private void incrementPredicateCountHashMap(long pred, HashMap<Long,Long> hm) {
		Long oldVal;
		if ((oldVal = hm.get(pred)) != null) {
			// increment count in hash map
			long newVal = oldVal + 1;
			hm.put(pred, newVal);
		} else {
			hm.put(pred, 1L);
		}
	}

	@Override
	public ConnectedPredicates getConnectedPredicates(UnifiedSet<Long> hsSubject, UnifiedSet<Long> hsObject, URI center,
			URI outsider) {

		ConnectedPredicates connectedPreds = new ConnectedPredicates();

		connectedPreds.outgoingPreds = getPredicateCons(center, outsider, CatConnection.OUT, hsSubject, TripleComponentRole.SUBJECT);
		connectedPreds.incomingPreds = getPredicateCons(center, outsider, CatConnection.IN, hsObject, TripleComponentRole.OBJECT);

		return connectedPreds;
	}

	@Override
	public int getTypePositionForURI(URI classURI) {

		return ArrayUtils.indexOf(typeURIs, classURI);
	}

	@Override
	public IteratorTripleString getSubsetFromIterator(UnifiedSet<String> hs, TripleComponentRole role,
			IteratorTripleString itString) {
		ArrayList<TripleString> result = new ArrayList<TripleString>();
		String sbj;
		String pred;
		String obj;
		TripleString tripleStr;

		if (role == TripleComponentRole.SUBJECT) {
			while (itString.hasNext()) {
				tripleStr = itString.next();

				sbj = tripleStr.getSubject().toString();
				pred = tripleStr.getPredicate().toString();
				obj = tripleStr.getObject().toString();

				if (hs.contains(sbj)) {
					result.add(new TripleString(sbj, pred, obj));
				}
			}
		} else if (role == TripleComponentRole.OBJECT) {
			while (itString.hasNext()) {
				tripleStr = itString.next();

				sbj = tripleStr.getSubject().toString();
				pred = tripleStr.getPredicate().toString();
				obj = tripleStr.getObject().toString();

				if (hs.contains(obj)) {
					result.add(new TripleString(sbj, pred, obj));
				}
			}
		} else {

			return null;
		}

		return new IteratorTripleStringImpl(result);
	}

	@Override
	public IteratorTripleID getSubsetFromIterator(UnifiedSet<Long> hs, TripleComponentRole role,
			IteratorTripleID itID) {
		ArrayList<TripleID> result = new ArrayList<TripleID>();
		long sbj;
		long pred;
		long obj;
		TripleID tID;

		if (role == TripleComponentRole.SUBJECT) {
			while (itID.hasNext()) {
				tID = itID.next();

				sbj = tID.getSubject();
				pred = tID.getPredicate();
				obj = tID.getObject();

				if (hs.contains(sbj)) {
					result.add(new TripleID(sbj, pred, obj));
				}
			}
			itID.goToStart();
		} else if (role == TripleComponentRole.OBJECT) {
			while (itID.hasNext()) {
				tID = itID.next();

				sbj = tID.getSubject();
				pred = tID.getPredicate();
				obj = tID.getObject();

				if (hs.contains(obj)) {
					result.add(new TripleID(sbj, pred, obj));
				}
			}
			itID.goToStart();
		} else {

			return null;
		}

		return new IteratorTripleIDExact(result);
	}

	public IteratorTripleID getSubsetFromIterator(UnifiedSet<Long> hs, TripleComponentRole role,
												  IteratorTripleID itID, long predToCheck) {
		ArrayList<TripleID> result = new ArrayList<TripleID>();
		long sbj;
		long pred;
		long obj;
		TripleID tID;

		if (role == TripleComponentRole.SUBJECT) {
			while (itID.hasNext()) {
				tID = itID.next();

				sbj = tID.getSubject();
				pred = tID.getPredicate();
				obj = tID.getObject();

				if (hs.contains(sbj) && pred == predToCheck) {
					result.add(new TripleID(sbj, pred, obj));
				}
			}
			itID.goToStart();
		} else if (role == TripleComponentRole.OBJECT) {
			while (itID.hasNext()) {
				tID = itID.next();

				sbj = tID.getSubject();
				pred = tID.getPredicate();
				obj = tID.getObject();

				if (hs.contains(obj) && pred == predToCheck) {
					result.add(new TripleID(sbj, pred, obj));
				}
			}
			itID.goToStart();
		} else {

			return null;
		}

		return new IteratorTripleIDExact(result);
	}

	@Override
	public IteratorTripleString executeSubsetQueryString(UnifiedSet<String> hs, TripleComponentRole role, URI typeURI1,
			URI typeURI2, String s, String p, String o) {
		IteratorTripleString itString = executeQueryItString(typeURI1, typeURI2, s, p, o);
		return getSubsetFromIterator(hs, role, itString);
	}

	@Override
	public IteratorTripleID executeSubsetQuery(UnifiedSet<Long> hs, TripleComponentRole role, URI typeURI1,
			URI typeURI2, long s, long p, long o) {

		IteratorTripleID itID = executeQuery(typeURI1, typeURI2,s, p, o);
		return getSubsetFromIterator(hs, role, itID);
	}

	@Override
	public IteratorTripleString getSubsetOfRDFClass(UnifiedSet<String> hs, URI typeURI) {

		IteratorTripleString itString = null;
		try {
			itString = hdt.search(null, RDFUtilities.rdfType, typeURI.toString());
		} catch (NotFoundException e) {
			e.printStackTrace();
		}

		System.out.println("Number of results for classURI: " + itString.estimatedNumResults());
		// save resulting subjects of query to hash set
		return getSubsetFromIterator(hs, TripleComponentRole.SUBJECT, itString);
	}

	@Override
	public void printTypeURIs() {
		for (int i = 0; i < typeURIs.length; i++) {
			IteratorTripleString itString;

			try {
				itString = hdt.search(null, RDFUtilities.rdfType, typeURIs[i].toString());
			} catch (NotFoundException e) {
				itString = new IteratorTripleStringImpl(new ArrayList<TripleString>());
			}

			System.out.println("[" + i + "] " + typeURIs[i] + ": " + itString.estimatedNumResults() + " results");
		}
	}

}

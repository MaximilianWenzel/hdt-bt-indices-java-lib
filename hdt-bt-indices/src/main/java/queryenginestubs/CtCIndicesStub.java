package queryenginestubs;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;

import btindexmodels.categoryexplorationmodels.CenterHashSets;
import btindexmodels.categoryexplorationmodels.ConnectedPredicates;
import btindexmodels.categoryexplorationmodels.ReachableCategories;
import btindices.statisticalquerygeneration.QueryModel;
import queryenginestubs.interfaces.JoinQueryCalc;
import queryenginestubs.interfaces.ReachableCategoriesCalc;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.rdfhdt.hdt.enums.TripleComponentRole;

import btindices.indicesmanager.CatExplorationManager;
import org.rdfhdt.hdt.triples.IteratorTripleID;

public class CtCIndicesStub extends QueryEngineStub implements JoinQueryCalc, ReachableCategoriesCalc {

	CatExplorationManager em;

	public CtCIndicesStub(String hdtPath, String btIndicesDir) {
		this.em = new CatExplorationManager(hdtPath, btIndicesDir);
	}


	@Override
	public ArrayList<String> executeQuery(QueryModel qm) {

		ArrayList<String> result = new ArrayList<String>();
		
		em.updateExplorationState(qm);
		
		if (qm.onlyCount) {
			long count;
			if (em.hsSubject != null) {

				count = em.hsSubject.size();
			} else {
				count = em.currentConnectionItID.estimatedNumResults();
			}
			result.add("" + count);
		} else {
			
			Iterator<Long> idsIt;

			if (qm.types.size() > 1) {
				// at least one join
				idsIt = em.hsSubject.iterator();
				while (idsIt.hasNext()) {
					result.add(em.dic.idToString(idsIt.next(), TripleComponentRole.SUBJECT).toString());
				}
			} else {
				// initial query
				IteratorTripleID itID = em.currentConnectionItID;
				while (itID.hasNext()) {
					result.add(em.dic.idToString(itID.next().getSubject(), TripleComponentRole.SUBJECT).toString());
				}
			}
		}

		return result;
	}

	@Override
	public ReachableCategories calcReachableCategories(UnifiedSet<Long> hsSubject, UnifiedSet<Long> hsObject, URI centerType) {
		em.changeExplorationState(hsSubject, hsObject, centerType);
		em.updateReachableCategories();
		return em.rc;
	}

	@Override
	public CenterHashSets getHashSetFromQueryModel(QueryModel qm) {
		em.updateExplorationState(qm);
		CenterHashSets chs = new CenterHashSets(em.hsSubject, em.hsObject);
		return chs;
	}

	@Override
	public ConnectedPredicates[] calcReachableCategoriesPreds(UnifiedSet<Long> hsSubject, UnifiedSet<Long> hsObject, URI centerType) {
		em.changeExplorationState(hsSubject, hsObject, centerType);
		em.updatePredicateConnections();
		return em.connectedPreds;
	}
}

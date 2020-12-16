package queryenginestubs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import btindices.statisticalquerygeneration.QueryModel;
import queryenginestubs.interfaces.ApplyFiltersCalc;
import queryenginestubs.interfaces.HybridQueryCalc;
import queryenginestubs.interfaces.JoinQueryCalc;
import btindices.statisticalquerygeneration.QueryModelFormatter;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdtjena.HDTGraph;

public class HDTJenaStub extends QueryEngineStub implements JoinQueryCalc, ApplyFiltersCalc, HybridQueryCalc {

	HDT hdt;


	public HDTGraph graph;
	public Model model;


	public HDTJenaStub(HDT hdt) {
		this.hdt = hdt;
		init();
	}

	public HDTJenaStub(String hdtPath) {
		// Create HDT
		try {
			this.hdt = HDTManager.mapIndexedHDT(hdtPath, null);

		} catch (IOException e) {
			e.printStackTrace();
		}
		init();
	}

	private void init() {
		// Create Jena wrapper on top of HDT.
		this.graph = new HDTGraph(hdt);
		this.model = ModelFactory.createModelForGraph(graph);
	}

	
	@Override
	public ArrayList<String> executeQuery(QueryModel q) {
		
		String sparqlQuery = QueryModelFormatter.getSparqlQuery(q);
		
		ArrayList<String> result = new ArrayList<String>();
		
		try {
			ResultSet rs = executeSparqlSelectQuery(sparqlQuery);
			
			List<String> varNames = rs.getResultVars();
			
			// it is always only one result
			String varName = varNames.get(0);
			
			if (q.onlyCount) {
				result.add("" + rs.next().getLiteral(varName).getInt());
				return result;
			}
			
			while (rs.hasNext()) {
				result.add(rs.next().get(varName).asResource().toString());
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
		return result;
	}

	public ResultSet executeSparqlSelectQuery(String sparqlQuery) throws IOException {
		// Use Jena ARQ to execute the query.
		Query query = QueryFactory.create(sparqlQuery);
		QueryExecution qe = QueryExecutionFactory.create(query, model);
		qe.setTimeout(15000);

		// Perform the query and output the results, depending on query type
		return qe.execSelect();


	}

	@Override
	public ArrayList<String> applyFacets(QueryModel qm) {
		return executeQuery(qm);
	}

	@Override
	public ArrayList<String> executeHybridQuery(QueryModel qm) {
		return executeQuery(qm);
	}
}

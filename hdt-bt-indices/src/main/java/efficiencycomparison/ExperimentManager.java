package efficiencycomparison;

import btindices.statisticalquerygeneration.QueryModel;
import queryenginestubs.QueryEngineStub;
import queryenginestubs.queryexecutors.*;
import btindices.statisticalquerygeneration.QueryModelManager;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.time.StopWatch;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.*;

/**
 * Executes a given set of query models and saves the time measurement to a CSV file.
 */
public class ExperimentManager {

    public ArrayList<QueryModel> queries;

	/**
     * The time that has been needed in order to execute the query.
     */
	protected long[][] timeMeasurement;
	protected ArrayList<String> result;

	protected final Duration timeout = Duration.ofSeconds(10);

	/**
     * Indices of the queries which are not considered anymore if a timeout occured in an experiment iteration.
     */
    ArrayList<Integer> timeoutQueries;
    public boolean discardTimeoutQueries;

	protected QueryEngineStub qeStub;
	protected StopWatch sw;
	protected QueryModel currentQuery;
	protected String resultsPath;
	protected ArrayList<Integer> indexArray;
	protected Future handler;
	protected ExecutorService executor;
	protected ArrayList<QueryModel> warmUpQueries;

	/**
	 * Specifies how often an experiment is executed.
	 */
	public int numExecutions;

	protected boolean experimentFinished;
	protected int currentQueryIndex;
	protected QueryExecutorChoice queryExecutor;



	public ExperimentManager(String queryPath, String resultsPath, QueryEngineStub qeStub, int numExecutions, QueryExecutorChoice queryExecutor, ArrayList<QueryModel> warmUpQueries) {

		this.queryExecutor = queryExecutor;
		this.warmUpQueries = warmUpQueries;
		discardTimeoutQueries = false;

		init(queryPath, resultsPath, qeStub, numExecutions, queryExecutor);
	}

    public ExperimentManager(String queryPath, String resultsPath, QueryEngineStub qeStub, int numExecutions, QueryExecutorChoice queryExecutor) {

		this.warmUpQueries = null;
		init(queryPath, resultsPath, qeStub, numExecutions, queryExecutor);
    }

    private void init(String queryPath, String resultsPath, QueryEngineStub qeStub, int numExecutions, QueryExecutorChoice queryExecutor) {
		this.queryExecutor = queryExecutor;
		queries = QueryModelManager.loadQueryModelsFromFile(queryPath);
		this.qeStub = qeStub;
		this.resultsPath = resultsPath;

		this.sw = new StopWatch();

		// index array in order to provide shuffling after the first execution round
		indexArray = new ArrayList<Integer>();
		for (int i = 0; i < queries.size(); i++) {
			indexArray.add(i);
		}

		this.numExecutions = numExecutions;
		timeMeasurement = new long[this.numExecutions][queries.size()];

		timeoutQueries = new ArrayList<Integer>();
	}

    public void runExperiment() {

		if (queries.size() == 0) {
			// no queries could be loaded
			return;
		}

    	currentQueryIndex = 0;
		executor = Executors.newSingleThreadExecutor();

		try {
			warmUp();

		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Warm up could not be executed!");
		}

		// in order to start with the specific round if a timeout occurs
		int currentRound = 0;

		while (!experimentFinished) {
			try {

				for (int i = currentRound; i < numExecutions; i++, currentRound++) {
					System.out.println("Round: " + (i+1));
					startExperiment(queries, timeMeasurement[i]);

					if (i != numExecutions - 1) {
						// if it was not the last execution of the experiment
						Collections.shuffle(indexArray);
					}

					// set index to 0 only if all queries have been succesfully executed
					currentQueryIndex = 0;
				}

				// save measurements
				writeTimeMeasurementToCSVFile(this.resultsPath);
				System.out.println("Experiment successfully executed.");
				experimentFinished = true;
				executor.shutdownNow();

			} catch (Exception e) {
				e.printStackTrace();
				executor.shutdownNow();
				// timeout occured -> do not execute the current query in the next experiment rounds (if discardTimeouts is set true)
				timeoutQueries.add(indexArray.get(currentQueryIndex));
				currentQueryIndex++;

				// restart thread
				executor = Executors.newSingleThreadExecutor();
			}
		}
    }

	protected void startExperiment(ArrayList<QueryModel> queries, long[] measurementForRound) throws InterruptedException, ExecutionException, TimeoutException {

		for (; currentQueryIndex < queries.size(); currentQueryIndex++) {

			if (discardTimeoutQueries && timeoutQueries.contains(indexArray.get(currentQueryIndex))) {
				// if the current query resulted in a timeout before, do not execute it anymore
				continue;
			}

			currentQuery = queries.get(indexArray.get(currentQueryIndex));
			handler = executor.submit(getQueryExecutor(currentQuery));

			sw.reset();
			sw.start();
			handler.get(timeout.toMillis(), TimeUnit.MILLISECONDS); // execute query
			sw.stop();

			measurementForRound[indexArray.get(currentQueryIndex)] = sw.getNanoTime();
			System.out.println("(" + (currentQueryIndex + 1) + "/" + queries.size() + ")");
		}
	}

	protected void warmUp() throws InterruptedException, ExecutionException, TimeoutException {
		if (warmUpQueries != null) {
			specificWarmUp();
		} else {
			warmUp10FirstQueries();
		}
	}

	private void warmUp10FirstQueries() throws InterruptedException, ExecutionException, TimeoutException {
		System.out.println("Start Warm-Up...");
		//int numQueries = Math.min(10, queries.size()); // run 10 first queries
		int numQueries = queries.size();

		for (currentQueryIndex = 0; currentQueryIndex < numQueries; currentQueryIndex++) {

			currentQuery = queries.get(currentQueryIndex);
			handler = executor.submit(getQueryExecutor(currentQuery));
			handler.get(timeout.toMillis(), TimeUnit.MILLISECONDS); // execute query

			System.out.println("(" + (currentQueryIndex + 1) + "/" + numQueries + ")");
		}
		currentQueryIndex = 0;
		System.out.println("Finished Warm-Up.");
	}

	private void specificWarmUp() throws InterruptedException, ExecutionException, TimeoutException {

		System.out.println("Start Warm-Up...");
		for (int i = 0; i < warmUpQueries.size(); i++) {

			currentQuery = warmUpQueries.get(i);
			handler = executor.submit(getQueryExecutor(currentQuery));
			handler.get(timeout.toMillis(), TimeUnit.MILLISECONDS); // execute query

			System.out.println("(" + (i + 1) + "/" + warmUpQueries.size() + ")");
		}
		System.out.println("Finished Warm-Up.");
	}

	private QueryExecutor getQueryExecutor(QueryModel qm) {

		switch (queryExecutor) {
			case RC:
				return new ReachableCatQueryExecutor(qeStub, qm);
			case CP:
				return new ConnectedPredicatesQueryExecutor(qeStub, qm);
			case Verbose:
				return new VerboseQueryExecutor(qeStub, qm);
			case ApplyFilters:
				return new ApplyFacetsQueryExecutor(qeStub, qm);
			case AvailableFilters:
				return new AvailableFacetsQueryExecutor(qeStub, qm);
			case Hybrid:
				return new HybridQueryExecutor(qeStub, qm);
			default:
				return new CountQueryExecutor(qeStub, qm);
		}
	}


    public void writeTimeMeasurementToCSVFile(String filePath) {

        if (!filePath.endsWith(".csv")) {
            filePath += ".csv";
        }

        // specify csv header
        String[] header = new String[numExecutions + 1];
        header[0] = "query";
		for (int i = 1; i < numExecutions + 1; i++) {
			header[i] = "round" + i;
		}

        // save all predicate URIs to the index directory
        try (CSVPrinter csvPrinter = new CSVPrinter(new FileWriter(filePath),
                CSVFormat.DEFAULT.withHeader(header))) {

            for (int i = 0; i < queries.size(); i++) {

            	ArrayList<Long> record = new ArrayList<Long>();
            	record.add((long) i);

            	for (int k = 0; k < numExecutions; k++) {
            		record.add(timeMeasurement[k][i]);
				}
                csvPrinter.printRecord(record);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

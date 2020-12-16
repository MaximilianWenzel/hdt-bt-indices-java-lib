package btindices;

import btindices.indicesmanager.CatExplorationManager;
import org.apache.commons.cli.*;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.util.StopWatch;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;

/**
 * This application represents a demo for all features of the Exploration
 * Manager class.
 * 
 * @author Maximilian Wenzel
 *
 */
public class CatExplorationApp {

	String inputString;

	BufferedReader br;
	CommandLine cl;
	CatExplorationManager em;

	public static final StopWatch sw = new StopWatch();

	public static void main(String[] args) {
		CatExplorationApp test = new CatExplorationApp();
		test.run(args);
	}

	public CatExplorationApp() {
		br = new BufferedReader(new InputStreamReader(System.in));
	}

	/**
	 * Runs the application
	 *
	 * @param args an array of String arguments to be parsed
	 */
	public void run(String[] args) {

		// args[0] -> path to hdt file
		// args[1] -> BT indices directory
		try {
			File hdtFile = new File(args[0]);
			File btIndicesDir = new File(args[1]);

			if (!hdtFile.exists() || !btIndicesDir.exists()) {
				throw new Exception();
			}
		} catch (Exception e) {
			System.err.println("HDT file or BT Indices directory does not exist.");
			System.err.println("ExplorationApp.java <HDT-File> <BT-INDICES-DIRECTORY>");
			return;
		}

		em = new CatExplorationManager(args[0], args[1]);

		chooseInitialClass();

		try {

			while (true) {

				cl = getUserInput();

				if (cl.hasOption("in")) {

					sw.reset();
					em.chooseIncomingReachableCategory(Integer.parseInt(cl.getOptionValue("in")), false);
					printHsAndIteratorSize();
					System.out.println("Time needed calculated incoming connection: " + sw.stopAndShow());

				} else if (cl.hasOption("out")) {

					sw.reset();
					em.chooseOutgoingReachableCategory(Integer.parseInt(cl.getOptionValue("out")), false);
					printHsAndIteratorSize();
					System.out.println("Time needed calculating outgoing connection: " + sw.stopAndShow());

				} else if (cl.hasOption("rcFilter")) {
					String[] values = cl.getOptionValues("rcFilter");
					String con = values[0].toLowerCase();
					int catIndex = Integer.parseInt(values[1]);

					sw.reset();
					if (con.equals("in")) {
						em.chooseIncomingReachableCategory(catIndex, true);
					} else if (con.equals("out")) {
						em.chooseOutgoingReachableCategory(catIndex, true);
					} else {
						System.out.println("Unknown command.");
						printAppHelp();
					}
					System.out.println("Time needed calculating filter operation: " + sw.stopAndShow());

				} else if (cl.hasOption("predFilter")) {
					String[] values = cl.getOptionValues("predFilter");
					String con = values[0].toLowerCase();
					int catIndex = Integer.parseInt(values[1]);
					int predIndex = Integer.parseInt(values[2]);

					if (con.equals("in")) {
						em.chooseIncomingPredicateConnection(catIndex, predIndex, true);
					} else if (con.equals("out")) {
						em.chooseOutgoingPredicateConnection(catIndex, predIndex, true);
					} else {
						System.out.println("Unknown command.");
						printAppHelp();
					}

				} else if (cl.hasOption("e")) {

					break;

				} else if (cl.hasOption("print")) {

					String v = cl.getOptionValue("print");

					if (v.equals("dicPreds")) {

						HDTUtil.printIteratorCharsequence(em.dic.getPredicates().getSortedEntries());
					} else if (v.equals("dossier")) {

						System.out.println("Dossier: ");
						sw.reset();
						em.printDossier();
						System.out.println("Time needed to print dossier: " + sw.stopAndShow());

					} else if (v.equals("allTriples")) {

						printCurrentResourceSet();
					} else if (v.equals("center")) {

						printHashSetResources();
					}

				} else if (cl.hasOption("pred")) {

					printPredicateRelations();

				} else if (cl.hasOption("search")) {

					peformHDTSearch();

				} else if (cl.hasOption("predIn")) {

					String[] values = cl.getOptionValues("predIn");
					int catIndex = Integer.parseInt(values[0]);
					int predIndex = Integer.parseInt(values[1]);
					em.chooseIncomingPredicateConnection(catIndex, predIndex, false);

				} else if (cl.hasOption("predOut")) {

					String[] values = cl.getOptionValues("predOut");
					int catIndex = Integer.parseInt(values[0]);
					int predIndex = Integer.parseInt(values[1]);
					em.chooseOutgoingPredicateConnection(catIndex, predIndex, false);

				} else if (cl.hasOption("fs")) {

					facetedSearch();

				} else if (cl.hasOption("rc")) {
					sw.reset();
					em.updateReachableCategories();
					String time = sw.stopAndShow();
					em.rc.printReachableCategories();
					System.out.println("Time needed calculating reachable categories: " + time);

				} else if (cl.hasOption("h")) {
					printAppHelp();
					continue;
				} else if (cl.hasOption("reset")) {

					chooseInitialClass();
				}

				System.out.println("Current RDF class: " + em.currentURI);
				System.out.println("Number of connections in relation to predecessor: " + em.currentConnectionItID.estimatedNumResults());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void printHashSetResources() {
		UnifiedSet<Long> hs;
		Iterator<Long> hsIt;
		TripleComponentRole role;

		if (em.hsObject.size() > em.hsSubject.size()) {
			hs = em.hsSubject;
			role = TripleComponentRole.SUBJECT;
		} else {
			hs = em.hsObject;
			role = TripleComponentRole.OBJECT;
		}
		hsIt = hs.iterator();
		String r;
		while (hsIt.hasNext()) {
			r  = em.dic.idToString(hsIt.next(), role).toString();
			System.out.println(r);
		}
	}

	public void chooseInitialClass() {

		try {
			em.btIndexManager.printTypeURIs();

			System.out.println();
			System.out.println("Choose initial class URI (e.g. '-init 3'): ");

			cl = getUserInput();
			int chosenIndex = Integer.parseInt(cl.getOptionValue("init"));
			em.chooseInitialClass(chosenIndex);
			
			em.updateReachableCategories();
			em.rc.printReachableCategories();

		} catch (IOException e) {

			System.out.println("Unknown command.");
			//e.printStackTrace();
		}
	}

	private void printHsAndIteratorSize() {
		System.out.println("Hash set subjects size: " + em.hsSubject.size());
		System.out.println("Hash set objects size: " + em.hsObject.size());
		System.out.println("Connection iterator size: " + em.currentConnectionItID.estimatedNumResults());
	}

	private void printCurrentResourceSet() {
		HDTUtil.printIteratorResultsAsURIFragments(em.dic, em.currentConnectionItID);
	}

	private void printPredicateRelations() {
		System.out.println("Predicates for current center: " + em.currentURI);

		sw.reset();
		em.updatePredicateConnections();
		String time = sw.stopAndShow();

		em.printConnectedPredicates();
		System.out.println("Time needed to calculate predicate connections: " + time);

	}

	/**
	 * Parses application arguments
	 *
	 * @param args application arguments
	 * @return <code>CommandLine</code> which represents a list of application
	 *         arguments.
	 */
	private CommandLine parseArguments(String[] args) {

		Options options = getOptions();
		CommandLine line = null;

		CommandLineParser parser = new DefaultParser();

		try {
			line = parser.parse(options, args);

		} catch (Exception ex) {

			System.out.println("Unkown command.");
			//System.err.println("Failed to parse command line arguments");
			//System.err.println(ex.toString());
			printAppHelp();

			//System.exit(1);
		}

		return line;
	}

	private CommandLine parseArguments(String args) {
		return parseArguments(args.split(" "));
	}

	private CommandLine getUserInput() throws IOException {
		return parseArguments(br.readLine());
	}

	private void peformHDTSearch() {
		String[] searchArgs = cl.getOptionValues("s");
		String s = null;
		String p = null;
		String o = null;

		if (!searchArgs[0].equals("?")) {
			s = HDTUtil.addQuotesIfNecessary(searchArgs[0]);
		}

		if (!searchArgs[1].equals("?")) {
			p = HDTUtil.addQuotesIfNecessary(searchArgs[1]);
		}

		if (!searchArgs[2].equals("?")) {
			o = HDTUtil.addQuotesIfNecessary(searchArgs[2]);

		}
		HDTUtil.printIteratorTripleString(HDTUtil.executeQuery(em.hdt, s, p, o));
	}

	private void facetedSearch() {
		String[] fsArgs = cl.getOptionValues("fs");

		fsArgs[1] = fsArgs[1].equals("?") ? null : HDTUtil.addQuotesIfNecessary(fsArgs[1]);
		fsArgs[2] = fsArgs[2].equals("?") ? null : HDTUtil.addQuotesIfNecessary(fsArgs[2]);
		String message = "Number of results after filtering: ";

		sw.reset();
		if (fsArgs[0].equals("SP?")) {

			em.facetedSearch(fsArgs[1], fsArgs[2], TripleComponentRole.SUBJECT);
			System.out.println("Subjects: " + message + em.hsSubject.size());
			System.out.println("Objects: " + message + em.hsObject.size());

		} else if (fsArgs[0].equals("?PO")) {

			em.facetedSearch(fsArgs[1], fsArgs[2], TripleComponentRole.OBJECT);
			System.out.println("Subjects: " + message + em.hsSubject.size());
			System.out.println("Objects: " + message + em.hsObject.size());

		} else {
			System.out.println("Command not supported. Choose between 'SP?' and '?PO' for facetted search.");
		}
		System.out.println("Time needed for filtering: " + sw.stopAndShow());
	}

	/**
	 * Generates application command line options.
	 *
	 */
	private Options getOptions() {

		Options options = new Options();

		options.addOption("in", "incoming", true, "Select incoming relation, e.g. '-in 3' ");
		options.addOption("out", "outgoing", true, "Select outgoing relation, e.g. '-out 0' ");
		options.addOption("pred", "predicate", false, "Print predicate relations for reachable class index");

		options.addOption(Option.builder("predOut")
						.desc("Chooses the connected outgoing predicate connection and updates the center correspondingly")
						.numberOfArgs(2)
						.argName("CATEGORY-INDEX> <PREDICATE-INDEX")
						.build());

		options.addOption(Option.builder("predIn")
				.desc("Chooses the connected incoming predicate connection and updates the center correspondingly")
				.numberOfArgs(2)
				.argName("CATEGORY-INDEX> <PREDICATE-INDEX")
				.build());

		options.addOption(Option.builder("pred")
				.desc("Prints all connected predicates in relation to the current center")
				.hasArg(false)
				.build());

		options.addOption(Option.builder("predFilter")
				.desc("Filters the current resources by the passed predicate connection, i.e., all resources " +
						"which do not have a connection are kicked out from the current center. For example: predFilter out 18 0")
				.numberOfArgs(3)
				.argName("IN/OUT> <CATEGORY-INDEX> <PREDICATE-INDEX")
				.build());

		options.addOption(Option.builder("rcFilter")
				.desc("Filters the current resources by the passed reachable category connection, i.e., all resources " +
						"which do not have a connection are kicked out from the current center. For example: rcFilter in 4")
				.numberOfArgs(2)
				.argName("IN/OUT> <CATEGORY-INDEX")
				.build());

		options.addOption(Option.builder("fs")
				.desc("Applies a filter to the current set of resources. First argument indicates the search order ('SP?' or '?PO')")
				.numberOfArgs(3)
				.argName("ORDER> <PRED-URI> <RESOURCE-URI>")
				.build());

		options.addOption(Option.builder("search")
				.desc("Performs a search for the given triple pattern on the HDT file. For example: '? predURI ?")
				.numberOfArgs(3)
				.argName("SUBJECT-URI> <PRED-URI> <OBJECT-URI")
				.build());

		options.addOption(Option.builder("print")
				.desc("Print arguments: center, allTriples, dicPreds, dossier")
				.numberOfArgs(1)
				.argName("PRINT-COMMAND")
				.build());

		options.addOption(Option.builder("rc")
				.desc("Prints all reachable categories for the current class")
				.hasArg(false)
				.build());

		options.addOption(Option.builder("init")
				.desc("Lets the user choose an initial class for exploration, e.g. '-init 5' ")
				.hasArg()
				.argName("CATEGORY-INDEX")
				.build());

		options.addOption("e", "exit", false, "Exit the application");
		options.addOption("reset", "reset", false, "Resets the exploration");
		options.addOption("h", "help", false, "Prints the application help");

		return options;
	}

	/**
	 * Prints application help.
	 */
	private void printAppHelp() {

		Options options = getOptions();

		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("JavaStatsEx", options, true);
	}

}

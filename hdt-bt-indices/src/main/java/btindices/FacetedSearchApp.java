package btindices;

import btindices.indicesmanager.FilterExplorationManager;
import btindices.indicesmanager.FilterExplorationManagerIndices;
import org.apache.commons.cli.*;
import org.rdfhdt.hdt.util.StopWatch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Represents a demo of the features which are provided by the PV indices.
 */
public class FacetedSearchApp {

    FilterExplorationManager fsManager;
    String hdtPath;
    BufferedReader br;
    CommandLine cl;
    String[] options;
    static final StopWatch sw = new StopWatch();

    public static void main(String[] args) {

        String hdtPath = args[0];
        String fsIndices = args[1];
        FacetedSearchApp fsApp = new FacetedSearchApp(hdtPath, fsIndices);
    }

    public FacetedSearchApp(String hdtPath, String fsIndices) {
        this.hdtPath = hdtPath;
        br = new BufferedReader(new InputStreamReader(System.in));
        fsManager = new FilterExplorationManagerIndices(hdtPath, fsIndices);
        run();
    }


    public void run() {

        System.out.println("Choose initial class:");
        fsManager.displayInitialRDFClasses();

        while (true) {
            try {
                cl = parseArguments(br.readLine().split(" "));


            } catch (IOException e) {
                e.printStackTrace();
            }

            if (cl.hasOption("select")) {
                options = cl.getOptionValues("select");
                boolean facetValueIsObject = options[0].equals("in") ? true : false;
                long facetID = Long.parseLong(options[1]);
                long facetValueID = Long.parseLong(options[2]);

                System.out.println("Apply facet: " + facetID + " " + facetValueID);
                applyFacet(facetValueIsObject, facetID, facetValueID);

                updateAvailableFacets();

            } else if (cl.hasOption("facet")) {

                options = cl.getOptionValues("facet");
                fsManager.displayFacetValues(true, Integer.parseInt(options[0]));
                fsManager.displayFacetValues(false, Integer.parseInt(options[0]));
            } else if (cl.hasOption("init")) {
                options = cl.getOptionValues("init");
                int classPos = Integer.parseInt(options[0]);
                sw.reset();
                fsManager.updateToInitialRDFClass(fsManager.typeURIs[classPos].toString());
                System.out.println("Time required to update to initial RDF class: " + sw.stopAndShow());
                updateAvailableFacets();

            } else if (cl.hasOption("help")) {
                printAppHelp();
            } else if (cl.hasOption("reset")) {
                fsManager.resetExplorationState();
                fsManager.displayInitialRDFClasses();
            } else {
                System.out.println("Could not parse command. Enter '-help' for all available commands.");
            }
        }
    }

    private void applyFacet(boolean outgoing, long facetID, long facetValueID) {
        sw.reset();
        fsManager.applyFacet(outgoing, facetID, facetValueID);
        System.out.println("Time required for applying facet: " + sw.stopAndShow());
    }

    private void updateAvailableFacets() {
        sw.reset();
        fsManager.updateAvailableFacets();
        fsManager.displayAvailableFacets();
        System.out.println("Time required for updating available facets: " + sw.stopAndShow());
        System.out.println("Number of current resources: " + fsManager.center.size());
    }

    private CommandLine parseArguments(String[] args) {

        Options options = getOptions();
        CommandLine line = null;

        CommandLineParser parser = new DefaultParser();

        try {
            line = parser.parse(options, args);

        } catch (Exception ex) {

            System.err.println("Unkown command.");
            //System.err.println("Failed to parse command line arguments");
            //System.err.println(ex.toString());
            printAppHelp();

            //System.exit(1);
        }

        return line;
    }

    private Options getOptions() {

        Options options = new Options();

        options.addOption(Option.builder("help")
                .desc("Prints all available commands")
                .hasArg(false)
                .build());

        options.addOption(Option.builder("select")
                .desc("Selects an available facet")
                .hasArg(true)
                .numberOfArgs(3)
                .argName("IN/OUT> <FACET-POS> <FACETVALUE-ID")
                .build());

        options.addOption(Option.builder("init")
                .desc("Initializes the faceted search with the corresponding class")
                .hasArg(true)
                .argName("RDF-CLASS-POS")
                .build());

        options.addOption(Option.builder("facet")
                .desc("Display facet values for the corresponding facet")
                .hasArg()
                .argName("FACET-POSITION")
                .build());

        options.addOption(Option.builder("reset")
                .desc("Resets the application")
                .hasArg(false)
                .build());
        options.addOption(Option.builder("init")
                .desc("Lets the user choose an initial class for exploration, e.g. '-init 5' ")
                .hasArg()
                .argName("CATEGORY-INDEX")
                .build());
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

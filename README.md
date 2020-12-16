# hdt-bt-indices-java-lib

## Overview
The HDT BT indices represent an extension of the HDT serialization format for the efficient exploration of RDF data sets. The original modules of the HDT Java library from Git (https://github.com/rdfhdt/hdt-java) have not been modified, which includes hdt-java-api, hdt-java-core, hdt-java-cli, hdt-jena, hdt-java-package and hdt-fuseki.

* hdt-bt-indices: represents the module which provides all classes in order to generate and manage the BT indices. 
* exploration-experiments: provides all classes which were required to conduct the experiments where the performance of the BT indices has been compared to other approaches. 
* hdt-bt-indices-tests: this module contains unit tests in order to prove the correctness of the implemented exploratory operations. Furthermore, the command line applications have been tested in this module. 

## Command Line Applications:
By the execution of a "mvn install" command in the exploration-experiments and hdt-bt-indices module, a single JAR file with all required dependencies can be generated. It is stored to the corresponding "target" folder of the directory where the pom-file is located. The respective applications can subsequently be executed by utilizing the JAR file and the classpath in a "java -cp \<SNAPSHOT-JAR\> \<CLASSPATH\>" command. 

## hdt-bt-indices JAR-File

### CtCIndicesGenerator
<b>Classpath:</b> btindices.indexgeneration.CtCIndicesGenerator<br>
Creates the HDT PS-O BT Class-to-Class indices for the efficient calculation of incremental joins and reachable categories.
* If a HDT file already exists for a given RDF graph:
	* Args: \<HDT-PATH\> \<CATEGORY-INDICES-DIRECTORY\>
	* \<HDT-PATH\>: path to the existing HDT file
	* \<CTC-DIRECTORY\>: path to the directory that will be created for the BT indices
* If the HDT file for the BT indices has to be generated first from an RDF dump file:
	* Args: \<RDF-PATH\> \<HDT-OUTPUT-PATH\> \<CtC-DIRECTORY\>
	* \<RDF-PATH\>: path to an existing RDF file. Notation is parsed automatically
	* \<HDT-OUTPUT-PATH\>: path where the generated HDT file should be stored

### PVIndicesGenerator
Args: \<HDT-PATH\> \<CTC-INDICES-DIRECTORY\><br>
<b>Classpath:</b> btindices.indexgeneration.PVIndicesGenerator<br>
Creates the PS-O and PO-S BT indices for the efficient computation of all available filters for a given set of resources.

### CatExplorationApp
Args: \<HDT-PATH\> \<CATEGORY-INDICES-DIRECTORY\><br>
<b>Classpath:</b> btindices.CatExplorationApp<br>
Represents a console application which enables the user to explore the given HDT file with the corresponding CtC BT Indices. Press "-h" or "--help" after the application has loaded in order to inspect all available commands. 

### FacetedSearchApp
Args: \<HDT-PATH\> \<PV-INDICES-DIRECTORY\><br>
<b>Classpath:</b> btindices.FacetedSearchApp<br>
Represents a console application which enables the user to explore the given HDT file with the corresponding Filter indices. Press "-h" or "--help" after the application has loaded in order to inspect all available commands. 

### ExperimentQueriesGenerator
Args: \<HDT-PATH\> \<CTC-INDICES-DIR\> \<PV-INDICES-DIR\><br>
<b>Classpath:</b> btindices.statisticalquerygeneration.querysets.ExperimentQueriesGenerator<br>
Generates all required category, filter and hybrid queries for a given RDF data set and reduces them to the specified size.

### CategoryExperiments
Args: \<TTL-FILE\> \<HDT-PATH\> \<CtC-INDICES-DIR\> \<CTC-QUERY-MODELS-DIR\><br>
<b>Classpath:</b> efficiencycomparison.individualexperiments.CategoryExperiments<br>
Executes all category experiments for the following approaches: CtC indices, RDFox, Plain HDT, HDT Jena

### FilterExperiments
Args: \<TTL-FILE\> \<HDT-PATH\> \<PV-INDICES-DIR\> \<PV-QUERY-MODELS-DIR\><br>
<b>Classpath:</b> efficiencycomparison.individualexperiments.FilterExperiments<br>
Executes all filter experiments for the following approaches: PV indices, RDFox, Plain HDT, HDT Jena

### HybridExperiments
Args: \<TTL-FILE\> \<HDT-PATH\> \<CTC-INDICES-DIR\> \<PV-INDICES-DIR\> \<HYBRID-QUERY-MODELS-DIR\><br>
<b>Classpath:</b> efficiencycomparison.individualexperiments.HybridExperiments<br>
Executes all hybrid experiments for the following approaches: Hybrid, RDFox, Plain HDT, HDT Jena

### FilterThresholdExperiment
Args: \<HDT-PATH\> \<FS-INDICES-DIRECTORY\> \<FS-QUERIES-DIRECTORY\> [\<THRESHOLD-1\>, \<THRESHOLD-2\>, ...]<br>
<b>Classpath:</b> efficiencycomparison.FilterThresholdExperiment<br>
Represents a command line application which executes a set of "Available Facet" experiments using different initializations of the FSIndices query engine stub in relation to a given threshold in order to compare different thresholds. This threshold is used in the following situation: <br>
If all available facets are calculated for the current set of resources and the number of resources "n" is less than the specified threshold, for each resource a HDT search is performed in O(log(n)) and all resources are added in O(n) to the updated hash set. Altogether: n \* O(log(n)) \+ O(n).<br>
If n is however greater than the threshold, only one HDT search is performed in order to fetch all resources which are annotated with the corresponding facet. Subsequently, it is iterated over all fetched resources and it is examined whether the resource is in the current set of resources. Altogether: log(size(Bitmap Triples)) + O(# fetched resources)) 
* \<HDT-PATH\>: path to the corresponding HDT file
* \<FS-INDICES-DIRECTORY\>: directory which contains the FS indices
* \<FS-QUERIES-DIRECTORY\>: directory which contains the FS queries (evaluates queries up to 3 applied filters with each having up to 3 different levels of difficulty)
* [\<THRESHOLD-1\> \<THRESHOLD-2\> ...]: thresholds which are used to initialize different versions of the FSIndices query engine stub
<br>

## Boxplots 
boxplots.R \<QUERY-RESULTS-DIR\> \<DATA-SET-NAME\> \<QUERY-TYPE-TITLE\> \<QUERY-TYPE-FILE\> \<NUM-JOINS/FILTERS\> [\<QE-STUB-1\> \<QE-STUB-2\> ...]<br>
Represents a command line application in order to evaluate the time measurements from the experiments and to generate corresponding box plots.
* \<QUERY-RESULTS-DIR\>: path to the directory where the experiment results are stored
* \<DATA-SET-NAME\>: name of the data set which appears in the title
* \<QUERY-TYPE-TITLE\>: query type which appears in the title ("count", "reachable categories" etc.)
* \<QUERY-TYPE-FILENAME\>: choose between Reachable Categories = 'RC', Count = 'Count' and String Queries = 'Verbose', 'AvailableFacets', 'ApplyFacets' &rarr; used in order to load ".csv" files with time measurements
* \<JOINS/FILTERS\>: specify if 'Filters' or 'Joins' appears in the title &rarr; title outcome: 'Filters = [...]'
* \<NUM-JOINS/FILTERS\>: maximum number of joins or filters in the query models 
* [\<QE-STUB-1\>, \<QE-STUB-2\> ...]: name of the query engine stubs for which the experiments have been conducted

**Note:** R has to be installed first.<br>
**Example:** 
Rscript.exe boxplots.R categoryQueries LinkedMDB Verbose "Category Queries" 4 HDTJena RDFox PlainHDT CtCIndices 



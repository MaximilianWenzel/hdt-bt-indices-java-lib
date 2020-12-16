#!/bin/bash
java -cp *.jar efficiencycomparison.individualexperiments.CategoryExperiments *.ttl *.hdt ctcIndices categoryQueries
$SHELL
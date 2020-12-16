library(tidyverse)
getCurrentFileLocation <-  function()
{
  this_file <- commandArgs() %>% 
    tibble::enframe(name = NULL) %>%
    tidyr::separate(col=value, into=c("key", "value"), sep="=", fill='right') %>%
    dplyr::filter(key == "--file") %>%
    dplyr::pull(value)
  if (length(this_file)==0)
  {
    this_file <- rstudioapi::getSourceEditorContext()$path
  }
  return(dirname(this_file))
}

rWorkingDir <- getCurrentFileLocation()

source(paste(rWorkingDir, "/", "DrawBoxplot.R", sep=""))
source(paste(rWorkingDir, "/", "ReadExperimentData.R", sep=""))

library("viridis")

# disable scientific notation, e.g. 100000 instead of 1e+05
library("dplyr")
library("ggpubr")
theme_pubclean()


# parse command line arguments
args <- commandArgs(trailingOnly = TRUE)

# boxplots.R <QUERY-RESULTS-DIR> <DATA-SET-NAME> <QUERY-TYPE-NAME> <TITLE-QUERY-TYPE> <NUM-JOINS> [<QE-STUB-1>, <QE-STUB-2> ...]

dir <- args[1]
dataSet <- args[2]
queryType <- args[3] # is used to load the files
titleQueryType <- args[4] # query type which appears in the title
numJoins = args[5] # joins or applied filters
stubsStrings = args[6:length(args)]
queryEngineStubs <- factor(stubsStrings, levels=stubsStrings)


df <- NULL


for (joins in 1:numJoins) {
  toAdd <- getAllLevelsInOneTable(queryEngineStubs, queryType, joins)
  if (is.null(df)) {
    df <- toAdd
  } else if (ncol(toAdd) > 0) {
    df <- rbind(df, toAdd)
  }
}

df <- df[order(df$stub),]
df$stub <- as.factor(df$stub)

# write.csv(nrow(subset(df, stub == queryEngineStubs[1])), file=paste("numQueries_", dataSet, "_", queryType, ".txt", sep=""))
numQueries = nrow(subset(df, stub == queryEngineStubs[1]))

pdf(paste(dataSet, "_", queryType, ".pdf", sep = ""))
#title <- paste(dataSet, ": ", titleQueryType," Queries, ", titleJoinsFilters, " = ", joins, sep="")
title <- paste(dataSet, ", n = ", numQueries, sep="")
print(getStandardBoxplot(df, queryEngineStubs, title))
dev.off()






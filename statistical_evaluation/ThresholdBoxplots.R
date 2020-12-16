
library(scales)
library(tidyverse)
getCurrentFileLocation <-  function() {
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


# parse command line arguments
args <- commandArgs(trailingOnly = TRUE)

# boxplots.R <QUERY-RESULTS-DIR> <DATA-SET-NAME> <QUERY-TYPE-NAME> <TITLE-QUERY-TYPE> <NUM-JOINS> [<QE-STUB-1>, <QE-STUB-2> ...]

dir <- args[1]
dataSet <- args[2]
queryType <- args[3] # is used to load the files
titleQueryType <- args[4] # query type which appears in the title
numJoins = args[5] # joins or applied filters
queryEngineStubs <- args[6:length(args)]

# give queryEngineStubs new names in order to sort them alphabetically
# queryEngineStubs <- sort(queryEngineStubs)
# print(queryEngineStubs)

df <- getTimeMeasurements(queryEngineStubs, queryType, numJoins)

thresholdValues <- c()
#get threshold value and replace queryEngineStub by threshold value
for (i in 1:length(queryEngineStubs)) {
  splitted <- unlist(strsplit(queryEngineStubs[i], "_"))
  print(splitted)
  thresholdValues <- c(thresholdValues, splitted[2])
  df$stub[df$stub == queryEngineStubs[i]] <- splitted[2]
}

usedStub <- splitted[1]

# convert to numeric value
df$stub = as.numeric(df$stub)
# transform(df, stub = as.numeric(stub))

# sort data frame
df <- df[order(df$stub),]

# convert to pretty string numbers -> c("time", "level", "stub")
# df$stub = sapply(df$stub, function(x) comma(x))

# convert threshold numbers back to factors
df$stub = as.factor(df$stub)

numQueries = nrow(subset(df, stub == thresholdValues[1]))

pdf(paste("Threshold_", dataSet, "_", usedStub, "_", queryType, ".pdf", sep = ""))
title <- paste(titleQueryType, ", n = ", numQueries, sep="")
boxplot <- getThresholdBoxplot(df, thresholdValues, title) +
  scale_x_discrete(labels=c("1,000","5,000","10,000", "25,000","50,000","100,000","250,000","500,000","1,000,000"))

print(boxplot)
dev.off()






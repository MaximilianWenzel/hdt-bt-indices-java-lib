
getTimeMeasurements <- function(queryEngineStubs, queryType, maxJoins) {
  df = NULL
  
  for (joins in 1:maxJoins) {
    toAdd <- getAllLevelsInOneTable(queryEngineStubs, queryType, joins)
    if (is.null(df)) {
      df <- toAdd
    } else if (ncol(toAdd) > 0) {
      df <- rbind(df, toAdd)
    }
  }
  return(df)
}

getTableForSpecificQueryStr <- function(queryEngineStubs, queryType, queryStr, joins, level) {
  colnames = c("time", "level", "stub")
  
  t = data.frame(matrix(ncol=3, nrow=0)) # Time, Level, QueryEngine
  colnames(t) <- colnames
  
  for (qeStubsIndex in 1:length(queryEngineStubs)) {
    
    queryEngineStub = queryEngineStubs[qeStubsIndex]
    fileName = paste(queryStr, "_", queryType, "_", queryEngineStub,
                     ".csv", sep = "", NULL)
    if (!file.exists(file.path(dir, fileName))) {
      next
    }
    
    # read results from file 
    qeResults = read.table(file.path(dir, fileName), header=TRUE, sep=",")
    if (ncol(qeResults) == 0) {
      next
    }
    
    
    # replace zero to 10 seconds = 10,000ms = 10,000,000,000ns = 10 * 10^9 ns
    qeResults[qeResults == 0] = 10 * 10^9 + 10
    
    qeResults$round1 = round(qeResults$round1 / 1000000, digits = 7)
    qeResults$round2 = round(qeResults$round2 / 1000000, digits = 7)
    #qeResults$round3 = round(qeResults$round3 / 1000000, digits = 7)
    
    #qeResults$result = rowMeans(subset(qeResults, select=c(nanoseconds1st, nanoseconds2nd), na.rm=TRUE))
    qeResults$result = apply(qeResults[,c(2:3)], 1, min, na.rm = TRUE)
    
    toAdd <- data.frame(matrix(ncol=3, nrow=nrow(qeResults)))
    colnames(toAdd) <- colnames
    toAdd["time"] = qeResults$result
    toAdd["level"] = level
    toAdd["stub"] = queryEngineStub
    t = rbind(t, toAdd)
  }
  
  return(t)
}

getAllLevelsInOneTable <- function(queryEngineStubs, queryType, joins) {
  
  lvl = 3 
  
  df = data.frame(matrix(ncol=length(queryEngineStubs), nrow=0))
  for (currLvl in 1:lvl) {
    
    if (joins > 0) {
      # queries with joins
      queryStr = paste("q", joins, "_", currLvl, sep ="", NULL)
      df = rbind(df, getTableForSpecificQueryStr(queryEngineStubs, queryType, queryStr, joins, currLvl))
    } 
    else {
      # queries without joins -> q0
      queryStr = paste("q", joins, sep = "")
      df = getTableForSpecificQueryStr(queryEngineStubs, queryType, queryStr, joins, currLvl)
      break
    }
    
  }
  
  return(df)
}



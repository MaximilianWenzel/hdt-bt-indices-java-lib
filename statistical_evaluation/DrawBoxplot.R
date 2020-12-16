library("ggplot2")

getNumberOfTimeouts <- function (queryEngineStubs, timeMeasurements) {
  # get number of NA values
  queryEngineNARatio = data.frame(matrix(nrow=0, ncol=2))
  
  for (i in 1:length(queryEngineStubs)) {
    qeMeasurements = subset(timeMeasurements, stub == queryEngineStubs[i])
    # replace measurements >= 10sec NA values
    naValues = sapply(qeMeasurements["time"], function(x) replace(x, x >= 10000,NA))
    #print(naValues)
    
    rowToAdd = data.frame(matrix(nrow=1, ncol=2))
    rowToAdd[1] = round(sum(is.na(naValues)) / nrow(naValues), digits = 2)
    rowToAdd[2] = i
    queryEngineNARatio = rbind(queryEngineNARatio, rowToAdd)
  }
  
  colnames(queryEngineNARatio) = c("na_ratio", "stub")
  queryEngineNARatio = subset(queryEngineNARatio, na_ratio > 0)
  print(queryEngineNARatio)
  return(queryEngineNARatio)
}

getBasicBoxplot <- function(timeMeasurements, queryEngineStubs, title) {
  # get number of queries for this experiment
  qe = subset(timeMeasurements, stub == queryEngineStubs[1])
  numLvl = list()
  numLvl[1] = nrow(qe[qe["level"] == 1,])
  numLvl[2] = nrow(qe[qe["level"] == 2,])
  numLvl[3] = nrow(qe[qe["level"] == 3,])
  numQueries = nrow(qe)
  
  # get number of NA values
  queryEngineNARatio = getNumberOfTimeouts(queryEngineStubs, timeMeasurements)
  
  if (nrow(queryEngineNARatio) > 0) {
    upperYLimit = 100000
  } else {
    upperYLimit = 10010
  }

  #  length of the whiskers as multiple of IQR (=range between 25th and 75th quantile)
  whiskersRange = 1000
  timeoutColor = "#b30000"
  
  # basic plot
  e = ggplot(timeMeasurements, aes(x=stub, y=time)) +
    scale_fill_hue(l=80, c=35) + # l = lightness, c = chroma (intensity)
    scale_y_log10(
      limits = c(min(df$time),upperYLimit),
      breaks=c(0.001, 0.01, 0.1, 1.0, 10, 100, 1000, 10000),
      # labels = scales::trans_format("log10", scales::math_format(10^.x)) # 10^x notation
      labels = c("0.001ms", "0.01ms", "0.1ms", "1ms", "10ms", "100ms", "1s", "10s") #"100s")
    ) +
    # coord_flip() + # not working with logticks !!!
    stat_boxplot(geom ='errorbar', width=0.3,
                 colour="#333333",
                 position = position_dodge(width=1.2),
                 coef = whiskersRange) + #  length of the whiskers as multiple of IQR (=range between 25th and 75th quantile)
    #facet_wrap(~level) +
    geom_boxplot(
      aes(fill = factor(stub)),
      coef = whiskersRange,
      position=position_dodge(1.0)
    )

  # timeout area
  # geom_rect(data=df, # timeout area
  #           inherit.aes=FALSE,
  #           aes(xmin=0,
  #               xmax=length(queryEngineStubs) + 1,
  #               ymin=10000,
  #               ymax=10000*10,
  #               group=stub),
  #           #color="transparent",
  #           fill="#666259")#, alpha=0.015) +
  
  labelColor = "#333333"
  labelFontColor = "white"
  
  # timeout labels
  if (nrow(queryEngineNARatio) > 0) {
    
    # additional padding to fill label exactly to 10 second limit
    e =  e + geom_label(
      # data=queryEngineNARatio %>% filter(na_ratio > 0), # Filter data first
      data=queryEngineNARatio,
      aes(x = stub, y = 10000, label = paste(na_ratio*100, "%", sep="")),
      # position = position_dodge(width = 0),
      size = 8,
      vjust = "bottom",
      hjust = "center",
      label.size = 0.0, # border size in mm
      label.padding = unit(1.5, "lines"), # rectangle size around label
      color = labelColor, # font color
      fill = labelColor # background color
      
    )
    # additional padding to fill the space above the plot
    e =  e + geom_label(
      # data=queryEngineNARatio %>% filter(na_ratio > 0), # Filter data first
      data=queryEngineNARatio,
      aes(x = stub, y = 80000, label = paste(na_ratio*100, "%", sep="")),
      # position = position_dodge(width = 0),
      size = 8,
      vjust = "center",
      hjust = "center",
      label.size = 0.0, # border size in mm
      label.padding = unit(1.5, "lines"), # rectangle size around label
      color = labelColor, # font color
      fill = labelColor # background color
      
    )
    # contains timeouts as percent values
    e =  e + geom_label(
      # data=queryEngineNARatio %>% filter(na_ratio > 0), # Filter data first
      data=queryEngineNARatio,
      aes(x = stub, y = 50000, label = paste(na_ratio*100, "%", sep="")),
      # position = position_dodge(width = 0),
      size = 8,
      fontface = "bold",
      vjust = "center",
      hjust = "center",
      label.size = 0.0, # border size in mm
      label.padding = unit(1.5, "lines"), # rectangle size around label
      color = labelFontColor, # font color
      fill = labelColor # background color
      
    )
    # horizontal timeout line
    e = e + geom_hline(yintercept=1e+4, linetype="solid", color = "#333333", size=0.8)
    
  }
  
  #geom_label(aes(x = 1, y = 1e+5, label ="Occured Timeouts"),
  #           color=timeoutColor,
  #           position = position_dodge(width = 0),
  #           size = 3,
  #           hjust = 0.5 # -> left-algin = 0, center-align = 0.5, right-align = 1.0
  #)
  
  
  # labels
  e = e +
    labs(
      title = title
      #subtitle = paste("Total Number of Queries: ", numQueries)
      #", Level 1: ", numLvl[1],
      #", Level 2: ", numLvl[2],
      #", Level 3: ", numLvl[3])
    ) +
    xlab("") +
    ylab("") +
    guides(fill=guide_legend(title="")) # legend title
  

  return(e)
}

getStandardBoxplot <- function(timeMeasurements, queryEngineStubs, title) {
  boxplot = getBasicBoxplot(timeMeasurements, queryEngineStubs, title) +
      annotation_logticks(sides = "l",
                  short = unit(1,"mm"),
                  mid = unit(3,"mm"),
                  long = unit(4,"mm"),
                  color="#333333"
      ) +
    # themes and colors
      theme_bw() +
      theme(
        plot.title = element_text(color="black", size=25, hjust =0.5, face="bold"),
        plot.subtitle = element_text(color="black", size=8, hjust =0.5),
        axis.title.x = element_text(color="black", size=18, face="bold"),
        axis.text.x = element_text(color="black", size=15),
        axis.title.y = element_text(color="black", size=18),
        axis.text.y = element_text(color="black", size=20),
        legend.position = "none" # remove entire legend
      )

  return(boxplot)
}

getThresholdBoxplot <- function(timeMeasurements, queryEngineStubs, title) {
  boxplot = getBasicBoxplot(timeMeasurements, queryEngineStubs, title) 
  
  boxplot = boxplot + 
    annotation_logticks(sides = "l",
                        short = unit(1,"mm"),
                        mid = unit(3,"mm"),
                        long = unit(4,"mm"),
                        color="grey"
    ) +
    theme_light() +
    theme(
      plot.title = element_text(color="black", size=20, hjust =0.5, face="bold", margin = margin(t = 0, r = 0, b = 10, l = 0)),
      plot.subtitle = element_text(color="black", size=8, hjust =0.5),
      axis.title.x = element_text(color="black", size=12, face="bold", margin = margin(t = 10, r = 0, b = 0, l = 0)),
      axis.text.x = element_text(color="black", size=9),
      axis.title.y = element_text(color="black", size=16),
      axis.text.y = element_text(color="black", size=13),
      legend.position = "none" # remove entire legend
    ) +
  xlab("Threshold")
  
  return(boxplot)
}

#!/bin/bash
java -cp *.jar btindices.indexgeneration.CtCIndicesGenerator *.ttl data.hdt ctcIndices
$SHELL
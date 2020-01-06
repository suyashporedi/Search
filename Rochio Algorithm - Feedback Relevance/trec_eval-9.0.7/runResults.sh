#!/bin/bash
groundTruth="../qrels.51-100"
for beta in 0.2 0.4 0.6 0.8 1.0; do
	for gamma in 0.0 0.2 0.4 0.6 0.8 1.0; do
		testfile="../betaGama/BetaGamma"$beta$gamma".txt"
		targetFile="../results/outputB"$beta"G"$gamma".txt"
		./trec_eval -m all_trec $groundTruth $testfile>$targetFile
		echo "Written to"$targetFile
	done
done

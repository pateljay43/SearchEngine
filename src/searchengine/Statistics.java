/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package searchengine;

import java.util.List;

/**
 *
 * @author JAY
 */
public class Statistics {

    private final int numOfTerms;
    private final int numOfTypes;
    private final int avgDocsPerTerm;
    private final List<Integer> mostFrequentTerms;
    private final int totalMemReq;

    public Statistics(int numOfTerms, int numOfTypes, int avgDocsPerTerm,
            List<Integer> mostFrequentTerms, int totalMemReq) {
        this.numOfTerms = numOfTerms;
        this.numOfTypes = numOfTypes;
        this.avgDocsPerTerm = avgDocsPerTerm;
        this.mostFrequentTerms = mostFrequentTerms;
        this.totalMemReq = totalMemReq;
    }

    public int getNumOfTerms() {
        return numOfTerms;
    }

    public int getNumOfTypes() {
        return numOfTypes;
    }

    public int getAvgDocsPerTerm() {
        return avgDocsPerTerm;
    }

    public List<Integer> getMostFrequentTerms() {
        return mostFrequentTerms;
    }

    public int getTotalMemReq() {
        return totalMemReq;
    }
}

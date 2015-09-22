/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package searchengine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

/**
 *
 * @author JAY
 */
public class PositionalInvertedIndex {

    private final TreeMap<String, TreeMap<Integer, ArrayList<Long>>> mIndex;
    private int longestWord;

    /**
     * creates new mIndex which stores terms with document in which it occurs
     * and positions where it occurs in that document
     */
    public PositionalInvertedIndex() {
        mIndex = new TreeMap<>();
        longestWord = 0;
    }

    /**
     * adds new term to positional inverted index with its position in given
     * documentId
     *
     * @param term term to be referred in dictionary
     * @param documentID documentId to be referred in dictionary for given term
     * @param position position to be added in dictionary for given term and
     * documentId
     */
    public void addTerm(String term, int documentID, long position) {
        TreeMap<Integer, ArrayList<Long>> postings = mIndex.getOrDefault(term, new TreeMap<>());
        ArrayList<Long> positionalList = postings.getOrDefault(documentID, new ArrayList());
        if (positionalList.isEmpty() || positionalList.get(positionalList.size() - 1) < position) {
            positionalList.add(position);
            postings.put(documentID, positionalList);
            mIndex.put(term, postings);
        }
        longestWord = Math.max(longestWord, term.length());
    }

    /**
     * gets postings for given term
     *
     * @param term term whose postings to be returned
     * @return postings containing list of documents paired with list of
     * positions (<document,[position1,..]>)
     */
    public TreeMap<Integer, ArrayList<Long>> getPostings(String term) {
        return mIndex.get(term);
    }

    /**
     * gets positional list for given term and documentId
     *
     * @param term term whose postings to be searched for documentId
     * @param documentId Id whose positional list will be returned
     * @return list of positions
     */
    public ArrayList<Long> getPositionalList(String term, int documentId) {
        return mIndex.get(term).get(documentId);
    }

    /**
     *
     * @return number of terms in dictionary
     */
    public int getTermCount() {
        return mIndex.size();
    }

    /**
     *
     * @return return sorted list of terms in dictionary
     */
    public String[] getDictionary() {
        String[] terms = mIndex.keySet().toArray(new String[mIndex.size()]);
        return terms;
    }

    /**
     *
     * @return length of largest term in dictionary
     */
    public int getLongestWordLength() {
        return longestWord;
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package searchengine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * @author JAY
 */
public class QueryProcessor {

    private static PositionalInvertedIndex index;
    private static PorterStemmer porterstemmer;
    private static boolean containPositive;

    public QueryProcessor(PositionalInvertedIndex _index) {
        index = _index;
        porterstemmer = new PorterStemmer();
        containPositive = false;
    }

    /**
     * process any query
     *
     * @param query to be processed
     * @return document IDs for the query
     */
    public Set<Integer> processQuery(String query) {
        TreeSet<Integer> result = new TreeSet<>();

        query = query.replaceAll("[^A-Za-z0-9-+)( \"]", "")
                .replaceAll("\\+", " + ")
                .replaceAll("(( )( )+)", " ")
                .trim()
                .toLowerCase();
        // First process query with in brackers () then double quotes then logical operations
        // Logical operation order -> NOT, AND, OR
        if (query.contains("+")) {
            String[] split = query.split("\\+");
            for (String queryLiteral : split) {
                result.addAll(processQuery(queryLiteral));
            }
        } else {    // series of tokens with optional '-'
            return processQueryLiterals(query);
        }
        return result;
    }

    /**
     * process each QueryLiteral (Q)
     *
     * @param query is Q
     * @return document IDs for Q
     */
    private Set<Integer> processQueryLiterals(String query) {
        List<String> split = Arrays.asList(query.split(" "));
        int target = 0;
        HashMap<Integer, Integer> andSplit = new HashMap<>();
        for (Iterator<String> it = split.iterator(); it.hasNext();) {
            String token = it.next().replaceAll("[)(]", "").trim();
            if (token.startsWith("\"")) {
                containPositive = true;
                target++;
                String substring = token.trim().substring(1, token.length());
                Set<Integer> processPhrase;
                if (substring.endsWith("\"")) {
                    processPhrase = processQuery(substring
                            .substring(0, substring.length() - 1));
                } else {
                    ArrayList<String> newquery = new ArrayList<>();
                    newquery.add(porterstemmer.
                            processToken(substring));
                    while (it.hasNext()) {
                        String next = it.next().replaceAll("[)(]", "").trim();
                        if (next.endsWith("\"")) {
                            newquery.add(porterstemmer.
                                    processToken(next.substring(0, next.length() - 1)));
                            break;
                        }
                        newquery.add(porterstemmer.processToken(next));
                    }
                    processPhrase = processPhrase(newquery);
                }
                processPhrase.stream().forEach((Integer docId) -> {
                    Integer value = andSplit.getOrDefault(docId, 0);
                    andSplit.put(docId, value + 1);
                });
            } else if (token.startsWith("(")) {
                containPositive = true;
                target++;
                String newquery = token.substring(1, token.length());
                if (newquery.endsWith(")")) {
                    newquery = newquery.substring(0, newquery.length() - 1);
                } else {
                    while (it.hasNext()) {
                        String next = it.next().trim();
                        if (next.endsWith(")")) {
                            newquery = newquery + " " + next.substring(0, next.length() - 1);
                            break;
                        }
                        newquery = newquery + " " + next;
                    }
                }
                Set<Integer> processQuery = processQuery(newquery);
                processQuery.stream().forEach((Integer docId) -> {
                    Integer value = andSplit.getOrDefault(docId, 0);
                    andSplit.put(docId, value + 1);
                });
            } else {
                token = token.trim();
                if (token.startsWith("-")) {  // NOT token
                    token = token.substring(1, token.length());
                    Iterator<Integer> keySet = index.getPostings(porterstemmer.
                            processToken(token)).keySet().iterator();
                    while (keySet.hasNext()) {
                        Integer docId = keySet.next();
                        Integer value = andSplit.getOrDefault(docId, 0);
                        andSplit.put(docId, value - 1);
                    }
                } else {        // single positive token
                    containPositive = true;
                    target++;
                    Iterator<Integer> keySet = index.getPostings(porterstemmer.
                            processToken(token)).keySet().iterator();
                    while (keySet.hasNext()) {
                        Integer docId = keySet.next();
                        Integer value = andSplit.getOrDefault(docId, 0);
                        andSplit.put(docId, value + 1);
                    }
                }
            }
        }
        Iterator<Integer> keys = andSplit.keySet().iterator();
        ArrayList<Integer> delete = new ArrayList<>();
        while (keys.hasNext()) {        // calculate docid to be deleted
            Integer docId = keys.next();
            if (andSplit.get(docId) < target) {
                delete.add(docId);
            }
        }
        Set<Integer> docIds = andSplit.keySet();
        docIds.removeAll(delete);
        return docIds;
    }

    /**
     * process the phrase query inside double quotes
     *
     * @param phrasetokens
     * @return document IDs which contains this phrase
     */
    private Set<Integer> processPhrase(ArrayList<String> phrasetokens) {
        TreeSet<Integer> result = new TreeSet<>();
        String[] terms = phrasetokens.toArray(new String[phrasetokens.size()]);
        String term1 = terms[0];
        HashMap<Integer, Set<Long>> docId_Positions = new HashMap<>();
        for (int i = 1; i < terms.length; i++) {
            String term2 = terms[i];
            HashMap<Integer, Set<Long>> pairs = matchTerms(term1, term2);
            Iterator<Integer> iterator = pairs.keySet().iterator();
            while (iterator.hasNext()) {
                Integer docId = iterator.next();
                Set<Long> values = docId_Positions.getOrDefault(docId, new HashSet<>());
                values.addAll(pairs.get(docId));
                docId_Positions.put(docId, values);
            }
            term1 = term2;
        }
        Iterator<Integer> iterator = docId_Positions.keySet().iterator();
        while (iterator.hasNext()) {
            Integer docId = iterator.next();
            Iterator<Long> positions = docId_Positions.get(docId).iterator();
            Long last = null;
            int count = 0;
            while (positions.hasNext()) {
                Long next = positions.next();
                if (last == null) {
                    last = next;
                    count++;
                } else {
                    if (last + 1 == next) {
                        count++;
                    } else {
                        count = 0;
                    }
                    last = next;
                }
            }
            if (count == phrasetokens.size()) {
                result.add(docId);
            }
        }
        return result;
    }

    /**
     * matches given terms in any document in the order "term1 term2"
     *
     * @param term1
     * @param term2
     * @return <documentId, all positions where "term1 term2" occured>
     */
    private HashMap<Integer, Set<Long>> matchTerms(String term1, String term2) {
        HashMap<Integer, Set<Long>> result = new HashMap<>();
        Iterator<Integer> term1_DocIds = index.getPostings(term1).keySet().iterator();
        Iterator<Integer> term2_DocIds = index.getPostings(term2).keySet().iterator();
        Integer term1_DocId = null;
        Integer term2_DocId = null;
        if (term1_DocIds.hasNext() && term2_DocIds.hasNext()) {
            term1_DocId = term1_DocIds.next();
            term2_DocId = term2_DocIds.next();
            boolean quit = false;
            while (!quit) {
                if (Objects.equals(term1_DocId, term2_DocId)) {
                    Iterator<Long> term1_DocId_posList = index.getPositionalList(term1, term1_DocId).iterator();
                    Iterator<Long> term2_DocId_posList = index.getPositionalList(term2, term2_DocId).iterator();
                    if (term1_DocId_posList.hasNext() && term2_DocId_posList.hasNext()) {
                        Long p1 = term1_DocId_posList.next();
                        Long p2 = term2_DocId_posList.next();
                        while (true) {
                            if (Objects.equals(p1 + 1, p2)) {
                                Set<Long> values = result.getOrDefault(term2_DocId, new HashSet<>());
                                values.add(p1);
                                values.add(p2);
                                result.put(term2_DocId, values);
                                if (term1_DocId_posList.hasNext()) {
                                    p1 = term1_DocId_posList.next();
                                } else {
                                    if (term1_DocIds.hasNext()) {
                                        term1_DocId = term1_DocIds.next();
                                    } else {
                                        quit = true;
                                    }
                                    break;
                                }
                            } else if (p1 < p2) {
                                if (term1_DocId_posList.hasNext()) {
                                    p1 = term1_DocId_posList.next();
                                } else {
                                    if (term1_DocIds.hasNext()) {
                                        term1_DocId = term1_DocIds.next();
                                    } else {
                                        quit = true;
                                    }
                                    break;
                                }
                            } else if (p2 < p1) {
                                if (term2_DocId_posList.hasNext()) {
                                    p2 = term2_DocId_posList.next();
                                } else {
                                    if (term2_DocIds.hasNext()) {
                                        term2_DocId = term2_DocIds.next();
                                    } else {
                                        quit = true;
                                    }
                                    break;
                                }
                            }
                        }
                    }
                } else if (term1_DocId < term2_DocId) {
                    if (term1_DocIds.hasNext()) {
                        term1_DocId = term1_DocIds.next();
                    } else {
                        break;
                    }
                } else if (term2_DocId < term1_DocId) {
                    if (term2_DocIds.hasNext()) {
                        term2_DocId = term2_DocIds.next();
                    } else {
                        break;
                    }
                }
            }
        }
        return result;
    }
}

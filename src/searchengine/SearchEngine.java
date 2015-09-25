/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package searchengine;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 *
 * @author JAY
 */
public class SearchEngine {

    private static PorterStemmer porterstemmer;
    private static int longestFile = 0;
    private static int mDocumentID = 0;
    private static Set<String> types = new HashSet<>();
    private static PositionalInvertedIndex index;
    private static ArrayList<String> fileNames;
    private static final String folderName = "Search Space";
    private static Path currentWorkingPath;

    public static void main(String[] args) throws IOException {

        currentWorkingPath = Paths.get(folderName).toAbsolutePath();
        // the inverted index
        porterstemmer = new PorterStemmer();
        index = new PositionalInvertedIndex();

        // the list of file names that were processed
        fileNames = new ArrayList<>();

        // This is our standard "walk through all .txt files" code.
        Files.walkFileTree(currentWorkingPath, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir,
                    BasicFileAttributes attrs) {
                // make sure we only process the current working directory
                if (currentWorkingPath.equals(dir)) {
                    return FileVisitResult.CONTINUE;
                }
                return FileVisitResult.SKIP_SUBTREE;
            }

            @Override
            public FileVisitResult visitFile(Path file,
                    BasicFileAttributes attrs) {
                // only process .txt files
                if (file.toString().endsWith(".txt")) {
                    // we have found a .txt file; add its name to the fileName list,
                    // then index the file and increase the document ID counter.
                    System.out.println("Indexing file " + file.getFileName());
                    longestFile = Math.max(longestFile, file.getFileName().toString().length());
                    fileNames.add(file.getFileName().toString());
                    indexFile(file.toFile(), index, mDocumentID);
                    mDocumentID++;
                }
                return FileVisitResult.CONTINUE;
            }

            // don't throw exceptions if files are locked/other errors occur
            @Override
            public FileVisitResult visitFileFailed(Path file,
                    IOException e) {
                return FileVisitResult.CONTINUE;
            }

        });

//        printIndex(index, fileNames);
        index.indexFinalize();
        printStatistics();

        processQueries();
    }

    /**
     * Indexes a file by reading a series of tokens from the file, treating each
     * token as a term, and then adding the given document's ID to the inverted
     * index for the term.
     *
     * @param file a File object for the document to index.
     * @param index the current state of the index for the files that have
     * already been processed.
     * @param docID the integer ID of the current document, needed when indexing
     * each term from the document.
     */
    private static void indexFile(File file, PositionalInvertedIndex index,
            int docID) {
        SimpleTokenStream simpleTokenStream = null;
        try {
            simpleTokenStream = new SimpleTokenStream(file);
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            return;
        }
        long position = 0;
        while (simpleTokenStream.hasNextToken()) {
            String term = simpleTokenStream.nextToken(false);
            types.add(term);
            if (term.contains("-")) { // process term with '-'
                // for ab-xy -> store (abxy, ab, xy) all three
                // all with same position
                index.addTerm(porterstemmer.processToken(term.replaceAll("-", "")),
                        docID, position);
                String[] subtokens = term.split("-");
                for (String subtoken : subtokens) {
                    if (subtoken.length() > 0) {
                        index.addTerm(porterstemmer.processToken(subtoken),
                                docID, position);
                    }
                }
            } else {
                index.addTerm(porterstemmer.processToken(term), docID, position);
            }
            position++;
        }
    }

    private static void printStatistics() {
        System.out.println("Number of Terms: " + index.getTermCount());
        System.out.println("Number of Types (distinct tokens): " + types.size());
        System.out.println("Average number of documents per term: "
                + ((double) index.getTotalDocumentCount()) / index.getTermCount());
        System.out.println("10 most frequent words statistics...");
        ArrayList<String> mostFrequentTerms = index.mostFrequentTerms(100);
        System.out.println("\t" + mostFrequentTerms);
        System.out.print("\t[");
        for (String key : mostFrequentTerms) {
            System.out.printf("%.2f, ", (double) index.getPostings(key).size() / mDocumentID);
        }
        System.out.println("\b\b]");
        System.out.println("Approximate total memory requirement: " + index.getTotalMemory() + "bytes");
    }

    private static void processQueries() {
        Scanner scan = new Scanner(System.in);
        String query;
        // set of all keys
        while (true) {
            System.out.print("Enter a query to search for: ");
            // remove extra space if any in query
            query = scan.nextLine().trim();
            if (query.equals("")) {
                System.out.println("Please Enter a search query!");
                continue;
            }
            if (query.equals("EXIT")) {
                System.out.println("Bye!");
                break;
            }
            TreeSet<String> result = new TreeSet<>();
            SimpleTokenStream querystream = new SimpleTokenStream(query);
            Set<String> negationTokens = new HashSet<>();
            while (querystream.hasNextToken()) {
                String token = querystream.nextToken(true);
                // check for phrase query
                if (token.startsWith("\"")) {
                    ArrayList<String> phrasetokens = new ArrayList<>();
                    phrasetokens.add(porterstemmer.
                            processToken(token.substring(1, token.length())));
                    boolean quit = false;
                    while (querystream.hasNextToken() && !quit) {
                        token = querystream.nextToken(true);
                        if (token.endsWith("\"")) {
                            phrasetokens.add(porterstemmer
                                    .processToken(token.substring(0, token.length() - 1)));
                            quit = true;
                        } else {
                            phrasetokens.add(porterstemmer.processToken(token));
                        }
                    }
                    // phrasetoken has all the tokens inorder to be processed
                    result.addAll(processPhrase(phrasetokens));
                } else if (token.startsWith("-")) {
                    negationTokens.add(
                            porterstemmer.processToken(token.substring(1, token.length())));
                } else {
                    result.addAll(processOR(porterstemmer.processToken(token)));
                }
            }
            if (!negationTokens.isEmpty()) {
                result.removeAll(processNegation(negationTokens));
            }
            if (result.isEmpty()) {
                System.out.println("No documents contain that term..!\n");
                continue;
            }
            System.out.println("These documents contain that term:");
            result.stream().forEach((String doc) -> {
                System.out.print(doc + " ");
            });
            System.out.print("\b\n\n");
        }
    }

    /**
     * process NOT queries - tokens containing '-'
     *
     * @param negationTokens
     */
    private static Set<String> processNegation(Set<String> negationTokens) {
        Set<Integer> docIds = new HashSet<>();
        Iterator<String> nToken = negationTokens.iterator();
        while (nToken.hasNext()) {
            docIds.addAll(index.getPostings(nToken.next()).keySet());
        }
        Iterator<Integer> iterator = docIds.iterator();
        Set<String> nDocument = new HashSet<>();
        while (iterator.hasNext()) {
            nDocument.add(fileNames.get(iterator.next()));
        }
        return nDocument;
    }

    /**
     * process the phrase query containing 'phrasetokens' tokens
     *
     * @param phrasetokens
     * @return
     */
    private static Set<String> processPhrase(ArrayList<String> phrasetokens) {
        TreeSet<String> result = new TreeSet<>();
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
                result.add(fileNames.get(docId));
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
    private static HashMap<Integer, Set<Long>> matchTerms(String term1, String term2) {
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

    /**
     * searches for documents which contains token1 or token2
     *
     * @param token1
     * @param token2
     * @return List of documentIds
     */
    private static Set<String> processOR(String token) {
        Set<String> documentIds = new HashSet<>();
        Set<Integer> docIds = index.getPostings(token).keySet();
        docIds.stream().forEach((Integer id) -> {
            documentIds.add(fileNames.get(id));
        });
        return documentIds;
    }

    // prints a bunch of spaces
    private static void printSpaces(int spaces) {
        for (int i = 0; i < spaces; i++) {
            System.out.print(" ");
        }
    }

    private static void printIndex(PositionalInvertedIndex index,
            ArrayList<String> fileNames) {

        String[] dictionary = index.getDictionary();

        // find the longest word length in the index
        int longestWord = index.getLongestWordLength();

        for (String term : dictionary) {
            System.out.print(term + ": ");
            printSpaces(longestWord - term.length());
            TreeMap<Integer, ArrayList<Long>> postings = index.getPostings(term);
            postings.keySet().stream().map((docId) -> {
                String file = fileNames.get(docId);
                System.out.print("{" + file);
                printSpaces(longestFile - file.length());
                System.out.print(" ");
                return docId;
            }).map((docId) -> postings.get(docId)).map((positions) -> {
                System.out.print(":<");
                return positions;
            }).map((positions) -> {
                positions.stream().forEach((position) -> {
                    System.out.print(position + ",");
                });
                return positions;
            }).forEach((_item) -> {
                System.out.print("\b>} ");
            });
            System.out.print("\b\n");
        }
    }

}

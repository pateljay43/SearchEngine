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
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;

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

//        printIndex(index);
        index.indexFinalize();
        printStatistics();
        processQueries();
    }

    /**
     * starts waiting for queries from user and gives response accordingly until
     * user types 'EXIT'
     */
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
            QueryProcessor queryProcessor = new QueryProcessor(index);
            Set<Integer> resultDocIDs = queryProcessor.processQuery(query);
            if (resultDocIDs.isEmpty()) {
                System.out.println("No documents satisfies that query..!\n");
                continue;
            }
            System.out.println("These documents satisfies that query:");
            resultDocIDs.stream().forEach((Integer docId) -> {
                System.out.print(fileNames.get(docId) + " ");
            });
            System.out.print("\b\n\n");
        }
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
            String term = simpleTokenStream.nextToken();
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

    /**
     * # of Terms, # of Types, Average number of documents per term, 10 most
     * frequent terms, Approximate total memory required by index
     */
    private static void printStatistics() {
        System.out.println("Number of Terms: " + index.getTermCount());
        System.out.println("Number of Types (distinct tokens): " + types.size());
        System.out.println("Average number of documents per term: "
                + ((double) index.getTotalDocumentCount()) / index.getTermCount());
        System.out.println("10 most frequent words statistics...");
        ArrayList<String> mostFrequentTerms = index.mostFrequentTerms(10);
        System.out.println("\t" + mostFrequentTerms);
        System.out.print("\t[");
        for (String key : mostFrequentTerms) {
            System.out.printf("%.2f, ", (double) index.getPostings(key).size() / mDocumentID);
        }
        System.out.println("\b\b]");
        System.out.println("Approximate total memory requirement: " + index.getTotalMemory() + "bytes");
    }

    // prints a bunch of spaces
    private static void printSpaces(int spaces) {
        for (int i = 0; i < spaces; i++) {
            System.out.print(" ");
        }
    }

    /**
     * print index details
     *
     * @param index positional index for the selected corpus
     */
    private static void printIndex(PositionalInvertedIndex index) {

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

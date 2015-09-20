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
import java.util.Arrays;
import java.util.Scanner;

/**
 *
 * @author JAY
 */
public class SearchEngine {

    private static PorterStemmer porterstemmer;
    private static int longestFile = 0;

    public static void main(String[] args) throws IOException {
        final String folderName = "Search Space";
        final Path currentWorkingPath = Paths.get(folderName).toAbsolutePath();
        // the inverted index
        porterstemmer = new PorterStemmer();
        final NaiveInvertedIndex index = new NaiveInvertedIndex();

        // the list of file names that were processed
        final ArrayList<String> fileNames = new ArrayList<String>();

        // This is our standard "walk through all .txt files" code.
        Files.walkFileTree(currentWorkingPath, new SimpleFileVisitor<Path>() {
            int mDocumentID = 0;

            public FileVisitResult preVisitDirectory(Path dir,
                    BasicFileAttributes attrs) {
                // make sure we only process the current working directory
                if (currentWorkingPath.equals(dir)) {
                    return FileVisitResult.CONTINUE;
                }
                return FileVisitResult.SKIP_SUBTREE;
            }

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
            public FileVisitResult visitFileFailed(Path file,
                    IOException e) {

                return FileVisitResult.CONTINUE;
            }

        });

//        printResults(index, fileNames);
        Scanner scan = new Scanner(System.in);
        String input;
        String[] dictionary = index.getDictionary();;
        ArrayList<Integer> postings;
        while (true) {
            System.out.print("Enter a term to search for: ");
            input = scan.nextLine().trim().toLowerCase();
            if (input.equalsIgnoreCase("quit")) {
                System.out.println("Bye!");
                break;
            }
            input = porterstemmer.processToken(input);
            int searchIndex = Arrays.binarySearch(dictionary, input);
            if (searchIndex < 0) {
                System.out.println("No documents contain that term..!\n");
                continue;
            }
            System.out.println("These documents contain that term:");
            postings = index.getPostings(dictionary[searchIndex]);
            for (Integer doc : postings) {
                System.out.print(fileNames.get(doc) + " ");
            }
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
    private static void indexFile(File file, NaiveInvertedIndex index,
            int docID) {
        SimpleTokenStream simpleTokenStream = null;
        try {
            simpleTokenStream = new SimpleTokenStream(file);
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
        while (simpleTokenStream.hasNextToken()) {
            String stemmedToken = porterstemmer.processToken(simpleTokenStream.nextToken().toLowerCase());
            index.addTerm(stemmedToken, docID);
        }
    }

    private static void printResults(NaiveInvertedIndex index,
            ArrayList<String> fileNames) {
        
        String[] dictionary = index.getDictionary();

        // find the longest word length in the index
        int longestWord = index.getLongestWordLength();

        for (String term : dictionary) {
            System.out.print(term + ": ");
            printSpaces(longestWord - term.length());

            ArrayList<Integer> postings = index.getPostings(term);
            for (Integer docId : postings) {
                String file = fileNames.get(docId);
                System.out.print(file);
                printSpaces(longestFile - file.length());
                System.out.print(" ");;
            }
            System.out.print("\b\n");
        }
    }

    // prints a bunch of spaces
    private static void printSpaces(int spaces) {
        for (int i = 0; i < spaces; i++) {
            System.out.print(" ");
        }
    }
}

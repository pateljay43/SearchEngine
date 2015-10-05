/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package searchengine;

import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JFileChooser;

/**
 *
 * @author JAY
 */
public class SearchEngine {

    private static PorterStemmer porterstemmer;
    private static int longestFile = 0;
    private static int mDocumentID = 0;
    private static final Set<String> types = new HashSet<>();
    private static PositionalInvertedIndex index;
    private static ArrayList<String> fileNames;
//    private static final String folderName = "Search Space";
    private static Path currentWorkingPath;

    public static void main(String[] args) throws IOException {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new java.io.File(""));
        chooser.setDialogTitle("Select any directory with text files");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setVisible(true);
        // selected directory
        System.out.println("started");
        File selectedDirectory = null;
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            selectedDirectory = chooser.getSelectedFile();
//            System.out.println("getSelectedFile() : " + chooser.getSelectedFile());
        } else {
            System.out.println("No Selection ");
            System.exit(0);
        }
        currentWorkingPath = Paths.get(selectedDirectory.getPath()).toAbsolutePath();
        // the inverted index
        porterstemmer = new PorterStemmer();
        index = new PositionalInvertedIndex();

        // the list of file names that were processed
        fileNames = new ArrayList<>();

        long sTime = System.nanoTime();
        System.out.println("Indexing Files.....\n");
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
//                    System.out.println("Indexing file " + file.getFileName());
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
        System.out.println("Indexing Completed!");

        // 10 most frequent terms
        index.indexFinalize(10);
        System.out.println("Elapsed Time:");
        System.out.println("\t" + BigDecimal.valueOf(((double) System.nanoTime() - sTime) / 1000000000)
                + " seconds");

        // start GUI for searching
        GUI gui = new GUI(index, fileNames);
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

    public static Set<String> getTypes() {
        return types;
    }

    public static int getmDocumentID() {
        return mDocumentID;
    }

    public static Path getCurrentWorkingPath() {
        return currentWorkingPath;
    }
}

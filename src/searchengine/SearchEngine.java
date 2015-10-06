/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package searchengine;

import java.awt.Cursor;
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
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.WindowConstants;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author JAY
 */
public class SearchEngine {

    private PorterStemmer porterstemmer;
    private int longestFile;
    static int mDocumentID;
    private Set<String> types;
    private PositionalInvertedIndex index;
    private ArrayList<String> fileNames;
//    private static final String folderName = "Search Space";
    private Path currentWorkingPath;
    private JFileChooser directoryPicker;
    private GUI UI;
    private JProgressBar pb;
    private JFrame frame;

    public SearchEngine() {
        initVariables();
        selectSearchSpace();
        createIndex();
        createUI();
    }

    public static void main(String[] args) throws IOException {
        SearchEngine searchEngine;
        while (true) {
            searchEngine = new SearchEngine();
            while (!searchEngine.UI.isChangeIndex()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SearchEngine.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (searchEngine.UI.isQuit()) {
                System.exit(0);
            }
            searchEngine.UI.dispose();
            try {
                searchEngine.finalize();
            } catch (Throwable ex) {
                Logger.getLogger(SearchEngine.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void initVariables() {
        longestFile = 0;
        mDocumentID = 0;
        types = new HashSet<>();
        directoryPicker = new JFileChooser();
        porterstemmer = new PorterStemmer();
        index = new PositionalInvertedIndex(this);
        fileNames = new ArrayList<>();
    }

    private void selectSearchSpace() {
        frame = new JFrame("Scanning Directory");
        frame.add(directoryPicker);
        frame.setSize(0, 0);
        frame.setLayout(null);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.setVisible(true);
        frame.setVisible(false);
        directoryPicker.setCurrentDirectory(new java.io.File(""));
        directoryPicker.setDialogTitle("Select any directory with text files");
        directoryPicker.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        directoryPicker.setAcceptAllFileFilterUsed(false);
        directoryPicker.setVisible(true);
        // selected directory
        File selectedDirectory = null;
        if (directoryPicker.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            selectedDirectory = directoryPicker.getSelectedFile();
        } else {
            System.exit(0);
        }
        frame.dispose();
        currentWorkingPath = Paths.get(selectedDirectory.getPath()).toAbsolutePath();
    }

    private void createIndex() {
        File dir = new File(currentWorkingPath.toString());
        String[] extensions = new String[]{"txt"};
        List<File> files = (List<File>) FileUtils.listFiles(dir, extensions, true);
        long sTime = System.nanoTime();
        pb = new JProgressBar(0, files.size());
        pb.setStringPainted(true);
        pb.setBorderPainted(true);
        pb.setBounds(10, 10, 370, 25);
        pb.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        int offset = System.getProperty("os.name").toLowerCase().contains("windows") ? 30 : 20;
        frame = new JFrame("Scanning Directory");
        frame.add(pb);
        frame.setSize(400, 45 + offset);
        frame.setLayout(null);
        frame.setResizable(false);
        frame.setVisible(true);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        try {
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
                        longestFile = Math.max(longestFile, file.getFileName().toString().length());
                        fileNames.add(file.getFileName().toString());
                        indexFile(file.toFile(), index, mDocumentID);
                        mDocumentID++;
                        pb.setValue(mDocumentID);
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
        } catch (IOException ex) {
            Logger.getLogger(SearchEngine.class.getName()).log(Level.SEVERE, null, ex);
        }
        frame.dispose();

        // 10 most frequent terms
        index.indexFinalize(10);
        System.out.println("Elapsed Time:");
        System.out.println("\t" + BigDecimal.valueOf(((double) System.nanoTime() - sTime) / 1000000000)
                + " seconds");
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
    private void indexFile(File file, PositionalInvertedIndex index,
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

    private void createUI() {
        UI = new GUI(this.index, this.fileNames, this.currentWorkingPath, this.types.size());
    }

}

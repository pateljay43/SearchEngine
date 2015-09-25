package searchengine;

import java.io.*;
import java.util.*;

/**
 * Reads tokens one at a time from an input stream. Returns tokens with minimal
 * processing: removing all non-alphanumeric characters, and converting to
 * lowercase.
 */
public class SimpleTokenStream implements TokenStream {

    private final Scanner mReader;

    /**
     * Constructs a SimpleTokenStream to read from the specified file.
     *
     * @param fileToOpen stream tokens from fileToOpen
     */
    public SimpleTokenStream(File fileToOpen) throws FileNotFoundException {
        mReader = new Scanner(new FileReader(fileToOpen));
    }

    /**
     * Constructs a SimpleTokenStream to read from a String of text.
     *
     * @param text stream tokens from given text
     */
    public SimpleTokenStream(String text) {
        mReader = new Scanner(text.replaceAll("\\+", " + ").replaceAll("(( )( )+)", " "));
    }

    /**
     * Returns true if the stream has tokens remaining.
     *
     * @return true if the scanner has next token, else false
     */
    @Override
    public boolean hasNextToken() {
        return mReader.hasNext();
    }

    /**
     * Returns the next token from the stream, or null if there is no token
     * available.
     *
     * @param query is this stream for query
     * @return next token
     */
    @Override
    public String nextToken(boolean query) {
        if (!hasNextToken()) {
            return null;
        }
        // remove any non-alphanumeric excluding '-'
        String next = mReader.next();

        if (!query) {    // remove any preceding '-'
            next = next.replaceAll("[^A-Za-z0-9-]", "").toLowerCase();
            while (next.startsWith("-")) {
                next = next.replaceFirst("-", "");
            }
            // remove any '-' at the end
            while (next.endsWith("-")) {
                next = next.substring(0, next.lastIndexOf("-"));
            }
        } else {
            next = next.replaceAll("[^A-Za-z0-9-+ \"]", "").toLowerCase();
        }
        if (next.length() > 0) {
            return next;
        } else if (hasNextToken()) {
            next = nextToken(query);
        }
        return next;
    }
}

package searchengine;

/**
 * TokenStreams read tokens one at a time from a stream of input.
 */
public interface TokenStream {

    /**
     * Returns the next token from the stream, or null if there is no token
     * available.
     *
     * @return single token
     */
    String nextToken();

    /**
     * Returns true if the stream has tokens remaining.
     *
     * @return true if stream has next token available, else false
     */
    boolean hasNextToken();
}

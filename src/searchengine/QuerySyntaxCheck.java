/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package searchengine;

/**
 *
 * @author metehan
 */
public class QuerySyntaxCheck {

    private String errorMessage = "valid";

//    public QuerySyntaxCheck() {
//        errorMessage = "valid";
//    }
    // returns a success or error message for the parenthesization of the query
    public String checkParenthesis(String query) {
        boolean isParenthesis = true;
        errorMessage = "valid";
        for (int i = 0; i < query.length(); i++) {
            char c = query.charAt(i);
            if (c == '(') {
                if (isParenthesis) {
                    isParenthesis = false;
                } else {
                    errorMessage = "There shouldn't be nested parenthesis";
                    return errorMessage;
                }
            } else if (c == ')') {
                if (isParenthesis) {
                    errorMessage = "Invalid parenthesization";
                    return errorMessage;
                } else {
                    isParenthesis = true;
                }
            }
        }
        if (!isParenthesis) {
            errorMessage = "Invalid parenthesization";
        }
        return errorMessage;
    }

    // checks if query has quotes properly closed
    public String checkQuotes(String query) {
        boolean isQuote = true;
        errorMessage = "valid";
        for (int i = 0; i < query.length(); i++) {
            char c = query.charAt(i);
            if (!isQuote && ("" + c).matches("[+)(-]")) {
                break;
            } else if (c == '"') {
                isQuote = !isQuote;
            }
        }
        if (!isQuote) {
            errorMessage = "Double Quotes not used correctly";
        }
        return errorMessage;
    }

    // returns a success or error message that reports if query contains no empty sequence of literals
    public String checkNoEmptyQ(String query) {
        query = query.replaceAll("[^A-Za-z0-9-+ \"]", "");
        errorMessage = "valid";
        if (query.endsWith("+") || query.endsWith("-")) {
            errorMessage = "Query contains an empty sequence of literals";
            return errorMessage;
        }
        if (query.contains("+")) {
            String[] split = query.split("\\+");
            for (String queryLiteral : split) {
                queryLiteral = queryLiteral.trim();
                if (!(queryLiteral.length() > 0)) {
                    errorMessage = "Query contains an empty sequence of literals";
                    return errorMessage;
                }
            }
        }
        return errorMessage;
    }

    // returns a success or error message that reports if each Q_i has at least one positive literal
    public String checkOnePosLit(String query) {
        boolean isPos = true;
        errorMessage = "valid";
        query = query.replaceAll("[^A-Za-z0-9-+)( \"]", "");
        // get Q_i: sequence of literals
        String[] split = query.split("\\+");
        for (String queryLiteralSeq : split) {
            if (!isPos) {
                errorMessage = "Each sequence of literals must contain at least one positive literal";
                return errorMessage;
            }
            isPos = false;
            if (queryLiteralSeq.contains("-")) {
                String[] splitLiteralSeq = queryLiteralSeq.split("\\s+");
                for (String queryLiteral : splitLiteralSeq) {
                    // check if there is a positive literal
                    if (!queryLiteral.contains("-") && queryLiteral.trim().length() > 0) {
                        isPos = true;
                        break;
                    }
                }
            } else if (queryLiteralSeq.trim().length() > 0) {
                isPos = true;
            }
        }
        if (!isPos) {
            errorMessage = "Each sequence of literals must contain at least one positive literal";
            return errorMessage;
        }
        return errorMessage;
    }

    // true if query passed all three syntax checking conditions
    public boolean isValidQuery(String query) {
        String message1 = checkParenthesis(query);
        if (!message1.equals("valid")) {
            return false;
        }
        String message2 = checkNoEmptyQ(query);
        if (!message2.equals("valid")) {
            return false;
        }
        String message3 = checkOnePosLit(query);
        if (!message3.equals("valid")) {
            return false;
        }
        String message4 = checkQuotes(query);
        if (!message4.equals("valid")) {
            return false;
        }
        return true;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}

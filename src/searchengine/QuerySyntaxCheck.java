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

    // checks if no empty Q_i
    boolean isNoEmptyQ;
    // checks if each Q_i has at least one positive literal
    boolean isOnePost;

    // returns a success or error message for the parenthesization of the query
    public String checkParenthesis(String query) {
        boolean isParenthesis = true;
        String message = "valid";
        for (int i = 0; i < query.length(); i++) {
            char c = query.charAt(i);
            if (c == '(') {
                if (isParenthesis) {
                    isParenthesis = false;
                } else {
                    message = "There shouldn't be nested parenthesis";
                    return message;
                }
            } else if (c == ')') {
                if (isParenthesis) {
                    message = "Invalid parenthesization";
                    return message;
                } else {
                    isParenthesis = true;
                }
            }
        }
        if (!isParenthesis) {
            message = "Invalid parenthesization";
        }
        return message;
    }

    // returns a success or error message that reports if query contains no empty sequence of literals
    public String checkNoEmptyQ(String query) {
        String message = "valid";
        query = query.replaceAll("[^A-Za-z0-9-+ \"]", "");
        if (query.endsWith("+") || query.endsWith("-")) {
            message = "Query contains an empty sequence of literals";
            return message;
        }
        if (query.contains("+")) {
            String[] split = query.split("\\+");
            for (String queryLiteral : split) {
                queryLiteral = queryLiteral.trim();
                if (!(queryLiteral.length() > 0)) {
                    message = "Query contains an empty sequence of literals";
                    return message;
                }
            }
        }
        return message;
    }

    // returns a success or error message that reports if each Q_i has at least one positive literal
    public String checkOnePosLit(String query) {
        String message = "valid";
        boolean isPos = true;
        query = query.replaceAll("[^A-Za-z0-9-+)( \"]", "");
        // get Q_i: sequence of literals
        String[] split = query.split("\\+");
        for (String queryLiteralSeq : split) {
            if (!isPos) {
                message = "Each sequence of literals must contain at least one positive literal";
                return message;
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
            message = "Each sequence of literals must contain at least one positive literal";
            return message;
        }
        return message;
    }

    // true if query passed all three syntax checking conditions
    public boolean isValidQuery(String query) {
        String message1 = checkParenthesis(query);
        String message2 = checkNoEmptyQ(query);
        String message3 = checkOnePosLit(query);
        if (!message1.equals("valid")) {
//            System.out.println(message1 + "\n");
            return false;
        } else if (!message2.equals("valid")) {
//            System.out.println(message2 + "\n");
            return false;
        } else if (!message3.equals("valid")) {
//            System.out.println(message3 + "\n");
            return false;
        }
        return true;
    }
}

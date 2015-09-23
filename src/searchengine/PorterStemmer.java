/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package searchengine;

import java.util.Scanner;
import java.util.regex.*;

public class PorterStemmer {

    // a single consonant
    private static final String c = "[^aeiou]";
    // a single vowel
    private static final String v = "[aeiouy]";

    // a sequence of consonants; the second/third/etc consonant cannot be 'y'
    private static final String C = c + "[^aeiouy]*";
    // a sequence of vowels; the second/third/etc cannot be 'y'
    private static final String V = v + "[aeiou]*";

    // this regex pattern tests if the token has measure > 0 [at least one VC].
    private static final Pattern mGr0 = Pattern.compile("^(" + C + ")?" + V + C);

    //private static final Pattern mGr0 = Pattern.compile("^(" + C + ")?" +
    //"(" + V + C + ")+(" + V + ")?");
    // add more Pattern variables for the following patterns:
    // m equals 1: token has measure == 1
    private static final Pattern mEq1 = Pattern.compile("^(" + C + ")?" + "(" + V + C + ")" + "(" + V + ")?$");
    // m greater than 1: token has measure > 1
    private static final Pattern mGr1 = Pattern.compile("^(" + C + ")?" + V + C + V + C);
    // vowel: token has a vowel after the first (optional) C
    private static final Pattern ConVow = Pattern.compile("^(" + V + ")?" + c + v);
    // double consonant: token ends in two consonants that are the same,
    private static final String last_c1 = "[^aeioulsz]$";
    private static final Pattern doubleCon = Pattern.compile("\\z(" + last_c1 + ")\1{2,}");
    //			unless they are L, S, or Z. (look up "backreferencing" to help 
    //			with this)
    // m equals 1, Cvc: token is in Cvc form, where the last c is not w, x, 
    //			or y.
    private static final String last_c2 = "[^aeiouwxy]$";
    private static final Pattern cvc = Pattern.compile("^(" + c + ")" + "(" + v + c + ")" + last_c2 + "$");
    // has a vowel
    private final static Pattern pV = Pattern.compile(v);

    public static void main(String[] args) {
        // Test Porter Stemmer 
        // Read a string from the keyboard
        Scanner scan = new Scanner(System.in);
        String s = scan.next();
        while (!s.equals("quit")) {
            // Print the stemmed string
            System.out.print(processToken(s) + " ");
            s = scan.next();
        }
        scan.close();
    }

    public static String processToken(String token) {
        //System.out.println(mGr0.pattern());
        if (token.length() < 3) {
            return token; // token must be at least 3 chars
        }
        // step 1a
        // program the other steps in 1a. 
        // note that Step 1a.3 implies that there is only a single 's' as the 
        //	suffix; ss does not count. you may need a regex pattern here for 
        // "not s followed by s".
        Pattern p_s = Pattern.compile("(.+[^s])s$");
        Matcher m_s = p_s.matcher(token);
        if (token.endsWith("sses")) {
            token = token.substring(0, token.length() - 2);
        } else if (token.endsWith("ies")) {
            token = token.substring(0, token.length() - 2);
        } else if (m_s.matches()) {
            token = token.substring(0, token.length() - 1);
        }

        // step 1b
        boolean doStep1bb = false;
        //		step 1b
        if (token.endsWith("eed")) { // 1b.1
            // token.substring(0, token.length() - 3) is the stem prior to "eed".
            // if that has m>0, then remove the "d".
            String stem = token.substring(0, token.length() - 3);
            if (mGr0.matcher(stem).find()) { // if the pattern matches the stem
                token = stem + "ee";
            }
        } //              step1b.2
        else if (token.endsWith("ed")) {
            doStep1bb = true;
            String stem = token.substring(0, token.length() - 2);
            if (pV.matcher(stem).find()) {
                token = stem;
            }
        } //             step1b.3
        else if (token.endsWith("ing")) {
            doStep1bb = true;
            String stem = token.substring(0, token.length() - 3);
            if (pV.matcher(stem).find()) {
                token = stem;
            }
        }

        // step 1b*, only if the 1b.2 or 1b.3 were performed.
        if (doStep1bb) {
            if (token.endsWith("at") || token.endsWith("bl")
                    || token.endsWith("iz")) {

                token = token + "e";
            } else if (doubleCon.matcher(token).find()) {
                token = token.substring(0, token.length() - 1);
            } else if (cvc.matcher(token).find()) {
                token = token + "e";
            }
            // use the regex patterns you wrote for 1b*.4 and 1b*.5
        }

        // step 1c
        // program this step. test the suffix of 'y' first, then test the 
        // condition *v*.
        if (token.endsWith("y")) {
            if (pV.matcher(token).find()) {
                token = token.substring(0, token.length() - 1) + "i";
            }
        }
        // step 2
        // program this step. for each suffix, see if the token ends in the 
        // suffix. 
        //		* if it does, extract the stem, and do NOT test any other suffix.
        //    * take the stem and make sure it has m > 0.
        //			* if it does, complete the step. if it does not, do not 
        //				attempt any other suffix.
        // you may want to write a helper method for this. a matrix of 
        // "suffix"/"replacement" pairs might be helpful. It could look like
        // string[][] step2pairs = {  new string[] {"ational", "ate"}, 
        // new string[] {"tional", "tion"}, ....
        String[][] step2pairs = {new String[]{"ational", "ate"}, new String[]{"tional", "tion"}, new String[]{"enci", "ence"}, new String[]{"anci", "ance"}, new String[]{"izer", "ize"}, new String[]{"abli", "able"}, new String[]{"alli", "al"}, new String[]{"entli", "ent"}, new String[]{"eli", "e"}, new String[]{"ousli", "ous"}, new String[]{"ization", "ize"}, new String[]{"ation", "ate"}, new String[]{"ator", "ate"}, new String[]{"alism", "al"}, new String[]{"iveness", "ive"}, new String[]{"fulness", "ful"}, new String[]{"ousness", "ous"}, new String[]{"aliti", "al"}, new String[]{"iviti", "ive"}, new String[]{"biliti", "ble"}};

        for (int i = 0; i < step2pairs.length; i++) {
            if (token.endsWith(step2pairs[i][0])) {
                int length = step2pairs[i][0].length();
                String stem = token.substring(0, token.length() - length);
                if (mGr0.matcher(stem).find()) {
                    token = stem + step2pairs[i][1];
                    break;
                }
            }
        }

        // step 3
        // program this step. the rules are identical to step 2 and you can use
        // the same helper method. you may also want a matrix here.
        String[][] step3pairs = {new String[]{"icate", "ic"}, new String[]{"ative", ""}, new String[]{"alize", "al"}, new String[]{"iciti", "ic"}, new String[]{"ical", "ic"}, new String[]{"ful", ""}, new String[]{"ness", ""}};
        for (int i = 0; i < step3pairs.length; i++) {
            if (token.endsWith(step3pairs[i][0])) {
                int length = step3pairs[i][0].length();
                String stem = token.substring(0, token.length() - length);
                if (mGr0.matcher(stem).find()) {
                    token = stem + step3pairs[i][1];
                    break;
                }
            }
        }
        // step 4
        // program this step similar to step 2/3, except now the stem must have
        // measure > 1.
        // note that ION should only be removed if the suffix is SION or TION, 
        // which would leave the S or T.
        // as before, if one suffix matches, do not try any others even if the 
        // stem does not have measure > 1.
        String[][] step4pairs = {new String[]{"al", ""}, new String[]{"ance", ""}, new String[]{"ence", ""}, new String[]{"er", ""}, new String[]{"ic", ""}, new String[]{"able", ""}, new String[]{"ible", ""}, new String[]{"ant", ""}, new String[]{"ement", ""}, new String[]{"ment", ""}, new String[]{"ent", ""}, new String[]{"sion", "s"}, new String[]{"tion", "t"}, new String[]{"ou", ""}, new String[]{"ism", ""}, new String[]{"ate", ""}, new String[]{"iti", ""}, new String[]{"ous", ""}, new String[]{"ive", ""}, new String[]{"ize", ""}};
        for (int i = 0; i < step4pairs.length; i++) {
            if (token.endsWith(step4pairs[i][0])) {
                int length = step4pairs[i][0].length();
                String stem = token.substring(0, token.length() - length);
                if (mGr1.matcher(stem).find()) {
                    token = stem + step4pairs[i][1];
                    break;
                }
            }
        }
        // step 5
        // program this step. you have a regex for m=1 and for "Cvc", which
        // you can use to see if m=1 and NOT Cvc.
        if (mGr1.matcher(token).find()) {
            if (token.endsWith("e")) {
                token = token.substring(0, token.length() - 1);
            }
        } else if (mEq1.matcher(token).find() && !cvc.matcher(token).find()) {
            if (token.endsWith("e")) {
                token = token.substring(0, token.length() - 1);
            }
        }
        if (mGr1.matcher(token).find() && doubleCon.matcher(token).find()) {
            if (token.endsWith("ll")) {
                token = token.substring(0, token.length() - 1);
            }
        }
        // all your code should change the variable token, which represents
        // the stemmed term for the token.
        return token;
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package searchengine;

import java.util.HashMap;
import java.util.regex.*;

public class PorterStemmer {

    /**
     * key - penultimate letter of suffix. value - array of suffix,replacement
     * pairs lined together.
     */
    private final HashMap<Character, String[]> step2pairs;
    /**
     * key - last letter of suffix. value - array of suffix,replacement pairs
     * lined together.
     */
    private final HashMap<Character, String[]> step3pairs;
    /**
     * key - penultimate letter of suffix. value - array of suffix,replacement
     * pairs lined together.
     */
    private final HashMap<Character, String[]> step4pairs;
    // a single consonant
    private final String c;
    // a single vowel
    private final String v;

    // a sequence of consonants; the second/third/etc consonant cannot be 'y'
    private final String C;
    // a sequence of vowels; the second/third/etc cannot be 'y'
    private final String V;

    // this regex pattern tests if the token has measure > 0 [at least one VC].
    private final Pattern mGr0;

    //private final Pattern mGr0 = Pattern.compile("^(" + C + ")?" +
    //"(" + V + C + ")+(" + V + ")?");
    // add more Pattern variables for the following patterns:
    // m equals 1: token has measure == 1
    private final Pattern mEq1;
    // m greater than 1: token has measure > 1
    private final Pattern mGr1;
    // vowel: token has a vowel after the first (optional) C
    private final Pattern containVowel;
    // double consonant: token ends in two consonants that are the same,
    //			unless they are L, S, or Z. (look up "backreferencing" to help 
    //			with this)
    private final String dC;
//    private final Pattern doubleCon = Pattern.compile("(?i)\\w+(?:(?![aeioulsz])[a-z]){2}$");
    private final Pattern notLSZ;
    // m equals 1, Cvc: token is in Cvc form, where the last c is not w, x, 
    //			or y.
//    private final String last_c2 = "[^aeiouwxy]$";
    private final Pattern o;
    // has a vowel
//    private final Pattern pV = Pattern.compile(v);

    public PorterStemmer() {
        c = "[^aeiou]";
        v = "[aeiouy]";
        C = c + "[^aeiouy]*";
        V = v + "[aeiou]*";
        mGr0 = Pattern.compile("^(" + C + ")?" + V + C);
        mEq1 = Pattern.compile("^(" + C + ")?" + "(" + V + C + ")" + "(" + V + ")?$");
        mGr1 = Pattern.compile("^(" + C + ")?" + V + C + V + C);
        containVowel = Pattern.compile("^(" + C + ")?(" + V + ")+.*");
        dC = "(.)*(" + c + ")\\2$";
        notLSZ = Pattern.compile(".*[^lsz]$");
        o = Pattern.compile("^(" + C + ")" + "(" + v + ")[^aeiouwxy]$");
        step2pairs = new HashMap() {
            {
                put('a', new String[]{"ational", "ate", "tional", "tion"});
                put('c', new String[]{"enci", "ence", "anci", "ance"});
                put('e', new String[]{"izer", "ize"});
                put('l', new String[]{"abli", "able", "alli", "al", "entli", "ent", "eli", "e", "ousli", "ous"});
                put('o', new String[]{"ization", "ize", "ation", "ate", "ator", "ate"});
                put('s', new String[]{"alism", "al", "iveness", "ive", "fulness", "ful", "ousness", "ous"});
                put('t', new String[]{"aliti", "al", "iviti", "ive", "biliti", "ble"});
            }
        };
        step3pairs = new HashMap() {
            {
                put('e', new String[]{"icate", "ic", "ative", "", "alize", "al"});
                put('i', new String[]{"iciti", "ic"});
                put('l', new String[]{"ical", "ic", "ful", ""});
                put('s', new String[]{"ness", ""});
            }
        };
        step4pairs = new HashMap() {
            {
                put('a', new String[]{"al", ""});
                put('c', new String[]{"ance", "", "ence", ""});
                put('e', new String[]{"er", ""});
                put('i', new String[]{"ic", ""});
                put('l', new String[]{"able", "", "ible", ""});
                put('n', new String[]{"ant", "", "ement", "", "ment", "", "ent", ""});
                put('o', new String[]{"sion", "", "tion", "", "ou", ""});
                put('s', new String[]{"ism", ""});
                put('t', new String[]{"ate", "", "iti", ""});
                put('u', new String[]{"ous", ""});
                put('v', new String[]{"ive", ""});
                put('z', new String[]{"ize", ""});
            }
        };
    }

    public String processToken(String token) {
        if (token.length() < 3) {
            return token; // token must be at least 3 chars
        }
        // step 1a
        Pattern p_s = Pattern.compile("(.+[^s])s$");
        if (token.endsWith("sses") || token.endsWith("ies")) {
            token = token.substring(0, token.length() - 2);
        } else if (p_s.matcher(token).find()) {
            token = token.substring(0, token.length() - 1);
        }

        // step 1b
        boolean doStep1bb = false;
        if (token.endsWith("eed")) { // 1b.1
            // token.substring(0, token.length() - 3) is the stem prior to "eed".
            // if that has m>0, then remove the "d".
            String stem = token.substring(0, token.length() - 3);
            if (mGr0.matcher(stem).find()) { // if the pattern matches the stem
                token = stem + "ee";
            }
        } else if (token.endsWith("ed")) {
            doStep1bb = true;
            String stem = token.substring(0, token.length() - 2);
            if (containVowel.matcher(stem).find()) {
                token = stem;
            }
        } else if (token.endsWith("ing")) {
            doStep1bb = true;
            String stem = token.substring(0, token.length() - 3);
            if (containVowel.matcher(stem).find()) {
                token = stem;
            }
        }

        // step 1b*, only if the 1b.2 or 1b.3 were performed.
        if (doStep1bb) {
            if (token.endsWith("at") || token.endsWith("bl")
                    || token.endsWith("iz")
                    || (mEq1.matcher(token).find() && o.matcher(token).find())) {
                token = token + "e";
            } else if (token.matches(dC) && notLSZ.matcher(token).find()) {
                token = token.substring(0, token.length() - 1);
            }
        }

        // step 1c
        if (token.endsWith("y")) {
            String stem = token.substring(0, token.length() - 1);
            if (containVowel.matcher(stem).find()) {
                token = stem + "i";
            }
        }
        // step 2
        token = stepHelper(token, step2pairs, mGr0, 2, false);

        // step 3
        token = stepHelper(token, step3pairs, mGr0, 1, false);

        // step 4
        token = stepHelper(token, step4pairs, mGr1, 2, true);

        // step 5
        if (token.endsWith("e")) {
            String stem = token.substring(0, token.length() - 1);
            if (mGr1.matcher(stem).find() || (mEq1.matcher(stem).find() && !o.matcher(stem).find())) {
                token = stem;
            }
        }
        if (mGr1.matcher(token).find() && token.matches(dC) && token.endsWith("l")) {
            token = token.substring(0, token.length() - 1);
        }

        return token.trim();
    }

    /**
     *
     * @param _token - token to be processed
     * @param _pairs - current step (suffix,replacement) pairs
     * @param _measure - measure to be compared with stem of token
     * @param _penultimatePos - position of token's character to be compared to
     * @param _exception - exception for *sion and *tion in step 4, s or t will
     * be included in stem before measure get (_pairs)
     * @return
     */
    private String stepHelper(String _token, HashMap<Character, String[]> _pairs,
            Pattern _measure, int _penultimatePos, boolean _exception) {
        if (_token.length() >= 4) {      // minimum length out of all suffix is 2 and for m>0 -> atleast 2 chars
            String[] pair = _pairs.getOrDefault(
                    _token.charAt(_token.length() - _penultimatePos),
                    new String[0]);
            for (int i = 0; i < pair.length; i = i + 2) {
                if (_token.endsWith(pair[i])) {
                    String stem = _token.substring(0, _token.length() - pair[i].length());
                    if (_exception && (pair[i].startsWith("s") || pair[i].startsWith("t"))) {
                        stem = stem + pair[i].charAt(0);
                    }
                    if (_measure.matcher(stem).find()) {
                        return (stem + pair[i + 1]);
                    }
                }
            }
        }
        return _token;
    }
}

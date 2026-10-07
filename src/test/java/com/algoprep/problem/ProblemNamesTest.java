package com.algoprep.problem;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProblemNamesTest {

    @Test
    void displayStripsLeadingZerosAndTitleCases() {
        assertEquals("1 - Two Sum", ProblemNames.displayForKey("0001_two-sum"));
        assertEquals("146 - Lru Cache", ProblemNames.displayForKey("0146_lru-cache"));
        assertEquals("1234 - Big", ProblemNames.displayForKey("1234_big"));
    }

    @Test
    void allZeroNumberIsZero() {
        assertEquals("0 - X", ProblemNames.displayForKey("0000_x"));
        assertEquals(0, ProblemNames.parseKey("0000_x").orElseThrow().number());
    }

    @Test
    void hyphensAndUnderscoresInTheSlugBecomeSpaces() {
        assertEquals("5 - Longest Palindromic Substring",
                ProblemNames.displayForKey("0005_longest-palindromic_substring"));
        assertEquals("3 - A B C", ProblemNames.displayForKey("0003_a--b__c"));
    }

    @Test
    void mixedCaseSlugIsNormalized() {
        assertEquals("7 - Two Sum", ProblemNames.displayForKey("0007_tWO-SUM"));
    }

    @Test
    void singleWordSlug() {
        assertEquals("9 - Palindrome", ProblemNames.displayForKey("0009_palindrome"));
    }

    @Test
    void parseKeyKeepsTheRestAsTheSlug() {
        ProblemNames.KeyParts p = ProblemNames.parseKey("0001_two-sum-ii").orElseThrow();
        assertEquals(1, p.number());
        assertEquals("two-sum-ii", p.slug());
    }

    @Test
    void keysThatDoNotMatchTheConventionDoNotParse() {
        assertTrue(ProblemNames.parseKey("two-sum").isEmpty());
        assertTrue(ProblemNames.parseKey("_two-sum").isEmpty());
        assertTrue(ProblemNames.parseKey("0001").isEmpty());
        assertTrue(ProblemNames.parseKey("0001_").isEmpty());
        assertTrue(ProblemNames.parseKey("0001_   ").isEmpty());
        assertTrue(ProblemNames.parseKey("abc_def").isEmpty());
        assertTrue(ProblemNames.parseKey("").isEmpty());
        assertTrue(ProblemNames.parseKey(null).isEmpty());
    }

    @Test
    void numberTooLargeForIntDoesNotParse() {
        assertTrue(ProblemNames.parseKey("99999999999999999999_big").isEmpty());
    }

    @Test
    void unparseableKeyIsShownUnchanged() {
        assertEquals("weird-name", ProblemNames.displayForKey("weird-name"));
    }
}

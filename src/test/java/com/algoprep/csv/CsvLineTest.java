package com.algoprep.csv;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvLineTest {

    @Test
    void splitsAtCommas() {
        assertEquals(List.of("a", "b"), CsvLine.parse("a,b"));
        assertEquals(List.of("a", "", "c"), CsvLine.parse("a,,c"));
        assertEquals(List.of("a", ""), CsvLine.parse("a,"));
        assertEquals(List.of("a"), CsvLine.parse("a"));
        assertEquals(List.of(""), CsvLine.parse(""));
    }

    @Test
    void handlesQuotes() {
        assertEquals(List.of("a,b", "c"), CsvLine.parse("\"a,b\",c"));
        assertEquals(List.of("say \"hi\"", "c"), CsvLine.parse("\"say \"\"hi\"\"\",c"));
    }

    @Test
    void anUnclosedQuoteGivesNull() {
        assertNull(CsvLine.parse("\"open,c"));
    }

    @Test
    void aQuoteInTheMiddleOfAFieldIsKeptAsText() {
        assertEquals(List.of("a\"b", "c"), CsvLine.parse("a\"b,c"));
    }
}

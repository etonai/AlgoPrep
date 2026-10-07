package com.algoprep.studylist;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StudyListParserTest {

    @TempDir
    Path tmp;

    private static StudyListEntry e(String key, String difficulty, String time) {
        return new StudyListEntry(key, difficulty, time);
    }

    @Test
    void theIdeasFourExampleLines() {
        var parsed = StudyListParser.parse(
                "0001_two-sum, Easy, 20 minutes\n"
                + "15_not-found, Tough, 15 minutes\n"
                + "99_nodifficulty, , 2 minutes\n"
                + "105_notime, Medium, \n");

        assertEquals(List.of(
                e("0001_two-sum", "Easy", "20 minutes"),
                e("15_not-found", "Tough", "15 minutes"),
                e("99_nodifficulty", "", "2 minutes"),
                e("105_notime", "Medium", "")), parsed.entries());
        assertEquals(0, parsed.skipped());
    }

    @Test
    void missingFieldsAreBlank() {
        var parsed = StudyListParser.parse("0001_a\n0002_b,Easy\n0003_c,,\n0004_d,Hard,\n");
        assertEquals(List.of(e("0001_a", "", ""), e("0002_b", "Easy", ""), e("0003_c", "", ""),
                e("0004_d", "Hard", "")), parsed.entries());
    }

    @Test
    void spacesAroundCommasAreIgnored() {
        var parsed = StudyListParser.parse("  0001_a  ,   Easy  ,  5 min  \n");
        assertEquals(List.of(e("0001_a", "Easy", "5 min")), parsed.entries());
    }

    @Test
    void blankLinesAndCommentsAreIgnoredAndNotCounted() {
        var parsed = StudyListParser.parse("# my list\n\n   \n0001_a,Easy,1\n  # another\n0002_b\n");
        assertEquals(2, parsed.entries().size());
        assertEquals(0, parsed.skipped());
    }

    @Test
    void quotedFieldsMayContainCommas() {
        var parsed = StudyListParser.parse("0001_a,\"Easy, really\",\"1, maybe 2 minutes\"\n");
        assertEquals(List.of(e("0001_a", "Easy, really", "1, maybe 2 minutes")), parsed.entries());
    }

    @Test
    void extraFieldsAreIgnored() {
        var parsed = StudyListParser.parse("0001_a,Easy,5 min,array,favorite\n");
        assertEquals(List.of(e("0001_a", "Easy", "5 min")), parsed.entries());
        assertEquals(0, parsed.skipped());
    }

    @Test
    void linesWithABlankKeyOrAnUnclosedQuoteAreSkippedAndCounted() {
        var parsed = StudyListParser.parse(", Easy, 5\n  ,,\n\"open,Easy\n0001_a,Easy\n");
        assertEquals(List.of(e("0001_a", "Easy", "")), parsed.entries());
        assertEquals(3, parsed.skipped());
    }

    @Test
    void windowsAndUnixLineEndings() {
        var parsed = StudyListParser.parse("0001_a,Easy\r\n0002_b,Hard\n0003_c\r\n");
        assertEquals(3, parsed.entries().size());
        assertEquals("Hard", parsed.entries().get(1).difficulty());
        assertEquals("", parsed.entries().get(2).difficulty(), "no stray carriage return");
    }

    @Test
    void byteOrderMarkIsDropped() {
        var parsed = StudyListParser.parse("\uFEFF0001_a,Easy\n");
        assertEquals("0001_a", parsed.entries().get(0).key());
    }

    @Test
    void aHeaderRowIsSkippedAndNotCounted() {
        for (String header : List.of(
                "problem,difficulty,time", "Problem, Difficulty, Time", "PROBLEM,DIFFICULTY,TIME",
                "key,difficulty,time", "problem,difficulty", "problem", "\uFEFFproblem,difficulty,time")) {
            var parsed = StudyListParser.parse(header + "\r\n0001_two-sum, Easy, 20 minutes\r\n");
            assertEquals(List.of(e("0001_two-sum", "Easy", "20 minutes")), parsed.entries(), header);
            assertEquals(0, parsed.skipped(), header);
        }
    }

    @Test
    void aHeaderAfterCommentsAndBlankLinesIsStillSkipped() {
        var parsed = StudyListParser.parse("# my list\n\nproblem,difficulty,time\n0001_a,Easy,1\n");
        assertEquals(List.of(e("0001_a", "Easy", "1")), parsed.entries());
        assertEquals(0, parsed.skipped());
    }

    @Test
    void onlyTheFirstLineCanBeAHeader() {
        var parsed = StudyListParser.parse("0001_a,Easy,1\nproblem,difficulty,time\n");
        assertEquals(2, parsed.entries().size(), "a later line is data, not a header");
        assertEquals("problem", parsed.entries().get(1).key());
    }

    @Test
    void aFirstLineThatIsARealProblemIsNotMistakenForAHeader() {
        var parsed = StudyListParser.parse("0001_problem, Easy, 5\nproblem-set_x\n");
        assertEquals(2, parsed.entries().size());
        assertEquals("0001_problem", parsed.entries().get(0).key());
    }

    @Test
    void aHeaderWithOtherColumnNamesIsNotRecognized() {
        // Only problem/key with difficulty is a header, so this is read as a (not found) problem
        var parsed = StudyListParser.parse("problem,hardness,time\n0001_a\n");
        assertEquals(2, parsed.entries().size());
    }

    @Test
    void emptyTextGivesAnEmptyList() {
        assertTrue(StudyListParser.parse("").entries().isEmpty());
        assertEquals(0, StudyListParser.parse("").skipped());
    }

    @Test
    void orderIsKeptAndDuplicatesAreKept() {
        var parsed = StudyListParser.parse("0010_z\n0002_a\n0010_z\n");
        assertEquals(List.of("0010_z", "0002_a", "0010_z"),
                parsed.entries().stream().map(StudyListEntry::key).toList());
    }

    @Test
    void extrasTextLeavesNoStrayCommas() {
        assertEquals(", Easy, 20 minutes", e("k", "Easy", "20 minutes").extrasText());
        assertEquals(", 2 minutes", e("k", "", "2 minutes").extrasText());
        assertEquals(", Medium", e("k", "Medium", "").extrasText());
        assertEquals("", e("k", "", "").extrasText());
    }

    // ---- reading the file ----

    @Test
    void loadReadsAFileAndToleratesInvalidUtf8() throws Exception {
        byte[] head = "0001_a,Easy\n0002_b,H".getBytes(StandardCharsets.UTF_8);
        byte[] tail = "rd\n".getBytes(StandardCharsets.UTF_8);
        byte[] all = new byte[head.length + 1 + tail.length];
        System.arraycopy(head, 0, all, 0, head.length);
        all[head.length] = (byte) 0xFF;
        System.arraycopy(tail, 0, all, head.length + 1, tail.length);
        Path file = tmp.resolve("list.csv");
        Files.write(file, all);

        var result = StudyListFile.load(file);

        assertTrue(result.ok());
        assertEquals(2, result.entries().size());
    }

    @Test
    void loadReportsAMissingFileADirectoryAndNeverThrows() throws Exception {
        var missing = StudyListFile.load(tmp.resolve("nope.csv"));
        assertFalse(missing.ok());
        assertTrue(missing.error().contains("not found"));
        assertTrue(missing.entries().isEmpty());

        var folder = StudyListFile.load(Files.createDirectory(tmp.resolve("folder.csv")));
        assertFalse(folder.ok());
        assertTrue(folder.error().contains("folder"));
    }

    @Test
    void anEmptyFileIsFineAndEmpty() throws Exception {
        Path file = Files.writeString(tmp.resolve("empty.csv"), "");
        var result = StudyListFile.load(file);
        assertTrue(result.ok());
        assertTrue(result.entries().isEmpty());
    }

    // ---- the tab title ----

    @Test
    void titleIsTheFileNameWithoutItsExtension() {
        assertEquals("Grind75", StudyListTitle.of(Path.of("C:\\lists\\Grind75.csv")));
        assertEquals("Grind75", StudyListTitle.of(Path.of("Grind75.CSV")));
        assertEquals("my.list.v2", StudyListTitle.of(Path.of("my.list.v2.csv")));
        assertEquals("noextension", StudyListTitle.of(Path.of("noextension")));
        assertEquals(".hidden", StudyListTitle.of(Path.of(".hidden")));
    }

    @Test
    void tabLabelShortensLongTitles() {
        assertEquals("Grind75", StudyListTitle.tabLabel("Grind75"));
        String shortened = StudyListTitle.tabLabel("A very long study list name indeed");
        assertEquals(20, shortened.length());
        assertTrue(shortened.endsWith("\u2026"));
    }
}

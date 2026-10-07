package com.algoprep.studied;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class StudiedStoreTest {

    @TempDir
    Path tmp;

    private final List<String> status = new ArrayList<>();
    private Path csv;
    private Clock clock;
    private StudiedStore store;

    private static Clock at(String date) {
        return Clock.fixed(Instant.parse(date + "T12:00:00Z"), ZoneOffset.UTC);
    }

    @BeforeEach
    void setUp() {
        csv = tmp.resolve(StudiedStore.FILE_NAME);
        clock = at("2026-10-07");
        store = new StudiedStore(clock, status::add);
        store.setFile(csv);
    }

    private String text() throws IOException {
        return Files.readString(csv, StandardCharsets.UTF_8);
    }

    @Test
    void fileForIsInsideHome() {
        assertEquals(Optional.of(Path.of("C:\\home").resolve("AlgoPrep_studied.csv")),
                StudiedStore.fileFor("C:\\home"));
        assertEquals(Optional.empty(), StudiedStore.fileFor(null));
        assertEquals(Optional.empty(), StudiedStore.fileFor("  "));
    }

    @Test
    void missingFileMeansNoRecords() {
        assertEquals(Optional.empty(), store.studiedOn("0001_two-sum"));
        assertTrue(status.isEmpty());
        assertFalse(Files.exists(csv), "loading never creates the file");
    }

    @Test
    void markStudiedWritesTodayAndRoundTrips() throws Exception {
        assertTrue(store.markStudied("0001_two-sum"));

        assertEquals(Optional.of(LocalDate.of(2026, 10, 7)), store.studiedOn("0001_two-sum"));
        assertEquals("key,date\r\n0001_two-sum,2026-10-07\r\n", text());

        StudiedStore again = new StudiedStore(clock, status::add);
        again.setFile(csv);
        assertEquals(Optional.of(LocalDate.of(2026, 10, 7)), again.studiedOn("0001_two-sum"));
    }

    @Test
    void keysMatchCaseInsensitively() {
        store.markStudied("0001_Two-Sum");
        assertTrue(store.studiedOn("0001_two-sum").isPresent());
    }

    @Test
    void markingAgainOnALaterDayOverwritesTheDate() throws Exception {
        store.markStudied("0001_two-sum");

        StudiedStore later = new StudiedStore(at("2026-10-20"), status::add);
        later.setFile(csv);
        assertTrue(later.markStudied("0001_two-sum"));

        assertEquals(Optional.of(LocalDate.of(2026, 10, 20)), later.studiedOn("0001_two-sum"));
        assertEquals("key,date\r\n0001_two-sum,2026-10-20\r\n", text(), "one row, not two");
    }

    @Test
    void markingTwiceTheSameDayChangesNothingAndDoesNotNotify() {
        AtomicInteger notified = new AtomicInteger();
        store.addListener(notified::incrementAndGet);

        store.markStudied("0001_two-sum");
        store.markStudied("0001_two-sum");

        assertEquals(1, notified.get());
    }

    @Test
    void clearRemovesOnlyThatRow() throws Exception {
        store.markStudied("0001_two-sum");
        store.markStudied("0002_add-two-numbers");

        assertTrue(store.clear("0001_two-sum"));

        assertEquals(Optional.empty(), store.studiedOn("0001_two-sum"));
        assertTrue(store.studiedOn("0002_add-two-numbers").isPresent());
        assertEquals("key,date\r\n0002_add-two-numbers,2026-10-07\r\n", text());
    }

    @Test
    void clearingAProblemWithNoRecordIsNotAnErrorAndDoesNotNotify() {
        AtomicInteger notified = new AtomicInteger();
        store.addListener(notified::incrementAndGet);

        assertTrue(store.clear("0009_none"));

        assertEquals(0, notified.get());
        assertTrue(status.isEmpty());
    }

    @Test
    void outputIsSortedByKey() throws Exception {
        store.markStudied("0010_b");
        store.markStudied("0002_a");
        store.markStudied("0001_z");

        assertEquals("key,date\r\n0001_z,2026-10-07\r\n0002_a,2026-10-07\r\n0010_b,2026-10-07\r\n", text());
    }

    @Test
    void loadAcceptsWithAndWithoutHeaderAndSkipsBlankLines() throws Exception {
        Files.writeString(csv, "\n0001_a,2026-01-02\n\n0002_b,2026-02-03\n");
        store.reload();
        assertEquals(Optional.of(LocalDate.of(2026, 1, 2)), store.studiedOn("0001_a"));
        assertEquals(Optional.of(LocalDate.of(2026, 2, 3)), store.studiedOn("0002_b"));

        Files.writeString(csv, "Key,Date\r\n0003_c,2026-03-04\r\n");
        store.reload();
        assertEquals(Optional.of(LocalDate.of(2026, 3, 4)), store.studiedOn("0003_c"));
        assertEquals(Optional.empty(), store.studiedOn("0001_a"), "reload replaces what was in memory");
        assertTrue(status.isEmpty());
    }

    @Test
    void badRowsAreSkippedAndReportedWhileGoodRowsLoad() throws Exception {
        Files.writeString(csv, "key,date\n"
                + "0001_a,2026-01-02\n"
                + "0002_b,not-a-date\n"
                + "just-one-field\n"
                + "0003_c,2026-01-02,extra\n"
                + ",2026-01-02\n"
                + "\"unbalanced,2026-01-02\n"
                + "0004_d,2026-04-05\n");

        store.reload();

        assertTrue(store.studiedOn("0001_a").isPresent());
        assertTrue(store.studiedOn("0004_d").isPresent());
        assertEquals(Optional.empty(), store.studiedOn("0002_b"));
        assertEquals(1, status.size());
        assertTrue(status.get(0).contains("Skipped 5 unreadable row(s)"), status.get(0));
        assertFalse(Files.exists(tmp.resolve(StudiedStore.FILE_NAME + ".bad")));
    }

    @Test
    void duplicateKeysLastRowWins() throws Exception {
        Files.writeString(csv, "0001_a,2026-01-01\n0001_A,2026-02-02\n");
        store.reload();
        assertEquals(Optional.of(LocalDate.of(2026, 2, 2)), store.studiedOn("0001_a"));
    }

    @Test
    void byteOrderMarkIsDropped() throws Exception {
        Files.writeString(csv, "\uFEFFkey,date\n0001_a,2026-01-02\n");
        store.reload();
        assertTrue(store.studiedOn("0001_a").isPresent());
        assertTrue(status.isEmpty());
    }

    @Test
    void invalidUtf8DoesNotFailTheLoad() throws Exception {
        byte[] head = "0001_a,2026-01-02\n0002_".getBytes(StandardCharsets.UTF_8);
        byte[] tail = ",2026-01-03\n".getBytes(StandardCharsets.UTF_8);
        byte[] all = new byte[head.length + 1 + tail.length];
        System.arraycopy(head, 0, all, 0, head.length);
        all[head.length] = (byte) 0xFF;
        System.arraycopy(tail, 0, all, head.length + 1, tail.length);
        Files.write(csv, all);

        store.reload();

        assertTrue(store.studiedOn("0001_a").isPresent());
    }

    @Test
    void keysWithCommasAndQuotesRoundTrip() throws Exception {
        store.markStudied("0001_a,b");
        store.markStudied("0002_say\"hi\"");

        assertTrue(text().contains("\"0001_a,b\",2026-10-07"));

        StudiedStore again = new StudiedStore(clock, status::add);
        again.setFile(csv);
        assertTrue(again.studiedOn("0001_a,b").isPresent());
        assertTrue(again.studiedOn("0002_say\"hi\"").isPresent());
    }

    @Test
    void aFileWithNoReadableRowsIsPreservedAsBadAndStartsEmpty() throws Exception {
        String junk = "this is not\na csv file at all\n";
        Files.writeString(csv, junk);

        store.reload();

        Path bad = tmp.resolve(StudiedStore.FILE_NAME + ".bad");
        assertTrue(Files.exists(bad));
        assertEquals(junk, Files.readString(bad));
        assertFalse(Files.exists(csv));
        assertEquals(Optional.empty(), store.studiedOn("0001_a"));
        assertEquals(1, status.size());
        assertTrue(status.get(0).contains(".bad"));

        assertTrue(store.markStudied("0001_a"), "usable again after the bad file was set aside");
    }

    @Test
    void aHeaderOnlyFileIsFineNotBad() throws Exception {
        Files.writeString(csv, "key,date\r\n");
        store.reload();
        assertTrue(status.isEmpty());
        assertTrue(Files.exists(csv));
    }

    @Test
    void unknownRowsAreKeptWhenSomethingElseIsSaved() throws Exception {
        Files.writeString(csv, "key,date\n0099_gone-problem,2025-12-31\n");
        store.reload();

        store.markStudied("0001_two-sum");

        assertEquals("key,date\r\n0001_two-sum,2026-10-07\r\n0099_gone-problem,2025-12-31\r\n", text());
    }

    @Test
    void handEditsMadeSinceTheLastLoadAreNotLostWhenSaving() throws Exception {
        store.markStudied("0001_a");
        // The user edits the file in another program, and AlgoPrep has not reloaded yet
        Files.writeString(csv, "key,date\n0001_a,2026-01-01\n0005_edited,2026-05-05\n");

        store.markStudied("0002_b");

        assertEquals("key,date\r\n0001_a,2026-01-01\r\n0002_b,2026-10-07\r\n0005_edited,2026-05-05\r\n", text());
        assertTrue(store.studiedOn("0005_edited").isPresent());
    }

    @Test
    void reloadPicksUpEditsAndNotifiesOnlyOnRealChange() throws Exception {
        store.markStudied("0001_a");
        AtomicInteger notified = new AtomicInteger();
        store.addListener(notified::incrementAndGet);

        store.reload();
        assertEquals(0, notified.get());

        Files.writeString(csv, "key,date\n0001_a,2026-03-03\n");
        store.reload();
        assertEquals(1, notified.get());
        assertEquals(Optional.of(LocalDate.of(2026, 3, 3)), store.studiedOn("0001_a"));
    }

    @Test
    void switchingTheFileLoadsTheOtherFileAndSamePathDoesNothing() throws Exception {
        store.markStudied("0001_a");
        Path other = tmp.resolve("other").resolve(StudiedStore.FILE_NAME);
        Files.createDirectories(other.getParent());
        Files.writeString(other, "0007_x,2026-07-07\n");

        AtomicInteger notified = new AtomicInteger();
        store.addListener(notified::incrementAndGet);

        store.setFile(csv);
        assertEquals(0, notified.get(), "same path: nothing happens");

        store.setFile(other);
        assertEquals(1, notified.get());
        assertEquals(Optional.empty(), store.studiedOn("0001_a"));
        assertTrue(store.studiedOn("0007_x").isPresent());
    }

    @Test
    void noFileMeansNoRecordsAndMarkingFailsWithAMessage() {
        store.setFile(null);

        assertFalse(store.markStudied("0001_a"));
        assertFalse(store.clear("0001_a"));
        assertEquals(Optional.empty(), store.studiedOn("0001_a"));
        assertTrue(status.stream().anyMatch(m -> m.contains("HOME")));
    }

    @Test
    void aFailedSaveIsReportedAndLeavesTheOldFileAndDateInPlace() throws Exception {
        store.markStudied("0001_a");
        String before = text();
        // A directory where the temp file must go makes the write fail
        Files.createDirectory(tmp.resolve(StudiedStore.FILE_NAME + ".tmp"));
        status.clear();

        StudiedStore later = new StudiedStore(at("2026-10-20"), status::add);
        later.setFile(csv);
        assertFalse(later.markStudied("0001_a"));

        assertEquals(before, text(), "the old file is untouched");
        assertEquals(Optional.of(LocalDate.of(2026, 10, 7)), later.studiedOn("0001_a"));
        assertEquals(1, status.size());
        assertTrue(status.get(0).startsWith("Could not save"), status.get(0));
    }
}

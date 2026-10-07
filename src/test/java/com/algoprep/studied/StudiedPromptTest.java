package com.algoprep.studied;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StudiedPromptTest {

    @Test
    void numberHasNoZeroPadding() {
        assertEquals(Optional.of("Studied leetcode problem #1"), StudiedPrompt.forKey("0001_two-sum"));
        assertEquals(Optional.of("Studied leetcode problem #105"), StudiedPrompt.forKey("0105_notime"));
        assertEquals(Optional.of("Studied leetcode problem #1"), StudiedPrompt.forKey("1_two-sum"));
        assertEquals(Optional.of("Studied leetcode problem #3456"), StudiedPrompt.forKey("3456_big"));
    }

    @Test
    void aKeyWithoutANumberGivesNothing() {
        assertEquals(Optional.empty(), StudiedPrompt.forKey("two-sum"));
        assertEquals(Optional.empty(), StudiedPrompt.forKey("0001_"));
        assertEquals(Optional.empty(), StudiedPrompt.forKey(null));
    }
}

package com.chatstory;

import com.chatstory.theme.NativeTheme;
import com.chatstory.theme.NativeThemeModel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NativeThemeModelTest {

    @Test
    void defaultsToDarkTheme() {
        assertEquals(NativeTheme.DARK, new NativeThemeModel().current());
    }

    @Test
    void notifiesThemeChanges() {
        NativeThemeModel model = new NativeThemeModel();
        List<NativeTheme> seen = new ArrayList<>();
        model.addListener((previous, current) -> seen.add(current));

        model.setTheme(NativeTheme.LIGHT);

        assertEquals(NativeTheme.LIGHT, model.current());
        assertEquals(List.of(NativeTheme.LIGHT), seen);
    }
}

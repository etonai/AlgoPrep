package com.algoprep.display;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class FontScaleModelTest {

    @Test
    void startsAtTheNormalSize() {
        FontScaleModel m = new FontScaleModel();

        assertEquals(100, m.percent());
        assertTrue(m.canIncrease());
        assertTrue(m.canDecrease());
    }

    @Test
    void increaseAndDecreaseMoveOneStep() {
        FontScaleModel m = new FontScaleModel();

        m.increase();
        assertEquals(110, m.percent());
        m.increase();
        assertEquals(120, m.percent());
        m.decrease();
        m.decrease();
        m.decrease();
        assertEquals(90, m.percent());
    }

    @Test
    void stopsAtTheLargestSize() {
        FontScaleModel m = new FontScaleModel();

        for (int i = 0; i < 100; i++) {
            m.increase();
        }

        assertEquals(FontScaleModel.MAX, m.percent());
        assertFalse(m.canIncrease());
        assertTrue(m.canDecrease());
    }

    @Test
    void stopsAtTheSmallestSize() {
        FontScaleModel m = new FontScaleModel();

        for (int i = 0; i < 100; i++) {
            m.decrease();
        }

        assertEquals(FontScaleModel.MIN, m.percent());
        assertFalse(m.canDecrease());
        assertTrue(m.canIncrease());
    }

    @Test
    void theNormalSizeIsReachableInWholeStepsFromBothLimits() {
        assertEquals(0, (FontScaleModel.DEFAULT - FontScaleModel.MIN) % FontScaleModel.STEP);
        assertEquals(0, (FontScaleModel.MAX - FontScaleModel.DEFAULT) % FontScaleModel.STEP);
    }

    @Test
    void listenersFireOnlyOnARealChange() {
        FontScaleModel m = new FontScaleModel();
        AtomicInteger fired = new AtomicInteger();
        m.addListener(fired::incrementAndGet);

        m.increase();
        assertEquals(1, fired.get());
        m.decrease();
        assertEquals(2, fired.get());

        for (int i = 0; i < 100; i++) {
            m.increase();
        }
        int atMax = fired.get();
        m.increase(); // already at the limit: nothing changes, nobody is told
        assertEquals(atMax, fired.get());
    }

    @Test
    void restoreAcceptsAValidSavedSize() {
        FontScaleModel m = new FontScaleModel();

        m.restore(140);

        assertEquals(140, m.percent());
    }

    @Test
    void restoreAcceptsTheLimits() {
        FontScaleModel m = new FontScaleModel();

        m.restore(FontScaleModel.MIN);
        assertEquals(FontScaleModel.MIN, m.percent());
        m.restore(FontScaleModel.MAX);
        assertEquals(FontScaleModel.MAX, m.percent());
    }

    @Test
    void aMissingOrInvalidSavedSizeMeansTheNormalSize() {
        for (Integer bad : new Integer[] {null, 0, -50, 59, 105, 255, 10_000, Integer.MIN_VALUE}) {
            FontScaleModel m = new FontScaleModel();
            m.increase();
            m.increase();

            m.restore(bad);

            assertEquals(100, m.percent(), "saved value " + bad);
        }
    }

    @Test
    void restoreNotifiesOnlyWhenTheSizeChanges() {
        FontScaleModel m = new FontScaleModel();
        AtomicInteger fired = new AtomicInteger();
        m.addListener(fired::incrementAndGet);

        m.restore(null); // already the normal size
        assertEquals(0, fired.get());
        m.restore(150);
        assertEquals(1, fired.get());
    }
}

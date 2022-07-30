package io.opsit.explang;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import org.junit.Test;
import static io.opsit.explang.Utils.isRoundNumber;


public class UtilsTests {
    @Test
    public void testIsRoundNumber() {
        assertFalse(isRoundNumber(null));
        assertTrue(isRoundNumber(0));

        assertTrue(isRoundNumber(1));
        assertTrue(isRoundNumber(-1));

        assertTrue(isRoundNumber(Integer.MAX_VALUE));
        assertTrue(isRoundNumber(Integer.MIN_VALUE));

        assertTrue(isRoundNumber(Long.MAX_VALUE));
        assertTrue(isRoundNumber(Long.MIN_VALUE));
        
        assertTrue(isRoundNumber((double)0.0));
        assertTrue(isRoundNumber((float)0.0));

        assertFalse(isRoundNumber((double)-0.1));
        assertFalse(isRoundNumber((float)-0.1));

        assertFalse(isRoundNumber((double)-10.00001));
        assertFalse(isRoundNumber((float)-10.00001));

        assertTrue(isRoundNumber((double)2e20));
        assertTrue(isRoundNumber((double)-2e20));
        assertTrue(isRoundNumber((float)2e20));
        assertTrue(isRoundNumber((float)-2e20));

        assertFalse(isRoundNumber((double)2e-20));
        assertFalse(isRoundNumber((double)-2e-20));
        assertFalse(isRoundNumber((float)2e-20));
        assertFalse(isRoundNumber((float)-2e-20));

    }
}

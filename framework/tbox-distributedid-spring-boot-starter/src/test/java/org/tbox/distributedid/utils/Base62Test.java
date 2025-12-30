package org.tbox.distributedid.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Base62Test {

    @Test
    void encodeDecodeRoundTrip_Unsigned() {
        long[] values = new long[]{
                0L,
                1L,
                61L,
                62L,
                63L,
                123456789L,
                Long.MAX_VALUE,
                -1L,               // unsigned: 2^64-1
                Long.MIN_VALUE     // unsigned: 2^63
        };

        for (long v : values) {
            String s = Base62.encodeUnsigned(v);
            long decoded = Base62.decodeUnsigned(s);
            assertEquals(v, decoded, "round-trip failed for v=" + v + ", s=" + s);
        }
    }

    @Test
    void encodeUsesOnlyBase62Charset() {
        for (int i = 0; i < 10_000; i++) {
            String s = Base62.encodeUnsigned((long) i * 99991L);
            assertTrue(s.matches("^[0-9a-zA-Z]+$"), "invalid chars: " + s);
        }
    }

    @Test
    void decodeRejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> Base62.decodeUnsigned(null));
        assertThrows(IllegalArgumentException.class, () -> Base62.decodeUnsigned(""));
        assertThrows(IllegalArgumentException.class, () -> Base62.decodeUnsigned("abc_"));
    }
}


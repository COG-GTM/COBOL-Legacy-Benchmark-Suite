package com.clbs.portfolio.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Alphanumeric storage semantics: right padding, right truncation and the SPACES test. */
class CobolTextTest {

    @Test
    @DisplayName("a short value is padded on the right to the declared length")
    void padsOnTheRight() {
        assertEquals("AB      ", CobolText.picX("AB", 8));
        assertEquals(8, CobolText.picX("AB", 8).length());
    }

    @Test
    @DisplayName("a long value loses its rightmost characters")
    void truncatesOnTheRight() {
        assertEquals("PORT0000", CobolText.picX("PORT00001", 8));
    }

    @Test
    @DisplayName("an unset field is spaces, not null or empty")
    void unsetFieldIsSpaces() {
        assertEquals("        ", CobolText.picX(null, 8));
        assertTrue(CobolText.isSpaces(CobolText.spaces(80)));
        assertTrue(CobolText.isSpaces(null));
        assertFalse(CobolText.isSpaces(CobolText.picX("x", 80)));
    }

    @Test
    @DisplayName("only the space character counts as SPACES")
    void onlySpacesAreBlank() {
        assertFalse(CobolText.isSpaces("\t"));
        assertFalse(CobolText.isSpaces("\0"));
        assertFalse(CobolText.isSpaces("\n"));
        assertTrue(CobolText.isSpaces("   "));
    }

    @Test
    @DisplayName("trim removes only the pad")
    void trimRemovesOnlyThePad() {
        assertEquals("GROWTH PORTFOLIO", CobolText.trim(CobolText.picX("GROWTH PORTFOLIO", 30)));
        assertEquals("", CobolText.trim(CobolText.spaces(30)));
    }

    @Test
    @DisplayName("PIC 9(n) display fields drop the sign and any high-order overflow")
    void pic9() {
        assertEquals(20240320, CobolText.pic9(20240320, 8));
        assertEquals(20240320, CobolText.pic9(-20240320, 8));
        assertEquals(20240320, CobolText.pic9(120240320L, 8));
        assertEquals("00000042", CobolText.pic9Image(42, 8));
    }
}

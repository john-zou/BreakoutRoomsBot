/*
 * Copyright (C) 2020 The University of British Columbia
 */
package ca.ubc.cs.zoombrb.android

import org.junit.jupiter.api.Test

import kotlin.test.expect

internal class TagParserTest {

    @Test
    fun `returns null if username has no tag`() {
        expect(null) { TagParser.tryParseTag("hello") }
        expect(null) { TagParser.tryParseTag("") }
        expect(null) { TagParser.tryParseTag("abc1234567") }
    }

    @Test
    fun `returns number tag correctly`() {
        expect(Tag(1)) { TagParser.tryParseTag("(1) User Name") }
        expect(Tag(1)) { TagParser.tryParseTag("(01) User Name") }
        expect(Tag(1)) { TagParser.tryParseTag("( 1) User Name") }
        expect(Tag(1)) { TagParser.tryParseTag("( 1 ) User Name") }
        expect(Tag(1)) { TagParser.tryParseTag("(1 ) User Name") }
        // Chinese parentheses
        expect(Tag(1)) { TagParser.tryParseTag("（1） User Name") }
        expect(Tag(1)) { TagParser.tryParseTag("（01） User Name") }
        expect(Tag(1)) { TagParser.tryParseTag("（ 1） User Name") }
        expect(Tag(1)) { TagParser.tryParseTag("（ 1 ） User Name") }
        expect(Tag(1)) { TagParser.tryParseTag("（1 ） User Name") }
    }


    @Test
    fun `returns lone wolf tag correctly`() {
        expect(Tag(isLoneWolf = true)) { TagParser.tryParseTag("(00) User Name") }
        expect(Tag(isLoneWolf = true)) { TagParser.tryParseTag("(0) User Name") }
        expect(Tag(isLoneWolf = true)) { TagParser.tryParseTag("( 00) User Name") }
        expect(Tag(isLoneWolf = true)) { TagParser.tryParseTag("( 00 ) User Name") }
        expect(Tag(isLoneWolf = true)) { TagParser.tryParseTag("(00 ) User Name") }
        // Chinese parentheses
        expect(Tag(isLoneWolf = true)) { TagParser.tryParseTag("（00） User Name") }
        expect(Tag(isLoneWolf = true)) { TagParser.tryParseTag("（0） User Name") }
        expect(Tag(isLoneWolf = true)) { TagParser.tryParseTag("（ 00） User Name") }
        expect(Tag(isLoneWolf = true)) { TagParser.tryParseTag("（ 00 ） User Name") }
        expect(Tag(isLoneWolf = true)) { TagParser.tryParseTag("（00 ） User Name") }
    }

    @Test
    fun `returns null if number tag out of bounds`() {
        expect(null) { TagParser.tryParseTag("(-1) User Name") }
        expect(null) { TagParser.tryParseTag("(9000) User Name") }
        expect(null) { TagParser.tryParseTag("(49) User Name") }

        expect(null) { TagParser.tryParseTag("(-1) User Name") }
        expect(null) { TagParser.tryParseTag("(9000) User Name") }
        expect(null) { TagParser.tryParseTag("(49) User Name") }
    }

    @Test
    fun `returns TA tag correctly`() {
        expect(Tag(isTA = true)) { TagParser.tryParseTag("(TA) User Name") }
    }

    @Test
    fun `returns Prof tag correctly`() {
        expect(Tag(isProf = true)) { TagParser.tryParseTag("(Prof) User Name") }
    }
}
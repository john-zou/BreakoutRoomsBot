/*
 * Copyright (C) 2020 The University of British Columbia
 */
package ca.ubc.cs.zoombrb.android
import java.util.regex.Pattern

data class Tag(val roomNumber: Int? = null,
               val isLoneWolf: Boolean = false,
               val isProf: Boolean = false,
               val isTA: Boolean = false)

const val TA_TAG = "TA"
const val PROF_TAG = "Prof"
const val MAX_ROOM_NUMBER = 48

object Log {
    fun v(tag: String, message: String) {
        println("[v] $tag - $message")
    }
    fun i(tag: String, message: String) {
        println("[i] $tag - $message")
    }
    fun d(tag: String, message: String) {
        println("[d] $tag - $message")
    }
    fun e(tag: String, message: String) {
        println("[e] $tag - $message")
    }
}

object TagParser {
    // https://stackoverflow.com/a/57313066
    // This regex matches the contents of parentheses and things that look like parentheses
    // https://www.fileformat.info/info/unicode/category/Ps/list.htm They are all single utf16 characters, which is the only possible Java char size
    private const val regex = "\\p{Ps}([^\\p{Ps}\\p{Pe}]*)\\p{Pe}"
    private val pattern = Pattern.compile(regex)

    /**
     *
     * @param username The Zoom username
     * @return null if a valid tag could not be parsed
     */
    fun tryParseTag(username: String): Tag? {
        val matcher = pattern.matcher(username)
        val tagStr: String?
        tagStr = if (matcher.find()) {
            val strWithParentheses = matcher.group() // single open parenthesis character, the tag, and single closed parenthesis character, so we need to remove the first and last char
            val insideParentheses = strWithParentheses.substring(1..(strWithParentheses.length - 2))
            insideParentheses.trim()
        } else {
            tryParseTagByNumberPrefix(username)
        }

        return when (tagStr) {
            null -> null
            PROF_TAG -> Tag(isProf = true)
            TA_TAG -> Tag(isTA = true)
            "0", "00" -> Tag(isLoneWolf = true)
            else -> try {
                val roomNumber = tagStr.toInt()
                if (roomNumber in 1..MAX_ROOM_NUMBER) Tag(roomNumber) else null
            } catch (e: NumberFormatException) {
                null
            }
        }
    }

    /**
     * Attempts to parse a numerical tag that doesn't have opening and closing parentheses
     *
     * @param username The Zoom username
     * @return the numerical tag
     */
    private fun tryParseTagByNumberPrefix(username: String): String? {
        val trimmed = username.trim { it <= ' ' }
        if (trimmed.isEmpty()) {
            Log.v("BRB", "No tag parsed because username is empty or only whitespace")
            return null
        }
        return if (trimmed.length == 1) {
            if (Character.isDigit(trimmed[0])) {
                // This matches the rare case of a student who has a single number as their username
                trimmed
            } else {
                Log.v("BRB", "No parentheses. First non-whitespace character is not a digit")
                null
            }
        } else if (Character.isDigit(trimmed[0]) && Character.isDigit(trimmed[1])) {
            trimmed.substring(0, 2)
        } else if (Character.isDigit(trimmed[0])) {
            // This matches e.g. "8Ada Lovelace" and assumes she wants to be in room 8
            trimmed.substring(0, 1)
        } else {
            Log.v("BRB", "No parentheses. First non-whitespace character is not a digit")
            null
        }
    }
}
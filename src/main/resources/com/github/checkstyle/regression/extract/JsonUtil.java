////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2020 the original author or authors.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
////////////////////////////////////////////////////////////////////////////////

package com.puppycrawl.tools.checkstyle;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Our own Json processing utility.
 * @author LuoLiangchen
 */
class JsonUtil {
    /** Delimiter starting and ending a string. */
    private static final String STRING_DELIMITER = "\"";

    /** Newline string. */
    private static final String NEWLINE = "\n";

    /**
     * Adds indent of the given text.
     * @param text the text to add indent
     * @return the result text with indent
     */
    private static String addIndent(String text) {
        return Arrays.stream(text.split(NEWLINE))
                .map(line -> "  " + line)
                .collect(Collectors.joining(NEWLINE));
    }

    /**
     * Formats the Json object.
     * @param object the object to format
     * @return the format result
     */
    private static String format(Object object) {
        final String result;

        if (object instanceof String) {
            result = STRING_DELIMITER + object + STRING_DELIMITER;
        }
        else {
            result = object.toString();
        }

        return result;
    }

    /** Represents an object type in Json. */
    public static final class JsonObject {
        /** Fields of this Json object. */
        private final List<KeyValue> members = new LinkedList<>();

        /**
         * Adds a member.
         * @param key the key of the field
         * @param value the value of the field
         */
        void addProperty(String key, Object value) {
            add(key, value);
        }

        /**
         * Adds a member.
         * @param key the key of the field
         * @param value the value of the field
         */
        void add(String key, Object value) {
            members.add(new KeyValue(key, value));
        }

        @Override
        public String toString() {
            final String keyValueLines = members.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining("," + NEWLINE));
            return "{" + NEWLINE + addIndent(keyValueLines) + NEWLINE + "}";
        }
    }

    /** Represents an array type in Json. */
    public static final class JsonArray {
        /** Items of this Json array. */
        private final List<Object> members = new LinkedList<>();

        /**
         * Adds a member.
         * @param object the member to add
         */
        void add(Object object) {
            members.add(object);
        }

        @Override
        public String toString() {
            final String membersLines = members.stream()
                    .map(JsonUtil::format)
                    .collect(Collectors.joining(",\n"));
            return "[" + NEWLINE + addIndent(membersLines) + NEWLINE + "]";
        }
    }

    /** Represents a key-value pair in Json object. */
    private static final class KeyValue {
        /** The key of the field. */
        private final String key;

        /** The value of the field. */
        private final Object value;

        /**
         * Creates a new instance of KeyValue.
         * @param key the key of the field
         * @param value the value of the field
         */
        KeyValue(String key, Object value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String toString() {
            return STRING_DELIMITER + key + STRING_DELIMITER + ": " + format(value);
        }
    }
}

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
    /**
     * Adds indent of the given text.
     * @param text the text to add indent
     * @return the result text with indent
     */
    private static String addIndent(String text) {
        return Arrays.stream(text.split("\n"))
                .map(line -> "  " + line)
                .collect(Collectors.joining("\n"));
    }

    /**
     * Formats the Json object.
     * @param object the object to format
     * @return the format result
     */
    private static String format(Object object) {
        if (object instanceof String) {
            return "\"" + object + "\"";
        } else {
            return object.toString();
        }
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
                    .collect(Collectors.joining(",\n"));
            return "{\n" + addIndent(keyValueLines) + "\n}";
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
            return "[\n" + addIndent(membersLines) + "\n]";
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
            return "\"" + key + "\": " + format(value);
        }
    }
}

package org.opendatamesh.platform.up.executor.gitlab.tasks.entities;

import java.util.Locale;

public enum InputType {
    STRING,
    NUMBER,
    BOOLEAN,
    ARRAY;

    public static InputType fromGitLab(String type) {
        if (type == null) {
            return STRING;
        }
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "number" -> NUMBER;
            case "boolean" -> BOOLEAN;
            case "array" -> ARRAY;
            default -> STRING;
        };
    }
}

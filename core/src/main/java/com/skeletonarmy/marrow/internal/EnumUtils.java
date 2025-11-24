package com.skeletonarmy.marrow.internal;

import androidx.annotation.NonNull;

import java.util.HashMap;

/**
 * Enum Utilities class
 *
 * <p><b>Internal API - Not Documented:</b> This class is public for
 * internal framework use. No formal documentation is provided
 * beyond these Javadoc comments. Contact the team in case you need support.
 *
 * <p><b>Warning:</b> Subject to change without notice.
 */
public class EnumUtils {
    /**
     * Maps a normalized string to its corresponding enum constant.
     * <p>
     * @param enumClass the enum class (must implement {@link LabeledEnum})
     * @param label the label to look up
     * @param <T> the enum type
     * @return the matching enum constant, or {@code null} if not found
     */
    public static <T extends Enum<T> & LabeledEnum> T labelToEnum(@NonNull Class<T> enumClass, @NonNull String label) {
        T[] enumConstants = enumClass.getEnumConstants();
        if (enumConstants == null) {
            throw new IllegalArgumentException(String.format("The class %s isn't an Enum class", enumClass.getName()));
            // If you manage to throw this exception you're special...
        }
        if (enumConstants.length == 0) {
            throw new IllegalArgumentException(String.format("Enum class %s doesn't contain any enum constants", enumClass.getName()));
        }

        HashMap<String, T> enumLabelsMap = new HashMap<>(enumConstants.length);
        for (T enumConstant : enumConstants) {
            enumLabelsMap.put(enumConstant.getLabel(), enumConstant);
        }
        return enumLabelsMap.getOrDefault(label, null);
    }
}

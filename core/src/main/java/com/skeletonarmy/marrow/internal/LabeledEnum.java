package com.skeletonarmy.marrow.internal;

/**
 * Interface for string representation of Enums.
 *
 * <p><b>Internal API - Not Documented:</b> This class is public for
 * internal framework use. No formal documentation is provided
 * beyond these Javadoc comments. Contact the team in case you need support.
 *
 * <p><b>Warning:</b> Subject to change without notice.
 */

public interface LabeledEnum {
/**
 * <p> Example implementation:
 * <pre> {@code
 * public enum Theme implements BaseEnum {
 *     DARK("Dark Mode"),
 *     LIGHT("Light Mode"),
 *     SWEET_DARK("Sweet Dark"),
 *     ADWAITA("Adwaita");
 *
 *     private final String label;
 *
 *     Theme(String label) {
 *         this.label = label;
 *     }
 *
 *     @Override
 *     public String getLabel() {
 *         return this.label;
 *     }
 * }
 * }</pre>
 * <p>
 * String representations can be turned back into their enum constat counterpart,see {@link EnumUtils#labelToEnum(Class, String)}
 */
    String getLabel();
}

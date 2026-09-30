package com.skeletonarmy.marrow;

import com.qualcomm.hardware.lynx.LynxModule;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to control the bulk caching mode for an OpMode.
 * <p>
 * By default, Marrow enables {@link LynxModule.BulkCachingMode#AUTO} for all OpModes.
 * Use this annotation to override that behavior.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface BulkCaching {
    /**
     * The bulk caching mode to use for this OpMode.
     * Defaults to {@link LynxModule.BulkCachingMode#AUTO}.
     */
    LynxModule.BulkCachingMode mode() default LynxModule.BulkCachingMode.AUTO;
}

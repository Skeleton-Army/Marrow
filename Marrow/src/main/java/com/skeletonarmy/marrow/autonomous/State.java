package com.skeletonarmy.marrow.autonomous;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface State {
  /** The amount of time it takes to complete the state (seconds) */
  double requiredTime() default 0;
}

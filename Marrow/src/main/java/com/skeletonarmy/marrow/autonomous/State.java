package com.skeletonarmy.marrow.autonomous;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface State {
  /** The amount of time in seconds that it takes to complete the state. */
  double requiredTime() default 0;

  /** Fallback to this state if there is not enough time to complete the state. */
  String timeoutState() default "";

  /** Immediately force exit this state when this many seconds remain in autonomous. */
  double forceExitTime() default 0;
}

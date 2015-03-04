package org.cooperari;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configure hotspots that should always be reached by a trial in a test session.
 * 
 * @see CNever
 * @see CSometimes
 * 
 * @since 0.2
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface CAlways {
  /**
   * @return Hotspot identifiers.
   */
  public String[] value() default {};
}

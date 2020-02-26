package io.github.SebastianDanielFrenz.WorldEconomyPlugin;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(value = { ElementType.METHOD })
@Retention(value = RetentionPolicy.CLASS)
public @interface MissuseWarning {

	String text();

}

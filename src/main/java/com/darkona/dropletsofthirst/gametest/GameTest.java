package com.darkona.dropletsofthirst.gametest;

import com.darkona.dropletsofthirst.DropletsOfThirst;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A game test: a public static method taking a {@code GameTestHelper}, in a class listed in {@link DropletsGameTests}.
 * Minecraft 26.1 has no annotation for this any more; {@link DropletsGameTests} registers each method as a test
 * function and a test of the same id.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface GameTest
{
    /** Structure path, in {@link #templateNamespace()}. */
    String template();

    String templateNamespace() default DropletsOfThirst.ID;

    int timeoutTicks() default 100;
}

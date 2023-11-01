package de.alphaomegait.aocommand.tabcompleter;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ITabCompleter {

  @NotNull String name();
  @NotNull String[] aliases() default {};
  @NotNull String permission() default "";
}
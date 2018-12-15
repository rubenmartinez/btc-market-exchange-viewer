package net.rubenmartinez.stpc.app.strategy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.web.socket.WebSocketHandler;

/**
 * Annotates a {@link Strategy} with a friendly type name.
 * 
 * The idea is: when a service needs to create a Strategy instance for type X, it will look for a Strategy annotated as <code>StrategyType("X")</code>
 * However this is not yet implemented
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface StrategyType {

	/**
	 * The path that the annotated {@link WebSocketHandler} will handle.
	 * This is an alias for {@link #path}.
	 */
	@AliasFor("type")
	String[] value() default {};

	/**
	 * The path that the annotated {@link WebSocketHandler} will handle.
	 * This is an alias for {@link #value}.
	 */
	@AliasFor("value")
	String[] type() default {};
}

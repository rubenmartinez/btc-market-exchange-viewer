package net.rubenmartinez.stpc.app.helper.websocket.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.web.socket.WebSocketHandler;

/**
 * Just an utility annotation to specify the endpoint for a websocket handler.
 * 
 * In SpringBoot 2.0.1, if chosen not to use STOMP, then one have to configure the endpoint <em>path</em> of the web socket handler in a
 * This is not consistent with Controllers, that carry the paths they are mapped to in their own class.
 * So this class allows to mark a websocket handler with a path, that will be read and registered at startup time.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface WebSocketEndpointPath {

	/**
	 * The path that the annotated {@link WebSocketHandler} will handle.
	 * This is an alias for {@link #path}.
	 */
	@AliasFor("path")
	String[] value() default {};

	/**
	 * The path that the annotated {@link WebSocketHandler} will handle.
	 * This is an alias for {@link #value}.
	 */
	@AliasFor("value")
	String[] path() default {};
}

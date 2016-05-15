package db.meta;

import db.action.ModelActions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents a Model with ModelActions
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Actions {

    /**
     * ModelActions class.
     *
     * @return ModelActions class
     */
    Class<? extends ModelActions<?, ?>> value();

}

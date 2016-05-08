package db.meta;

import db.Model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents a one-to-many relationship between two models.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface HasMany {

    /**
     * The "many" Model class.
     *
     * @return Model class
     */
    Class<? extends Model<?>>[] value();

}

package db.meta.relation;

import db.Model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Many-to-many relationship metadata.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ManyToMany {

    /**
     * The Model the class has a relationship with.
     *
     * @return Model class
     */
    Class<? extends Model> modelClass();

    /**
     * The mediator table to link the two models.
     *
     * @return Mediator table class
     */
    Class<?> tableClass();

}

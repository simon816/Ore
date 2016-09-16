package db.meta.relation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A collection of {@link ManyToMany} relations.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ManyToManyCollection {

    /**
     * The bindings.
     *
     * @return ManyToMany bindings
     */
    ManyToMany[] value();

}

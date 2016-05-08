package db.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field within a Model to be bound to it's corresponding ModelTable.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Bind {

    /**
     * The key to be used in updating and referencing this field. This should
     * match the name of the corresponding column definition in the Model's
     * ModelTable. If no value is specified, the bound field's name will be
     * used instead.
     *
     * <p>The field may also have a leading underscore in it's name which will
     * be removed automatically in trying to resolve the column definition on
     * the ModelTable.</p>
     *
     * @return Field key name
     */
    String value() default "";

}

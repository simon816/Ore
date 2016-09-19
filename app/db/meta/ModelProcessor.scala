package db.meta

import java.lang.reflect.Field

import db.meta.relation.{ManyToManyCollection, OneToMany}
import db.{Model, ModelService, ModelTable}

import scala.reflect.runtime.universe._

/**
  * Processes an annotated Model and bind's the appropriate fields and
  * children.
  */
class ModelProcessor(service: ModelService) {

  /**
    * Processes and generates annotation bindings for the specified model.
    *
    * @param model  Model to bind
    * @tparam T     Model table
    * @tparam M     Model type
    */
  def process[T <: ModelTable[M], M <: Model: TypeTag](model: M): M = {
    model.service = this.service
    bindFields(model)
    bindRelations(model)
    model.setProcessed(true)
    model
  }

  /**
    * Binds the specified Model fields to the appropriate registered
    * TypeSetter.
    *
    * @param model    Model to process
    * @tparam T       Model table type
    * @tparam M       Model type
    */
  def bindFields[T <: ModelTable[M], M <: Model: TypeTag](model: M): M = {
    val modelClass = model.getClass
    //noinspection ComparingUnrelatedTypes
    val bindFields = getModelFields(modelClass)
      .filter(_.getAnnotations.exists(_.annotationType.equals(classOf[Bind])))

    for (bindField <- bindFields) {
      val bindData = bindField.getAnnotation(classOf[Bind])
      val fieldName = bindField.getName.substring(bindField.getName.lastIndexOf("$") + 1)
      var fieldType = bindField.getType

      // Default to field name for key
      var key = bindData.value
      if (key.isEmpty) {
        key = fieldName
        // Remove leading underscore that demarks private fields
        if (key.startsWith("_")) key = key.substring(1)
      }

      // If a field is an option, bind the field to the inner Option type
      if (fieldType.equals(classOf[Option[_]])) {
        fieldType = runtimeMirror(getClass.getClassLoader)
          .runtimeClass(typeTag[M].tpe.members
            .filterNot(_.isMethod)
            .filter(m => m.name.decodedName.toString.trim.equals(fieldName))
            .map(_.typeSignature.typeArgs.head).head.typeSymbol.asClass)
      }

      service.registry.getTypeSetter(fieldType).bindTo(model, key, bindField)(service)
    }
    model
  }

  private def getModelFields(clazz: Class[_]): Set[Field] = {
    var fields: Set[Field] = Set.empty
    var currentClass = clazz
    while (!currentClass.equals(classOf[Model])) {
      fields ++= currentClass.getDeclaredFields
      currentClass = currentClass.getSuperclass
    }
    fields
  }

  /**
    * Binds the [[OneToMany]] relationships within a [[Model]].
    *
    * @param model  Model to bind
    * @tparam T     Model table
    * @tparam M     Model type
    * @return       Model
    */
  //noinspection ComparingUnrelatedTypes
  def bindRelations[T <: ModelTable[M], M <: Model](model: M): M = {
    val modelClass = model.getClass
    val key = modelClass.getSimpleName.toLowerCase + "Id"
    if (modelClass.getDeclaredAnnotations.exists(_.annotationType.equals(classOf[OneToMany]))) {
      val relations = modelClass.getDeclaredAnnotation(classOf[OneToMany])
      for (relation <- relations.value)
        model.bindOneToMany(relation, t => BootstrapTypeSetters.getRep[Int](key, t))
    }

    if (modelClass.getDeclaredAnnotations.exists(_.annotationType.equals(classOf[ManyToManyCollection]))) {
      val relations = modelClass.getDeclaredAnnotation(classOf[ManyToManyCollection])
      for (relation <- relations.value) {
        val assoc = model.actions.getAssociation(relation.tableClass)
        val childClass = relation.modelClass
        val childKey = childClass.getSimpleName.toLowerCase + "Id"
        model.bindManyToMany(
          relation.modelClass, assoc.assocTable,
          t => BootstrapTypeSetters.getRep[Int](key, t),
          t => BootstrapTypeSetters.getRep[Int](childKey, t)
        )
      }
    }

    model
  }

}

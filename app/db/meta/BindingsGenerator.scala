package db.meta

import db.{Model, ModelService, ModelTable}
import util.Conf.debug

import scala.reflect.runtime.universe._

/**
  * Processes an annotated Model and bind's the appropriate fields and
  * children.
  */
class BindingsGenerator {

  /**
    * Generates bindings for the specified model.
    *
    * @param model  Model to bind
    * @tparam T     Model table
    * @tparam M     Model type
    */
  def generateFor[T <: ModelTable[M], M <: Model[_]: WeakTypeTag](service: ModelService, model: M) = {
    debug("\n********** Generating bindings for model " + model + " **********")
    generateFieldsFor(service, model)
    generateRelationsFor(model)
    debug("*****************************************************************\n")
  }

  def generateFieldsFor[T <: ModelTable[M], M <: Model[_]: WeakTypeTag](service: ModelService, model: M) = {
    debug("Generating field for model " + model)
    // Bind marked fields
    val modelClass = model.getClass

    debug("Model class: " + modelClass)
    //noinspection ComparingUnrelatedTypes
    val bindFields = modelClass.getDeclaredFields
      .filter(_.getDeclaredAnnotations.exists(_.annotationType.equals(classOf[Bind])))

    debug("----- Fields to bind -----")
    bindFields.foreach(f => debug(f.toString))
    debug("--------------------------")

    for (bindField <- bindFields) {
      val bindData = bindField.getAnnotation(classOf[Bind])
      val fieldName = bindField.getName.substring(bindField.getName.lastIndexOf("$") + 1)
      var fieldType = bindField.getType

      // Default to field name for key
      var key = bindData.value
      if (key.isEmpty) key = fieldName
      if (key.startsWith("_")) key = key.substring(1)

      if (fieldType.equals(classOf[Option[_]])) {
        debug("OPTION FOUND: " + fieldName)
        // TODO: TypeTag not available and WeakTypeTag only seems to return
        // abstract members
        weakTypeTag[M].tpe.members.foreach(println)
        // Lift type out of option
        fieldType = runtimeMirror(getClass.getClassLoader)
          .runtimeClass(weakTypeTag[M].tpe.members
            .filterNot(_.isMethod)
            .filter(m => m.name.decodedName.toString.trim.equals(fieldName))
            .map(_.typeSignature.typeArgs.head).head.typeSymbol.asClass)
      }

      service.registrar.getSetter(fieldType)
        .getOrElse(throw new RuntimeException("No type setter found for type: " + fieldType))
        .bindTo(model, key, bindField)(service)
    }
  }

  def generateRelationsFor[T <: ModelTable[M], M <: Model[_]](model: M) = {
    debug("Generating relations for model " + model)
    val modelClass = model.getClass
    val key = modelClass.getSimpleName.toLowerCase + "Id"
    //noinspection ComparingUnrelatedTypes
    if (modelClass.getDeclaredAnnotations.exists(_.annotationType.equals(classOf[HasMany]))) {
      val relations = modelClass.getDeclaredAnnotation(classOf[HasMany])
      for (relation <- relations.value) model.bindMany(relation, t => TypeSetters.getRep[Int](key, t))
    }
  }

}

class OreBindingsGenerator extends BindingsGenerator

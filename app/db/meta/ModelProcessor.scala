package db.meta

import db.{Model, ModelService, ModelTable}
import util.OreConfig

import scala.reflect.runtime.universe._

/**
  * Processes an annotated Model and bind's the appropriate fields and
  * children.
  */
class ModelProcessor(service: ModelService, config: OreConfig) {

  import config.debug

  /**
    * Generates bindings for the specified model.
    *
    * @param model  Model to bind
    * @tparam T     Model table
    * @tparam M     Model type
    */
  def process[T <: ModelTable[M], M <: Model[_]: TypeTag](service: ModelService, model: M) = {
    debug("\n********** Generating bindings for model " + model + " **********")
    bindFields(service, model)
    bindRelations(model)
    println("service = " + this.service)
    model.service = this.service
    model.setProcessed(true)
    println("processed")
    debug("*****************************************************************\n")
  }

  /**
    * Binds the specified Model fields to the appropriate registered
    * TypeSetter.
    *
    * @param service  ModelService with registrar that contains the TypeSetters
    *                 to use
    * @param model    Model to process
    * @tparam T       Model table type
    * @tparam M       Model type
    */
  def bindFields[T <: ModelTable[M], M <: Model[_]: TypeTag](service: ModelService, model: M) = {
    debug("Generating field for model " + model)

    val modelClass = model.getClass
    debug("Model class: " + modelClass)
    //noinspection ComparingUnrelatedTypes
    val bindFields = modelClass.getDeclaredFields
      .filter(_.getAnnotations.exists(_.annotationType.equals(classOf[Bind])))

    debug("----- Fields to bind -----")
    bindFields.foreach(f => debug(f.toString))
    debug("--------------------------")

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
        debug("--- OPTION FOUND: " + fieldName + " ---")
        fieldType = runtimeMirror(getClass.getClassLoader)
          .runtimeClass(typeTag[M].tpe.members
            .filterNot(_.isMethod)
            .filter(m => m.name.decodedName.toString.trim.equals(fieldName))
            .map(_.typeSignature.typeArgs.head).head.typeSymbol.asClass)
      }

      service.registrar.getSetter(fieldType)
        .getOrElse(throw new RuntimeException("No type setter found for type: " + fieldType))
        .bindTo(model, key, bindField)(service)
    }
  }

  def bindRelations[T <: ModelTable[M], M <: Model[_]](model: M) = {
    debug("Generating relations for model " + model)
    val modelClass = model.getClass
    val key = modelClass.getSimpleName.toLowerCase + "Id"
    //noinspection ComparingUnrelatedTypes
    if (modelClass.getDeclaredAnnotations.exists(_.annotationType.equals(classOf[HasMany]))) {
      val relations = modelClass.getDeclaredAnnotation(classOf[HasMany])
      for (relation <- relations.value) model.bindMany(relation, t => BootstrapTypeSetters.getRep[Int](key, t))
    }
  }

}

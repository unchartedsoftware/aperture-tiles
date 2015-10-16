package software.uncharted.spark.execution.graph.typesupport

import scala.language.higherKinds



/**
 * A type that encapsulates truth or falseness (as opposed to an object that does so), for
 * matching and if/then/else types.
 *
 * Based tightly on the Bool type in https://github.com/harrah/up
 *
 * Created by nkronenfeld on 10/14/2015.
 */
sealed trait TypeBoolean {
  type If[T <: Up, F <: Up, Up] <: Up
}
sealed trait True extends TypeBoolean {
  type If[T <: Up, F <: Up, Up] = T
}
sealed trait False extends TypeBoolean {
  type If[T <: Up, F <: Up, Up] = F
}
object TypeBoolean
{
  type &&[A <: TypeBoolean, B <: TypeBoolean] = A#If[B, False, TypeBoolean]
  type || [A <: TypeBoolean, B <: TypeBoolean] = A#If[True, B, TypeBoolean]
  type Not[A <: TypeBoolean] = A#If[False, True, TypeBoolean]

  final class TypeBooleanRep[B <: TypeBoolean](val value: Boolean)
  implicit val falseRep: TypeBooleanRep[False] = new TypeBooleanRep(false)
  implicit val trueRep: TypeBooleanRep[True] = new TypeBooleanRep(true)
  def toBoolean[B <: TypeBoolean](implicit b: TypeBooleanRep[B]): Boolean = b.value
}

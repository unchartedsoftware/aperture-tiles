package software.uncharted.spark.pipeline.typesupport

import scala.language.higherKinds



/**
 * A type that allows comparisons of types by the compiler.
 *
 * Based tightly on the Comparison class in https://github.com/harrah/up
 *
 * Created by nkronenfeld on 10/14/2015.
 */
sealed trait TypeComparison {
  type gt <: TypeBoolean
  type ge <: TypeBoolean
  type eq <: TypeBoolean
  type le <: TypeBoolean
  type lt <: TypeBoolean
  type Match[IfLT <: Up, IfEQ <: Up, IfGT <: Up, Up] <: Up
}
sealed trait GT extends TypeComparison {
  type Match[IfLT <: Up, IfEQ <: Up, IfGT <: Up, Up] = IfGT
  type eq = False
  type gt = True
  type lt = False
  type le = False
  type ge = True
}
sealed trait LT extends TypeComparison {
  type Match[IfLT <: Up, IfEQ <: Up, IfGT <: Up, Up] = IfLT
  type eq = False
  type gt = False
  type lt = True
  type le = True
  type ge = False
}
sealed trait EQ extends TypeComparison {
  type Match[IfLT <: Up, IfEQ <: Up, IfGT <: Up, Up] = IfEQ
  type eq = True
  type gt = False
  type lt = False
  type le = True
  type ge = True
}
object Comparison {
  def show[C <: TypeComparison](implicit rep: ComparisonRep[C]): String = rep.value
  implicit def eqToRep: ComparisonRep[EQ] = new ComparisonRep[EQ]("eq")
  implicit def ltToRep: ComparisonRep[LT] = new ComparisonRep[LT]("lt")
  implicit def gtToRep: ComparisonRep[GT] = new ComparisonRep[GT]("gt")
  final class ComparisonRep[+C <: TypeComparison](val value: String)
}

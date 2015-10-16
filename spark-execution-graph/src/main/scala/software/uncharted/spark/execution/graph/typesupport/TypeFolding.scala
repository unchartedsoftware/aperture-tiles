package software.uncharted.spark.execution.graph.typesupport

import scala.language.higherKinds



/**
 * A foldable type trait, based on the Folds class in https://github.com/harrah/up, for
 * foldable types - i.e., compound types for which some information is needed by the compiler
 * about the component types.
 */
trait Foldable[-Element, Value] {
  type Apply[E <: Element, Accumulation <: Value] <: Value
  def apply[E <: Element, Accumulation <: Value](e: E, accum: Accumulation): Apply[E, Accumulation]
}
/**
 * Some standard folding functions
 */
object Foldable {
  type Inc = Foldable[Any, TypeOrdinal] {
    type Apply[E <: Any, Accumulation <: TypeOrdinal] = Succ[Accumulation]
  }
}

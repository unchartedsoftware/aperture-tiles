package software.uncharted.spark.execution.graph

import software.uncharted.spark.execution.graph.typesupport.Foldable

import scala.language.higherKinds
import scala.language.implicitConversions
import ExecutionGraphData._



/**
 * Created by nkronenfeld on 10/15/2015.
 */
object ExecutionGraphData {
  // type alias for writing EGDCons[H, T] as H :: T
  type ::[H, T <: ExecutionGraphData] = EGDCons[H, T]
  object :: {
    def unapply[H, T <: ExecutionGraphData](data: EGDCons[H, T]) = Some((data.head, data.tail))
  }

  type :::[A <: ExecutionGraphData, B <: ExecutionGraphData] = A#FoldR[ExecutionGraphData, ApplyEGDCons.type, B]

  implicit def egDataOps[B <: ExecutionGraphData](b: B): EGDataOps[B] =
    new EGDataOps[B] {
      def ::[A](a: A) = a :: b
      def :::[A <: ExecutionGraphData](a: A): A#FoldR[ExecutionGraphData, ApplyEGDCons.type, B] =
        a.foldR[ExecutionGraphData, ApplyEGDCons.type, B](ApplyEGDCons, b)
    }

  object ApplyEGDCons extends Foldable[Any, ExecutionGraphData] {
    type Apply[A <: Any, D <: ExecutionGraphData] = A :: D
    def apply[A, B <: ExecutionGraphData] (a: A, b: B) = EGDCons(a, b)
  }
}

sealed trait ExecutionGraphData {
  type Head
  type Tail <: ExecutionGraphData

  type FoldR[Value, F <: Foldable[Any, Value], I <: Value] <: Value
  def foldR[Value, F <: Foldable[Any, Value], I <: Value](f: F, i: I): FoldR[Value, F, I]
}

final case class EGDCons[H, T <: ExecutionGraphData] (head: H, tail: T) extends ExecutionGraphData {
  type Head = H
  type Tail = T

  type FoldR[Value, F <: Foldable[Any, Value], I <: Value] = F#Apply[H, tail.FoldR[Value, F, I]]
  def foldR[Value, F <: Foldable[Any, Value], I <: Value](f: F, i: I): FoldR[Value, F, I] = f(head, tail.foldR[Value, F, I](f, i) )

  override def toString = head + " :: " + tail
  def ::[HH] (newHead: HH) = EGDCons(newHead, this)
}

sealed class EGDNil extends ExecutionGraphData {
  type Head = Nothing
  type Tail = EGDNil

  type FoldR[Value, F <: Foldable[Any, Value], I <: Value] = I
  def foldR[Value, F <: Foldable[Any, Value], I <: Value](f: F, i: I) = i

  def ::[HH] (newHead: HH) = EGDCons(newHead, this)
}
// One must be careful with use of this - it is a slightly different type than class EGDNil; if one can specifically
// assign the type, that's fine, but where the type is inferred, and there is a warning of EGDNil.type vs. EGDNil, just
// use new EGDNil instead..
object EGDNil extends EGDNil

sealed trait EGDataOps[B <: ExecutionGraphData] {
  def :::[A <: ExecutionGraphData](a: A): A ::: B
  def ::[A](b: A): A :: B
}

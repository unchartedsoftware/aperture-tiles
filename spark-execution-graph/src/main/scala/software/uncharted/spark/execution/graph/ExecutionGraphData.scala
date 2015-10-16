/*
 * Copyright © 2013-2015 Uncharted Software Inc.
 *
 * Property of Uncharted™, formerly Oculus Info Inc.
 * http://uncharted.software/
 *
 * Released under the MIT License.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package software.uncharted.spark.execution.graph



import scala.language.higherKinds
import scala.language.implicitConversions
import ExecutionGraphData._

import software.uncharted.spark.execution.graph.typesupport.Foldable



/**
 * A trait describing the data passed from one or more nodes of an execution graph to a single subsequent node.  Using
 * a container type rather than a simple type allows us to form graphs, where a node takes input from not one, but
 * from an arbitrary number of input nodes.
 *
 * Essentially, this type is a linked meta-list of the individual datum types required by a node.
 *
 * Created by nkronenfeld on 10/15/2015.
 */
sealed trait ExecutionGraphData {
  type Head
  type Tail <: ExecutionGraphData

  type FoldR[Value, F <: Foldable[Any, Value], I <: Value] <: Value
  def foldR[Value, F <: Foldable[Any, Value], I <: Value](f: F, i: I): FoldR[Value, F, I]
}

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

/**
 * An single cons cell in the linked meta-list that is an ExecutionGraphData
 * @param head The first element
 * @param tail The rest of the elements
 * @tparam H The type of the first element
 * @tparam T The conglomerated type of the rest of the elements
 */
final case class EGDCons[H, T <: ExecutionGraphData] (head: H, tail: T) extends ExecutionGraphData {
  type Head = H
  type Tail = T

  type FoldR[Value, F <: Foldable[Any, Value], I <: Value] = F#Apply[H, tail.FoldR[Value, F, I]]
  def foldR[Value, F <: Foldable[Any, Value], I <: Value](f: F, i: I): FoldR[Value, F, I] = f(head, tail.foldR[Value, F, I](f, i) )

  override def toString = head + " :: " + tail
  def ::[HH] (newHead: HH) = EGDCons(newHead, this)
}

/**
 * A representation of an empty input
 */
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

/**
 * An operations type representing operations that can be performed on ExecutionGraphNodes
 * @tparam B The type of the element on the right-hand side of these operations.
 */
sealed trait EGDataOps[B <: ExecutionGraphData] {
  def :::[A <: ExecutionGraphData](a: A): A ::: B
  def ::[A](b: A): A :: B
}

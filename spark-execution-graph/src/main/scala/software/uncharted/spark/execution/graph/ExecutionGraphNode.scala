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



import software.uncharted.spark.execution.graph.ExecutionGraphData._


/**
 * A trait to describe how caching is to be performed on graph nodes
 */
object CacheType {
  class CacheTypeImpl (val input: Boolean, val output: Boolean) extends CacheType
  case object CACHE_NONE extends CacheTypeImpl(false, false)
  case object CACHE_INPUT extends CacheTypeImpl(true, false)
  case object CACHE_OUTPUT extends CacheTypeImpl(false, true)
  // There is no need for a CACHE_INPUT_AND_OUTPUT - if output is cached, input will never be hit a second time.

  // I've currently got the default cache level set to "none", which is what my tests currently expect; I'm unconvinced
  // that's what we actually want - I suspect "output" is at least as likely a candidate.
  val defaultCacheLevel = CACHE_NONE
}
sealed trait CacheType {
  val input: Boolean
  val output: Boolean
}

/**
 * A trait representing a single node in an execution graph
 *
 * @tparam O the type of output data produced by this stage
 */
sealed trait ExecutionGraphNode[O <: ExecutionGraphData] {
  def execute: O
}
object ExecutionGraphNode {
  // Old, possibly obsolete construction helpers; see the node(...) alternatives below.
  // These versions are required to handle output of other than 1 (0 or (2 or more)) nodes.  It's not clear to me
  // if that's a useful thing to have or not.  If not, we can get rid of the NoInput... and SingleInput
  // ExecutionGraphNodes.
  def advancedNode[O <: ExecutionGraphData] (data: O): ExecutionGraphNode[O] = {
    new NoInputExecutionGraphNode[O](data)
  }
  def advancedNode[I <: ExecutionGraphData, O <: ExecutionGraphData] (fcn: I => O,
                                                                      parent: ExecutionGraphNode[I]): ExecutionGraphNode[O] = {
    new SingleInputExecutionGraphNode[I, O](fcn)(parent)
  }
  def advancedNode[I <: ExecutionGraphData, O <: ExecutionGraphData] (fcn: I => O,
                                                                      parents: ExecutionGraphNodeInputContainer[I],
                                                                      caching: CacheType = CacheType.CACHE_NONE): ExecutionGraphNode[O] = {
    new MultiInputExecutionGraphNode[I, O](fcn)(parents, caching.output)
  }


  // Alternate construction method, possibly easier to use
  // These helper functions all assume single-valued output
  //
  // This may or may not be consolidatable into a single function using apply/map functionality that can be added to
  // ExecutionGraphNodeInputContainer and ExecutionGraphData.  If not, it's because of the desire to have simple
  // multi-variable argument lists as inputs to the transformation function that is the heart of any given node.
  //
  // The ability to wrap functions around the central transformation function for side-effects like caching of input
  // and output values, or for recording node execution order, could certainly be done that way.  I haven't bothered
  // to implement it here as it is a piece of advanced functionality isn't needed until we decide definitely to use
  // this approach.
  def node[O] (fcn: () => O, caching: CacheType = CacheType.CACHE_NONE): ExecutionGraphNode[O :: EGDNil] =
    new MultiInputExecutionGraphNode[EGDNil, O :: EGDNil](input => fcn() :: EGDNil)(new EGNINil(caching.input), caching.output)
  def node[A, O] (fcn: A => O,
                  parentA: ExecutionGraphNode[A :: EGDNil],
                  caching: CacheType = CacheType.CACHE_NONE): ExecutionGraphNode[O :: EGDNil] =
    new MultiInputExecutionGraphNode[A :: EGDNil, O :: EGDNil]((input: A :: EGDNil) => {
      val a :: endNil = input
      fcn(a) :: EGDNil
    })(parentA :: new EGNINil(caching.input), caching.output)
  def node[A, B, O] (fcn: (A, B) => O,
                     parentA: ExecutionGraphNode[A :: EGDNil],
                     parentB: ExecutionGraphNode[B :: EGDNil],
                     caching: CacheType = CacheType.CACHE_NONE): ExecutionGraphNode[O :: EGDNil] =
    new MultiInputExecutionGraphNode[A :: B :: EGDNil, O :: EGDNil]((input: A :: B :: EGDNil) => {
      val a :: b :: endNil = input
      fcn(a, b) :: EGDNil
    })(parentA :: parentB :: new EGNINil(caching.input), caching.output)
  def node[A, B, C, O] (fcn: (A, B, C) => O,
                        parentA: ExecutionGraphNode[A :: EGDNil],
                        parentB: ExecutionGraphNode[B :: EGDNil],
                        parentC: ExecutionGraphNode[C :: EGDNil],
                        caching: CacheType = CacheType.CACHE_NONE): ExecutionGraphNode[O :: EGDNil] =
    new MultiInputExecutionGraphNode[A :: B :: C :: EGDNil, O :: EGDNil]((input: A :: B :: C :: EGDNil) => {
      val a :: b :: c :: endNil = input
      fcn(a, b, c) :: EGDNil
    })(parentA :: parentB :: parentC :: new EGNINil(caching.input), caching.output)
  def node[A, B, C, D, O] (fcn: (A, B, C, D) => O,
                           parentA: ExecutionGraphNode[A :: EGDNil],
                           parentB: ExecutionGraphNode[B :: EGDNil],
                           parentC: ExecutionGraphNode[C :: EGDNil],
                           parentD: ExecutionGraphNode[D :: EGDNil],
                           caching: CacheType = CacheType.CACHE_NONE): ExecutionGraphNode[O :: EGDNil] =
    new MultiInputExecutionGraphNode[A :: B :: C :: D :: EGDNil, O :: EGDNil]((input: A :: B :: C :: D :: EGDNil) => {
      val a :: b :: c :: d :: endNil = input
      fcn(a, b, c, d) :: EGDNil
    })(parentA :: parentB :: parentC :: parentD :: new EGNINil(caching.input), caching.output)
  def node[A, B, C, D, E, O] (fcn: (A, B, C, D, E) => O,
                              parentA: ExecutionGraphNode[A :: EGDNil],
                              parentB: ExecutionGraphNode[B :: EGDNil],
                              parentC: ExecutionGraphNode[C :: EGDNil],
                              parentD: ExecutionGraphNode[D :: EGDNil],
                              parentE: ExecutionGraphNode[E :: EGDNil],
                              caching: CacheType = CacheType.CACHE_NONE): ExecutionGraphNode[O :: EGDNil] =
    new MultiInputExecutionGraphNode[A :: B :: C :: D :: E :: EGDNil, O :: EGDNil]((input: A :: B :: C :: D :: E:: EGDNil) => {
      val a :: b :: c :: d :: e :: endNil = input
      fcn(a, b, c, d, e) :: EGDNil
    })(parentA :: parentB :: parentC :: parentD :: parentE :: new EGNINil(caching.input), caching.output)
}

/**
 * An implementation of ExecutionGraphNode which requires no input.
 *
 * Technically, this is extraneous; one could use a MultiInputExecutionGraphNode for everything, in this case, with
 * an input type of EGDNil.  This class is here just for convenience.
 *
 * @param output The output to be passed to any child nodes when this node is executed.
 * @tparam O The type of output data produced by this stage
 */
class NoInputExecutionGraphNode[O <: ExecutionGraphData](output: O) extends ExecutionGraphNode[O] {
  def execute: O = output
}

/**
 * An implementation of ExecutionGraphNode which requires a single other node as input.
 *
 * Technically, this is extraneous; one could use a MultiInputExecutionGraphNode for everything, in this case, with
 * an ExecutionGraphData with only one datum.
 *
 * @param fcn The function to run when this graph node is asked to execute
 * @param parent The parent node, from which this node gets its input data
 * @tparam I The type of input data expected by this stage.
 * @tparam O The type of output data produced by this stage
 */
class SingleInputExecutionGraphNode[I <: ExecutionGraphData, O <: ExecutionGraphData] (fcn: I => O)(parent: ExecutionGraphNode[I]) extends ExecutionGraphNode[O] {
  def execute: O = fcn(parent.execute)
}

/**
 * An implementation of ExecutionGraphNode which can handle an arbitrary list of input nodes feeding into it.
 *
 * @param fcn The function to run on the output of all nodes feeding into this node when it is called upon to execute.
 * @param parents The parent nodes from which this node requires data.
 * @tparam I The conglomerated
 * @tparam O the type of output data produced by this stage
 */
class MultiInputExecutionGraphNode[I <: ExecutionGraphData, O <: ExecutionGraphData] (fcn: I => O)
                                                                                     (parents: ExecutionGraphNodeInputContainer[I],
                                                                                      caching: Boolean = false)

  extends ExecutionGraphNode[O] {
  lazy val cache = fcn(parents.get)

  def execute: O = if (caching) cache else fcn(parents.get)
}

/**
 * A trait to represent the parent node(s) from which a graph node gets its input data.  This works much the same
 * way as ExecutionGraphNode, as a linked meta-list of types, but with a further level of indirection that allows
 * getting both the conglomerated type and data of the input nodes.
 * @tparam D
 */
sealed trait ExecutionGraphNodeInputContainer[D <: ExecutionGraphData] {
  type Head = D#Head
  type Tail = ExecutionGraphNodeInputContainer[D#Tail]
  type Data = D

  def get: D
  val cacheInput: Boolean

  def ::[H <: ExecutionGraphData] (head: ExecutionGraphNode[H]): ExecutionGraphNodeInputContainer[H ::: D] = EGNICons[H, D](head, this, cacheInput)
}

/**
 * A representation of the type of an empty list of input nodes
 */
sealed class EGNINil(val cacheInput: Boolean = false) extends ExecutionGraphNodeInputContainer[EGDNil] {
  def get = EGDNil
}
case object UncachedNil extends EGNINil(false)
case object CachedNil extends EGNINil(true)

/**
 * A representation of a single cons cell in a linked list of input nodes
 * @param headNode The first input node (at least, as is known from this point)
 * @param tail The rest of the input nodes (at least, as is known from this point)
 * @tparam H The type of the first input node
 * @tparam T The conglomerated type of the rest of the input nodes.
 */
final case class EGNICons[H <: ExecutionGraphData, T <: ExecutionGraphData] (headNode: ExecutionGraphNode[H],
                                                                             tail: ExecutionGraphNodeInputContainer[T],
                                                                             val cacheInput: Boolean = false)
  extends ExecutionGraphNodeInputContainer[H ::: T]
{
  lazy val cache: H ::: T = headNode.execute ::: tail.get
  def get =
    if (cacheInput) cache
    else headNode.execute ::: tail.get
}

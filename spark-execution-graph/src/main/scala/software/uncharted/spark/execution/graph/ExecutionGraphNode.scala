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
 * A trait representing a single node in an execution graph
 *
 * @tparam O the type of output data produced by this stage
 */
sealed trait ExecutionGraphNode[O <: ExecutionGraphData] {
  def execute: O
}
object ExecutionGraphNode {
  def apply[O <: ExecutionGraphData] (data: O): ExecutionGraphNode[O] = {
    new NoInputExecutionGraphNode[O](data)
  }

  def apply[I <: ExecutionGraphData, O <: ExecutionGraphData] (fcn: I => O, parent: ExecutionGraphNode[I]): ExecutionGraphNode[O] = {
    new SingleInputExecutionGraphNode[I, O](fcn)(parent)
  }

  def apply[I <: ExecutionGraphData, O <: ExecutionGraphData] (fcn: I => O, parents: ExecutionGraphNodeInputContainer[I]): ExecutionGraphNode[O] = {
    new MultiInputExecutionGraphNode[I, O](fcn)(parents)
  }
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
                                                                                     (parents: ExecutionGraphNodeInputContainer[I])
  extends ExecutionGraphNode[O] {
  def execute: O = fcn(parents.get)
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

  def ::[H <: ExecutionGraphData] (head: ExecutionGraphNode[H]): ExecutionGraphNodeInputContainer[H ::: D] = EGNICons[H, D](head, this)
}

/**
 * A representation of the type of an empty list of input nodes
 */
sealed class EGNINil extends ExecutionGraphNodeInputContainer[EGDNil] {
  def get = EGDNil
}
case object EGNINil extends EGNINil

/**
 * A representation of a single cons cell in a linked list of input nodes
 * @param headNode The first input node (at least, as is known from this point)
 * @param tail The rest of the input nodes (at least, as is known from this point)
 * @tparam H The type of the first input node
 * @tparam T The conglomerated type of the rest of the input nodes.
 */
final case class EGNICons[H <: ExecutionGraphData, T <: ExecutionGraphData] (headNode: ExecutionGraphNode[H],
                                                                             tail: ExecutionGraphNodeInputContainer[T])
  extends ExecutionGraphNodeInputContainer[H ::: T]
{
  def get = headNode.execute ::: tail.get
}

package software.uncharted.spark.pipeline



import software.uncharted.spark.pipeline.ExecutionGraphData._



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

sealed trait ExecutionGraphNodeInputContainer[D <: ExecutionGraphData] {
  type Head = D#Head
  type Tail = ExecutionGraphNodeInputContainer[D#Tail]
  type Data = D

  def get: D

  def ::[H <: ExecutionGraphData] (head: ExecutionGraphNode[H]): ExecutionGraphNodeInputContainer[H ::: D] = EGNICons[H, D](head, this)
}
sealed class EGNINil extends ExecutionGraphNodeInputContainer[EGDNil] {
  def get = EGDNil
}
case object EGNINil extends EGNINil

final case class EGNICons[H <: ExecutionGraphData, T <: ExecutionGraphData] (headNode: ExecutionGraphNode[H],
                                                                             tail: ExecutionGraphNodeInputContainer[T])
  extends ExecutionGraphNodeInputContainer[H ::: T]
{
  def get = headNode.execute ::: tail.get
}

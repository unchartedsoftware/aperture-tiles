package software.uncharted.spark.pipeline



import scala.language.implicitConversions

import software.uncharted.spark.pipeline.PipelineData._



/**
 * @tparam O the type of output data produced by this stage
 */
sealed trait PipelineStage[O <: PipelineData] {
  def execute: O
}
object PipelineStage {
  def apply[O <: PipelineData] (data: O): PipelineStage[O] = {
    new ZeroInputPipelineStage[O](data)
  }

  def apply[I <: PipelineData, O <: PipelineData] (fcn: I => O, parent: PipelineStage[I]): PipelineStage[O] = {
    new OneInputPipelineStage[I, O](fcn)(parent)
  }

  def apply[I <: PipelineData, O <: PipelineData] (fcn: I => O, parents: PipelineStageInputContainer[I]): PipelineStage[O] = {
    new MultiInputPipelineStage[I, O](fcn)(parents)
  }
}
class ZeroInputPipelineStage[O <: PipelineData](output: O) extends PipelineStage[O] {
  def execute: O = output
}
class OneInputPipelineStage[I <: PipelineData, O <: PipelineData] (fcn: I => O)(parent: PipelineStage[I]) extends PipelineStage[O] {
  def execute: O = fcn(parent.execute)
}
class MultiInputPipelineStage[I <: PipelineData, O <: PipelineData] (fcn: I => O)
                                                                    (parents: PipelineStageInputContainer[I])
  extends PipelineStage[O] {
  def execute: O = fcn(parents.get)
}

sealed trait PipelineStageInputContainer[D <: PipelineData] {
  type Head = D#Head
  type Tail = PipelineStageInputContainer[D#Tail]
  type Data = D

  def get: D

  def ::[H <: PipelineData] (head: PipelineStage[H]): PipelineStageInputContainer[H ::: D] = PSICons[H, D](head, this)
}
sealed class PSINil extends PipelineStageInputContainer[PDNil] {
  def get = PDNil
}
case object PSINil extends PSINil

final case class PSICons[H <: PipelineData, T <: PipelineData] (headStage: PipelineStage[H], tail: PipelineStageInputContainer[T]) extends PipelineStageInputContainer[H ::: T] {
  def get = headStage.execute ::: tail.get
}



// Ideal code:
//  attempted structures:
//
//
//
//    val pipeline = new Node(a) to (new Node(b) to (new Node(c) to (new Node(d))))
//
//
//    val root = new Node(1a) to (new Node(2a) to new Node(3a)) andTo (new Node(2b) to new Node(3b) andTo new Node(3c))

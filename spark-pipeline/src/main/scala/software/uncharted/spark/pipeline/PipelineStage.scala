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

  // Just some compilation tests
  def testComplexExample: Unit = {
    //  complex example:
    //     1a      1b  1c      1d
    //       \    /      \    /
    //        \  /        \  /
    //         2a          2b
    //        /  \__    __/  \____
    //       /      \  /      \   \
    //     3a        3b        3c  3d
    //
    //    val node2a = new Node(2a) from new Node(1a) andFrom new Node(1b) to new Node(3a)
    //    val node2b = new Node(2b) from new Node(1c) andFrom new Node(1d) to new Node(3c) andTo new Node(3d)
    //    val node3b = new Node(3b) from node2a andFrom node2b
    val n1a = PipelineStage(1 :: PDNil)
    val n1b = PipelineStage("1" :: PDNil)
    val n1c = PipelineStage(1.2 :: PDNil)
    val n1d: PipelineStage[PDNil] = PipelineStage(PDNil)

    val n2a = PipelineStage((input: (Int :: PDNil) :: (String :: PDNil) :: PDNil) => {
      val (aI :: aN) :: (bS :: bN) :: cN = input
      ((aI * 3) + "=" + bS) :: PDNil
    }, n1a :: n1b :: PSINil)
    val n2b = PipelineStage((input: (Double :: PDNil) :: PDNil :: PDNil) => {
      val (aD :: aN) :: bN :: cN = input
      (aD * 4.5) :: PDNil
    }, n1c :: n1d :: PSINil)

    val n3a = PipelineStage((input: String :: PDNil) => {
      val value :: endnil = input
      (value + value) :: PDNil
    }, n2a)
    val n3b = PipelineStage((input: (String :: PDNil) :: (Double :: PDNil) :: PDNil) => {
      val (aS :: aN) :: (dD :: dN) :: n = input
      """{"string-value": "%s", "double-value": %f}""".format(aS, dD) :: PDNil
    }, n2a :: n2b :: PSINil)
    val n3c = PipelineStage((input: Double :: PDNil) => {
      val value :: endnil = input
      (value + ":" + value) :: PDNil
    }, n2b)
    val nfc = PipelineStage((input: Double :: PDNil) => {
      val value :: endnil = input
      (value + value) :: PDNil
    }, n2b)

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

  def ::[H <: PipelineData] (head: PipelineStage[H]): PipelineStageInputContainer[H :: D] = PSICons[H, D](head, this)
}
sealed class PSINil extends PipelineStageInputContainer[PDNil] {
  def get = PDNil
}
case object PSINil extends PSINil

final case class PSICons[H <: PipelineData, T <: PipelineData] (headStage: PipelineStage[H], tail: PipelineStageInputContainer[T]) extends PipelineStageInputContainer[PDCons[H, T]] {
  def get = PDCons[H, T](headStage.execute, tail.get)
}



// Ideal code:
//  attempted structures:
//
//
//  simple example:
//     a --> b --> c --> d
//
//    val pipeline = new Node(a) to (new Node(b) to (new Node(c) to (new Node(d))))
//
//  tree example:
//
//        1a
//       /  \
//      /    \
//    2a      2b
//     |     /  \
//     |    /    \
//    3a  3b      3c
//
//    val root = new Node(1a) to (new Node(2a) to new Node(3a)) andTo (new Node(2b) to new Node(3b) andTo new Node(3c))

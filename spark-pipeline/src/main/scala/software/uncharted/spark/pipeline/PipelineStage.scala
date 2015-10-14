package software.uncharted.spark.pipeline



import scala.language.implicitConversions

import software.uncharted.spark.pipeline.PipelineData._



/**
 * @tparam O the type of output data produced by this stage
 */
sealed trait PipelineStage[O <: PipelineData] {
  def execute (): O
}

class PSNil extends PipelineStage[PDNil] {
  def execute (): PDNil = PDNil
}

class PSSingle[I <: PipelineData, O <: PipelineData] (fcn: I => O)
                                                     (parent: PipelineStage[I])
  extends PipelineStage[O]
{
  def execute (): O = {
    fcn(parent.execute())
  }
}

class PSCollection[I1 <: PipelineData, I2 <: PipelineData, O <: PipelineData] (fcn: I1 :: I2 => O)
                                                                              (parent1: PipelineStage[I1],
                                                                               parent2: PipelineStage[I2])
  extends PipelineStage[O]
{
  def execute (): O = {
    fcn(parent1.execute() :: parent2.execute())
  }
}



object PipelineStage {
  // Just some compilation tests
  def testCompilation: Unit = {
    val n1a = new PSNil
    val n1b = new PSNil
    val n1c = new PSNil
    val n1d = new PSNil

    val n2a = new PSNil
//    val n2a = new PSCollection2[PDNil :: PDNil :: PDNil, PDNil](
//      (i: PDNil :: PDNil :: PDNil) => PDNil)
//    (n1a and_: n1b and_: PRNil)
//    val n2a = new PSCollection[PDNil, PDNil, PDNil](i => PDNil)(n1a, n1b)
    val n2b = new PSCollection[PDNil, PDNil, PDNil](i => PDNil)(n1c, n1d)

    val n3a = new PSSingle[PDNil, PDNil](i => PDNil)(n2a)
    val n3b = new PSCollection[PDNil, PDNil, PDNil](i => PDNil)(n2a, n2b)
    val n3c = new PSSingle[PDNil, PDNil](i => PDNil)(n2b)
    val n3d = new PSSingle[PDNil, PDNil](i => PDNil)(n2b)
  }
}


// Ideal code:
//  attempted structures:
//
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

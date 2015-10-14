package software.uncharted.spark.pipeline



import software.uncharted.spark.pipeline.PipelineData._



trait Indirect[D <: PipelineData] {
  def get: D
}
object Indirect {
  def apply[D <: PipelineData] (d: D): Indirect[D] = IndirectImpl(d)

  def testCompilation: Unit = {
    //    val i1: Indirect[PDNil] = IndirectImpl(PDNil)
    //    val i2: Indirect[PDNil] = IndirectImpl(PDNil)

    val i1: Indirect[PDNil] = Indirect(PDNil)
    val i2: Indirect[PDNil] = Indirect(PDNil)

    val list1: IList[PDNil :: PDNil] = i1 :: INil
    val value1: PDNil :: PDNil = list1.get

    val list2: IList[PDNil :: PDNil :: PDNil] = i1 :: (i2 :: INil)
    val value2: PDNil :: PDNil :: PDNil = list2.get
  }
}

case class IndirectImpl[D <: PipelineData] (d: D) extends Indirect[D] {
  def get = d
}

sealed trait IList[D <: PipelineData] {
  type Head = D#Head
  type Tail = IList[D#Tail]
  type Data = 

  def get: D

  def ::[H <: PipelineData] (head: Indirect[H]): IList[H :: D] = ICons[H, D, PDCons[H, D]](head, this)
}

sealed class INil extends IList[PDNil] {
  def get = PDNil
}
case object INil extends INil

final case class ICons[H <: PipelineData, T <: PipelineData, PD =: PDCons[H, T]] (headContainer: Indirect[H], tail: IList[T]) extends IList[PD] {
  def get = PDCons[H, T](headContainer.get, tail.get)
}

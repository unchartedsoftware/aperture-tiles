package software.uncharted.spark.pipeline



import scala.language.implicitConversions
import software.uncharted.spark.pipeline.PipelineData._



/**
 * An type that acts as an index into compound types.
 *
 * Based tightly on Indexed in https://github.com/harrah/up
 *
 * Created by nkronenfeld on 10/14/2015.
 */
sealed trait PipelineDataIndex {
  type Before <: PipelineData
  type After <: PipelineData
  type At

  def withIndex[R](f: (Before, At, After) => R): R
  def at = withIndex((_, a, _) => a)
  def drop = withIndex((_, a, t) => a :: t)
  def take = withIndex((b, _, _) => b)
  def replace[A](a: A) = withIndex((b, _, t) => b ::: a :: t)
  def remove = withIndex((b, _, t) => b ::: t)
  def map[B](f: At => B) = withIndex((b, a, t) => b ::: f(a) :: t)
  def flatMap[B <: PipelineData](f: At => B) = withIndex((b, a, t) => b ::: f(a) ::: t)
  def insert[C](c: C) = withIndex((b, a, t) => b ::: c :: a :: t)
  def insertH[C <: PipelineData](c: C) = withIndex((b, a, t) => b ::: c ::: a :: t)
  def splitAt = withIndex((b, a, t) => (b, a :: t))
}

final class PipelineDataIndex0[H, T <: PipelineData](val hc: H :: T) extends PipelineDataIndex {
  type Before = PDNil
  type After = T
  type At = H

  def withIndex[R](f: (Before, At, After) => R): R = f(PDNil, hc.head, hc.tail)
}

final class PipelineDataIndexN[H, I <: PipelineDataIndex](h: H, iTail: I) extends PipelineDataIndex {
    type Before = H :: I#Before
    type At = I#At
    type After = I#After
    def withIndex[R](f: (Before, At, After) =>  R): R = iTail.withIndex( (before, at, after) => f( PDCons(h, before), at, after) )
}

object PipelineDataIndex {
  implicit def index0[H, T <: PipelineData](hc: H :: T): PipelineDataIndex0[H, T] = new PipelineDataIndex0[H, T](hc)
  implicit def indexN[H, T <: PipelineData, I <: PipelineDataIndex](hc: H :: T)(implicit iTail: T => I): PipelineDataIndexN[H, I] = new PipelineDataIndexN[H, I](hc.head, iTail(hc.tail))
}

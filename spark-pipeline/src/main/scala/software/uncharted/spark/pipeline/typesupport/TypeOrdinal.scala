package software.uncharted.spark.pipeline.typesupport

import scala.language.higherKinds
import software.uncharted.spark.pipeline.typesupport.TypeOrdinal._



/**
 * An ordinal number type, based on the Nat type in https://github.com/harrah/up, for
 * referencing and matching within foldable types; this allows types that are indices
 * into compound, foldable types.
 *
 * Created by nkronenfeld on 10/14/2015.
 */
object TypeOrdinal {
  type _0 = TypeOrdinal0
  type _1 = Succ[_0]
  type _2 = Succ[_1]
  type _3 = Succ[_2]
  type _4 = Succ[_3]
  type _5 = Succ[_4]
  type _6 = Succ[_5]
  type _7 = Succ[_6]
  type _8 = Succ[_7]
  type _9 = Succ[_8]
  type _10 = Succ[_9]

  type Compare[A <: TypeOrdinal, B <: TypeOrdinal] = A#Compare[B]
  type ===[A <: TypeOrdinal, B <: TypeOrdinal] = A#Compare[B]#eq
  type Is0[A <: TypeOrdinal] = A#Match[ConstFalse, True, TypeBoolean]

  type +[A <: TypeOrdinal, B <: TypeOrdinal] = A#FoldR[B, TypeOrdinal, Foldable.Inc]

  type ConstLT[A] = LT
  type ConstFalse[A] = False

  implicit def ordinalRep0: OrdinalRep[_0] = new OrdinalRep[_0](0)
  implicit def ordinalRepN[N <: TypeOrdinal](implicit rep: OrdinalRep[N]): OrdinalRep[Succ[N]] = rep.succ
  final class OrdinalRep[N <: TypeOrdinal](val value: Int) { def succ: OrdinalRep[Succ[N]] = new OrdinalRep[Succ[N]](value+1) }

  // for converting a type to a value.  Note that this will stack overflow for reasonably large numbers
  def toInt[N <: TypeOrdinal](implicit rep: OrdinalRep[N]): Int = rep.value

}

sealed trait TypeOrdinal {
  type FoldR[Init <: Type, Type, F <: Foldable[TypeOrdinal, Type]] <: Type
  type FoldL[Init <: Type, Type, F <: Foldable[TypeOrdinal, Type]] <: Type
  type Compare[N <: TypeOrdinal] <: TypeComparison
  type Match[NonZero[N <: TypeOrdinal] <: Up, IfZero <: Up, Up] <: Up
}
sealed trait TypeOrdinal0 extends TypeOrdinal {
  type FoldR[Init <: Type, Type, F <: Foldable[TypeOrdinal, Type]] = Init
  type FoldL[Init <: Type, Type, F <: Foldable[TypeOrdinal, Type]] = Init
  type Match[NonZero[N <: TypeOrdinal] <: Up, IfZero <: Up, Up] = IfZero
  type Compare[N <: TypeOrdinal] = N#Match[ConstLT, EQ, TypeComparison]
}
sealed trait Succ[N <: TypeOrdinal] extends TypeOrdinal {
  type FoldR[Init <: Type, Type, F <: Foldable[TypeOrdinal, Type]] = F#Apply[Succ[N], N#FoldR[Init, Type, F]]
  type FoldL[Init <: Type, Type, F <: Foldable[TypeOrdinal, Type]] = N#FoldL[F#Apply[Succ[N],Init], Type, F]
  type Match[NonZero[N <: TypeOrdinal] <: Up, IfZero <: Up, Up] = NonZero[N]
  type Compare[O <: TypeOrdinal] = O#Match[N#Compare, GT, TypeComparison]
}

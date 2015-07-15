/*
 * Copyright (c) 2015 Uncharted Software Inc.
 * http://www.oculusinfo.com/
 *
 * Released under the MIT License.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.uncharted.tile.source.util

import java.io.{ObjectOutputStream, ByteArrayInputStream, ObjectInputStream}

import org.apache.commons.io.output.ByteArrayOutputStream

import scala.reflect.runtime.universe.TypeTag



object ByteArrayCommunicator {
  private val _default: ByteArrayCommunicator = new JavaSerializationByteArrayCommunicator
  def defaultCommunicator: ByteArrayCommunicator = _default
}

/**
 * A parser that parses a byte array into objects of an expected type
 */
trait ByteArrayCommunicator {
  def read[T0: TypeTag] (source: Array[Byte]): T0
  def read[T0: TypeTag, T1: TypeTag] (source: Array[Byte]): (T0, T1)
  def read[T0: TypeTag, T1: TypeTag, T2: TypeTag] (source: Array[Byte]): (T0, T1, T2)
  def read[T0: TypeTag, T1: TypeTag, T2: TypeTag, T3: TypeTag] (source: Array[Byte]): (T0, T1, T2, T3)
  def read[T0: TypeTag, T1: TypeTag, T2: TypeTag, T3: TypeTag, T4: TypeTag] (source: Array[Byte]): (T0, T1, T2, T3, T4)
  def read[T0: TypeTag, T1: TypeTag, T2: TypeTag, T3: TypeTag, T4: TypeTag, T5: TypeTag] (source: Array[Byte]): (T0, T1, T2, T3, T4, T5)
  def read[T0: TypeTag, T1: TypeTag, T2: TypeTag, T3: TypeTag, T4: TypeTag, T5: TypeTag, T6: TypeTag] (source: Array[Byte]): (T0, T1, T2, T3, T4, T5, T6)

  def write (values: Any*): Array[Byte]
}

class JavaSerializationByteArrayCommunicator extends ByteArrayCommunicator {
  private def readNext[T: TypeTag] (source: ObjectInputStream): T = {
    val tag = scala.reflect.runtime.universe.typeTag[T]
    val rawValue = tag match {
      case TypeTag.Boolean => source.readBoolean
      case TypeTag.Byte => source.readByte
      case TypeTag.Char => source.readChar
      case TypeTag.Short => source.readShort
      case TypeTag.Int => source.readInt
      case TypeTag.Long => source.readLong
      case TypeTag.Float => source.readFloat
      case TypeTag.Double => source.readDouble
      case _ => source.readObject()
    }
    rawValue.asInstanceOf[T]
  }

  override def read[T0: TypeTag](source: Array[Byte]): T0 = {
    val input = new ObjectInputStream(new ByteArrayInputStream(source))
    try {
      (readNext[T0](input))
    } finally {
      input.close()
    }
  }

  override def read[T0: TypeTag, T1: TypeTag](source: Array[Byte]): (T0, T1) =  {
    val input = new ObjectInputStream(new ByteArrayInputStream(source))
    try {
      (readNext[T0](input), readNext[T1](input))
    } finally {
      input.close()
    }
  }

  override def read[T0: TypeTag, T1: TypeTag, T2: TypeTag](source: Array[Byte]): (T0, T1, T2) =  {
    val input = new ObjectInputStream(new ByteArrayInputStream(source))
    try {
      (readNext[T0](input), readNext[T1](input), readNext[T2](input))
    } finally {
      input.close()
    }
  }

  override def read[T0: TypeTag, T1: TypeTag, T2: TypeTag, T3: TypeTag](source: Array[Byte]): (T0, T1, T2, T3) =  {
    val input = new ObjectInputStream(new ByteArrayInputStream(source))
    try {
      (readNext[T0](input), readNext[T1](input), readNext[T2](input), readNext[T3](input))
    } finally {
      input.close()
    }
  }

  override def read[T0: TypeTag, T1: TypeTag, T2: TypeTag, T3: TypeTag, T4: TypeTag](source: Array[Byte]): (T0, T1, T2, T3, T4) =  {
    val input = new ObjectInputStream(new ByteArrayInputStream(source))
    try {
      (readNext[T0](input), readNext[T1](input), readNext[T2](input), readNext[T3](input), readNext[T4](input))
    } finally {
      input.close()
    }
  }

  override def read[T0: TypeTag, T1: TypeTag, T2: TypeTag, T3: TypeTag, T4: TypeTag, T5: TypeTag](source: Array[Byte]): (T0, T1, T2, T3, T4, T5) =  {
    val input = new ObjectInputStream(new ByteArrayInputStream(source))
    try {
      (readNext[T0](input), readNext[T1](input), readNext[T2](input), readNext[T3](input), readNext[T4](input), readNext[T5](input))
    } finally {
      input.close()
    }
  }

  override def read[T0: TypeTag, T1: TypeTag, T2: TypeTag, T3: TypeTag, T4: TypeTag, T5: TypeTag, T6: TypeTag](source: Array[Byte]): (T0, T1, T2, T3, T4, T5, T6) =  {
    val input = new ObjectInputStream(new ByteArrayInputStream(source))
    try {
      (readNext[T0](input), readNext[T1](input), readNext[T2](input), readNext[T3](input), readNext[T4](input), readNext[T5](input), readNext[T6](input))
    } finally {
      input.close()
    }
  }



  private def writeNext[T] (value: T, target: ObjectOutputStream): Unit = {
    value match {
      case b: Boolean => target.writeBoolean(b)
      case b: Byte => target.writeByte(b)
      case c: Char => target.writeChar(c)
      case s: Short => target.writeShort(s)
      case i: Int => target.writeInt(i)
      case l: Long => target.writeLong(l)
      case f: Float => target.writeFloat(f)
      case d: Double => target.writeDouble(d)
      case _ => target.writeObject(value)
    }
  }

  def write (values: Any*): Array[Byte] = {
    val baos = new ByteArrayOutputStream()
    val output = new ObjectOutputStream(baos)
    values.foreach(v => writeNext(v, output))
    output.flush
    output.close
    baos.toByteArray
  }
}

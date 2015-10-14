package software.uncharted.spark.pipeline



import scala.collection.mutable.{Map => MutableMap}
import scala.reflect.ClassTag

/**
 * A typed hashmap
 */
class PipelineMetadata {
  private val _data = MutableMap[(String, Class[_]), Any]()

  def put[T: ClassTag] (key: String, value: T): Option[T] = {
    val valueType = implicitly[ClassTag[T]].runtimeClass
    _data.put((key, valueType), value).asInstanceOf[Option[T]]
  }

  def get[T: ClassTag] (key: String): Option[T] = {
    val valueType = implicitly[ClassTag[T]].runtimeClass
    _data.get((key, valueType)).asInstanceOf[Option[T]]
  }

  def apply[T: ClassTag] (key: String): T = {
    val valueType = implicitly[ClassTag[T]].runtimeClass
    _data((key, valueType)).asInstanceOf[T]
  }

  def remove[T: ClassTag] (key: String): Option[T] = {
    val valueType = implicitly[ClassTag[T]].runtimeClass
    _data.remove((key, valueType)).asInstanceOf[Option[T]]
  }
}

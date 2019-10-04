package example

import org.apache.spark.{
  Dependency,
  Partition,
  Partitioner,
  RangeDependency,
  TaskContext
}
import org.apache.spark.rdd.{RDD, ShuffledRDD}
import scala.reflect.ClassTag

/**
  * An RDD that matches the prev RDD, but can have different preferredLocations
  *
  * @param prev starting RDD
  * @param locations new list of preferred (or fixed if they start with '*') locations
  */
class LocationRDD[T: ClassTag](
    prev: RDD[T],
    private val locations: Seq[String]
) extends RDD[T](prev.context, Nil) {

  override protected def getPreferredLocations(
      partition: Partition
  ): Seq[String] = locations

  override def getPartitions: Array[Partition] =
    prev.partitions

  override def compute(split: Partition, context: TaskContext): Iterator[T] =
    prev.iterator(split, context)

  override def getDependencies: Seq[Dependency[_]] =
    Seq(new RangeDependency(prev, 0, 0, prev.partitions.length))

}

package example

import org.apache.spark.SparkContext
import org.apache.spark.rdd.{CoalescedRDD, RDD}
import org.apache.spark.sql.{SQLContext, DataFrame}
import scala.reflect.ClassTag

package object extend_spark_api {

  implicit class ExtendedRDD[T: ClassTag](val rdd: RDD[T]) {

    /**
      * Return a repartitioned RDD that has new preferred or fixed locations.
      *
      * @param locations a list of Worker related IPs addresses
      * @param numPartitions opportunity to change the number of partitions.  Default to match the number of provided locations
      * @param fixedHost if true, locations will be prepended with an "*".  This is the marker for a fixed location.  A partition
      * will wait (forever) until a Worker is made available that is in the provided locations list
      */
    def repartitionTo(
        locations: Seq[String],
        numPartitions: Int = 0,
        fixedHost: Boolean = true
    ): RDD[T] = {
      val evaluatedNumPartitions =
        if (numPartitions == 0) locations.size else numPartitions

      val fixedOrPreferredLocations =
        if (fixedHost) locations.map(s => "*" + s).seq else locations

      new LocationRDD(
        rdd.repartition(evaluatedNumPartitions),
        fixedOrPreferredLocations
      )
    }
  }

  implicit class ExtendedSparkContext(val sparkContext: SparkContext) {

    lazy val getWorkersFromTagMap = getWorkerTagMap()

    /**
      * Return a list of Worker IP addresses given the provided Tag. Lazily loop through all Workers,
      * evaluate the SPARK_TAGS environment variable, and create a map from TAG to Worker
      *
      * @param tag to search for
      */
    def getWorkersFromTag(tag: String): Seq[String] = getWorkersFromTagMap(tag)

    private def getWorkerTagMap(): Map[String, Seq[String]] = {
      val MAX_WORKERS = 20

      // Spark bug?  This is needed to 'wake-up' all the workers
      sparkContext
        .emptyRDD[String]
        .repartition(MAX_WORKERS)
        .collect()

      // Ask each worker for the tags they support
      val data: Seq[String] = sparkContext
        .emptyRDD[String]
        .repartition(MAX_WORKERS)
        .pipe("/worker-info.sh")
        .distinct()
        .collect()

      // for example, data = Seq("172.19.0.11=GPU", "172.19.0.13=CPU1", "172.19.0.3=CPU1,CPU2,GPU")
      // transforms into Map(GPU -> List(172.19.0.11, 172.19.0.3), CPU1 -> List(172.19.0.13, 172.19.0.3), CPU2 -> List(172.19.0.3))
      data
        .map(entry => entry.split("="))
        .flatMap(
          entry => entry(1).split(',').map(tag => Seq(tag.trim, entry(0).trim))
        )
        .groupBy(_(0))
        .mapValues(_.map(_(1)))
    }

  }

  implicit class ExtendedDataFrame(val df: DataFrame) {

    /**
      * Return a repartitioned DataFrame that has new preferred or fixed locations.
      *
      * @param locations a list of Worker related IPs addresses
      * @param numPartitions opportunity to change the number of partitions.  Default to match the number of provided locations
      * @param fixedHost if true, locations will be prepended with an "*".  This is the marker for a fixed location.  A partition
      * will wait (forever) until a Worker is made available that is in the provided locations list
      */
    def repartitionTo(
        locations: Seq[String],
        numPartitions: Int = 0,
        fixedHost: Boolean = true
    ): DataFrame = {
      val rdd = df.rdd.repartitionTo(locations, numPartitions, fixedHost)
      df.sparkSession.createDataFrame(rdd, df.schema)
    }
  }

}

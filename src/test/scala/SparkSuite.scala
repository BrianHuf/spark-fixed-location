package example.extend_spark_api

import java.io.{FileWriter, BufferedWriter, File}

import org.apache.spark.SparkContext
import org.apache.spark.sql.{Row, SparkSession, SQLContext, SQLImplicits}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.{IntegerType, StringType, StructField, StructType}
import org.scalatest.{BeforeAndAfterAll, FunSuite}

trait Output {
  def print(s: String) = Console.println(s + '\n')
}

class SparkSuite extends FunSuite with BeforeAndAfterAll {
  private var spark: SparkSession = _

  private var sparkContext: SparkContext = _

  private var sqlContext: SQLContext = _

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    spark = SparkSession.builder
      .master("spark://localhost:7077")
      .appName("TEST")
      .config("spark.jars", "target/scala-2.11/spark-fixed-location_2.11-0.0.1.jar")
      .getOrCreate()
    sqlContext = spark.sqlContext
    sparkContext = spark.sparkContext
  }

  override protected def afterAll(): Unit = {
    try {
      spark.stop()
    } finally {
      super.afterAll()
    }
  }

  test("verify reparitionTo only runs on expected workers") {
    val gpuWorkers = sparkContext.getWorkersFromTag("GPU")

    val result: Seq[String] = sparkContext
      .parallelize(1 to 100)
      .repartitionTo(gpuWorkers)
      .pipe("printenv SPARK_TAGS")
      .distinct()
      .collect()

    assert(result.filter(!_.contains("GPU")).isEmpty)
  }

  test(
    "demonstrate stackoverflow question https://stackoverflow.com/questions/57909678/spark-repartition-to-specific-resources"
  ) {
    val cpu_only = udf((v: String) => v)
    val gpu_only = udf((v: String) => v)

    val dataset = spark.createDataFrame(
      sparkContext.parallelize(
        Seq(
          Row(0, "hello"),
          Row(1, "world")
        )
      ),
      StructType(
        List(
          StructField("id", IntegerType),
          StructField("text", StringType)
        )        
      )
    )

    val cpu = sparkContext.getWorkersFromTag("CPU")
    val gpu = sparkContext.getWorkersFromTag("GPU")

    dataset
      .repartitionTo(cpu)
      .withColumn("from_cpu", cpu_only(col("text")))
      .repartitionTo(gpu)
      .withColumn("from_gpu", gpu_only(col("from_cpu")))
      .show
  }
}

# Spark with Fixed (~~Preferred~~) Locations
Customize Spark to run partitions on a subset of workers.
Although Spark has the ability to define *preferred locations*, there's
no way to force partitions to a specific set of workers

This repo is an attempt to add this capability

# Why?
Suppose there's a large Spark cluster with a mixture of machines **AND** UDFs will be used that
have certain expectations on the machine type (e.g. GPU or no GPU).  Being able to run
on particular machines is critical and allow a single Spark pipeline to transition
between machine types.

# Solution Strategy
1. Create new RDD to allow manual setting of preferredLocations
2. Create the notion of a FixedLocation which prevents running on a Node that isn't preferred

To help with bookkeeping, all workers are assigned a SPARK_TAGS environment variable.
A shell script (```worker-info.sh```) is executed at runtime to tags with Workers IP addresses.

# Modified Spark
Two Spark 2.4.4 were modified.  *.ORIG files are added for reference 

## TaskLocation.scala
Add a new location subtype ```FixedHostTaskLocation```.
This class is created when a preferred location begins with a '*'

## TaskSetManager.scala
When a task set has a ```FixedHostTaskLocation```, do not allow locality to exceed NODE_LOCAL

# Example Usage
```scala
import org.apache.spark.sql.functions.udf
val cpu_only = udf((v: String) => v)
val gpu_only = udf((v: String) => v)

val dataset = Seq((0, "hello"), (1, "world")).toDF("id", "text")

import example.extend_spark_api._ // NEW

val cpu = sc.getWorkersFromTag("CPU") // NEW
val gpu = sc.getWorkersFromTag("GPU") // NEW

dataset
  .repartitionTo(cpu) // NEW
  .withColumn("from_cpu", cpu_only('text))
  .repartitionTo(gpu) // NEW
  .withColumn("from_gpu", gpu_only('from_cpu))
  .show
```

# Testing
A docker-compose is provided to simulate a heterogeneous cluster.  

```bash
docker-compose up
sbt test
```



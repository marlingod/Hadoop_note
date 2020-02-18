Advanced Configuration
dfs.namenode.handler.count :
  * the number of the server threads for the namenode
  * Threads used for RPC calls from clients  and Datanodes( heartbeats and metadata operations)
  * Defaut in CM: 30 (non_CM default is 10)
  * Recomomded: Natualal logarithm of number of cluster nodes * 20
  * Symptoms of this being is connection refused messages in datanode logs as they tried to trasnmit block report to the namenode
  * used by namenode

dfs.datanode.failed.volumes.tolerated:( set in Hdfs /Datanodes )
* the number of volumes  allowed to fail before Datanodes takes itself offline, ultimately resulting in all of its blocks being replicated
CM_default: 0
Often Increased in machines with several disk
* used by Datanodes

dfs.datanode.max.locked.memory (off heap memory space)
* the maximum amount of memory in bytes a Datanodes used for caching
* Cm default : 4GB
* Must be less than the value of the OS configuration property
* Ulimt -1 for the datande user
* used by datanode

io.compress.codecs
* list of compression codecs that hadoop can use for file compression
* used by clients and all nodes running hadoop daemons
* Cm defaul include may

mapreduce.job.reduce.slowstart.completed.maps(yarn)
* the percenrtage of Map tasks which must be completed before the resourcemanager will schedule Reucers
on the cluster
* CM default is 0.8
*Recommendation 0.8
*Used by resourcemanager

mapreduce.reduce.shuffle.parallelcopies( yarn)
* number of threads a reducer can use in parallel fectch mapper output
cm_default : 10
Recommendation: ln(clusternodes )* 4
Used by sufflehandler

mapreduce.map.speculative
* whether to allow speculative Execution for map tasks
* CM default: false Recommendation false
* Used by Mapreduce Application Master

mapreduce.reduce.speculative
* whether to allow speculative Execution for reduce tasks
* CM default: false Recommendation false
* Used by Mapreduce Application Master

The Fair Scheduler is the only scheduler recommended by cloudera for production cluster
* the fair scheduler  organized YARN applications into pools
  - Pools are also known as queues in YARN technologies
  - Each user get a pool named after the user
  - Resources  are divided fairly between the pools
* Fair scheduler
  - Allows resources  to be controlled proportionally
  -Promotes  efficient utilization of clusters resources
  - Promotes fairness between schedulable entities
  - Allows short interactive and long production applications to co-exist
  - Awards  resources  to pools that most underserved
  - Gives  a container to the pool that has fewest resources  allocated
* Fair share of resources assigned to the pool is based on:
  - the total resources available across the cluster
  - the number of pools competing for cluster resources
* Excess cluster capacity is spread across all pools
  -the aim is to maintain the most even allocation possible so every pool received its
  fair share of resources
  - the fair share will never be higher than actual demand
* Pool can use more than their fair share when other pools are not in need of resourcemanager
 - this happened when their are no tasks eligible to run in  other pools

 * Single Resources Fairness
  - Fair Scheduling with single Resource fairness
    - Single applications on the basis of a single resource: Memory
* Dominant Resource Fairness (DRF)
  - fair Scheduling with DRF(Recommended)
    Schedule based applications on the basis of both memory and CPU


MINIMIN RESOURCE:
* Minimum Resources are the minimum amount of resources that must be allocated to the
pool prior to fair share allocation
NO resources are about to pool even when the minimum resources are configured

Pools with weightsPools with high weights received more resources during allocation

Fair Scheduler Preemption
if yarn.scheduler.fair.preemption is enabled:
 - the fair scheduler kills containers  that belong to pools operating over their fair share beyond configurable timeouts
 - pools operating below fair share received those reaped resources
  - not enable by default
2 Types of scheduler preemption
- Minimum share Preemption
- Fair share Preemption
Preemption avoiding killing container in a pool if it would cause that pool to being preemption container in other pools
  -this prevent a potentially endless cycle of pools
Use fair share preemption conservatively
  - Set min share preemption timeouts to the number of seconds a pool under fair share before preemption should begin
  - Default is infinite


Yarn Memory and CPU Settings
yarn.nodemanager.resource.memory-mb:
* Amount of RAM available on this host for YARN-managed Tasks
* Recommendation: the amount of RAM on host host minus the amount of ram need for non YARN managed work. (included memory needed by datanode daemons)
* Used by nodemanagers
yarn.nodemanager.resource.cpu-vcore
* Number of cores avaialbel on this host for yarn managed tasks
* recommendation: Number of cores - 1

yarn.scheduler.minimum-allocation-mb/yarn.scheduler.minimum-allocation-vcores
* Minimum Amount of memory of cpu cores to allocate for a containers
* Tasks request lower than these minimum will set these values
* CM defaults L 1 GB 1 core
Memory recommendations: Increase up to 4gb depending on your developer requirements
* Core: Keep the defaults

yarn.scheduler.increment-allocation-mb/vcores
* tasks with request that not multiples of these increment-allocation values  will rounded up to the nearest increments
CM default 512MB 1 core

mapreduce.Imapala.memory.mb /mapreduce.reduce.memory.mb
* Amount of memory to allocation for map and reduce tasks
* CM defaults 1Gb
Recommendation Increase up to 4 gb depending on your application

yarn.app.mapreduce.am.resource.mb
* amount of the memory to allocation for the applicationMaster
CM default 1Gb
Recommendation 1Gb

yarn.app.mapreduce.am.command-opts
Java options passed to application Master
By default application master get 1Gb of Heap Space
used by Mapreduce and APplication Master

mapreduce.map.java.opts / mapreduce.reduce.java.opts
* java options passed to Mappers and Reducers
default in Xmx=200m (200MB of heap space)
Recommedations: Increase to value from 1GB to 4Gb depending on the requirements from developpers
Used when Mappers and Reducers are lunch with rule of thumb of using 75% of the heap space as you had in the container
Example :

yarn.nodemanager.resource.memory-mb: 8192
mapreduce.map.memory.mb 4096
mapreduce.map.java.opts defaults -Xmx=200
Recommendation: set java heap size for Mappers and Reducers to 75% of the
mapreduce.map.memory.mb and mapreduce.reduce.memory.mb

Inventory the vcores and memory and disks
* Calculate the resource needed for other processes
  - reserve 3gb or 20 % of total memory for the os
  - Reserve resources for any non-hadoop Component
  - Reserve resource for other any hadoop components
    Hdfs caching
    Impalad , hbase region server, solr
* grant the resources not used by the above to your yarn containers
* Configure the YARN scheduler and application frameworks setting
  - based on the worker node profile determined above
  - Determine the number of containers needed to best support YARN applications based on the type of workload
  monitor usage and tune estimated values to find optimal settings

  From Cloudera Training

  The main rules to consider in mind are:

* yarn.scheduler.minimum-allocation-mb <= mapreduce.map.memory.mb <= yarn.scheduler.maximum-allocation-mb <= yarn.nodemanager.resource.memory-mb
* yarn.scheduler.minimum-allocation-mb <= mapreduce.reduce.memory.mb <= yarn.scheduler.maximum-allocation-mb <= yarn.nodemanager.resource.memory-mb
* yarn.scheduler.minimum-allocation-mb <= yarn.app.mapreduce.am.resource.mb <= yarn.scheduler.maximum-allocation-mb <= yarn.nodemanager.resource.memory-mb
* mapreduce.map.java.opts = mapreduce.job.heap.memory-mb.ratio * mapreduce.map.memory.mb
* mapreduce.reduce.java.opts = mapreduce.job.heap.memory-mb.ratio * mapreduce.reduce.memory.mb
* yarn.app.mapreduce.am.command-opts = mapreduce.job.heap.memory-mb.ratio * yarn.app.mapreduce.am.resource.mb

Example:
```
-Dmapreduce.map.memory.mb=6144
-Dmapreduce.map.java.opts=-Xmx4096m
-Dmapreduce.reduce.memory.mb=6144
-Dmapreduce.reduce.java.opts=-Xmx4096m
-Dyarn.app.mapreduce.am.resource.mb=6144
-Dyarn.app.mapreduce.am.command-opts=-Xmx4096m
```
for hive queries. this can be set to
```
SET mapreduce.map.memory.mb=6144
SET mapreduce.map.java.opts=-Xmx4096M
SET mapreduce.reduce.memory.mb=6144
SET mapreduce.reduce.java.opts=-Xmx4096M
```

  Fixing git Issues
  https://codewithhugo.com/fix-git-failed-to-push-updates-were-rejected/

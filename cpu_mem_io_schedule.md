### I/O Schedulers
The I/O scheduler determines when and for how long I/O operations run on a storage device. It is also known as the I/O elevator. provides 3 operations:
#### deadline
The default I/O scheduler for all block devices, except for SATA disks
it is the default for RHEL7 and onward.
This scheduler is suitable for most use cases, but particularly those in which read operations occur more often than write operations.
Read batches take precedence over write batches by default, as applications are more likely to block on read I/O.
TODO( CONFIGURATION EXAMPLE)
configuration parameters
The following parameters affect the behavior of the deadline scheduler.

**fifo_batch**    
The number of read or write operations to issue in a single batch. The default value is 16. A higher value can increase throughput, but will also increase latency.   
**front_merges**  
If your workload will never generate front merges, this tunable can be set to 0. However, unless you have measured the overhead of this check, Red Hat recommends the default value of 1.   
**read_expire**   
The number of milliseconds in which a read request should be scheduled for service. The default value is 500 (0.5 seconds).   
**write_expire**    
The number of milliseconds in which a write request should be scheduled for service. The default value is 5000 (5 seconds).     
**writes_starved**  
The number of read batches that can be processed before processing a write batch. The higher this value is set, the greater the preference given to read batches.

#### cfq
The default scheduler only for devices identified as SATA disks.  The Completely Fair Queueing scheduler, cfq, divides processes into three separate classes: real time, best effort, and idle. the performance are in that order. cfq uses historical data to anticipate whether an application will issue more I/O requests in the near future.
Because of this tendency to idle, the cfq scheduler should not be used in conjunction with hardware that does not incur a large seek penalty unless it is tuned for this purpose. It should also not be used in conjunction with other non-work-conserving schedulers, such as a host-based hardware RAID controller, as stacking these schedulers tends to cause a large amount of latency.
TODO(CONFIGURATION)
These parameters are set on a per-device basis by altering the specified files under the /sys/block/devname/queue/iosched directory.              
**back_seek_max**     
The maximum distance in kilobytes that CFQ will perform a backward seek. The default value is 16 KB. Backward seeks typically damage performance, so large values are not recommended.    
**back_seek_penalty**     
The multiplier applied to backward seeks when the disk head is deciding whether to move forward or backward. The default value is 2. If the disk head position is at 1024 KB, and there are equidistant requests in the system (1008 KB and 1040 KB, for example), the back_seek_penalty is applied to backward seek distances and the disk moves forward.      
**fifo_expire_async**     
The length of time in milliseconds that an asynchronous (buffered write) request can remain unserviced. After this amount of time expires, a single starved asynchronous request is moved to the dispatch list. The default value is 250 milliseconds.      
**fifo_expire_sync**      
The length of time in milliseconds that a synchronous (read or O_DIRECT write) request can remain unserviced. After this amount of time expires, a single starved synchronous request is moved to the dispatch list. The default value is 125 milliseconds.     
**group_idle**      
This parameter is set to 0 (disabled) by default. When set to 1 (enabled), the cfq scheduler idles on the last process that is issuing I/O in a control group. This is useful when using proportional weight I/O control groups and when slice_idle is set to 0 (on fast storage).      
**group_isolation**     
This parameter is set to 0 (disabled) by default. When set to 1 (enabled), it provides stronger isolation between groups, but reduces throughput, as fairness is applied to both random and sequential workloads. When group_isolation is disabled (set to 0), fairness is provided to sequential workloads only. For more information, see the installed documentation in /usr/share/doc/kernel-doc-version/Documentation/cgroups/blkio-controller.txt.      
**low_latency**     
This parameter is set to 1 (enabled) by default. When enabled, cfq favors fairness over throughput by providing a maximum wait time of 300 ms for each process issuing I/O on a device. When this parameter is set to 0 (disabled), target latency is ignored and each process receives a full time slice.      
**quantum**     
This parameter defines the number of I/O requests that cfq sends to one device at one time, essentially limiting queue depth. The default value is 8 requests. The device being used may support greater queue depth, but increasing the value of quantum will also increase latency, especially for large sequential write work loads.   
**slice_async**       
This parameter defines the length of the time slice (in milliseconds) allotted to each process issuing asynchronous I/O requests. The default value is 40 milliseconds.     
**slice_idle**           
This parameter specifies the length of time in milliseconds that cfq idles while waiting for further requests. The default value is 0 (no idling at the queue or service tree level). The default value is ideal for throughput on external RAID storage, but can degrade throughput on internal non-RAID storage as it increases the overall number of seek operations.      
**slice_sync**        
This parameter defines the length of the time slice (in milliseconds) allotted to each process issuing synchronous I/O requests. The default value is 100 ms.
If your use case requires cfq to be used on this storage, you will need to edit the following configuration files:
* Set /sys/block/devname/queue/iosched/slice_idle to 0
* Set /sys/block/devname/queue/iosched/quantum to 64
* Set /sys/block/devname/queue/iosched/group_idle to 1

#### noop
The noop I/O scheduler implements a simple FIFO (first-in first-out) scheduling algorithm. Requests are merged at the generic block layer through a simple last-hit cache. This can be the best scheduler for CPU-bound systems using fast storage.
Also, the noop I/O scheduler is commonly, but not exclusively, used on virtual machines when they are performing I/O operations to virtual disks.

Notes  from
https://access.redhat.com/documentation/en-us/red_hat_enterprise_linux/7/html/performance_tuning_guide/chap-red_hat_enterprise_linux-performance_tuning_guide-storage_and_file_systems#sect-Red_Hat_Enterprise_Linux-Performance_Tuning_Guide-Considerations-IO_Schedulers


### Tools for monitoring
#### top
 It can display a variety of information, including a system summary and a list of tasks currently being managed by the Linux kernel.By default, the processes displayed are ordered according to the percentage of CPU usage
#### ps
The ps tool, provided by the procps-ng package, takes a snapshot of a select group of active processes. By default, the group examined is limited to processes that are owned by the current user and associated with the terminal in which ps is run
#### vmstat
The Virtual Memory Statistics tool, vmstat, provides instant reports on your system's processes, memory, paging, block input/output, interrupts, and CPU activity. Vmstat lets you set a sampling interval so that you can observe system activity in near-real time.
#### iostat
The iostat tool, provided by the sysstat package, monitors and reports on system input/output device loading to help administrators make decisions about how to balance input/output load between physical disks. The iostat tool reports on processor or device utilization since iostat was last run, or since boot

#### TODO (TUNED)

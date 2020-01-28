### Run the dd command to check io operations
* checking the write throughput
```shell
dd bs=1M count=10000 if=/dev/zero of=/data01/test.img conv=fdatasync
```
* cheking the read throughput
```shell
dd if=/data01/test.img of=/dev/null bs=1M count=10000
```
* checking the io Scheduler
```shell
cat /sys/block/sd*/queue/scheduler
```
it should deadline. It is deadline (default) in Rhel 7 and onward but
cfq or noop for 6 and below

### Check the networks
Using iperf
* to check the connectivity between 2 servers
  * on serverA
  ```iperf - s -p 2000```
  
  * on server B
  ```iperf -c serverA -p 2000 -n 10000M```

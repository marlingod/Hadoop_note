#### Use ing bulk load to load data into hbase
1. Download the data
```
curl -O https://people.apache.org/~jdcryans/word_count.csv
```

2. Put the data in hhdfs
```
hdfs dfs -put word_count.csv /tmp
chmod 777 /tmp/word_count.csv
```
3. create the habse table
```
hbase shell
create 'wordcount', {NAME => 'f'},   {SPLITS => ['g', 'm', 'r', 'w']}
```
  * table name: word_count
  * column family: f
  * split: it create 5 regions servers

4. Create StoreFile first using importtsv

```
hbase org.apache.hadoop.hbase.mapreduce.ImportTsv \
-Dimporttsv.separator=',' \
-Dimporttsv.bulk.output=output \
-Dimporttsv.columns=HBASE_ROW_KEY,f:count wordcount /tmp/word_count.csv
```
**-Dimporttsv.separator=**, specifies that the separator is a comma.
**-Dimporttsv.bulk.output=output** is a relative path to where the HFiles will be written.
 Skipping this option will make the job write directly to HBase.
**-Dimporttsv.columns=HBASE_ROW_KEY,f:count** is a list of all the columns contained in this file. The row key needs to be identified using the all-caps HBASE_ROW_KEY string; otherwise it won’t start the job. "count" is the column name
```
sudo -u hdfs hdfs dfs -chown -R
```
5. Load Data to HBase Table using completebulkload
```
hbase org.apache.hadoop.hbase.mapreduce.LoadIncrementalHFiles output wordcount
```

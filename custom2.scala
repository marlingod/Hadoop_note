
package com.hadoop.custom

import org.apache.commons.logging.LogFactory
import org.apache.hadoop.io.{LongWritable, Text}
import org.apache.hadoop.mapreduce.{InputSplit, RecordReader, TaskAttemptContext}
import org.apache.hadoop.util.LineReader
import org.apache.hadoop.mapreduce.lib.input.{FileInputFormat, FileSplit}
import org.apache.hadoop.fs.FSDataInputStream
import org.apache.hadoop.fs.Seekable
import org.apache.hadoop.io.compress.Decompressor
import org.apache.hadoop.io.compress.CompressionCodecFactory
import scala.util.control.Breaks._

class AnotherCustom extends RecordReader[LongWritable, Text] {
  var start, end, pos = 0L
  var reader: LineReader = null
  var key = new LongWritable
  var value = new Text

  override def initialize(inputSplit: InputSplit, context: TaskAttemptContext): Unit = {
    // split position in data (start one byte earlier to detect if
    // the split starts in the middle of a previous record)
    val split = inputSplit.asInstanceOf[FileSplit]
    start = 0L.max(split.getStart -1)
    end = start + split.getLength
    // open a stream to the data, pointing to the start of the split
    val stream = split.getPath.getFileSystem(context.getConfiguration)
      .open(split.getPath)
    stream.seek(start)
    reader = new LineReader(stream, context.getConfiguration)
    // if the split starts at a newline, we want to start yet another byte
    // earlier to check if the newline was escaped or not
    val firstByte = stream.readByte().toInt
 /*   if (firstByte == '\n') {
      //start = 0L.max(start - 1)
      stream.seek(start)
    }
    */
    /*if (start != 0)
      skipRemainderFromPreviousSplit(reader)
*/
  }

  def skipRemainderFromPreviousSplit(reader: LineReader): Unit = {
    var readAnotherLine = true
    while (readAnotherLine) {
      // read next line
      val buffer = new Text()
      start += reader.readLine(buffer, Integer.MAX_VALUE, Integer.MAX_VALUE)
      pos = start
      // detect if delimiter was escaped
      readAnotherLine = buffer.getLength >= 1 && //&amp;&amp; // something was read
        buffer.charAt(buffer.getLength - 1) == '\\' && //&amp;&amp; // newline was escaped
        pos <= end // seek head hasn't passed the split
    }
  }

  override def nextKeyValue(): Boolean = {
    key.set(pos)
    // read newlines until an unescaped newline is read
    var lastNewlineWasEscaped = false
    while (pos < end || lastNewlineWasEscaped) {
      // read next line
      val buffer = new Text
      pos += reader.readLine(buffer, Integer.MAX_VALUE, Integer.MAX_VALUE)
      // append newly read data to previous data if necessary
      value = if (lastNewlineWasEscaped) new Text(value + "\n" + buffer) else buffer
      // detect if delimiter was escaped
      //lastNewlineWasEscaped = buffer.charAt(buffer.getLength - 1) == '\\'
      // let Spark know that a key-value pair is ready!
      if (!lastNewlineWasEscaped)
        return true
    }
    // end of split reached?
    return false
  }
  override def getCurrentKey: LongWritable =return key

  override def getCurrentValue: Text = return value

  override def getProgress: Float = {
    if (start == end) {
      0.0f
    } else {
      Math.min(1.0f, (pos -start) /(end - start ))
    }
  }

  override def close(): Unit = {
    if (reader != null){
      reader.close()
    }
  }

}

class CustomFileInputFormat2 extends FileInputFormat[LongWritable, Text] {
  override def createRecordReader(split: InputSplit, context: TaskAttemptContext) = new AnotherCustom
}

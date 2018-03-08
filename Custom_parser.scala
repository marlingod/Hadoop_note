package com.hadoop.custom

import java.util.regex.Pattern

import org.apache.commons.logging.LogFactory
import org.apache.hadoop.io.{LongWritable, Text}
import org.apache.hadoop.mapreduce.{InputSplit, RecordReader, TaskAttemptContext}
import org.apache.hadoop.util.LineReader
import org.apache.hadoop.mapreduce.lib.input.{FileInputFormat, FileSplit}
import org.apache.hadoop.fs.FSDataInputStream
import org.apache.hadoop.fs.Seekable
import org.apache.hadoop.io.compress.Decompressor
import org.apache.hadoop.io.compress.CompressionCodecFactory
import scala.util.matching.Regex
import scala.util.control.Breaks._
import java.util.regex.Pattern

class CustomReader  extends RecordReader[LongWritable, Text] {
  private val NLINESTOPROCESS = 2
  private var in :LineReader = _
  private var key : LongWritable =_
  private var value : Text =_
  private var start: Long = 0L
  private var end : Long = 0L
  private var pos: Long = 0L
  private var maxLineLength = 0
  private var delimiterRegex : String = _
  private var delimiterPattern: Regex = _ // private val delimiterPattern: Pattern = null



  override def initialize(inputSplit: InputSplit, context: TaskAttemptContext): Unit = {
    val fileSplit = inputSplit.asInstanceOf[FileSplit]
    val conf = context.getConfiguration
    this.maxLineLength  = conf.getInt("linerecordreader.maxlength",Integer.MAX_VALUE);
    this.delimiterRegex = conf.get("record.delimiter.regex");

    delimiterPattern = delimiterRegex.r

    start = fileSplit.getStart()
    end = start + fileSplit.getLength

    val path =fileSplit.getPath
    val fs = path.getFileSystem(conf)
    val filein : FSDataInputStream= fs.open(fileSplit.getPath)

    var skipFirstLine = false
    if (start != 0) {
      skipFirstLine =true
      start -=1
      filein.seek(start)
    }

    in= new LineReader(filein, conf)
    if(skipFirstLine) {
      start += in.readLine(new Text(), 0, Math.min(
        Integer.MAX_VALUE.toLong, end - start).asInstanceOf[Int]
      )
    }
      this.pos =start
  }

  override def nextKeyValue(): Boolean = {
    if (key == null) key = new LongWritable
    key.set(pos)
    if (value == null) value = new Text
    value.clear()
    val endline: Text = new Text("\n");
    var newSize = 0
   //for (i<- 0 to NLINESTOPROCESS-1) {
     var v = new Text()
     while (pos < end) {
       newSize = in.readLine(
         v,
         maxLineLength,
         Math.max(
           Math.min(Integer.MAX_VALUE.toLong, end - start).asInstanceOf[Int],
           maxLineLength))
       value.append(v.getBytes(), 0, v.getLength)
       value.append(endline.getBytes, 0, endline.getLength)
       breakable {
         if (newSize == 0) {
           break()
         }
       }
       pos += newSize
     }
     breakable {
       if (newSize < maxLineLength) {
         break()
       }

     }
   //}
    if (newSize == 0) {
      key =null
      value =null
      return false
    } else {
      return true
    }

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
    if (in != null){
      in.close()
    }
  }

}

class CustomFileInputFormat extends FileInputFormat[LongWritable, Text] {
  override def createRecordReader(split: InputSplit, context: TaskAttemptContext) = new CustomReader
}

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
import java.util.regex._


class PatternReader extends RecordReader[LongWritable, Text] {

  private var in :LineReader = _
  private var key : LongWritable =_
  private var value : Text =_
  private var start: Long = 0L
  private var end : Long = 0L
  private var pos: Long = 0L
  private var maxLineLength = 0
  private var delimiterRegex : String = _
  private val EOL: Text = new Text("\n")
  // private var delimiterPattern: Regex = _ // private val delimiterPattern: Pattern = null
  private var delimiterPattern: Pattern = null
  var newSize: LineReader =_

  override def initialize(inputSplit: InputSplit, context: TaskAttemptContext): Unit = {
    val conf = context.getConfiguration
    val fileSplit = inputSplit.asInstanceOf[FileSplit]
    this.maxLineLength  = conf.getInt("linerecordreader.maxlength",Integer.MAX_VALUE);
    this.delimiterRegex = conf.get("record.delimiter.regex");

    delimiterPattern = Pattern.compile(delimiterRegex)
    //delimiterPattern = delimiterRegex.r //import java.util.regex.Pattern --delimiterPattern = Pattern.compile(delimiterRegex)

    start = fileSplit.getStart
    end = start + fileSplit.getLength

    val path  = fileSplit.getPath
    val fs  = path.getFileSystem(conf)
    val filein: FSDataInputStream = fs.open(fileSplit.getPath)
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

  def readNext( text: Text, maxLineLength: Int, maxBytesToConsume: Int): Int ={
    var offset =0
    text.clear()
    var tmp : Text = new Text;

    for (i <-0 to maxBytesToConsume) {
        var offsetTmp = in.readLine(
          tmp,
          maxLineLength,
          maxBytesToConsume
        )
      offset += offsetTmp
      var m: Matcher = delimiterPattern.matcher(tmp.toString)

      //End of File
      breakable {
        if (offsetTmp == 0) {
          break()
        }
      }
      breakable {
        if (m.matches()) {
          break()
        } else {
          //apend value to records
          text.append(EOL.getBytes, 0, EOL.getLength)
          text.append(tmp.getBytes, 0, tmp.getLength)
        }
      }
    }
    return offset
  }
  override def nextKeyValue(): Boolean =  {
    var newSize = readNext(
        value,
        maxLineLength,
        Math.max(
          Math.min(Integer.MAX_VALUE.toLong, end - start).asInstanceOf[Int],
          maxLineLength))
  breakable {
    if (newSize < maxLineLength) {
      break()
    }

  }

    if (newSize == 0) {
      key =null
      value =null
      return false
    } else {
      return true
    }
  }

  override def getCurrentKey: LongWritable = return key

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

class CustomFileInputFormat3 extends FileInputFormat[LongWritable, Text] {
  override def createRecordReader(split: InputSplit, context: TaskAttemptContext) = new PatternReader
}

object CustomFileInputFormat3 {
  var DELIMT : String = ""
}

package com.hadoop.custom

import java.io.{IOException, InputStream}
import java.nio.charset.Charset
import java.util.regex.Pattern

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Seekable
import org.apache.hadoop.io.compress._
import org.apache.hadoop.io.{DataOutputBuffer, LongWritable, Text}
import org.apache.hadoop.mapreduce.{InputSplit, RecordReader, TaskAttemptContext}
import org.apache.hadoop.mapreduce.lib.input.{FileSplit, TextInputFormat}

/**
  * Reads records that delimiter by specific start/end tag
  */
class AnotherPattern  extends TextInputFormat{
  override def createRecordReader(
                                   split: InputSplit,
                                   context: TaskAttemptContext): RecordReader[LongWritable, Text] = {
    new PattenRead
  }

}

object AnotherPattern {
  /** Configuration for the start tag */
  val START_TAG : String = "startInput"
  val END_TAG: String = "endInput"
  val ENCODING: String = "encodeHere"
}

private class PattenRead extends RecordReader[LongWritable, Text] {
  private var startTag: Array[Byte] = _
  private var currentStartTag: Array[Byte] = _
  private var endTag: Array[Byte] = _
  private var currentKey: LongWritable = _
  private var currentValue: Text = _

  private var start: Long = _
  private var end: Long = _
  private var in: InputStream = _
  private var filePosition: Seekable = _
  private var decompressor: Decompressor = _
  private val buffer: DataOutputBuffer = new DataOutputBuffer
  private var StartPattern: Pattern = null
  private var EndPattern: Pattern = null

  override def initialize(split: InputSplit, context: TaskAttemptContext): Unit = {
    val fileSplit: FileSplit = split.asInstanceOf[FileSplit]
    val conf: Configuration = context.getConfiguration
    val charset = Charset.forName(conf.get(AnotherPattern.ENCODING))
    startTag = conf.get(AnotherPattern.START_TAG).getBytes(charset)
    endTag = conf.get(AnotherPattern.END_TAG).getBytes(charset)
    //pattern matching
    StartPattern = Pattern.compile(AnotherPattern.START_TAG)
    EndPattern = Pattern.compile(AnotherPattern.END_TAG)

    start  = fileSplit.getStart
    end  =start + fileSplit.getLength
    // open the file and seek to the start of the split
    val path = fileSplit.getPath
    val fs = path.getFileSystem(conf)
    val fsin = fs.open(fileSplit.getPath)

    val codec = new CompressionCodecFactory(conf).getCodec(path)
    if (codec !=null) {
      decompressor =CodecPool.getDecompressor(codec)
      codec match {
        case sc: SplittableCompressionCodec =>
          val cIn = sc.createInputStream(
            fsin,
            decompressor
            ,start
            ,end
            ,SplittableCompressionCodec.READ_MODE.BYBLOCK)
          start = cIn.getAdjustedStart
          end   = cIn.getAdjustedEnd
          in =cIn
          filePosition =cIn
        case c: CompressionCodec =>
          if (start !=0) {
            throw new IOException("Cannot split the the flit")
          }
          val cIn = c.createInputStream(
            fsin
            ,decompressor
          )
          in = cIn
          filePosition =cIn
      }
    } else {
      in =fsin
      filePosition =fsin
      filePosition.seek(start)
    }
  }

  override def nextKeyValue(): Boolean = {
    currentKey = new LongWritable
    currentValue =new Text
    next(currentKey, currentValue)
  }

  /**
    * Find the start of the next record.
    * It treats data from startTag and EndTag as record
    *
    * @param key the current that will be written
    * @param value current value that will be written
    * @return whether it reads successfully
    */

  def next(key: LongWritable, value: Text): Boolean ={
    if (readUntilStartElement()) {
      try {
        buffer.write(currentStartTag)
        if (readUntilEndElement()) {
          key.set(filePosition.getPos)
          value.set(buffer.getData,0, buffer.getLength)
          true
        } else {
          false
        }
      } finally {
        buffer.reset()
      }
    } else {
      false
    }
  }

  def readUntilStartElement(): Boolean ={
    currentStartTag = startTag
    var i = 0
    while(true) {
      val b = in.read()
      if (b == -1 || (i == 0 && filePosition.getPos > end)) {
        // End of file or end of split.
        return false
      } else {
        if (b == startTag(i)) {
          if (i >= startTag.length -1){
            //found start tag
            return true
          } else {
            i +=1
          }
        } else {
          i =0
        }
      }
    }
    false
  }

  private def readUntilEndElement(): Boolean = {
    var si = 0
    var ei = 0
    var depth = 0
    while (true) {
      val b = in.read()
      if (b == -1) {
        // End of file (ignore end of split).
        return false
      } else {
        buffer.write(b)
        if (b == startTag(si) && b == endTag(ei)) {
          // In start tag or end tag.
          si += 1
          ei += 1
        } else if (b == startTag(si)) {
          if (si >= startTag.length - 1) {
            // Found start tag.
            si = 0
            ei = 0
            depth += 1
          } else {
            // In start tag.
            si += 1
            ei = 0
          }
        } else if (b == endTag(ei)) {
          if (ei >= endTag.length - 1) {
            if (depth == 0) {
              // Found closing end tag.
              return true
            } else {
              // Found nested end tag.
              si = 0
              ei = 0
              depth -= 1
            }
          } else {
            // In end tag.
            si = 0
            ei += 1
          }
        } else {
          // Not in start tag or end tag.
          si = 0
          ei = 0
        }
      }
    }
    // Unreachable.
    false
  }
   override def getCurrentKey: LongWritable = currentKey

  override def getCurrentValue: Text = currentValue

  override def getProgress: Float = (filePosition.getPos - start) / (end - start).toFloat

  override def close(): Unit =try {
    if (in != null) {
      in.close()
    }
  } finally {
    if (decompressor != null) {
      CodecPool.returnDecompressor(decompressor)
      decompressor = null
    }
  }

}


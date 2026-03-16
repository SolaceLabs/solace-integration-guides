package com.solace.sample;

import java.util.Arrays;
import java.util.regex.Pattern;
import org.apache.spark.SparkConf;
import org.apache.spark.storage.StorageLevel;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import scala.Tuple2;

public class PubSubPlusJMSReceiverTest {
  private static final Pattern SPACE = Pattern.compile(" ");

  public static void main(String[] args) throws Exception {
    if (args.length < 6) {
      System.err.println("Usage: PubSubPlusJMSReceiverTest <brokerURL> <vpn> <username> <password> <queue> <connectionFactory>");
          System.exit(1);
    }
    // Create the context with a 1 second batch size
    SparkConf sparkConf = new SparkConf().setAppName("JavaCustomReceiver");
    JavaStreamingContext ssc = new JavaStreamingContext(sparkConf, new Duration(1000));
      
    // Create a input stream with the custom receiver on provided PubSub+ broker connection config and count the
    // words in input stream of \n delimited text
    JavaReceiverInputDStream<String> lines = ssc.receiverStream(
        new PubSubPlusJMSReceiver(args[0], args[1], args[2], args[3], args[4], args[5], StorageLevel.MEMORY_ONLY_SER_2()));
    JavaDStream<String> words = lines.flatMap(x -> Arrays.asList(SPACE.split(x)).iterator());
    JavaPairDStream<String, Integer> wordCounts = words.mapToPair(s -> new Tuple2<>(s, 1))
        .reduceByKey((i1, i2) -> i1 + i2);
    wordCounts.print();
    ssc.start();
    ssc.awaitTermination();
    ssc.close();
  }
}
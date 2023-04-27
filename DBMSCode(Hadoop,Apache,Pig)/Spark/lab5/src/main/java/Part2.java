import java.util.Arrays;
import java.util.Iterator;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.api.java.function.Function;
import scala.Tuple2;

public class Part2 {

	private final static int numOfReducers = 2;

	
	@SuppressWarnings("serial")
	public static void main(String[] args) throws Exception {

		if (args.length != 4) {
			System.err.println(
					"Did you not read the instructions: " + "Part2 <ip_trace> <raw_block> <output> <output2");
			System.exit(1);
		}
		SparkConf sparkConf = new SparkConf().setAppName("Experiment 2");
		JavaSparkContext context = new JavaSparkContext(sparkConf);
		JavaRDD<String> input1 = context.textFile(args[0]);
		JavaRDD<String> input2 = context.textFile(args[1]);

		JavaPairRDD<Integer, String> partA1 = input1.mapToPair(new PairFunction<String, Integer, String>() {
			public Tuple2<Integer, String> call(String s) {
				String time = s.split("\\s+")[0];
				String connectionID = s.split("\\s+", 7)[1];
				String sourceIP = s.split("\\s+", 7)[2];
				String destinationIP = s.split("\\s+", 7)[4];

				String shortSource = sourceIP.split("\\.")[0] + "." + sourceIP.split("\\.")[1] + "."
						+ sourceIP.split("\\.")[2] + "." + sourceIP.split("\\.")[3];
				String shortDestination = destinationIP.split("\\.")[0] + "." + destinationIP.split("\\.")[1] + "."
						+ destinationIP.split("\\.")[2] + "." + destinationIP.split("\\.")[3];
				return new Tuple2<Integer, String>(Integer.parseInt(connectionID),
						time + " " + shortSource + " " + shortDestination);

			}
		});

		JavaPairRDD<Integer, String> PartA2 = input2.mapToPair(new PairFunction<String, Integer, String>() {
			public Tuple2<Integer, String> call(String s) {
				String actionTaken = s.split("\\s+")[1];
				return new Tuple2<Integer, String>(Integer.parseInt(s.split("\\s+")[0]), actionTaken);
			}
		});

		Function<Tuple2<Integer, String>, Boolean> blocked = t -> (t._2.equals("Blocked"));

		JavaPairRDD<Integer, String> filtered = PartA2.filter(blocked);

		JavaPairRDD<Integer, Tuple2<String, String>> join = partA1.join(filtered).sortByKey();

		JavaRDD<String> PartB = join.map(new Function<Tuple2<Integer, Tuple2<String, String>>, String>() {
			public String call(Tuple2<Integer, Tuple2<String, String>> t) {
				String time = t._2._1.split("\\s+")[0];
				String connectionID = String.valueOf(t._1);
				String sourceIP = t._2._1.split("\\s+")[1];
				String destIP = t._2._1.split("\\s+")[2];
				String blocked = t._2._2;

				return time + " " + connectionID + " " + sourceIP + " " + destIP + " " + blocked;
			}
		});

		JavaPairRDD<String, Integer> ones =PartB.mapToPair(new PairFunction<String, String, Integer>() {
			public Tuple2<String, Integer> call(String s) {
				String source = s.split("\\s+")[2];
				return new Tuple2<String, Integer>(source, 1);
			}
		});

		JavaPairRDD<String, Integer> counts = ones.reduceByKey(new Function2<Integer, Integer, Integer>() {
			public Integer call(Integer i1, Integer i2) {
				return i1 + i2;
			}
		}, numOfReducers);

		JavaPairRDD<Integer, String> swap = counts
				.mapToPair(new PairFunction<Tuple2<String, Integer>, Integer, String>() {
					public Tuple2<Integer, String> call(Tuple2<String, Integer> t) {
						return t.swap();
					}
				});

		JavaPairRDD<Integer, String> sorted = swap.sortByKey(false);
		PartB.saveAsTextFile(args[2]);
		sorted.saveAsTextFile(args[3]);
		context.stop();
		context.close();

	}

}

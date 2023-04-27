import java.io.*;
import java.util.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.KeyValueTextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class customSort {

	public static void main(String[] args) throws Exception {

		///////////////////////////////////////////////////
		///////////// First Round MapReduce ///////////////
		////// where you might want to do some sampling ///
		///////////////////////////////////////////////////
		int reduceNumber = 1;

		Configuration conf = new Configuration();

		String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();

		if (otherArgs.length != 2) {
			System.err.println("Usage: Patent <in> <out>");
			System.exit(2);
		}

		Job job = Job.getInstance(conf, "Exp2");

		job.setJarByClass(customSort.class);
		job.setNumReduceTasks(reduceNumber);

		job.setMapperClass(mapOne.class);
		job.setReducerClass(reduceOne.class);

		job.setMapOutputKeyClass(IntWritable.class);
		job.setMapOutputValueClass(Text.class);

		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(NullWritable.class);

		job.setInputFormatClass(KeyValueTextInputFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);

		FileInputFormat.addInputPath(job, new Path(otherArgs[0]));

		FileOutputFormat.setOutputPath(job, new Path("lab3/e2/temp/"));
//Can't figure out stupid ass null pointer exceptions after this point..........................................
		System.out.println("PLS NO ERROR");
		job.waitForCompletion(true);
		Path tmp = new Path("lab3/e2/temp/part-r-00000");
		FileSystem file = FileSystem.get(new Configuration());
		BufferedReader reader = new BufferedReader(new InputStreamReader(file.open(tmp)));

		String current;
		current = reader.readLine();
		int counter = 0;

		while (current != null) {
			conf.set(String.valueOf(counter), current);
			current = reader.readLine();
			counter++;
		}

		tmp = new Path("lab3/e2/temp/");
		if (file.exists(tmp)) {
			file.delete(tmp, true);
		}
		///////////////////////////////////////////////////
		///////////// Second Round MapReduce //////////////
		///////////////////////////////////////////////////
		Job job_two = Job.getInstance(conf, "Round Two");
		job_two.setJarByClass(customSort.class);
		conf.setInt("Count", 0);
		// Providing the number of reducers for the second round
		reduceNumber = 10;
		job_two.setNumReduceTasks(reduceNumber);

		// Should be match with the output datatype of mapper and reducer
		job_two.setMapOutputKeyClass(Text.class);
		job_two.setMapOutputValueClass(Text.class);

		job_two.setOutputKeyClass(Text.class);
		job_two.setOutputValueClass(Text.class);
		// job_two.setPartitionerClass(MyPartitioner.class);

		job_two.setMapperClass(mapTwo.class);
		job_two.setReducerClass(reduceTwo.class);

		// Partitioner is our custom partitioner class
		job_two.setPartitionerClass(MyPartitioner.class);
		// Input and output format class
		job_two.setInputFormatClass(KeyValueTextInputFormat.class);
		job_two.setOutputFormatClass(TextOutputFormat.class);

		// The output of previous job set as input of the next
		FileInputFormat.addInputPath(job_two, new Path(otherArgs[0]));
		FileOutputFormat.setOutputPath(job_two, new Path(otherArgs[1]));

		// Run the job
		System.exit(job_two.waitForCompletion(true) ? 0 : 1);

	}

	public static class mapOne extends Mapper<Text, Text, IntWritable, Text> {
		private static IntWritable index = new IntWritable(1);
		private static Text Val1 = new Text();
		public void map(Text key, Text value, Context context) throws IOException, InterruptedException {

			Val1.set(key.toString() + " " + value.toString());
			context.write(index, Val1);
		}
	}

	
	//MAIN METHODOLOGY: Sampling 10% at a time by taking 50000, I tried to get this to work using simple base
	//Level Arrays, however couldn't get past or figure out the null pointer exceptions I was getting when using
	//Arrays.sort I know this would have been faster but it was far easier to code with arraylists.
	//This just samples 10% of entries of the 500k data sheet and can be easily modified by changing the
	//value of max to 10% of any data sheet size. This is the best strategy I could figure out with Reducing
	//Essentially the rand variable gives it a 50% chance of being sampled furthering the partitioning
	public static class reduceOne extends Reducer<IntWritable, Text, Text, NullWritable> {
		private final int max = 50000;
		private static int counter = 0;
		private Text string = new Text();

		public void reduce(IntWritable key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
			ArrayList<String> array = new ArrayList<String>();
			for (Text val : values) {
				double rand = Math.random();
				if (counter <= max && rand > 0.5) {
					String holder = val.toString().substring(0, 15);
					array.add(holder);
					counter += 1;
				}
				if (counter > max) {
					break;
				}
			}
			Collections.sort(array);
			int factor = (int) max / 10;
			for (int i = factor; i < max; i += factor) {
				string.set(array.get(i));
				context.write(string, null);
			}

		}
	}

	

// Compare each input key with the boundaries we get from the first round
// And add the partitioner information in the end of values
	public static class mapTwo extends Mapper<Text, Text, Text, Text> {
		private Configuration config;

		public void setup(Context context) {
			config = context.getConfiguration();
		}

		public void map(Text key, Text value, Context context) throws IOException, InterruptedException {
			// For instance, pF0 to pF8 are the discovered boundaries,
			// based on which you add the No ID 0 to 9 at the end of value
			// How to find the boundaries is your job for this experiment
			//This is finding the boundaries of the newly created configuration from this context
			String tmp4Com = key.toString();
			String pF0 = config.get("0");
			String pF1 = config.get("1");
			String pF2 = config.get("2");
			String pF3 = config.get("3");
			String pF4 = config.get("4");
			String pF5 = config.get("5");
			String pF6 = config.get("6");
			String pF7 = config.get("7");
			String pF8 = config.get("8");
			
			if (tmp4Com.compareTo(pF0) <= 0) {
				context.write(new Text(key), new Text(value + ";" + Integer.toString(0)));
			} else if (tmp4Com.compareTo(pF1) <= 0) {
				context.write(new Text(key), new Text(value + ";" + Integer.toString(1)));
			} else if (tmp4Com.compareTo(pF2) <= 0) {
				context.write(new Text(key), new Text(value + ";" + Integer.toString(2)));
			} else if (tmp4Com.compareTo(pF3) <= 0) {
				context.write(new Text(key), new Text(value + ";" + Integer.toString(3)));
			} else if (tmp4Com.compareTo(pF4) <= 0) {
				context.write(new Text(key), new Text(value + ";" + Integer.toString(4)));
			} else if (tmp4Com.compareTo(pF5) <= 0) {
				context.write(new Text(key), new Text(value + ";" + Integer.toString(5)));
			} else if (tmp4Com.compareTo(pF6) <= 0) {
				context.write(new Text(key), new Text(value + ";" + Integer.toString(6)));
			} else if (tmp4Com.compareTo(pF7) <= 0) {
				context.write(new Text(key), new Text(value + ";" + Integer.toString(7)));
			} else if (tmp4Com.compareTo(pF8) <= 0) {
				context.write(new Text(key), new Text(value + ";" + Integer.toString(8)));
			} else if (tmp4Com.compareTo(pF8) > 0) {
				context.write(new Text(key), new Text(value + ";" + Integer.toString(9)));
			}

		}
	}

	public static class reduceTwo extends Reducer<Text, Text, Text, Text> {
		private Text string1 = new Text();

		public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
			for (Text val : values) {
				String[] temp = val.toString().split(";");
				string1.set(temp[0].toString());
				context.write(key, string1);
			}
		}
	}

// Extract the partitioner information from the input values, which decides the
// destination of data
	public static class MyPartitioner extends Partitioner<Text, Text> {
		public int getPartition(Text key, Text value, int numReduceTasks) {
			String[] desTmp = value.toString().split(";");
			return Integer.parseInt(desTmp[1]);

		}
	}
}

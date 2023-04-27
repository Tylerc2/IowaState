import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.io.PrintWriter;

import org.apache.flink.api.common.functions.FlatMapFunction;

import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.util.Collector;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamUtils;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

@SuppressWarnings("serial")
public class BigramFlink {

	public static void main(String[] args) throws Exception {

		final ParameterTool params = ParameterTool.fromArgs(args);

		// set up the execution environment
		final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

		// make parameters available in the web interface
		env.getConfig().setGlobalJobParameters(params);

		// get input data
		// <PATH_TO_DATA>: The path to input data, e.g.,
		// "/home/cpre419/Downloads/shakespeare"
		DataStream<String> text = env.readTextFile("/home/cpre419/Downloads/shakespeare.txt");

		DataStream<Tuple2<String, Integer>> counts = text.flatMap(new Tokenizer()).keyBy(0).sum(1);

		// emit result

		HashMap<String, Integer> hmap = new HashMap<String, Integer>();
		Iterator<Tuple2<String, Integer>> out = DataStreamUtils.collect(counts);

		for (; out.hasNext();) {
			Tuple2<String, Integer> t = out.next();
			hmap.put(t.f0, t.f1);
		}

		List<Map.Entry<String, Integer>> list = new LinkedList<Map.Entry<String, Integer>>(hmap.entrySet());

		Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
			public int compare(Map.Entry<String, Integer> t1, Map.Entry<String, Integer> t2) {

				return (t1.getValue()).compareTo(t2.getValue());

			}
		});

		PrintWriter w = new PrintWriter("output2.txt");
		int i = list.size() - 1;
		while (i >= list.size() - 11) {
			String output = list.get(i).getKey() + " " + list.get(i).getValue();
			w.println(output);
			i--;
		}
		w.close();
	}

	public static final class Tokenizer implements FlatMapFunction<String, Tuple2<String, Integer>> {

		public void flatMap(String value, Collector<Tuple2<String, Integer>> out) {
			value = value.toLowerCase();
			value = value.replaceAll("\\s+", " ");
			value = value.replaceAll("[^a-z0-9]", " ");
			String[] words = value.split(" ");
			String first = null;
			for (String word : words) {
				if (first != null && !first.equals(".") && !first.equals("") && !word.equals(".") & 
						!word.equals("") && word != null) {
					String line = first + " " + word;	
						out.collect(new Tuple2<String, Integer>(line, 1));
				}
				first = word;
			}
		}
	}
}

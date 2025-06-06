package de.opal.tests;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class Worker implements Callable<Integer> {
	String schema = "", objectType = "", objectName = "";

	public Worker(String schema, String objectType, String objectName) {
		this.schema = schema;
		this.objectType = objectType;
		this.objectName = objectName;
	}

	@Override
	public Integer call() throws Exception {
		Integer retVal=0; //0=success
		
		System.out.println("schema:"+this.schema);
		System.out.println("objectType:"+this.objectType);
		
				

		return retVal;
	}

}

public class ParallelExecution {

	String outputDirName = "/tmp/out";

	public static void main(String[] args) {
		ExecutorService executors = Executors.newFixedThreadPool(4);

		@SuppressWarnings("unchecked")
		Future<Integer>[] futures = new Future[5];

		Callable<Integer> w = new Worker("JRI_TEST", "TABLE", "XLIB_LOGS");
		try {
			for (int i = 0; i < 5; i++) {
				Future<Integer> future = executors.submit(w);
				futures[i] = future;

			}

			for (int i = 0; i < futures.length; i++) {
				try {
					System.out.println("Result from Future " + i + ":" + futures[i].get());
				} catch (InterruptedException e) {

					e.printStackTrace();
				} catch (ExecutionException e) {

					e.printStackTrace();
				}
			}
		} finally {
			executors.shutdown();
		}

	}

}

package com.graham.controller;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import org.apache.hadoop.net.ConnectTimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.graham.model.Cluster;
import com.graham.model.benchmarks.BenchmarkResult;
import com.graham.model.dbaccess.BenchmarkResultService;
import com.graham.model.dbaccess.ClusterService;

@Controller
@RequestMapping("dfsio")
public class DFSIOController {

	@Autowired
	private BenchmarkResultService benchmarkResultService;
	@Autowired
	private ClusterService clusterService;
	
	// === DEPRACATED JSP CODE === //

	// GET /benchmarks/
	@RequestMapping("/dfsiobenchmarks")
	public ModelAndView benchmarksById(@RequestParam("id") String id) {
		ArrayList<BenchmarkResult> results = (ArrayList<BenchmarkResult>) benchmarkResultService.listClusterBenchmarkResultByDate(id);

		ModelAndView mv = new ModelAndView("dfsiobenchmarks");
		mv.addObject("cluster",  clusterService.getCluster(id));
		mv.addObject("clusters", clusterService.listClusterById(id));
		mv.addObject("dfsio", results);
		return mv;
	}
	
	// POST /benchmarks/{id}
	@RequestMapping(value = "/delete", method = RequestMethod.GET)
	public ModelAndView delete(@RequestParam("id") String id, @RequestParam("clusterId") String clusterId) {

		// Get benchmark result

		BenchmarkResult result = benchmarkResultService.getBenchmarkResult(id); 
		benchmarkResultService.deleteBenchmarkResult(result);
		return new ModelAndView("redirect:/dfsio/dfsiobenchmarks?id="+clusterId);
	}
	
	/// === REST FUNCTIONS === ///
	
	// GET /test/
	@RequestMapping(value = "/dfsio", method = RequestMethod.POST)
	public @ResponseBody Callable<BenchmarkResult> dfsioAsync(String id, int numFiles, int fileSize) throws Exception {
		// Run benchmark and store it
		BenchmarkResult result = benchmarkDFSIOAsync(id, numFiles, fileSize);
		benchmarkResultService.addBenchmarkResult(result);

		return new Callable<BenchmarkResult>() {

			@Override
			public BenchmarkResult call() throws Exception {
				// TODO Auto-generated method stub
				return result;
			}
		};
	}
	
	// REST GET /benchmarks/
	@RequestMapping("/benchmarks/{id}")
	public ResponseEntity<ArrayList<BenchmarkResult>> getBenchmarks(@PathVariable("id") String id) {
		// Get benchmarks from DB
		ArrayList<BenchmarkResult> results = (ArrayList<BenchmarkResult>) benchmarkResultService.listClusterBenchmarkResultByDate(id);
		return new ResponseEntity<ArrayList<BenchmarkResult>>(results, HttpStatus.OK);
	}

	// GET /benchmarks/{id}
	@RequestMapping(value = "/benchmark/{id}", method = RequestMethod.GET)
	public ResponseEntity<BenchmarkResult> benchmark(@PathVariable("id") String id) {
		
		// Get benchmark result
		BenchmarkResult result = benchmarkResultService.getBenchmarkResult(id);
		if(result != null) {
			return new ResponseEntity<BenchmarkResult>(result, HttpStatus.OK);
		} else {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
	}
	
	// POST /delete/{id}
	@RequestMapping(value = "/delete/{id}", method = RequestMethod.POST)
	public ResponseEntity<String> deleteResult(@PathVariable("id") String id) {
		// Get benchmark result
		BenchmarkResult result = benchmarkResultService.getBenchmarkResult(id); 
		benchmarkResultService.deleteBenchmarkResult(result);
		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	// === PRIVATE CODE === // 

	private BenchmarkResult benchmarkDFSIOAsync(String id, int numFiles, int fileSize) throws IOException {
		Cluster cluster = clusterService.getCluster(id);
		BenchmarkResult result = cluster.runDFSIOBenchmark(numFiles, fileSize);
		result.setClusterName(cluster.getName());
		result.setClusterId(cluster.getId());
		return result;
	}
	
	
	/// === ERROR HANDLING === ///

	// Handles an error in Cluster connection
	@ExceptionHandler({IOException.class, ConnectTimeoutException.class})
	public ResponseEntity<String> handleConnectionFailure(Exception ex) {
		ex.printStackTrace();
		System.out.println("Timeout handled");
		return new ResponseEntity<String>("Cluster Connection Failure", HttpStatus.BAD_REQUEST);
	}

	// Handles an error in Cluster connection
	@ExceptionHandler(RemoteException.class)
	public ResponseEntity<String> handleRemoteError(RemoteException ex) {
		ex.printStackTrace();
		return new ResponseEntity<String>("Cluster error", HttpStatus.TOO_MANY_REQUESTS);
	}

	// Handles an error in Cluster connection
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<String> handleIllegalArgumentError(IllegalArgumentException ex) {
		ex.printStackTrace();
		return new ResponseEntity<String>("Bad IP address", HttpStatus.BAD_REQUEST);
	}
}
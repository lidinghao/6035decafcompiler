package decaf.parallel;

import java.util.HashMap;
import java.util.List;

import decaf.dataflow.cfg.MethodIR;

// Given a loopId, this handles the job of creating a method out of the loop which takes
// in a thread ID, manipulating the loop boundaries based on the thread ID and updating
// the original location of the loop to call the pthread library
// Also, this creates two globals for each loopId which maintains the loop iteration boundaries
// before the loop method is called
public class LoopParallelizer {
	HashMap<String, MethodIR> mMap;
	List<String> parallelizableLoops;
	
	public LoopParallelizer(HashMap<String, MethodIR> mMap, List<String> parallelLoops) {
		this.mMap = mMap;
		this.parallelizableLoops = parallelLoops;
		for (String loopId : parallelLoops) {
			parallelizeLoop(loopId);
		}
	}
	
	public void parallelizeLoop(String loopId) {
		
	}
}

package decaf.parallel;

// 
public class NamesDefinedTest {

	// Returns string of loop ids which pass the name definition test
	public List<String> getLoopIDsWhichPass() {
		List<String> uniqueLoopIds = getAllLoopIds();
		List<String> parallelizableLoops = new ArrayList<String>();
		for (String loopId : uniqueLoopIds) {
			if (passesArrayResolverTest(loopId)) {
				parallelizableLoops.add(loopId);
			}
		}
		return parallelizableLoops;
	}
}

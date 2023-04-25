package staticTypechecker;

import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map.Entry;

import org.junit.Test;

import jolie.util.Pair;
import staticTypechecker.entities.Module;
import staticTypechecker.entities.Symbol;
import staticTypechecker.entities.SymbolTable;
import staticTypechecker.entities.Symbol.SymbolType;
import staticTypechecker.visitors.InputPortProcessor;
import staticTypechecker.visitors.InterfaceProcessor;
import staticTypechecker.visitors.OutputPortProcessor;
import staticTypechecker.visitors.SymbolCollector;
import staticTypechecker.visitors.TypeCheckerVisitor;
import staticTypechecker.visitors.TypeProcessor;

/**
 * Unit test for simple App.
 */
public class AppTest{
	public static final String BASE_PATH = "./src/test/files/";

    /**
     * Test the symbol checker
     */
    @Test
    public void testSymbolChecking(){
		assertTrue(SymbolCollectorTester.run());
    }

	@Test
	public void testTypeProcessor(){
		assertTrue(TypeProcessorTester.run());
	}

	@Test
	public void testInterfaceProcessor(){
		assertTrue(InterfaceProcessorTester.run());
	}

	@Test
	public void testInputPortProcessor(){
		assertTrue(InputPortProcessorTester.run());
	}

	@Test
	public void testOutputPortProcessor(){
		assertTrue(OutputPortProcessorTester.run());
	}

	@Test
	public void testNil(){
		assertTrue(BehaviourProcessorTester.testNil());
	}

	@Test
	public void testSeq(){
		assertTrue(BehaviourProcessorTester.testSeq());
	}

	@Test
	public void testNotify(){
		assertTrue(BehaviourProcessorTester.testNotify());
	}

	@Test
	public void testOneWay(){
		assertTrue(BehaviourProcessorTester.testOneWay());
	}

	/**
	 * Runs the processors in order up to and including the one specified in parameter steps on the given modules.
	 * Step 0: symbolcollector,
	 * Step 1: type processor,
	 * Step 2: interface processor,
	 * Step 3: input port processor,
	 * Step 4: output port processor,
	 * Step 5: behaviour processor 
	 */
	public static void runProcessors(List<Module> modules, int steps){
		TypeCheckerVisitor[] visitors = {
			new SymbolCollector(),
			new TypeProcessor(),
			new InterfaceProcessor(),
			new InputPortProcessor(),
			new OutputPortProcessor()
		};

		for(int i = 0; i <= steps; i++){
			for(Module m : modules){
				visitors[i].process(m, false);
			}

			for(Module m : modules){
				visitors[i].process(m, true);
			}
		}
	}

	public static boolean testSymbolsForEquality(SymbolTable result, SymbolTable target){
		for(Entry<String, Pair<SymbolType, Symbol>> ent : target.entrySet()){
			String symbolName = ent.getKey();
			Symbol targetSymbol = ent.getValue().value();
			Symbol resultSymbol = result.get(symbolName);

			if(targetSymbol == null || resultSymbol == null){
				System.out.println("One is null for symbol: " + symbolName + "\n  Result: " + resultSymbol + "\n  Target: " + targetSymbol);
				return false;
			}

			if(!Symbol.equals(targetSymbol, resultSymbol)){
				System.out.println("FAIL on symbol " + symbolName + ":\n" + resultSymbol.prettyString() + "\n\nis not equal to\n\n" + targetSymbol.prettyString());
				return false;
			}

		}
		
		return true;
	}
}

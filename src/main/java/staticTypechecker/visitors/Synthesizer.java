package staticTypechecker.visitors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import jolie.lang.Constants.OperandType;
import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.ast.AddAssignStatement;
import jolie.lang.parse.ast.AssignStatement;
import jolie.lang.parse.ast.CompareConditionNode;
import jolie.lang.parse.ast.CompensateStatement;
import jolie.lang.parse.ast.CorrelationSetInfo;
import jolie.lang.parse.ast.CurrentHandlerStatement;
import jolie.lang.parse.ast.DeepCopyStatement;
import jolie.lang.parse.ast.DefinitionCallStatement;
import jolie.lang.parse.ast.DefinitionNode;
import jolie.lang.parse.ast.DivideAssignStatement;
import jolie.lang.parse.ast.DocumentationComment;
import jolie.lang.parse.ast.EmbedServiceNode;
import jolie.lang.parse.ast.EmbeddedServiceNode;
import jolie.lang.parse.ast.ExecutionInfo;
import jolie.lang.parse.ast.ExitStatement;
import jolie.lang.parse.ast.ForEachArrayItemStatement;
import jolie.lang.parse.ast.ForEachSubNodeStatement;
import jolie.lang.parse.ast.ForStatement;
import jolie.lang.parse.ast.IfStatement;
import jolie.lang.parse.ast.ImportStatement;
import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.InstallFixedVariableExpressionNode;
import jolie.lang.parse.ast.InstallStatement;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.InterfaceExtenderDefinition;
import jolie.lang.parse.ast.LinkInStatement;
import jolie.lang.parse.ast.LinkOutStatement;
import jolie.lang.parse.ast.MultiplyAssignStatement;
import jolie.lang.parse.ast.NDChoiceStatement;
import jolie.lang.parse.ast.NotificationOperationStatement;
import jolie.lang.parse.ast.NullProcessStatement;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.ast.OneWayOperationDeclaration;
import jolie.lang.parse.ast.OneWayOperationStatement;
import jolie.lang.parse.ast.OutputPortInfo;
import jolie.lang.parse.ast.ParallelStatement;
import jolie.lang.parse.ast.PointerStatement;
import jolie.lang.parse.ast.PostDecrementStatement;
import jolie.lang.parse.ast.PostIncrementStatement;
import jolie.lang.parse.ast.PreDecrementStatement;
import jolie.lang.parse.ast.PreIncrementStatement;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.ast.ProvideUntilStatement;
import jolie.lang.parse.ast.RequestResponseOperationDeclaration;
import jolie.lang.parse.ast.RequestResponseOperationStatement;
import jolie.lang.parse.ast.RunStatement;
import jolie.lang.parse.ast.Scope;
import jolie.lang.parse.ast.SequenceStatement;
import jolie.lang.parse.ast.ServiceNode;
import jolie.lang.parse.ast.SolicitResponseOperationStatement;
import jolie.lang.parse.ast.SpawnStatement;
import jolie.lang.parse.ast.SubtractAssignStatement;
import jolie.lang.parse.ast.SynchronizedStatement;
import jolie.lang.parse.ast.ThrowStatement;
import jolie.lang.parse.ast.TypeCastExpressionNode;
import jolie.lang.parse.ast.UndefStatement;
import jolie.lang.parse.ast.ValueVectorSizeExpressionNode;
import jolie.lang.parse.ast.VariablePathNode;
import jolie.lang.parse.ast.WhileStatement;
import jolie.lang.parse.ast.courier.CourierChoiceStatement;
import jolie.lang.parse.ast.courier.CourierDefinitionNode;
import jolie.lang.parse.ast.courier.NotificationForwardStatement;
import jolie.lang.parse.ast.courier.SolicitResponseForwardStatement;
import jolie.lang.parse.ast.expression.AndConditionNode;
import jolie.lang.parse.ast.expression.ConstantBoolExpression;
import jolie.lang.parse.ast.expression.ConstantDoubleExpression;
import jolie.lang.parse.ast.expression.ConstantIntegerExpression;
import jolie.lang.parse.ast.expression.ConstantLongExpression;
import jolie.lang.parse.ast.expression.ConstantStringExpression;
import jolie.lang.parse.ast.expression.FreshValueExpressionNode;
import jolie.lang.parse.ast.expression.InlineTreeExpressionNode;
import jolie.lang.parse.ast.expression.InstanceOfExpressionNode;
import jolie.lang.parse.ast.expression.IsTypeExpressionNode;
import jolie.lang.parse.ast.expression.NotExpressionNode;
import jolie.lang.parse.ast.expression.OrConditionNode;
import jolie.lang.parse.ast.expression.ProductExpressionNode;
import jolie.lang.parse.ast.expression.SumExpressionNode;
import jolie.lang.parse.ast.expression.VariableExpressionNode;
import jolie.lang.parse.ast.expression.VoidExpressionNode;
import jolie.lang.parse.ast.types.BasicTypeDefinition;
import jolie.lang.parse.ast.types.TypeChoiceDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeInlineDefinition;
import jolie.lang.parse.context.ParsingContext;
import jolie.util.Pair;
import staticTypechecker.entities.ChoiceType;
import staticTypechecker.entities.InlineType;
import staticTypechecker.entities.Type;
import staticTypechecker.utils.BasicTypeUtils;
import staticTypechecker.utils.ToString;
import staticTypechecker.utils.TreeUtils;
import staticTypechecker.utils.TypeConverter;
import staticTypechecker.entities.Module;
import staticTypechecker.entities.Operation;
import staticTypechecker.entities.Path;
import staticTypechecker.faults.FaultHandler;
import staticTypechecker.faults.MiscFault;
import staticTypechecker.faults.TypeFault;
import staticTypechecker.faults.WarningHandler;

/**
 * Synthesizer for a parsed Jolie abstract syntax tree. Synthesizes the type of each node
 * 
 * @author Kasper Bergstedt, kberg18@student.sdu.dk
 */
public class Synthesizer implements OLVisitor<Type, Type> {
	private static HashMap<String, Synthesizer> synths = new HashMap<>(); // maps module name to synthesizer
	private boolean inWhileLoop = false;
	private Stack<ArrayList<Path>> pathsAlteredInWhile = new Stack<>();
	
	public static Synthesizer get(Module module){
		Synthesizer ret = Synthesizer.synths.get(module.name());

		if(ret == null){
			ret = new Synthesizer(module);
			Synthesizer.synths.put(module.name(), ret);
		}
	
		return ret;
	}

	private Module module;
	
	private Synthesizer(Module module){
		this.module = module;
	}

	public Type synthesize(OLSyntaxNode node, Type T){
		return node.accept(this, T);
	}

	public Type visit(Program p, Type T){
		return T;
	}

	public Type visit(TypeInlineDefinition t, Type T){
		return T;
	}

	public Type visit( OneWayOperationDeclaration decl, Type T ){
		return T;
	};

	public Type visit( RequestResponseOperationDeclaration decl, Type T ){
		return T;
	};

	public Type visit( DefinitionNode n, Type T ){
		return T;
	};

	public Type visit( ParallelStatement n, Type T ){
		return T;
	};

	public Type visit( SequenceStatement n, Type T ){
		for(OLSyntaxNode child : n.children()){
			T = child.accept(this, T);
		}
		return T;
	};

	public Type visit( NDChoiceStatement n, Type T ){
		ArrayList<Type> trees = new ArrayList<>(n.children().size()); // save all the possible outcomes

		for(int i = 0; i < n.children().size(); i++){
			Type T1 = n.children().get(i).key().accept(this, T); // synthesize the label (in the [])
			Type T2 = n.children().get(i).value().accept(this, T1); // synthesize the behaviour (in the {})
			trees.add(T2);
		}
		
		if(trees.size() == 1){
			return trees.get(0);
		}
		else{
			return new ChoiceType(trees);		
		}
	};

	public Type visit( OneWayOperationStatement n, Type T ){
		Operation op = (Operation)this.module.symbols().get(n.id());

		Type T_in = op.requestType(); // the data type which is EXPECTED by the oneway operation
		Path p_in = new Path(n.inputVarPath().path()); // the path to the node which is given as input to the operation

		// update the node at the path to the input type
		Type T1 = T.shallowCopyExcept(p_in);
		TreeUtils.setTypeOfNodeByPath(p_in, T_in, T1);

		if(this.inWhileLoop){
			this.pathsAlteredInWhile.peek().add(p_in);
		}

		return T1;
	};

	public Type visit( RequestResponseOperationStatement n, Type T ){
		Operation op = (Operation)this.module.symbols().get(n.id());
		
		Type T_in = op.requestType(); // the type of the data the operation EXPECTS as input
		Type T_out = op.responseType(); // the type of the data RETURNED from the operation
		
		Path p_in = new Path(n.inputVarPath().path()); // the path to the node which is given AS INPUT to the operation
		Path p_out = new Path(n.outputExpression().toString()); // the path to the node in which the OUTPUT of the operation is stored

		// given that p_in is of type T_in find the type of the behaviour
		Type T_update = T.shallowCopyExcept(p_in);
		TreeUtils.setTypeOfNodeByPath(p_in, T_in, T_update);
		Type T1 = n.process().accept(this, T_update);
		
		// check that p_out is a subtype of T_out 
		ArrayList<Type> possibleTypes = TreeUtils.findNodesExact(p_out, T1, true, false); // the possible types of p_out after the behaviour
		
		Type p_out_type;
		if(possibleTypes.size() == 1){
			p_out_type = possibleTypes.get(0);
		}
		else{
			p_out_type = new ChoiceType(possibleTypes);
		}

		this.check(p_out_type, T_out, n.context(), op.name() + " does not have the expected return type");

		return T1;
	};

	public Type visit( NotificationOperationStatement n, Type T ){
		Operation op = (Operation)this.module.symbols().get(n.id());

		// if the notify is inside a while-loop and it is an assertion, it is a typehint, so we must check 
		String operationName = op.name();
		String outputPortName = n.outputPortId();

		if(operationName.equals("assert") && outputPortName.equals(System.getProperty("typehint"))){
			if(!(n.outputExpression() instanceof InstanceOfExpressionNode)){
				FaultHandler.throwFault(new MiscFault("argument given to assert must be an instanceof expression"), n.context(), true);
			}
			InstanceOfExpressionNode parsedNode = (InstanceOfExpressionNode)n.outputExpression();
			OLSyntaxNode expression = parsedNode.expression();

			if(!(expression instanceof VariableExpressionNode)){
				FaultHandler.throwFault(new MiscFault("first argument of instanceof must be a path to a variable"), n.context(), true);
			}
			
			Type type = TypeConverter.convert(parsedNode.type(), this.module.symbols());
			this.check(T, expression, type, ToString.of(expression) + " does not have the same type as the typehint");

			Path path = new Path(((VariableExpressionNode)expression).variablePath().path());
			Type T1 = T.shallowCopyExcept(path);
			TreeUtils.setTypeOfNodeByPath(path, type, T1);
			return T1;
		}
		
		Type T_out = op.requestType(); // the type of the data which is EXPECTED of the oneway operation
		Type p_out = n.outputExpression().accept(this, T); // the type which is GIVEN to the oneway operation

		this.check(p_out, T_out, n.context(), "type given to " + op.name() + " is different from what is expected");

		return T;
	};

	public Type visit( SolicitResponseOperationStatement n, Type T ){
		Operation op = (Operation)this.module.symbols().get(n.id());

		Type T_in = op.responseType(); // the type of the data which is RETURNED by the reqres operation
		Type T_out = op.requestType(); // the type of the data which is EXPECTED of the reqres operation
		
		Path p_in = new Path(n.inputVarPath().path()); // the path to the node in which to store the returned data
		Type p_out = n.outputExpression().accept(this, T); // the type which is GIVEN to the reqres operation
		
		// check that p_out is subtype of T_out
		this.check(p_out, T_out, n.context(), "type given to " + op.name() + " is different from what is expected");

		// update type of p_in to T_in
		Type T1 = T.shallowCopyExcept(p_in);
		TreeUtils.setTypeOfNodeByPath(p_in, T_in, T1);

		if(this.inWhileLoop){
			this.pathsAlteredInWhile.peek().add(p_in);
		}

		return T1;
	};

	public Type visit( LinkInStatement n, Type T ){
		return null;
	};

	public Type visit( LinkOutStatement n, Type T ){
		return null;
	};

	public Type visit( AssignStatement n, Type T ){
		// retrieve the type of the expression
		OLSyntaxNode e = n.expression();
		Type T_e = e.accept(this, T);
		
		// update the type of the node
		Path path = new Path(n.variablePath().path());
		Type T1 = T.shallowCopyExcept(path);
		ArrayList<BasicTypeDefinition> basicTypes = Type.getBasicTypesOfNode(T_e);
		TreeUtils.setBasicTypeOfNodeByPath(path, basicTypes, T1);

		if(this.inWhileLoop){
			this.pathsAlteredInWhile.peek().add(path);
		}

		return T1;
	};

	@Override
	public Type visit(AddAssignStatement n, Type T) {
		Path path = new Path(n.variablePath().path());

		Type T1 = T.shallowCopyExcept(path);
		
		Type typeOfRightSide = n.variablePath().accept(this, T);
		Type typeOfExpression = n.expression().accept(this, T);

		this.deriveTypeAndUpdateNode(path, T1, OperandType.ADD, typeOfRightSide, typeOfExpression, n.context());

		return T1;
	}

	@Override
	public Type visit(SubtractAssignStatement n, Type T) {
		Path path = new Path(n.variablePath().path());

		Type T1 = T.shallowCopyExcept(path);

		Type typeOfRightSide = n.variablePath().accept(this, T);
		Type typeOfExpression = n.expression().accept(this, T);

		this.deriveTypeAndUpdateNode(path, T1, OperandType.SUBTRACT, typeOfRightSide, typeOfExpression, n.context());

		return T1;
	}

	@Override
	public Type visit(MultiplyAssignStatement n, Type T) {
		Path path = new Path(n.variablePath().path());

		Type T1 = T.shallowCopyExcept(path);

		Type typeOfRightSide = n.variablePath().accept(this, T);
		Type typeOfExpression = n.expression().accept(this, T);

		this.deriveTypeAndUpdateNode(path, T1, OperandType.MULTIPLY, typeOfRightSide, typeOfExpression, n.context());
		
		return T1;
	}

	@Override
	public Type visit(DivideAssignStatement n, Type T) {
		Path path = new Path(n.variablePath().path());

		Type T1 = T.shallowCopyExcept(path);

		Type typeOfRightSide = n.variablePath().accept(this, T);
		Type typeOfExpression = n.expression().accept(this, T);

		this.deriveTypeAndUpdateNode(path, T1, OperandType.DIVIDE, typeOfRightSide, typeOfExpression, n.context());
		
		return T1;
	}

	private void deriveTypeAndUpdateNode(Path path, Type tree, OperandType opType, Type rightSide, Type expression, ParsingContext ctx){
		List<BasicTypeDefinition> newBasicTypes = BasicTypeUtils.deriveTypeOfOperation(opType, rightSide, expression, ctx);
		TreeUtils.setBasicTypeOfNodeByPath(path, newBasicTypes, tree);

		if(this.inWhileLoop){
			this.pathsAlteredInWhile.peek().add(path);
		}
	}

	public Type visit( IfStatement n, Type T ){
		ChoiceType resultType = new ChoiceType();

		for(Pair<OLSyntaxNode, OLSyntaxNode> p : n.children()){
			OLSyntaxNode expression = p.key();
			OLSyntaxNode body = p.value();

			if(!(expression instanceof InstanceOfExpressionNode)){ // COND-1, e is an expression of anything else than instanceof
				this.check(T, expression, Type.BOOL(), "guard of if-statement is not of type bool"); // check that expression is of type bool
				Type T1 = body.accept(this, T);
				resultType.addChoiceUnsafe(T1);
			}
		}

		OLSyntaxNode elseProcess = n.elseProcess();
		if(elseProcess != null){ // there is an else clause
			resultType.addChoiceUnsafe(elseProcess.accept(this, T));
		}
		else{ // there is not else clause, here we add the initial state as choice as well, since we may not enter the if statement
			resultType.addChoiceUnsafe(T);
		}
		
		if(resultType.choices().size() == 1){
			return resultType.choices().get(0);
		}
			
		return resultType;
	};

	public Type visit( DefinitionCallStatement n, Type T ){
		return null;
	};

	public Type visit( WhileStatement n, Type T ){
		this.setInWhileStatus(true);

		OLSyntaxNode condition = n.condition();
		OLSyntaxNode body = n.body();
		this.check(T, condition, Type.BOOL(), "guard of while loop is not of type bool"); // check that the initial condition is of type bool

		Type originalState = T; // saved here, since it is used in the fallback plan

		// the return type is a conjunction between the original state and the one found through the iterations OR the fallback
		ChoiceType result = new ChoiceType();
		result.addChoiceUnsafe(originalState);

		ChoiceType mergedState = new ChoiceType();
		mergedState.addChoiceUnsafe(originalState);

		for(int i = 0; i < 10; i++){
			// System.out.println("-------------- ITERATION " + i + " -----------------" );
			// System.out.println("T:\n" + T.prettyString() + "\n");
			
			Type R = body.accept(this, T); // synthesize the type of the body after an iteration
			this.check(R, condition, Type.BOOL(), "guard of while loop is not of type bool"); // check that the condition is of type bool
			// System.out.println("R:\n" + R.prettyString() + "\n");
			
			if(R.isSubtypeOf(mergedState)){ // the new state is a subtype of one of the previous states (we have a steady state)
				// System.out.println("subtype!");
				result.addChoiceUnsafe(mergedState);
				this.setInWhileStatus(false);
				return result;
			}

			mergedState.addChoiceUnsafe(R);
			// System.out.println("merged state:\n" + mergedState.prettyString() + "\n");
			T = R;
		}

		// we did not find a steady state in the while loop. Here we do the fallback plan, which is to undefine all variables changed in the while loop
		// System.out.println("FALLBACK");
		result.addChoiceUnsafe( TreeUtils.undefine(originalState, this.pathsAlteredInWhile.peek()) );
		// System.out.println("\n\n");

		WarningHandler.throwWarning("could not determine the resulting type of the while loop, affected types may be incorrect from here", n.context());
		
		this.setInWhileStatus(false);
		return result.convertIfPossible();
	};

	private void setInWhileStatus(boolean status){
		this.inWhileLoop = status;

		if(status){
			this.pathsAlteredInWhile.push(new ArrayList<>());
		}
		else{
			this.pathsAlteredInWhile.pop();
		}
	}

	public Type visit( OrConditionNode n, Type T ){
		return Type.BOOL();
	};

	public Type visit( AndConditionNode n, Type T ){
		return Type.BOOL();
	};

	public Type visit( NotExpressionNode n, Type T ){
		return Type.BOOL();
	};

	public Type visit( CompareConditionNode n, Type T ){
		return Type.BOOL();
	};

	public Type visit( ConstantIntegerExpression n, Type T ){
		return Type.INT();
	};

	public Type visit( ConstantDoubleExpression n, Type T ){
		return Type.DOUBLE();
	};

	public Type visit( ConstantBoolExpression n, Type T ){
		return Type.BOOL();
	};

	public Type visit( ConstantLongExpression n, Type T ){
		return Type.LONG();
	};

	public Type visit( ConstantStringExpression n, Type T ){
		return Type.STRING();
	};

	public Type visit( ProductExpressionNode n, Type T ){
		return this.getTypeOfSumOrProduct(n, T);
	};

	public Type visit( SumExpressionNode n, Type T ){
		return this.getTypeOfSumOrProduct(n, T);
	};

	private Type getTypeOfSumOrProduct(OLSyntaxNode n, Type T){
		List<Pair<OperandType, OLSyntaxNode>> operands;

		if(n instanceof SumExpressionNode){
			operands = ((SumExpressionNode)n).operands();
		}
		else{
			operands = ((ProductExpressionNode)n).operands();
		}

		Type currType = Type.VOID(); // set initial type to void to make sure it will be overwritten by any other type

		for(Pair<OperandType, OLSyntaxNode> pair : operands){
			OperandType currOp = pair.key();
			Type nextType = pair.value().accept(this, T);

			List<BasicTypeDefinition> basicTypes = BasicTypeUtils.deriveTypeOfOperation(currOp, currType, nextType, n.context());

			if(basicTypes.size() == 1){
				currType = new InlineType(basicTypes.get(0), null, null, false);
			}
			else{
				currType = ChoiceType.fromBasicTypes(basicTypes);
			}
		}

		return currType;
	}

	public Type visit( VariableExpressionNode n, Type T ){
		Path path = new Path(n.variablePath().path());
		ArrayList<Type> types = TreeUtils.findNodesExact(path, T, false, false);

		if(types.isEmpty()){ // return void type if no nodes was found
			return Type.VOID();
		}
		else if(types.size() == 1){
			return types.get(0);
		}
		else{
			return new ChoiceType(types);
		}
	};

	public Type visit( NullProcessStatement n, Type T ){
		return T;
	};

	public Type visit( Scope n, Type T ){
		return T;
	};

	public Type visit( InstallStatement n, Type T ){
		return T;
	};

	public Type visit( CompensateStatement n, Type T ){
		return T;
	};

	public Type visit( ThrowStatement n, Type T ){
		return T;
	};

	public Type visit( ExitStatement n, Type T ){
		return T;
	};

	public Type visit( ExecutionInfo n, Type T ){
		return T;
	};

	public Type visit( CorrelationSetInfo n, Type T ){
		return T;
	};

	public Type visit( InputPortInfo n, Type T ){
		return T;
	};

	public Type visit( OutputPortInfo n, Type T ){
		return T;
	};

	public Type visit( PointerStatement n, Type T ){
		return T;
	};

	public Type visit( DeepCopyStatement n, Type T ){
		Path leftPath = new Path(n.leftPath().path());
		
		Type T1 = T.shallowCopyExcept(leftPath);
		T1 = TreeUtils.unfold(leftPath, T1);

		ArrayList<InlineType> trees = new ArrayList<>();
	
		if(T1 instanceof InlineType){
			trees.add((InlineType)T1);
		}
		else{
			ChoiceType parsed = (ChoiceType)T1;
			trees = parsed.choices();
		}

		for(InlineType tree : trees){
			Type typeOfExpression = n.rightExpression().accept(this, tree);
	
			// find the nodes to update and their parents
			ArrayList<Pair<InlineType, String>> leftSideNodes = TreeUtils.findParentAndName(leftPath, tree, true, false);
	
			// update the nodes with the deep copied versions
			for(Pair<InlineType, String> pair : leftSideNodes){
				InlineType parent = pair.key();
				String childName = pair.value();
				Type child = parent.getChild(childName);

				
				// TreeUtils.fold(child);
				Type resultOfDeepCopy = Type.deepCopy(child, typeOfExpression);
				parent.addChildUnsafe(childName, resultOfDeepCopy);
			}
		}

		if(this.inWhileLoop){
			this.pathsAlteredInWhile.peek().add(leftPath);
		}

		return T1;
	};

	public Type visit( RunStatement n, Type T ){
		return T;
	};

	public Type visit( UndefStatement n, Type T ){
		Path path = new Path(n.variablePath().path());
		Type T1 = T.shallowCopyExcept(path);

		ArrayList<Pair<InlineType, String>> nodesToRemove = TreeUtils.findParentAndName(path, T1, false, false);

		for(Pair<InlineType, String> pair : nodesToRemove){
			pair.key().removeChildUnsafe(pair.value());
		}

		if(this.inWhileLoop){
			this.pathsAlteredInWhile.peek().add(path);
		}

		return T1;
	};

	public Type visit( ValueVectorSizeExpressionNode n, Type T ){
		return T;
	};

	public Type visit( PreIncrementStatement n, Type T ){
		return T;
	};

	public Type visit( PostIncrementStatement n, Type T ){
		return T;
	};

	public Type visit( PreDecrementStatement n, Type T ){
		return T;
	};

	public Type visit( PostDecrementStatement n, Type T ){
		return T;
	};

	public Type visit( ForStatement n, Type T ){
		return T;
	};

	public Type visit( ForEachSubNodeStatement n, Type T ){
		return T;
	};

	public Type visit( ForEachArrayItemStatement n, Type T ){
		return T;
	};

	public Type visit( SpawnStatement n, Type T ){
		return T;
	};

	public Type visit( IsTypeExpressionNode n, Type T ){
		return T;
	};

	public Type visit( InstanceOfExpressionNode n, Type T ){
		return Type.BOOL();
	};

	public Type visit( TypeCastExpressionNode n, Type T ){
		return new InlineType(BasicTypeDefinition.of(n.type()), null, null, false);
	};

	public Type visit( SynchronizedStatement n, Type T ){
		return T;
	};

	public Type visit( CurrentHandlerStatement n, Type T ){
		return T;
	};

	public Type visit( EmbeddedServiceNode n, Type T ){
		return T;
	};

	public Type visit( InstallFixedVariableExpressionNode n, Type T ){
		return T;
	};

	public Type visit( VariablePathNode n, Type T ){
		Path path = new Path(n.path());
		ArrayList<Type> types = TreeUtils.findNodesExact(path, T, false, false);

		if(types.size() == 1){
			return types.get(0);
		}
		else{
			return new ChoiceType(types);
		}
	};

	public Type visit( TypeDefinitionLink n, Type T ){
		return T;
	};

	public Type visit( InterfaceDefinition n, Type T ){
		return T;
	};

	public Type visit( DocumentationComment n, Type T ){
		return T;
	};

	public Type visit( FreshValueExpressionNode n, Type T ){
		return T;
	};

	public Type visit( CourierDefinitionNode n, Type T ){
		return T;
	};

	public Type visit( CourierChoiceStatement n, Type T ){
		return T;
	};

	public Type visit( NotificationForwardStatement n, Type T ){
		return T;
	};

	public Type visit( SolicitResponseForwardStatement n, Type T ){
		return T;
	};

	public Type visit( InterfaceExtenderDefinition n, Type T ){
		return T;
	};

	public Type visit( InlineTreeExpressionNode n, Type T ){
		return T;
	};

	public Type visit( VoidExpressionNode n, Type T ){
		return T;
	};

	public Type visit( ProvideUntilStatement n, Type T ){
		return T;
	};

	public Type visit( TypeChoiceDefinition n, Type T ){
		return T;
	};

	public Type visit( ImportStatement n, Type T ){
		return T;
	};

	public Type visit( ServiceNode n, Type T ){
		return T;
	};

	public Type visit( EmbedServiceNode n, Type T ){
		return T;
	};

	/**
	 * Checks that the type of the node, n, is a subtype of S
	 * @param T the tree in which n resides
	 * @param n the node to check the type of
	 * @param S the type of which the type of n must be a subtype
	 */
	public void check(Type T, OLSyntaxNode n, Type S, String faultPreamble){
		Type typeOfN = n.accept(this, T);
		if(!typeOfN.isSubtypeOf(S)){
			FaultHandler.throwFault(new TypeFault(typeOfN, S, faultPreamble), n.context(), false);
		}
	}

	public void check(Type T, Type S, ParsingContext ctx, String faultPreamble){
		if(!T.isSubtypeOf(S)){
			FaultHandler.throwFault(new TypeFault(T, S, faultPreamble), ctx, false);
		}
	}
}

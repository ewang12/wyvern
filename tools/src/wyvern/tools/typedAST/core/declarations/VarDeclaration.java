package wyvern.tools.typedAST.core.declarations;


import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;

import wyvern.target.corewyvernIL.decltype.DeclType;
import wyvern.target.corewyvernIL.decltype.VarDeclType;
import wyvern.target.corewyvernIL.expression.Expression;
import wyvern.target.corewyvernIL.expression.Let;
import wyvern.target.corewyvernIL.expression.MethodCall;
import wyvern.target.corewyvernIL.expression.Variable;
import wyvern.target.corewyvernIL.modules.TypedModuleSpec;
import wyvern.target.corewyvernIL.support.GenContext;
import wyvern.target.corewyvernIL.support.TopLevelContext;
import wyvern.target.corewyvernIL.type.StructuralType;
import wyvern.target.corewyvernIL.type.ValueType;
import wyvern.tools.errors.ErrorMessage;
import wyvern.tools.errors.FileLocation;
import wyvern.tools.errors.ToolError;
import wyvern.tools.typedAST.abs.Declaration;
import wyvern.tools.typedAST.core.binding.NameBinding;
import wyvern.tools.typedAST.core.binding.NameBindingImpl;
import wyvern.tools.typedAST.core.binding.evaluation.VarValueBinding;
import wyvern.tools.typedAST.core.binding.typechecking.AssignableNameBinding;
import wyvern.tools.typedAST.core.expressions.New;
import wyvern.tools.typedAST.interfaces.CoreAST;
import wyvern.tools.typedAST.interfaces.CoreASTVisitor;
import wyvern.tools.typedAST.interfaces.ExpressionAST;
import wyvern.tools.typedAST.interfaces.TypedAST;
import wyvern.tools.typedAST.interfaces.Value;
import wyvern.tools.typedAST.transformers.ExpressionWriter;
import wyvern.tools.typedAST.transformers.GenerationEnvironment;
import wyvern.tools.typedAST.transformers.ILWriter;
import wyvern.tools.types.Environment;
import wyvern.tools.types.Type;
import wyvern.tools.types.TypeResolver;
import wyvern.tools.util.EvaluationEnvironment;
import wyvern.tools.util.TreeWriter;

import java.util.LinkedList;
import java.util.List;

public class VarDeclaration extends Declaration implements CoreAST {
	ExpressionAST definition;
	Type definitionType;
	NameBinding binding;

	private boolean isClass;
	public boolean isClassMember() {
		return isClass;
	}

	@Deprecated
	public VarDeclaration(String varName, Type parsedType, TypedAST definition) {
		this(varName, parsedType, definition, FileLocation.UNKNOWN);
	}
	public VarDeclaration(String varName, Type parsedType, TypedAST definition, FileLocation loc) {
		this.definition=(ExpressionAST)definition;
		binding = new AssignableNameBinding(varName, parsedType);
		this.location = loc;
	}

	@Override
	public void writeArgsToTree(TreeWriter writer) {
		writer.writeArgs(binding.getName(), definition);
	}

	@Override
	protected Type doTypecheck(Environment env) {
		if (this.definition != null) {
			Type varType = definitionType;
			boolean defType = this.definition.typecheck(env, Optional.of(varType)).subtype(varType);
			if (!defType)
				ToolError.reportError(ErrorMessage.ACTUAL_FORMAL_TYPE_MISMATCH, this);
		}
		return binding.getType();
	}

	@Override
	public void accept(CoreASTVisitor visitor) {
		visitor.visit(this);
	}
	
	public NameBinding getBinding() {
		return binding;
	}

	@Override
	public Type getType() {
		return binding.getType();
	}

	@Override
	public String getName() {
		return binding.getName();
	}
	
	public TypedAST getDefinition() {
		return definition;
	}

	@Override
	protected Environment doExtend(Environment old, Environment against) {
		return old.extend(binding);
	}

	@Override
	public EvaluationEnvironment extendWithValue(EvaluationEnvironment old) {
		return old.extend(new VarValueBinding(binding.getName(), binding.getType(), null));
		//Environment newEnv = old.extend(new ValueBinding(binding.getName(), defValue));
	}

	@Override
	public void evalDecl(EvaluationEnvironment evalEnv, EvaluationEnvironment declEnv) {
		VarValueBinding vb = declEnv.lookupValueBinding(binding.getName(), VarValueBinding.class).get();
		if (definition == null) {
            vb.assign(null);
			return;
		}
		Value defValue = definition.evaluate(evalEnv);
		vb.assign(defValue);
	}

	@Override
	public Map<String, TypedAST> getChildren() {
		Hashtable<String, TypedAST> children = new Hashtable<>();
		children.put("definition", definition);
		return children;
	}

	@Override
	public TypedAST cloneWithChildren(Map<String, TypedAST> nc) {
		return new VarDeclaration(getName(), getType(), nc.get("definition"), location);
	}


    @Override
    public void codegenToIL(GenerationEnvironment environment, ILWriter writer) {
    	ValueType valType = getType().generateILType();
        environment.register(getName(), valType);
        String genName = GenerationEnvironment.generateVariableName();
        writer.wrap(e->new Let(genName, valType, Optional.ofNullable(definition).<Expression>map(d -> ExpressionWriter.generate(ew -> d.codegenToIL(environment, ew))).orElse(null), (Expression)e));
        writer.write(new wyvern.target.corewyvernIL.decl.VarDeclaration(getName(), valType, new Variable(genName), location));
    }

    @Override
	public Environment extendType(Environment env, Environment against) {
		return env;
	}

	@Override
	public Environment extendName(Environment env, Environment against) {
		definitionType = TypeResolver.resolve(binding.getType(), against);
		binding = new AssignableNameBinding(binding.getName(), definitionType);

		return env.extend(binding);
	}

	private FileLocation location = FileLocation.UNKNOWN;
	public FileLocation getLocation() {
		return this.location; //TODO
	}

	@Override
	public DeclType genILType(GenContext ctx) {
		ValueType vt = binding.getType().getILType(ctx);
		return new VarDeclType(getName(), vt);
	}

	@Override
	public wyvern.target.corewyvernIL.decl.Declaration generateDecl(GenContext ctx, GenContext thisContext) {
		
		// Create a var declaration. Getters and setters for the var are generated by the enclosing instance of New.
		// TODO: ideally want the getters and setters to be generated here?
		wyvern.target.corewyvernIL.decl.VarDeclaration varDecl;
		varDecl = new wyvern.target.corewyvernIL.decl.VarDeclaration(getName(), binding.getType().getILType(ctx), definition.generateIL(ctx, null), location);
		return varDecl;

	}

	@Override
	public wyvern.target.corewyvernIL.decl.Declaration topLevelGen(GenContext ctx, List<TypedModuleSpec> dependencies) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * Get the ValueType of this VarDeclaration. Sets the definitionType if it hasn't been already.
	 * @param ctx: context to evaluate in.
	 * @return ValueType of this VarDeclaration.
	 */
	private ValueType getILValueType (GenContext ctx) {
		return binding.getType().getILType(ctx);
	}
	
	@Override
	public void genTopLevel (TopLevelContext tlc) {
		
		GenContext ctx = tlc.getContext();
		
		// Figure out name and type of this variable.
		String varName = this.getName();
		Type varType = this.getType();
		ValueType varValueType = getILValueType(ctx);
		
		// Create a temp object with a single var declaration.
		VarDeclaration varDecl = new VarDeclaration(varName, this.binding.getType(), this.definition, location);
		DeclSequence tempObjBody = new DeclSequence(varDecl);
		New tempObj = new New(tempObjBody, location);
		String tempObjName = varNameToTempObj(varName);
		ValDeclaration letDecl = new ValDeclaration(tempObjName, tempObj, null);
		
		// Update context.
		letDecl.genTopLevel(tlc);
		ctx = tlc.getContext();
		
		// Create variables for the temp object to be used for the getter and setter.
		wyvern.tools.typedAST.core.expressions.Variable tempObjForSetter, tempObjForGetter;
		tempObjForGetter = new wyvern.tools.typedAST.core.expressions.Variable(new NameBindingImpl(tempObjName, null), null);
		tempObjForSetter = new wyvern.tools.typedAST.core.expressions.Variable(new NameBindingImpl(tempObjName, null), null);
		
		// Create getter and setter.
		DefDeclaration getter, setter;
		getter = DefDeclaration.generateGetter(ctx, tempObjForGetter, varName, varType);
		setter = DefDeclaration.generateSetter(ctx, tempObjForSetter, varName, varType);
		
		// Figure out structural type from declared types.
		List<DeclType> declarationTypes = new LinkedList<>();
		declarationTypes.add(getter.genILType(ctx));
		declarationTypes.add(setter.genILType(ctx));
		String newName = GenContext.generateName();
		// If it is a var declaration, it must be of resource type
		StructuralType structType = new StructuralType(newName, declarationTypes, true);
		ctx = ctx.extend(newName, new Variable(newName), structType);
		tlc.updateContext(ctx);
		
		// Group getter and setter into a single declaration block.
		List<wyvern.target.corewyvernIL.decl.Declaration> declarations = new LinkedList<>();
		wyvern.target.corewyvernIL.decl.Declaration getterIL, setterIL;
		getterIL = getter.generateDecl(ctx, ctx);
		setterIL = setter.generateDecl(ctx, ctx);
		declarations.add(getterIL);
		declarations.add(setterIL);
		
		// Wrap declarations with a New expression and add to top-level.
		wyvern.target.corewyvernIL.expression.New newExp;
		newExp = new wyvern.target.corewyvernIL.expression.New(declarations, newName, structType, getLocation());
		tlc.addLet(newName, structType, newExp, true);
		
		// Equate the var with a method call on its getter.
		// This means top-lever var reads are actually calls to the getter method.
		MethodCall methodCallExpr = new MethodCall(new Variable(newName), getter.getName(), new LinkedList<>(), this);
		ctx = ctx.extend(varName, methodCallExpr, varValueType);
		tlc.updateContext(ctx);
	}
	
	public static String varNameToTempObj (String s) {
		return "__temp" + Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}
	
	public static String varNameToGetter (String s) {
		return "get" + Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}
	
	public static String varNameToSetter (String s) {
		return "set" + Character.toUpperCase(s.charAt(0)) + s.substring(1);		
	}

	public static String getterToVarName (String s) {
		return s.replaceFirst("get", "");
	}
	
	@Override
	public void addModuleDecl(TopLevelContext tlc) {
		// do nothing--adding module declarations handled by genTopLevel method above.
		// overriding this is needed as the default throws an exception.
		return;
	}
	
}
package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.*;

public class SemanticAnalayzer extends VisitorAdaptor {

	boolean errorDetected = false;
	boolean hasMain = false; 
	boolean isArray = false;
	int nVars;
	Struct currentType = Tab.noType;
	String typeName;
	Struct currentMethodType = Tab.noType;
	Obj currentMethod;
	Logger log = Logger.getLogger(getClass());
	boolean returnFound = false;

	public SemanticAnalayzer() {
		Tab.currentScope.addToLocals(new Obj(Obj.Type, "bool", TabExtension.boolType));
	}

	public void report_error(String message, SyntaxNode info) {
		errorDetected = true;
		StringBuilder msg = new StringBuilder(message);
		int line = (info == null) ? 0 : info.getLine();
		if (line != 0)
			msg.append(" on the line: ").append(line);
		log.error(msg.toString());
	}

	public void report_info(String message, SyntaxNode info) {
		StringBuilder msg = new StringBuilder(message);
		int line = (info == null) ? 0 : info.getLine();
		if (line != 0)
			msg.append(" on the line: ").append(line);
		log.info(msg.toString());
	}

	public void visit(ProgName progName) {
		progName.obj = Tab.insert(Obj.Prog, progName.getProgName(), Tab.noType);
		Tab.openScope();
		log.info("Started program: " + progName.getProgName());
	}

	public void visit(Program program) {
		nVars = Tab.currentScope.getnVars();
		Tab.chainLocalSymbols(program.getProgName().obj);
		if (!hasMain) {
			report_error("No main method!", null);
		}
		Tab.closeScope();
		log.info("Finished");
	}

	public void visit(Type type) {
		Obj typeNode = Tab.find(type.getTypeName());
		if (typeNode == Tab.noObj) {
			report_error("No type: " + type.getTypeName() + " in the symbol table! ", null);
			type.struct = Tab.noType;
		} else {
			if (Obj.Type == typeNode.getKind()) {
				type.struct = typeNode.getType();
			} else {
				report_error("Error: Name " + type.getTypeName() + " is not a type! " + Obj.Type + " sta ti je? " + typeNode.getKind(), type);
				type.struct = Tab.noType;
			}
		}
		currentType = type.struct;
	}

	public boolean checkUniqueConst(String constName) {
		Obj constNode = Tab.find(constName);
		if (constNode != Tab.noObj) {
			report_error("Const: " + constName + " already declared!", null);
			return false;
		}
		return true;
	}

	// Const...
	public void visit(IntegerConstValue intConst) {
		String name = intConst.getConstName();
		Obj obj = Tab.find(name);

		if (!checkUniqueConst(name)) {
			return;
		} else if (!Tab.intType.equals(currentType)) {
			report_error(" Wrong const " + name + " type, it should be int!", intConst);
			return;
		}
		obj = Tab.insert(Obj.Con, name, currentType);
		obj.setAdr(intConst.getNumberConstValue());
	}

	public void visit(CharConstValue charConst) {
		String name = charConst.getConstName();
		Obj obj = Tab.find(name);

		if (!checkUniqueConst(name)) {
			return;
		} else if (!Tab.charType.equals(currentType)) {
			report_error(" Wrong const " + name + " type, it should be char!", charConst);
			return;
		}
		obj = Tab.insert(Obj.Con, name, currentType);
		obj.setAdr(charConst.getCharConstValue());
	}

	public void visit(BoolConstValue boolConst) {
		String name = boolConst.getConstName();
		Obj obj = Tab.find(name);

		if (!checkUniqueConst(name)) {
			return;
		} else if (!TabExtension.boolType.equals(currentType)) {
			report_error(" Wrong const " + name + " type, it should be boolean!", boolConst);
			return;
		}
		obj = Tab.insert(Obj.Con, name, currentType);
		obj.setAdr(boolConst.getBoolConstValue() ? 1 : 0);
	}

	// Var...
	public boolean checkUniqueVariableLocal(String variableName) {
		Obj varNode = Tab.find(variableName);
		if (varNode != Tab.noObj) {
			if (Tab.currentScope.findSymbol(variableName) != null) {
				report_error("Variable: " + variableName + " already declared!", null);
				return false;
			}
		}
		return true;
	}

	public void visit(VarDeclArray array) {
		isArray = true;
	}

	public void visit(VariableIsNotArray array) {
		isArray = false;
	}

	public void visit(LastVar lastVar) {
		String name = lastVar.getVarName();
		if (!checkUniqueVariableLocal(name)) {
			return;
		}
		if (!isArray) {
			Tab.insert(Obj.Var, name, currentType);
		} else {
			Tab.insert(Obj.Var, name, new Struct(Struct.Array, currentType));
			isArray = false;
			Obj obj = Tab.find(name);
//			report_info("Ovde je za niz: " + name + " tip= " + obj.getType().getKind() , lastVar);
		}
		nVars++;
	}

	
	// Method..
	public boolean checkUniqueMethod(String methodName) {
		if (Tab.currentScope.findSymbol(methodName) != null) {
			report_error("Method: " + methodName + " already declared!", null);
			return false;
		}
		return true;
	}
	
	//da li se potudara return parametar - da li mi to treba za A?	
	public void visit (FormalParamDeclaration formalParam) {
		String name = formalParam.getParmName();
		Obj obj = Tab.find(name);
		if (!checkUniqueVariableLocal(name)) {
			return;
		}
		if (!isArray) {
			Tab.insert(Obj.Var, name, currentType);
		} else {
			Tab.insert(Obj.Var, name, new Struct(Struct.Array, currentType));
			isArray = false;
		}
	}
	
	public void visit(VoidType voidType) {
		currentMethodType = Tab.noType;
		typeName = "void";
	}
	
	public void visit(ConcreteType returnType) {
		Obj typeNode = Tab.find(returnType.getRetTypeName());
		
		if (typeNode == Tab.noObj) {
			report_error("No type: " + returnType.getRetTypeName() + " in the symbol table! ", null);
			returnType.struct = Tab.noType;
		} else {
			if (Obj.Type == typeNode.getKind()) {
				returnType.struct = typeNode.getType();
			} else {
				report_error("Error: Name " + returnType.getRetTypeName() + " is not a type!", returnType);
				returnType.struct = Tab.noType;
			}
		}
		currentMethodType = returnType.struct;
		typeName = returnType.getRetTypeName();
	}
	
	public void visit(MethodType methodPass) {
		String name = methodPass.getMethName();
		Obj obj = Tab.find(name);
		methodPass.obj = obj;
		if (!checkUniqueVariableLocal(name)) {
			return;
		}
		if (name.equalsIgnoreCase("main")) {
			hasMain = true;
		}
		
		currentMethod = Tab.insert(Obj.Meth, name, currentMethodType);
		
		Tab.openScope();
		report_info("Processing function: " + name, methodPass);
	}
	
	public void visit(MethodDecl methodDecl) {
		if(!returnFound && currentMethod.getType() != Tab.noType){
			report_error("Semantic error on the line: " + methodDecl.getLine() + ": function " + currentMethod.getName() + " doesn't have return!", null);
    	}
    	Tab.chainLocalSymbols(currentMethod);
    	
    	Tab.closeScope();
    	
    	returnFound = false;
    	currentMethod = null;
    	currentMethodType = null;
    	typeName = "noTyp";
	}
	

	//valjda mi samo void za main ?
	public void visit(ReturnExpr returnExpr) {
		returnFound = true;
		if (currentMethodType != returnExpr.getExpr().struct) {
			report_error("Error on the line: " + returnExpr.getLine() + " : " + "wrong return type " + currentMethod.getName(), null);
		}
		
	}
	
	public void visit (ReturnNoExpr returnNoExpr) {
		returnFound = true;
		if (currentMethod.getType() != Tab.noType) {
			report_error("Error on the line: " + returnNoExpr.getLine() + " : " + "wrong return type it should be void " + currentMethod.getName(), null);
		}
	}
	
	public void visit(NumConst numConst) {
		numConst.struct = Tab.intType;
	}
	
	public void visit(CharConst charConst) {
		charConst.struct = Tab.charType;
	}
	
	public void visit(BoolConst boolConst) {
		boolConst.struct = TabExtension.boolType;
	}
	
	public void visit (TermFact termFacor) {
		termFacor.struct = termFacor.getFactor().struct;
	}
	
	public void visit (TermMul termMul) {
		if (termMul.getFactor().struct != Tab.intType || termMul.getTerm().struct != Tab.intType) {
			report_error("Operands needs to be int type!", termMul);
			termMul.struct = Tab.noType;
		}
		termMul.struct = termMul.getFactor().struct;
	}
	
	public void visit (FactorConst factorConst) {
		factorConst.struct = factorConst.getConstFactor().struct;
	}
	
	public void visit (OneTermExpr oneTerm) {
		oneTerm.struct = oneTerm.getTerm().struct;
	}
	
// Factor Designator
	public void visit (FactorVariable factorVariable) {
		// simple var
		// or array element
		factorVariable.struct = factorVariable.getDesignator().obj.getType();
		if (factorVariable.getDesignator().obj.getKind() == Obj.Var) {
			if (factorVariable.getDesignator().obj.getKind() == Struct.Array) {
//				report_info("Pristup nizu " + factorVariable.getDesignator().obj.getName(), factorVariable);
			} else {
//				report_info("Pristup promenljivoj " + factorVariable.getDesignator().obj.getName(), factorVariable);
			}
		}
	}
	
	public void visit(ExprCall factorExpr) {
		factorExpr.struct = factorExpr.getExpr().struct;
	}
	
	public void visit (NewCallWithPar factorNew) {
		if (factorNew.getExpr().struct  != Tab.intType) {
			factorNew.struct = Tab.noType;
			report_error("Array size needs to be an int!", factorNew);
			return;
		}
		factorNew.struct = new Struct(Struct.Array, factorNew.getType().struct); // ?
//		report_info("Ovde mi je tip: " + factorNew.getType().struct.getKind(), factorNew);
	}
	
	public void visit(Actuals actualParam) {
		actualParam.struct = actualParam.getActualParamList().struct;
	}
	
	public void visit(ActualParam parametar) {
		parametar.struct = parametar.getExpr().struct;
	}
	
	public void visit(ActualParams parametar) {
		parametar.struct = parametar.getExpr().struct;
	}
//Designator...
	
	public void visit(DesignatorOnly designatorOnly) {
		if (designatorOnly == null) {
			report_error("Designator is null!", designatorOnly);
			return;
		}
		Obj node = Tab.find(designatorOnly.getDesignatorName());
		if (node == Tab.noObj) {
			report_error("Var: " + designatorOnly.getDesignatorName() + " is not declared!" , designatorOnly);
		}
		if (node == null) {
			report_error("Node is null designatorOnly error", designatorOnly);
		}
		designatorOnly.obj = node;
//		report_info("Designator : " + designatorOnly.getDesignatorName() + " node.kind() = " + node.getKind(), designatorOnly);
	}
	
	public void visit(ExprDesignator designatorWithExpr) {
		//To do
		//niz[3] = 7;
//		Obj node = Tab.find(designatorWithExpr.getDesigatorNameExpr());
		Obj node = designatorWithExpr.getDesignatorArray().obj;
		if (node == null) {
//			report_error("Node je null???", designatorWithExpr);
			return;
		}
		if ( node.getType().getKind() != Struct.Array) {
			report_error("Var needs to be an array!" + node.getKind(), designatorWithExpr);
		}
		if ( designatorWithExpr.getExpr().struct != Tab.intType) {
			report_error("Array index needs to be an int!" , designatorWithExpr);
		}
		designatorWithExpr.obj = new Obj(Obj.Elem, node.getName(), node.getType().getElemType());
//		designatorWithExpr.obj = node;
	}
	
	public void visit(DesStatmentAssign designatorAssign) {
		Obj designatorObject = designatorAssign.getDesignator().obj;
		if (designatorObject.getKind() != Obj.Var && designatorObject.getKind() != Obj.Elem) {
			report_error("Assignment can be done only on a Var or Elem of an array! " + designatorObject.getType().getKind(), designatorAssign);
		}
		
		if (designatorObject.getType().getKind() != Struct.Array) {
//			report_info("Ovde mi tip: " + designatorObject.getType().getKind(), designatorAssign);
			if (designatorObject.getType() != designatorAssign.getExpr().struct) {
				report_error("Error1: Left and right side of assign operator, are diffrent type!" + " left type = " + designatorAssign.getDesignator().obj.getType().getKind() + 
						" right type = " + designatorAssign.getExpr().struct.getKind(), designatorAssign);
			}
		} else {
//			if (designatorObject.getType().getElemType().getKind() != designatorAssign.getExpr().struct.getKind()) {
//				report_error("Error2: Left and right side of assign operator, are diffrent type! "
//						+ "left type = " + designatorAssign.getDesignator().obj.getType().getElemType().getKind()
//						+ " right type = " + designatorAssign.getExpr().struct.getKind(), designatorAssign);
//			}
			if (!designatorObject.getType().assignableTo(designatorAssign.getExpr().struct)) {
				report_error("Error2: Left and right side of assign operator, are diffrent type! ", designatorAssign);
			}
		} 
	}
	
	public void visit(DesStatmentInc designatorInc) {
//		report_info("Increment on " + designatorInc.getDesignator(), designatorInc);
		if (designatorInc.getDesignator().obj.getKind() != Obj.Var && designatorInc.getDesignator().obj.getKind() != Obj.Elem) {
			report_error("Increment can be done only on a Var or Elem of an array!", designatorInc);
		}
		if (designatorInc.getDesignator().obj.getType() != Tab.intType) {
			report_error("Increment with non int value!" , designatorInc);
		}
	}
	
	public void visit(DesStatmentDec designatorDec) {
//		report_info("Decrement on " + designatorDec.getDesignator(), designatorDec);
		if (designatorDec.getDesignator().obj.getKind() != Obj.Var && designatorDec.getDesignator().obj.getKind() != Obj.Elem) {
			report_error("Decrement can be done only on a Var or Elem of an array!", designatorDec);
		}
		if (designatorDec.getDesignator().obj.getType() != Tab.intType) {
			report_error("Decrement with non int value!" , designatorDec);
		}
	}
	
	public void visit(OneDesignator oneDesig) {
		oneDesig.struct = oneDesig.getDesignator().obj.getType();
	}
	
	public void visit(DesignatorMany designMany) {
		designMany.struct = designMany.getDesignatorLine().struct;
	}
	
	public void visit(NoDesignatorList desigOne) {
		desigOne.struct = desigOne.getDesignatorLine().struct;
	}
	
	public void visit(DesignatorArray desigArray) {
		desigArray.obj = desigArray.getDesignator().obj;
	}
	
	//--------------------------------------------------TO DO-------------------------------------
	
	public void visit(FormParams formParmas) {
		//To do
	}
	
	public void visit(FormalParamDecls formalParamDecl) {
		//To do
	}
	
	public void visit(SingleFormalParamDecl singleFormalParamDecl) {
		//To do
		
	}
	
	
	//--------------------------------------------------------------------
	public void visit(OneNegTermExpr termNeg) {
		if (termNeg.getTerm().struct == Tab.intType) {
			termNeg.struct = Tab.intType;
		} else {
			report_error("Operand needs to be an int!", termNeg);
			termNeg.struct = Tab.noType;
		}
	}
	
	public void visit (AddExpr addTermExpr) {
		if (addTermExpr.getTerm().struct != Tab.intType || addTermExpr.getExpr().struct != Tab.intType) {
			report_error("Operands need to be int type! ", addTermExpr);
			addTermExpr.struct = Tab.noType;
		} else {
			addTermExpr.struct = Tab.intType;
		}
	}
	
	public void visit(FindAny findAny) {
		if (findAny.getDummyDesignator().obj.getType().getKind() != Struct.Array) {
			report_error("FindAny, right operand needs to be an array!", findAny);
			return;
		}
		if (findAny.getDesignator().obj.getType() != TabExtension.boolType) {
			report_error("FindAny, left operand needs to be a bool!", findAny);
			return;
		}
	}
	
	public void visit (DummyDesignator dummyDesignator) {
		dummyDesignator.obj = dummyDesignator.getDesignator().obj;
	}
	
	public void visit(ReadStatment readStetm) {
//		if (readStetm.getDesignator().obj.getType().getKind() != Struct.Array && readStetm.getDesignator().obj.getType().getKind() != Struct. )
		if (readStetm.getDesignator().obj.getKind() != Obj.Var && readStetm.getDesignator().obj.getKind() != Obj.Elem) {
			report_error("Read can be done only on a Var or Elem of an array!", readStetm);
		}
		if (readStetm.getDesignator().obj.getType() != Tab.intType && readStetm.getDesignator().obj.getType() != Tab.charType && readStetm.getDesignator().obj.getType() != TabExtension.boolType) {
			report_error("Read can only be done with int, char or bool type!" , readStetm);
		}
	}
	
	public void visit(PrintStatment printStetm) {
		if (printStetm.getExpr().struct != Tab.intType && printStetm.getExpr().struct != Tab.charType && printStetm.getExpr().struct != TabExtension.boolType) {
			report_error("Print can only be done with int, char or bool type!" , printStetm);
		}
	}
	
	public void visit(DesStatmentFunc funcCall) {
		String funcName = funcCall.getDesignator().obj.getName();
		report_info("Proverava za funkciju: " + funcName, funcCall);
		if ("chr".equalsIgnoreCase(funcName)) {
			if (funcCall.getActualPars().struct != Tab.intType) {
				report_error("chr() function needs to be used with int type!", funcCall);
			}
		} else if ("ord".equalsIgnoreCase(funcName)) {
			if (funcCall.getActualPars().struct != Tab.charType) {
				report_error("ord() function needs to be used with char type!", funcCall);
			}
		} else if ("len".equalsIgnoreCase(funcName)) {
			if (funcCall.getActualPars().struct.getKind() != Struct.Array) {
				report_error("len() function needs to be used with an array!", funcCall);
			}
		}
		funcCall.obj = Tab.find(funcName);
		
	}
	
	public void visit (FuncCall funCall) {
		funCall.struct = funCall.getDesignator().obj.getType();
	}
	
	public boolean passed() {
		return !errorDetected;
	}
	
	
}

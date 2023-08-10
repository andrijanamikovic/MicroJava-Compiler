package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;

import java.lang.reflect.MalformedParameterizedTypeException;

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
				report_error("Error: Name " + type.getTypeName() + " is not a type!", type);
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
		report_info("Udjes li ti pobratime ovde majke ti? " + returnType.getRetTypeName(), returnType);
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
		if (!checkUniqueVariableLocal(name)) {
			return;
		}
		if (name.equalsIgnoreCase("main")) {
			hasMain = true;
		}
		
		currentMethod = Tab.insert(Obj.Meth, typeName, currentMethodType);
		
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
//		if (currentMethodType != returnExpr.getExpr().struct) {
//			report_error("Error on the line: " + returnExpr.getLine() + " : " + "wrong return type " + currentMethod.getName() + " a ovde je: " + returnExpr.getExpr().struct, null);
//		}
	}
	
	public void visit (ReturnNoExpr returnNoExpr) {
		returnFound = true;
//		if (currentMethod.getType() != Tab.noType) {
//			report_error("Error on the line: " + returnNoExpr.getLine() + " : " + "wrong return type it should be void " + currentMethod.getName(), null);
//		}
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
				report_info("Pristup nizu " + factorVariable.getDesignator().obj.getName(), factorVariable);
			} else {
				report_info("Pristup promenljivoj " + factorVariable.getDesignator().obj.getName(), factorVariable);
			}
		}
	}
	
	public void visit(ExprCall factorExpr) {
		//TO DO
	}
	
	public void visit (NewCallWithPar factorNew) {
		//TO DO
		if (factorNew.getActualPars().struct != Tab.intType) {
			factorNew.struct = Tab.noType;
			report_error("Array size needs to be an int!", factorNew);
			return;
		}
		
		factorNew.struct = new Struct(Struct.Array, factorNew.getActualPars().struct);
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
	
	public boolean passed() {
		return !errorDetected;
	}
}

package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;
import org.apache.log4j.Logger;
import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.*;

public class SemanticAnalayzer extends VisitorAdaptor {

	boolean errorDetected = false;
	boolean hasMain = true; //TO DO: prebaci na false
	int nVars;
	Struct currentType;
	Logger log = Logger.getLogger(getClass());
	
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
	
	public void visit (Type type) {
		Obj typeNode = Tab.find(type.getTypeName());
    	if(typeNode == Tab.noObj){
    		report_error("No type: " + type.getTypeName() + " in the symbol table! ", null);
    		type.struct = Tab.noType;
    	}else{
    		if(Obj.Type == typeNode.getKind()){
    			type.struct = typeNode.getType();
    		}else{
    			report_error("Error: Name " + type.getTypeName() + " is not a type!", type);
    			type.struct = Tab.noType;
    		}
    	}
    	currentType = type.struct;
	}
	
	public boolean checkUniqueConst(String constName) {
		Obj constNode = Tab.find(constName);
		if (constNode != Tab.noObj) {
			report_error("Const: " + constName + " already declared!",  null);
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

	public boolean passed() {
		return !errorDetected;
	}
}

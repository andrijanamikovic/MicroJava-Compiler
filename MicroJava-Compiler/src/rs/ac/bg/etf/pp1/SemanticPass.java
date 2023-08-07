package rs.ac.bg.etf.pp1;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.*;


public class SemanticPass extends VisitorAdaptor {

	int printCallCount = 0;
	int varDeclCount = 0;
	int constDeclCount = 0;
	Type type = null;
	Obj currentMethod = null;
	
	Logger log = Logger.getLogger(getClass());
	
	public void report_error(String message, SyntaxNode info) {
		StringBuilder msg = new StringBuilder(message);
		int line = (info == null) ? 0: info.getLine();
		if (line != 0)
			msg.append (" na liniji ").append(line);
		log.error(msg.toString());
	}

	public void report_info(String message, SyntaxNode info) {
		StringBuilder msg = new StringBuilder(message); 
		int line = (info == null) ? 0: info.getLine();
		if (line != 0)
			msg.append (" na liniji ").append(line);
		log.info(msg.toString());
	}
	

	public void visit(LastVar lastvardecl){
		varDeclCount++;
		report_info("Deklarisana promenljiva "+ lastvardecl.getVarName(), lastvardecl);
		if (type == null) {
			report_info("Ne mogu ovako da se bahcem sa type u lastvardecl", lastvardecl);
			return;
			
		} else {
			Obj varNode = Tab.insert(Obj.Var, lastvardecl.getVarName(), type.struct);
		}
	}
	
	public void visit (VarDecl vardecl) {
		type = vardecl.getType();
	}
	
    public void visit(PrintStatment print) {
		printCallCount++;
	}   
    
    public void visit(ConstDecl constdecl) {
    	constDeclCount++;
    }
    
    public void visit(ProgName progName) {
    	progName.obj = Tab.insert(Obj.Prog, progName.getProgName(), Tab.noType);
    	Tab.openScope();
    }
    
    public void visit(Program program) {
    	Tab.chainLocalSymbols(program.getProgName().obj);
    	Tab.closeScope();
    }
    
    public void visit(Type type) {
    	if (type == null) {
    		report_info("Type ne valja u type visit metodi" , type);
    		return;
    	}
    	Obj typeNode = Tab.find(type.getTypeName());
    	if (typeNode == Tab.noObj) {
    		report_error("Nije pronadjen tip " + type.getTypeName() + " u tabeli simbola!", null);
    		type.struct = Tab.noType;
    	} else {
    		if (Obj.Type == typeNode.getKind()) {
    			type.struct = typeNode.getType();
    		} else {
    			report_error("Greska: Ime: " + type.getTypeName() + " ne prestavlja tip! ", type);
        		type.struct = Tab.noType;
    		}
    	}
    }
    
    public void visit(MethodType methodType) {
    	currentMethod = Tab.insert(Obj.Meth, methodType.getMethName(), null); // ja imam samo void? i opet je meni to razdeljeno na vise onih grananja
    	methodType.obj = currentMethod;
    	Tab.openScope();
		report_info("Obradjuje se funkcija "+ methodType.getMethName(), null);
    }
    
    public void visit(MethodDecl methodDecl) {
    	Tab.chainLocalSymbols(currentMethod);
    	Tab.closeScope();
    	
    	currentMethod = null;
    }
    
    public void visit(Designator desig) {
    		//    	Obj obj = Tab.find(desig.getDesigName());
    		//nema mi ove funkcije??
    }
}

package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.CounterVisitor.FormParamCounter;
import rs.ac.bg.etf.pp1.CounterVisitor.VarCounter;
import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.Obj;

public class CodeGenerator extends VisitorAdaptor {
private int mainPc;
	
	public int getMainPc(){
		return mainPc;
	}
	
	public void visit(PrintStatment printStmt){
		if(printStmt.getExpr().struct == Tab.intType){
			Code.loadConst(5);
			Code.put(Code.print);
		}else{
			Code.loadConst(1);
			Code.put(Code.bprint);
		}
	}
	
	public void visit(NumConst cnst){
		Obj con = Tab.insert(Obj.Con, "$", cnst.struct);
		con.setLevel(0);
		con.setAdr(cnst.getValNum());
		
		Code.load(con);
	}
	
	public void visit(MethodType methodTypeName) {
		if ("main".equals(methodTypeName.getMethName())) {
			mainPc = Code.pc;
		}
		methodTypeName.obj.setAdr(Code.pc);
		
		// Collect arguments and local variables
		SyntaxNode methodeNode = methodTypeName.getParent();
		
		VarCounter vcnt = new VarCounter();
		methodeNode.traverseTopDown(vcnt);
		
		FormParamCounter fpCnt = new FormParamCounter();
		methodeNode.traverseTopDown(fpCnt);
		
		// Generate the entry
		Code.put(Code.enter);
		Code.put(fpCnt.getCount());
		Code.put(fpCnt.getCount() + vcnt.getCount());
		
	}
	
	//mozda mi ne trebaju ove naredne dve
	public void visit(VoidType methodDecl) {
		
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	public void visit(ConcreteType methodDecl) {
		
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	public void visit(ReturnNoExpr returnNoExpr) {
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	public void visit(ReturnExpr returnExpr) {
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	public void visit(MethodDecl methodDecl) {
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	public void visit(DesStatmentAssign designatorAssign) {
		Code.store(designatorAssign.getDesignator().obj);
	}
	
	public void visit(DesignatorOnly designatorOnly) {
		SyntaxNode parent = designatorOnly.getParent();
		
		if (DesStatmentAssign.class != parent.getClass() && FuncCall.class != parent.getClass()) {
			Code.load(designatorOnly.obj);
		}
	}
}

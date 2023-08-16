package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.CounterVisitor.FormParamCounter;
import rs.ac.bg.etf.pp1.CounterVisitor.VarCounter;
import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.Obj;

public class CodeGenerator extends VisitorAdaptor {
private int mainPc;
private int adr1;
private int adr2;
private int adr3;
public int getMainPc(){
		return mainPc;
	}
	
	public void visit(PrintStatment printStmt){
		if(printStmt.getExpr().struct == Tab.intType){
			if(printStmt.getPrintArgs().getClass() == NoArgumentsForPrint.class) {
				Code.loadConst(5);
			} else if (printStmt.getPrintArgs().getClass() == ArgumentsForPrint.class) {
				ArgumentsForPrint args = (ArgumentsForPrint) printStmt.getPrintArgs();
				Code.loadConst(args.getN1());
			}
			Code.put(Code.print);
		}else if (printStmt.getExpr().struct == TabExtension.boolType) {
			if(printStmt.getPrintArgs().getClass() == NoArgumentsForPrint.class) {
				Code.loadConst(5);
			} else if (printStmt.getPrintArgs().getClass() == ArgumentsForPrint.class) {
				ArgumentsForPrint args = (ArgumentsForPrint) printStmt.getPrintArgs();
				Code.loadConst(args.getN1());
			}
			Code.put(Code.print);
		}else {
			if(printStmt.getPrintArgs().getClass() == NoArgumentsForPrint.class) {
				Code.loadConst(1);
			} else if (printStmt.getPrintArgs().getClass() == ArgumentsForPrint.class) {
				ArgumentsForPrint args = (ArgumentsForPrint) printStmt.getPrintArgs();
				Code.loadConst(args.getN1());
			}
			Code.put(Code.bprint);
		}
	}
	
	public void visit(ReadStatment readStmt) {
		if (readStmt.getDesignator().obj.getType() == Tab.charType) {
			Code.put(Code.bread);
		} else {
			Code.put(Code.read);
		}
		Code.store(readStmt.getDesignator().obj);
	}
	
	public void visit(NumConst cnst){
		Obj con = Tab.insert(Obj.Con, "$", cnst.struct);
		con.setLevel(0);
		con.setAdr(cnst.getValNum());
		Code.load(con);
	}
	
	public void visit(CharConst cnst) {
		Obj con = Tab.insert(Obj.Con, "$", cnst.struct);
		con.setLevel(0);
		con.setAdr(cnst.getValChar());
		Code.load(con);
	}
	
	public void visit(BoolConst cnst) {
		Obj con = Tab.insert(Obj.Con, "$", cnst.struct);
		con.setLevel(0);
		con.setAdr(cnst.getValBool() ? 1 : 0);
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
//	public void visit(VoidType methodDecl) {
//		
//		Code.put(Code.exit);
//		Code.put(Code.return_);
//	}
//	
//	public void visit(ConcreteType methodDecl) {
//		
//		Code.put(Code.exit);
//		Code.put(Code.return_);
//	}
	///
	
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
	
	public void visit(FactorVariable designatorOnly) {
		SyntaxNode parent = designatorOnly.getParent();
		
		if (DesStatmentAssign.class != parent.getClass() && FuncCall.class != parent.getClass()) {
			Code.load(designatorOnly.getDesignator().obj); 
		}
	}
	
//	//
//	public void visit(DesStatmentMore desMore) {
//		Code.load(desMore.getDesignator().obj);
//	}
//	//
	
	public void visit(AddExpr addExpr) {
		if (addExpr.getAddop().getClass() == PlusOp.class) {
			Code.put(Code.add);
		} else {
			Code.put(Code.sub);
		}
	}
	
	public void visit(OneNegTermExpr oneNeg) {
		Code.put(Code.neg);
	}
	
	
	
	public void visit(TermMul mullOp) {
		if (mullOp.getMulop().getClass() == MultplyOp.class) {
			Code.put(Code.mul);
		} else if (mullOp.getMulop().getClass() == DivideOp.class) {
			Code.put(Code.div);
		} else {
			Code.put(Code.rem);
		}
	}
	
//	public void visit(IntegerConstValue cnst) {
//		Obj con = Tab.insert(Obj.Con, "$", Tab.intType);
//		con.setLevel(0);
//		con.setAdr(cnst.getNumberConstValue());
//		
//		Code.load(con);
////		Code.loadConst(intConst.getNumberConstValue());
//	}
//	
//	public void visit(CharConstValue cnst) {
//		Obj con = Tab.insert(Obj.Con, "$", Tab.charType);
//		con.setLevel(0);
//		con.setAdr(cnst.getCharConstValue());
//		
//		Code.load(con);
////		Code.loadConst(charConst.getCharConstValue());
//	}
//	
//	public void visit (BoolConstValue cnst) {
//		Obj con = Tab.insert(Obj.Con, "$", TabExtension.boolType);
//		con.setLevel(0);
//		con.setAdr(cnst.getBoolConstValue() ? 1 : 0);
//		
//		Code.load(con);
////		Code.loadConst(boolConst.getBoolConstValue() ? 1 : 0);
//	}
	
	public void visit(DesStatmentInc desInc) {
		if (desInc.getDesignator().obj.getKind() == Obj.Elem){
			// array elem
			Code.put(Code.dup2);
		} 
		Code.load(desInc.getDesignator().obj);
		Code.loadConst(1);
		Code.put(Code.add);
		Code.store(desInc.getDesignator().obj);
		
	}
	
	public void visit(DesStatmentDec desDec) {
		if (desDec.getDesignator().obj.getKind() == Obj.Elem){
			// array elem
			Code.put(Code.dup2);	
		}
		Code.load(desDec.getDesignator().obj);
		Code.loadConst(1);
		Code.put(Code.sub);
		Code.store(desDec.getDesignator().obj);
	}
	
	public void visit(DesignatorArray desigArray) {
		Code.load(desigArray.getDesignator().obj);
	}
	
	public void visit(NewCallWithPar newArray) {
		Code.put(Code.newarray);
		if (newArray.struct.getElemType() == Tab.charType) {
			Code.put(0);
		} else {
			Code.put(1);
		}
	}
	
	public void visit(FindAny findAny) {
		Code.loadConst(-1);
		//pocetno
		adr1 = Code.pc;
		Code.loadConst(1);
		Code.put(Code.add);
		Code.put(Code.dup2);
		Code.put(Code.dup);
		Code.load(findAny.getDummyDesignator().obj); //adresa 
		Code.put(Code.dup_x2);
		Code.put(Code.arraylength);
		adr2 = Code.pc + 1;
		Code.putFalseJump(Code.ne, 0);
		Code.put(Code.aload);
		Code.putFalseJump(Code.eq, adr1);
		Code.put(Code.pop);
		Code.put(Code.pop);
		Code.loadConst(1); //nadjeno
		Code.store(findAny.getDesignator().obj);
		adr3 = Code.pc + 1;
		Code.putJump(0);
		Code.fixup(adr2);
		Code.put(Code.pop);
		Code.put(Code.pop);
		Code.put(Code.pop);
		Code.put(Code.pop);
		Code.put(Code.pop);
		Code.loadConst(0); //nije
		Code.store(findAny.getDesignator().obj);
		Code.fixup(adr3);
		return;
	}
	
	public void visit(DesStatmentFunc funcCall) {
		String funcName = funcCall.getDesignator().obj.getName();
		if ("len".equalsIgnoreCase(funcName)) {
			Code.put(Code.arraylength);
			return;
		}
	}
	
	public void visit(FuncCall funCall) {
		String funcName = funCall.getDesignator().obj.getName();
		if ("len".equalsIgnoreCase(funcName)) {
			Code.put(Code.arraylength);
			return;
		}
	}
}

package rs.ac.bg.etf.pp1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import java_cup.runtime.Symbol;
import rs.ac.bg.etf.pp1.ast.Program;
import rs.ac.bg.etf.pp1.util.Log4JUtils;
import rs.etf.pp1.symboltable.Tab;

public class MJSemanticTest {

	static {
		DOMConfigurator.configure(Log4JUtils.instance().findLoggerConfigFile());
		Log4JUtils.instance().prepareLogFile(Logger.getRootLogger());
	}

	public static void main(String[] args) throws Exception {

		Logger log = Logger.getLogger(MJParserTest.class);

		Reader br = null;

		SemanticAnalayzer v = new SemanticAnalayzer();

		try {
			File sourceCode = new File("test/program.mj");
			log.info("Compiling source file: " + sourceCode.getAbsolutePath());

			br = new BufferedReader(new FileReader(sourceCode));
			Yylex lexer = new Yylex(br);

			MJParser p = new MJParser(lexer);
			Symbol s = p.parse(); // pocetak parsiranja

			Program prog = (Program) (s.value);
			// ispis sintaksnog stabla
			Tab.init();
			log.info(prog.toString(""));
			log.info("===================================");

			// ispis prepoznatih programskih konstrukcija
			prog.traverseBottomUp(v);
			log.info("===================================");
			Tab.dump();

		} finally {
			if (br != null)
				try {
					br.close();
				} catch (IOException e1) {
					log.error(e1.getMessage(), e1);
					log.info("Finally block called ");
				}

		}

	}

}
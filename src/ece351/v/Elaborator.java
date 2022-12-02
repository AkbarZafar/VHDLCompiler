/* *********************************************************************
 * ECE351 
 * Department of Electrical and Computer Engineering 
 * University of Waterloo 
 * Term: Fall 2021 (1219)
 *
 * The base version of this file is the intellectual property of the
 * University of Waterloo. Redistribution is prohibited.
 *
 * By pushing changes to this file I affirm that I am the author of
 * all changes. I affirm that I have complied with the course
 * collaboration policy and have not plagiarized my work. 
 *
 * I understand that redistributing this file might expose me to
 * disciplinary action under UW Policy 71. I understand that Policy 71
 * allows for retroactive modification of my final grade in a course.
 * For example, if I post my solutions to these labs on GitHub after I
 * finish ECE351, and a future student plagiarizes them, then I too
 * could be found guilty of plagiarism. Consequently, my final grade
 * in ECE351 could be retroactively lowered. This might require that I
 * repeat ECE351, which in turn might delay my graduation.
 *
 * https://uwaterloo.ca/secretariat-general-counsel/policies-procedures-guidelines/policy-71
 * 
 * ********************************************************************/

package ece351.v;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.parboiled.common.ImmutableList;

import ece351.common.ast.AndExpr;
import ece351.common.ast.AssignmentStatement;
import ece351.common.ast.ConstantExpr;
import ece351.common.ast.EqualExpr;
import ece351.common.ast.Expr;
import ece351.common.ast.NAndExpr;
import ece351.common.ast.NOrExpr;
import ece351.common.ast.NaryAndExpr;
import ece351.common.ast.NaryOrExpr;
import ece351.common.ast.NotExpr;
import ece351.common.ast.OrExpr;
import ece351.common.ast.Statement;
import ece351.common.ast.VarExpr;
import ece351.common.ast.XNOrExpr;
import ece351.common.ast.XOrExpr;
import ece351.common.visitor.PostOrderExprVisitor;
import ece351.util.CommandLine;
import ece351.v.ast.Architecture;
import ece351.v.ast.Component;
import ece351.v.ast.DesignUnit;
import ece351.v.ast.IfElseStatement;
import ece351.v.ast.Process;
import ece351.v.ast.VProgram;

/**
 * Inlines logic in components to architecture body.
 */
public final class Elaborator extends PostOrderExprVisitor {

	private final Map<String, String> current_map = new LinkedHashMap<String, String>();
	
	public static void main(String[] args) {
		System.out.println(elaborate(args));
	}
	
	public static VProgram elaborate(final String[] args) {
		return elaborate(new CommandLine(args));
	}
	
	public static VProgram elaborate(final CommandLine c) {
        final VProgram program = DeSugarer.desugar(c);
        return elaborate(program);
	}
	
	public static VProgram elaborate(final VProgram program) {
		final Elaborator e = new Elaborator();
		return e.elaborateit(program);
	}

	private VProgram elaborateit(final VProgram root) {

		// our ASTs are immutable. so we cannot mutate root.
		// we need to construct a new AST that will be the return value.
		// it will be like the input (root), but different.
		VProgram result = new VProgram();
		int compCount = 0;

		// iterate over all of the designUnits in root.
		// for each one, construct a new architecture.
		for(DesignUnit du: root.designUnits) {
			Architecture a = du.arch.varyComponents(ImmutableList.<Component>of());
			for(Component component:du.arch.components){
				compCount++;
				current_map.clear();
				for(DesignUnit rdu: result.designUnits){
					if(component.entityName.equals(rdu.entity.identifier)){
						
						int i = 0;
						for(String in: rdu.entity.input){
							current_map.put(in,component.signalList.get(i));
							i++;
						}
						i = 0;
						for(String out: rdu.entity.output){
							current_map.put(out,component.signalList.get(i+rdu.entity.input.size()));
							i++;
						}
						for(String s: rdu.arch.signals){
							String w = "comp"+compCount+"_"+s;
							a = a.appendSignal(w);
							current_map.put(s,w);
						}
						for(Statement s: rdu.arch.statements){
							if(s instanceof IfElseStatement){
								a =a.appendStatement(changeIfVars((IfElseStatement)s));
							}
							if(s instanceof Process){
								a =a.appendStatement(expandProcessComponent((Process)s));

							}
							if(s instanceof AssignmentStatement){
								a =a.appendStatement(changeStatementVars((AssignmentStatement)s));

							}
						}
					}

				}
			}
			
			result=result.append(du.setArchitecture(a));
		}
		// Architecture a = du.arch.varyComponents(ImmutableList.<Component>of());
		// this gives us a copy of the architecture with an empty list of components.
		// now we can build up this Architecture with new components.
			// In the elaborator, an architectures list of signals, and set of statements may change (grow)
						//populate dictionary/map	
						//add input signals, map to ports
						//add output signals, map to ports
						//add local signals, add to signal list of current designUnit						
						//loop through the statements in the architecture body		
							// make the appropriate variable substitutions for signal assignment statements
							// i.e., call changeStatementVars
							// make the appropriate variable substitutions for processes (sensitivity list, if/else body statements)
							// i.e., call expandProcessComponent
			 // append this new architecture to result
// TODO: longer code snippet
//throw new ece351.util.Todo351Exception();
		assert result.repOk();
		return result;
	}
	
	// you do not have to use these helper methods; we found them useful though
	private Process expandProcessComponent(final Process process) {
// TODO: longer code snippet
		ImmutableList<Statement> a = ImmutableList.of();
		for(Statement s: process.sequentialStatements){
			if(s instanceof IfElseStatement){
				a =a.append(changeIfVars((IfElseStatement)s));
			}
			if(s instanceof Process){
				a =a.append(expandProcessComponent((Process)s));

			}
			if(s instanceof AssignmentStatement){
				a =a.append(changeStatementVars((AssignmentStatement)s));

			}
		}
		return new Process(a,ImmutableList.copyOf(process.sensitivityList.stream().map(x->current_map.getOrDefault(x, x)).collect(Collectors.toList())));
	}
	
	// you do not have to use these helper methods; we found them useful though
	private  IfElseStatement changeIfVars(final IfElseStatement s) {
// TODO: longer code snippet
		return new IfElseStatement(ImmutableList.copyOf(s.elseBody.stream().map(x->changeStatementVars(x)).collect(Collectors.toList())),
		ImmutableList.copyOf(s.ifBody.stream().map(x->changeStatementVars(x)).collect(Collectors.toList()))
		,traverseExpr(s.condition));
	}

	// you do not have to use these helper methods; we found them useful though
	private AssignmentStatement changeStatementVars(final AssignmentStatement s){
// TODO: short code snippet
return new AssignmentStatement((VarExpr)visitVar(s.outputVar),traverseExpr(s.expr));

	}
	
	
	@Override
	public Expr visitVar(VarExpr e) {
		// TODO replace/substitute the variable found in the map
// TODO: short code snippet
		return new VarExpr(current_map.getOrDefault(e.identifier, e.identifier));
	}
	
	// do not rewrite these parts of the AST
	@Override public Expr visitConstant(ConstantExpr e) { return e; }
	@Override public Expr visitNot(NotExpr e) { return e; }
	@Override public Expr visitAnd(AndExpr e) { return e; }
	@Override public Expr visitOr(OrExpr e) { return e; }
	@Override public Expr visitXOr(XOrExpr e) { return e; }
	@Override public Expr visitEqual(EqualExpr e) { return e; }
	@Override public Expr visitNAnd(NAndExpr e) { return e; }
	@Override public Expr visitNOr(NOrExpr e) { return e; }
	@Override public Expr visitXNOr(XNOrExpr e) { return e; }
	@Override public Expr visitNaryAnd(NaryAndExpr e) { return e; }
	@Override public Expr visitNaryOr(NaryOrExpr e) { return e; }
}

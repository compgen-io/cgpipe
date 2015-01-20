package org.ngsutils.mvpipe.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ngsutils.mvpipe.parser.context.ExecContext;
import org.ngsutils.mvpipe.parser.op.Add;
import org.ngsutils.mvpipe.parser.op.AddAssign;
import org.ngsutils.mvpipe.parser.op.And;
import org.ngsutils.mvpipe.parser.op.Assign;
import org.ngsutils.mvpipe.parser.op.CondAssign;
import org.ngsutils.mvpipe.parser.op.Div;
import org.ngsutils.mvpipe.parser.op.Eq;
import org.ngsutils.mvpipe.parser.op.Gt;
import org.ngsutils.mvpipe.parser.op.Gte;
import org.ngsutils.mvpipe.parser.op.Lt;
import org.ngsutils.mvpipe.parser.op.Lte;
import org.ngsutils.mvpipe.parser.op.Mul;
import org.ngsutils.mvpipe.parser.op.Not;
import org.ngsutils.mvpipe.parser.op.NotEq;
import org.ngsutils.mvpipe.parser.op.Operator;
import org.ngsutils.mvpipe.parser.op.Or;
import org.ngsutils.mvpipe.parser.op.Pow;
import org.ngsutils.mvpipe.parser.op.Range;
import org.ngsutils.mvpipe.parser.op.Rem;
import org.ngsutils.mvpipe.parser.op.Sub;
import org.ngsutils.mvpipe.parser.statement.DumpVars;
import org.ngsutils.mvpipe.parser.statement.Echo;
import org.ngsutils.mvpipe.parser.statement.ElIf;
import org.ngsutils.mvpipe.parser.statement.Else;
import org.ngsutils.mvpipe.parser.statement.EndIf;
import org.ngsutils.mvpipe.parser.statement.ForLoop;
import org.ngsutils.mvpipe.parser.statement.If;
import org.ngsutils.mvpipe.parser.statement.Include;
import org.ngsutils.mvpipe.parser.statement.Statement;
import org.ngsutils.mvpipe.parser.variable.VarValue;
import org.ngsutils.mvpipe.support.StringUtils;

public class Eval {
	final public static Map<String, Operator> ops = new HashMap<String, Operator>();
	final public static List<String> opsOrder = new ArrayList<String>();
	final public static Map<String, Statement> statements = new HashMap<String, Statement>();

	private static void addOp(String op, Operator obj) {
		opsOrder.add(op);
		ops.put(op, obj);
	}
	
	static {
		statements.put("echo", new Echo());
		statements.put("dump", new DumpVars());

		statements.put("if", new If());
		statements.put("else", new Else());
		statements.put("elif", new ElIf());
		statements.put("endif", new EndIf());

		statements.put("for", new ForLoop());

		statements.put("include", new Include());

		addOp("=", new Assign());
		addOp("+=", new AddAssign());
		addOp("?=", new CondAssign());

		addOp("!", new Not());
		addOp("&&", new And());
		addOp("||", new Or());
		addOp("==", new Eq());
		addOp("!=", new NotEq());
		addOp(">=", new Gte());
		addOp("<=", new Lte());
		addOp(">", new Gt());
		addOp("<", new Lt());
		addOp("**", new Pow());
		addOp("*", new Mul());
		addOp("/", new Div());
		addOp("%", new Rem());
		addOp("+", new Add());
		addOp("-", new Sub());
		addOp("..", new Range());
		
		addOp(":", null); // placeholder
	}

	public static ExecContext evalTokenLine(ExecContext context, Tokens tokens) throws SyntaxException {
		System.err.println("#evalTokens: " + StringUtils.join(", ", tokens.getList()));
		if (statements.containsKey(tokens.get(0))) {
			System.err.println("#statement: " + tokens.get(0));
			List<String> right = new ArrayList<String>();
			right.addAll(tokens.getList().subList(1, tokens.size()));
			
			// correct for starting with a neg number...
			if (right.size() > 0 && right.get(0).equals("-")) {
				right.remove(0);
				right.set(0, "-" + right.get(0));
			}
			
			return statements.get(tokens.get(0)).eval(context, tokens.clone(right));
		} else if (context.isActive()) {
			evalTokenExpression(context, tokens);
		}
		
		return context;
	}
	
	public static VarValue evalTokenExpression(ExecContext context, Tokens tokens) throws SyntaxException {
		if (tokens.size() == 1) {
			return VarValue.parseString(tokens.get(0), context);
		}

		List<String> leftTokens = new ArrayList<String>();
		List<String> inner = new ArrayList<String>();
		List<String> rightTokens = new ArrayList<String>();

		int parenCount = 0;
		boolean done = false;
		boolean found = false;
		
		// Look for parens...
		for (String token: tokens.getList()) {
			if (done) {
				rightTokens.add(token);
			} else if (token.equals("(")) {
				if (parenCount > 0) {
					inner.add(token);
				}

				parenCount++;
				found = true;
			} else if (token.equals(")")) {
				parenCount--;

				if (parenCount == 0) {
					done=true;
				} else {
					inner.add(token);
				}
			} else if (parenCount > 0) {
				inner.add(token);
			} else {
				leftTokens.add(token);
			}
		}
		
		if (found) {
			VarValue innerVal = evalTokenExpression(context, tokens.clone(inner));
			leftTokens.add(innerVal.toString());
			leftTokens.addAll(rightTokens);
			return evalTokenExpression(context, tokens.clone(leftTokens));
		}
		
		for (String op: opsOrder) {
			for (int i=0; i<tokens.size(); i++) {
				if (tokens.get(i).equals(op)) {
					Tokens left;
					if (i == 0) {
						left = tokens.clone(new ArrayList<String>());
					} else {
						left = tokens.subList(0, i);
					}
					Tokens right;
					if (i < tokens.size() - 1) {
						right = tokens.subList(i+1, tokens.size());
					} else {
						right = tokens.clone(new ArrayList<String>());
					}

					VarValue rval = evalTokenExpression(context, right);
					VarValue ret;
					VarValue lval = null;
					if (ops.get(op).evalLeft()) {
						if (left.size() > 0) {
							lval = evalTokenExpression(context, left);
						}
						ret = ops.get(op).eval(context, lval, rval);
					} else {
						ret = ops.get(op).eval(context, left, rval);
					} 

					return ret;
				}
			}
		}
		
		return evalTokenExpression(context, tokens);
	}
}

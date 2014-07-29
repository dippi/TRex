//
// This file is part of T-Rex, a Complex Event Processing Middleware.
// See http://home.dei.polimi.it/margara
//
// Authors: Daniele Rogora
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with this program.  If not, see http://www.gnu.org/licenses/.
//

package polimi.trex.ruleparser;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import polimi.trex.common.Constraint;
import polimi.trex.common.Consts.AggregateFun;
import polimi.trex.common.Consts.CompKind;
import polimi.trex.common.Consts.ConstraintOp;
import polimi.trex.common.Consts.Op;
import polimi.trex.common.Consts.StateType;
import polimi.trex.common.Consts.ValType;
import polimi.trex.common.ComplexParameter;
import polimi.trex.common.EventPredicate;
import polimi.trex.common.EventTemplate;
import polimi.trex.common.EventTemplateAttr;
import polimi.trex.common.EventTemplateStaticAttr;
import polimi.trex.common.Negation;
import polimi.trex.common.OpTree;
import polimi.trex.common.ParametersMap;
import polimi.trex.common.RulePktValueReference;
import polimi.trex.common.StaticValueReference;
import polimi.trex.common.TAggregate;
import polimi.trex.packets.RulePkt;
import polimi.trex.ruleparser.TESLAParser.Agg_betweenContext;
import polimi.trex.ruleparser.TESLAParser.Agg_one_referenceContext;
import polimi.trex.ruleparser.TESLAParser.Aggregate_atomContext;
import polimi.trex.ruleparser.TESLAParser.Attr_constraintContext;
import polimi.trex.ruleparser.TESLAParser.Attr_declarationContext;
import polimi.trex.ruleparser.TESLAParser.Attr_definitionContext;
import polimi.trex.ruleparser.TESLAParser.Attr_parameterContext;
import polimi.trex.ruleparser.TESLAParser.ExprContext;
import polimi.trex.ruleparser.TESLAParser.Neg_betweenContext;
import polimi.trex.ruleparser.TESLAParser.Neg_one_referenceContext;
import polimi.trex.ruleparser.TESLAParser.Packet_referenceContext;
import polimi.trex.ruleparser.TESLAParser.Param_atomContext;
import polimi.trex.ruleparser.TESLAParser.Param_mappingContext;
import polimi.trex.ruleparser.TESLAParser.PredicateContext;
import polimi.trex.ruleparser.TESLAParser.StaticAttr_definitionContext;

/**
 * This class provides an empty implementation of {@link TESLAListener},
 * which can be extended to create a listener which only needs to handle a subset
 * of the available methods.
 */
public class TESLABaseListener implements TESLAListener {
	private RulePkt rule;
	private Map<String, ParametersMap> parameters_map;
	private Map<String, Integer> predicates_map;
	private Map<String, ValType> eventTemplateAttrTypes;
	private List<EventPredicate> rule_predicates;
	private List<String> rule_predicates_names;
	private Set<ComplexParameter> rule_parameters;
	private EventTemplate rule_template;
	private List<TAggregate> rule_aggregates;
	private List<Negation> rule_negations;
	private List<Integer> rule_consuming;

	public TESLABaseListener(Map<String, Integer> pPredicates) {
		this.predicates_map = pPredicates;
		this.parameters_map = new HashMap<String, ParametersMap>();
		this.rule_predicates = new ArrayList<EventPredicate>();
		this.rule_predicates_names = new ArrayList<String>();
		this.rule_parameters = new HashSet<ComplexParameter>();
		this.eventTemplateAttrTypes = new HashMap<String, ValType>();
		this.rule_aggregates = new ArrayList<TAggregate>();
		this.rule_negations = new ArrayList<Negation>();
		this.rule_consuming = new ArrayList<Integer>();
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterTrex_rule(@NotNull TESLAParser.Trex_ruleContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitTrex_rule(@NotNull TESLAParser.Trex_ruleContext ctx) { }

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterNeg_between(@NotNull TESLAParser.Neg_betweenContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitNeg_between(@NotNull TESLAParser.Neg_betweenContext ctx) { }

	private ConstraintOp getConstrOp(String source) {
		if (source.equals("=")) 		return ConstraintOp.EQ;
		else if (source.equals(">"))	return ConstraintOp.GT;
		else if (source.equals("<"))	return ConstraintOp.LT;
		else if (source.equals("!=")) 	return ConstraintOp.DF;
		else return null;
	}

	private ValType getValType(String vtype) {
		if (vtype.equals("int")) 		 return ValType.INT;
		else if (vtype.equals("float"))  return ValType.FLOAT;
		else if (vtype.equals("bool")) 	 return ValType.BOOL;
		else if (vtype.equals("string")) return ValType.STRING;
		else return null;
	}


	CompKind getCompKind(@NotNull TESLAParser.Positive_predicateContext ctx) {
		if (ctx.SEL_POLICY().getText().equals("each")) 		 return CompKind.EACH_WITHIN;
		else if (ctx.SEL_POLICY().getText().equals("last"))  return CompKind.LAST_WITHIN;
		else if (ctx.SEL_POLICY().getText().equals("first")) return CompKind.FIRST_WITHIN;
		else return null;
	}

	private Op getBinOp(String op) {
		if (op.equals("+")) return Op.ADD;
		else if (op.equals("-")) return Op.SUB;
		else if (op.equals("/")) return Op.DIV;
		else if (op.equals("*")) return Op.MUL;
		else if (op.equals("&")) return Op.AND;
		else if (op.equals("|")) return Op.OR;
		else return null;
	}

	private AggregateFun getAggregateFun(String fun) {
		if (fun.equals("AVG")) 	 return AggregateFun.AVG;
		if (fun.equals("SUM")) 	 return AggregateFun.SUM;
		if (fun.equals("COUNT")) return AggregateFun.COUNT;
		if (fun.equals("MIN")) 	 return AggregateFun.MIN;
		if (fun.equals("MAX")) 	 return AggregateFun.MAX;
		else return null;
	}

	private Constraint getConstraint(Attr_constraintContext cons) {
		ConstraintOp op = getConstrOp(cons.OPERATOR().getText());
		Constraint c = null;
		if (cons.static_reference().INTEGER() != null) {
			int val = Integer.parseInt(cons.static_reference().INTEGER().getText());
			c = new Constraint(cons.ATTR_NAME().getText(), op, val);
		}
		else if (cons.static_reference().FLOAT() != null) {
			float val = Float.parseFloat(cons.static_reference().FLOAT().getText());
			c = new Constraint(cons.ATTR_NAME().getText(), op, val);
		}
		else if (cons.static_reference().BOOL_VAL() != null) {
			Boolean val = Boolean.parseBoolean(cons.static_reference().BOOL_VAL().getText());
			c = new Constraint(cons.ATTR_NAME().getText(), op, val);
		}
		else if (cons.static_reference().STRING_VAL() != null) {
			String val = cons.static_reference().STRING_VAL().getText();
			//remove quotes from the string
			val = val.substring(1, val.length()-1);
			c = new Constraint(cons.ATTR_NAME().getText(), op, val);
		}
		return c;
	}

	private OpTree recursivelyNavigateExpression(ExprContext expr, OpTree tree, ValType valType) {
		if (expr.param_atom() != null) {
			//This is a leaf!
			Param_atomContext ctx = expr.param_atom();
			StaticValueReference value = null;
			ValType vtype= null ;
			if (ctx.static_reference() != null) {
				if (ctx.static_reference().INTEGER() != null) {
					int val = Integer.parseInt(ctx.static_reference().INTEGER().getText());
					value = new StaticValueReference(val);
					vtype = ValType.INT;
				}
				else if (ctx.static_reference().FLOAT() != null) {
					float val = Float.parseFloat(ctx.static_reference().FLOAT().getText());
					value = new StaticValueReference(val);
					vtype = ValType.FLOAT;
				}
				else if (ctx.static_reference().BOOL_VAL() != null) {
					Boolean val = Boolean.parseBoolean(ctx.static_reference().BOOL_VAL().getText());
					value = new StaticValueReference(val);
					vtype = ValType.BOOL;
				}
				else if (ctx.static_reference().STRING_VAL() != null) {
					String val = ctx.static_reference().STRING_VAL().getText();
					//remove quotes from the string
					val = val.substring(1, val.length()-1);
					value = new StaticValueReference(val);
					vtype = ValType.STRING;
				}
				return new OpTree(value, vtype);
			}
			else {
				if (ctx.PARAM_NAME()!=null) {
					//This is a reference to a parameter	
					int index = parameters_map.get(ctx.PARAM_NAME().getText()).indexInSequence;
					String attrName = parameters_map.get(ctx.PARAM_NAME().getText()).attribute_name;
					RulePktValueReference ref = new RulePktValueReference(index, StateType.STATE, attrName);
					return new OpTree(ref, valType);
				}
				else if (ctx.packet_reference()!=null) {
					//this is a reference to an attribute from another packet
					Packet_referenceContext pkt_ctx = ctx.packet_reference(); 
					//I need to figure out the idx of the event in the sequence; it's not an aggregate; attr_name
					RulePktValueReference ref = new RulePktValueReference(rule_predicates_names.indexOf(pkt_ctx.PRED_NAME().getText()), StateType.STATE, pkt_ctx.ATTR_NAME().getText());
					return new OpTree(ref, valType);
				}
			}			
		}
		else if (expr.aggregate_atom() != null) {
			//This is a reference to an aggregate
			Aggregate_atomContext agCtx = expr.aggregate_atom();
			AggregateFun fun = getAggregateFun(agCtx.AGGR_FUN().getText());
			TAggregate aggr = null;
			RulePktValueReference ref = null;
			if (agCtx.packet_reference() !=null) {
				String predName = agCtx.packet_reference().PRED_NAME().getText();
				String attrName = agCtx.packet_reference().ATTR_NAME().getText();
				aggr = getAggregate(predName, attrName, fun);
				if (aggr==null) {
					//This aggregate doesn't exist; let's create it!
					if (agCtx.agg_between() != null) {
						//between
						String pname1, pname2;
						pname1 = agCtx.agg_between().PRED_NAME(0).getText();
						pname2 = agCtx.agg_between().PRED_NAME(1).getText();
						aggr = new TAggregate(predicates_map.get(predName), (int)rule_predicates_names.indexOf(pname1), rule_predicates_names.indexOf(pname2), fun, attrName);
					}
					else if (agCtx.agg_one_reference() != null) {
						//within time
						String pname = agCtx.agg_one_reference().PRED_NAME().getText();
						int win = Integer.parseInt(agCtx.agg_one_reference().INTEGER().getText());
						aggr = new TAggregate(predicates_map.get(predName), (long)win, rule_predicates_names.indexOf(pname), fun, attrName);
					}
					rule_aggregates.add(aggr);
				}
				ref  = new RulePktValueReference(rule_aggregates.size()-1);
				return new OpTree(ref, valType);
			}
		}
		else {
			//this is an inner node
			List<ExprContext> subExpressions = expr.expr();
			for (ExprContext ex : subExpressions) {
				Op operation = null;
				if (expr.BINOP_MUL()!=null) operation = getBinOp(expr.BINOP_MUL().getText());
				else if (expr.BINOP_ADD()!=null) operation = getBinOp(expr.BINOP_ADD().getText());
				if (tree!=null) tree = new OpTree(tree, recursivelyNavigateExpression(ex, tree, valType), operation, ValType.INT);
				else tree = recursivelyNavigateExpression(ex, tree, valType);
			}
		}
		return tree;	
	}

	private OpTree buildOpTree(ExprContext expr, ValType valType) {
		OpTree tree = null; 
		tree = recursivelyNavigateExpression(expr, tree, valType);
		return tree;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterPositive_predicate(@NotNull TESLAParser.Positive_predicateContext ctx) { 
		int eventId = predicates_map.get(ctx.predicate().PRED_NAME().getText());
		int eventId2 = rule_predicates_names.indexOf(ctx.PRED_NAME().getText());

		//Constraints for this predicate
		List<Attr_constraintContext> constraints = ctx.predicate().attr_constraint();
		Set<Constraint> constraintsSet = new HashSet<Constraint>();
		for (Attr_constraintContext cons : constraints) {
			constraintsSet.add(getConstraint(cons));
		}
		EventPredicate ep = new EventPredicate(eventId, constraintsSet, eventId2, Integer.parseInt(ctx.INTEGER().getText()), getCompKind(ctx));
		addPredicate(ep, ctx.predicate().PRED_NAME().getText());

		//Parameters
		List<Attr_parameterContext> params = ctx.predicate().attr_parameter();
		for (Attr_parameterContext par : params) {
			addParameter(par, rule_predicates_names.indexOf(ctx.predicate().PRED_NAME().getText()), StateType.STATE);
		}
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitPositive_predicate(@NotNull TESLAParser.Positive_predicateContext ctx) { }

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterStatic_reference(@NotNull TESLAParser.Static_referenceContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitStatic_reference(@NotNull TESLAParser.Static_referenceContext ctx) { }

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterNegative_predicate(@NotNull TESLAParser.Negative_predicateContext ctx) { 
		int eventId = predicates_map.get(ctx.predicate().PRED_NAME().getText());
		Negation neg = null;
		if (ctx.neg_one_reference() != null) {
			//within X from Y
			Neg_one_referenceContext nctx = ctx.neg_one_reference();
			int eventId2 = rule_predicates_names.indexOf(nctx.PRED_NAME().getText());
			long win = Long.parseLong(nctx.INTEGER().getText());
			neg = new Negation(eventId, eventId2, win);
		}
		else if (ctx.neg_between() != null) {
			Neg_betweenContext nctx = ctx.neg_between();
			int eventId2 = rule_predicates_names.indexOf(nctx.PRED_NAME(0).getText());
			int eventId3 = rule_predicates_names.indexOf(nctx.PRED_NAME(1).getText());
			neg = new Negation(eventId, eventId2, eventId3);
		}

		//Constraints for this predicate
		List<Attr_constraintContext> constraints = ctx.predicate().attr_constraint();
		for (Attr_constraintContext cons : constraints) {
			neg.addConstraint(getConstraint(cons));
		}				
		rule_negations.add(neg);
		//Parameters
		List<Attr_parameterContext> params = ctx.predicate().attr_parameter();
		for (Attr_parameterContext par : params) {
			addParameter(par, rule_negations.size()-1, StateType.NEG);
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitNegative_predicate(@NotNull TESLAParser.Negative_predicateContext ctx) { }

	private void addPredicate(EventPredicate ep, String name) {
		if (!rule_predicates.contains(ep)) {
			rule_predicates.add(ep);
			rule_predicates_names.add(name);
		}
	}

	private void addParameter(Attr_parameterContext par, int index, StateType type) {
		OpTree rightTree = buildOpTree(par.expr(), getValType(par.VALTYPE().getText()));
		RulePktValueReference ref = new RulePktValueReference(index, type, par.ATTR_NAME().getText());
		OpTree leftTree = new OpTree(ref, getValType(par.VALTYPE().getText()));
		ComplexParameter compPar = new ComplexParameter(getConstrOp(par.OPERATOR().getText()), type, rightTree.getValType(), leftTree, rightTree);
		rule_parameters.add(compPar);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterTerminator(@NotNull TESLAParser.TerminatorContext ctx) { 
		int eventId = predicates_map.get(ctx.predicate().PRED_NAME().getText());
		assert(rule_predicates.size() > 0);

		//Constraints for this predicate
		List<Attr_constraintContext> constraints = ctx.predicate().attr_constraint();
		Set<Constraint> constraintsSet = new HashSet<Constraint>();
		for (Attr_constraintContext cons : constraints) {
			constraintsSet.add(getConstraint(cons));
		}
		EventPredicate ep = new EventPredicate(eventId, constraintsSet);
		addPredicate(ep, ctx.predicate().PRED_NAME().getText());

		//Parameters
		List<Attr_parameterContext> params = ctx.predicate().attr_parameter();
		for (Attr_parameterContext par : params) {
			addParameter(par, rule_predicates_names.indexOf(ctx.predicate().PRED_NAME().getText()), StateType.STATE);
		}
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitTerminator(@NotNull TESLAParser.TerminatorContext ctx) { }

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterCe_definition(@NotNull TESLAParser.Ce_definitionContext ctx) {
		rule_template = new EventTemplate(predicates_map.get(ctx.PRED_NAME().getText()));
		List<Attr_declarationContext> attrList = ctx.attr_declaration();
		for (Attr_declarationContext attr : attrList) {
			eventTemplateAttrTypes.put(attr.ATTR_NAME().getText(), getValType(attr.VALTYPE().getText()));
		}
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitCe_definition(@NotNull TESLAParser.Ce_definitionContext ctx) { }

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterPredicate(@NotNull TESLAParser.PredicateContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitPredicate(@NotNull TESLAParser.PredicateContext ctx) { }

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterExpr(@NotNull TESLAParser.ExprContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitExpr(@NotNull TESLAParser.ExprContext ctx) { }

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterAttr_parameter(@NotNull TESLAParser.Attr_parameterContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitAttr_parameter(@NotNull TESLAParser.Attr_parameterContext ctx) { }

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterDefinitions(@NotNull TESLAParser.DefinitionsContext ctx) {
		//STATIC ATTRIBUTES
		List<StaticAttr_definitionContext> statics = ctx.staticAttr_definition();
		for (StaticAttr_definitionContext sdef : statics) {
			EventTemplateStaticAttr sattr = null;
			ValType vtype = eventTemplateAttrTypes.get(sdef.ATTR_NAME().getText());
			if (vtype == ValType.INT) {
				int val = Integer.parseInt(sdef.static_reference().INTEGER().getText());
				sattr = new EventTemplateStaticAttr(sdef.ATTR_NAME().getText(), val);
			}
			else if (vtype == ValType.FLOAT) {
				float val = Float.parseFloat(sdef.static_reference().FLOAT().getText());
				sattr = new EventTemplateStaticAttr(sdef.ATTR_NAME().getText(), val);
			}
			else if (vtype == ValType.BOOL) {
				Boolean val = Boolean.parseBoolean(sdef.static_reference().BOOL_VAL().getText());
				sattr = new EventTemplateStaticAttr(sdef.ATTR_NAME().getText(), val);
			}
			else if (vtype == ValType.STRING) {
				String val = sdef.static_reference().STRING_VAL().getText();
				//remove quotes from the string
				val = val.substring(1, val.length()-1);
				sattr = new EventTemplateStaticAttr(sdef.ATTR_NAME().getText(), val);
			}
			rule_template.addStaticAttribute(sattr);
		}

		//PARAMS/AGGREGATES
		List<Attr_definitionContext> attributes = ctx.attr_definition();
		for (Attr_definitionContext attr_ctx : attributes) {
			OpTree tree = buildOpTree(attr_ctx.expr(), eventTemplateAttrTypes.get(attr_ctx.ATTR_NAME().getText()));
			EventTemplateAttr attr = new EventTemplateAttr(attr_ctx.ATTR_NAME().getText(), tree);
			rule_template.addAttribute(attr);
		}
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitDefinitions(@NotNull TESLAParser.DefinitionsContext ctx) { }

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterAttr_constraint(@NotNull TESLAParser.Attr_constraintContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitAttr_constraint(@NotNull TESLAParser.Attr_constraintContext ctx) { }

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterConsuming(@NotNull TESLAParser.ConsumingContext ctx) { 
		List<TerminalNode> preds = ctx.PRED_NAME();
		for (TerminalNode p : preds) {
			rule_consuming.add(new Integer(rule_predicates_names.indexOf(p.getText())));
		}
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitConsuming(@NotNull TESLAParser.ConsumingContext ctx) { }

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterPacket_reference(@NotNull TESLAParser.Packet_referenceContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitPacket_reference(@NotNull TESLAParser.Packet_referenceContext ctx) { }

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterAttr_declaration(@NotNull TESLAParser.Attr_declarationContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitAttr_declaration(@NotNull TESLAParser.Attr_declarationContext ctx) { }

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterNeg_one_reference(@NotNull TESLAParser.Neg_one_referenceContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitNeg_one_reference(@NotNull TESLAParser.Neg_one_referenceContext ctx) { }

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterParam_atom(@NotNull TESLAParser.Param_atomContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitParam_atom(@NotNull TESLAParser.Param_atomContext ctx) { }

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterAttr_definition(@NotNull TESLAParser.Attr_definitionContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitAttr_definition(@NotNull TESLAParser.Attr_definitionContext ctx) { }

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterPattern(@NotNull TESLAParser.PatternContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitPattern(@NotNull TESLAParser.PatternContext ctx) { }

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterEnding_rule(@NotNull TESLAParser.Ending_ruleContext ctx) { 
		//Ok, parsing is done; let's pack up the rule
		rule = new RulePkt(rule_template);
		for (EventPredicate pred : rule_predicates) {
			rule.addPredicate(pred);
		}
		for (ComplexParameter par : rule_parameters) {
			rule.addParameter(par);
		}
		for (TAggregate aggr : rule_aggregates) {
			rule.addAggregate(aggr);
		}
		for (Negation neg : rule_negations) {
			rule.addNegation(neg);
		}
		for (Integer idx : rule_consuming) {
			rule.addConsuming(idx.intValue());
		}
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitEnding_rule(@NotNull TESLAParser.Ending_ruleContext ctx) { }

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterPattern_predicate(@NotNull TESLAParser.Pattern_predicateContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitPattern_predicate(@NotNull TESLAParser.Pattern_predicateContext ctx) { }

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterStaticAttr_definition(@NotNull TESLAParser.StaticAttr_definitionContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitStaticAttr_definition(@NotNull TESLAParser.StaticAttr_definitionContext ctx) { }

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterEveryRule(@NotNull ParserRuleContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void exitEveryRule(@NotNull ParserRuleContext ctx) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void visitTerminal(@NotNull TerminalNode node) { }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void visitErrorNode(@NotNull ErrorNode node) { }

	public RulePkt getRule() {
		return rule;
	}
	@Override
	public void enterParam_mapping(Param_mappingContext ctx) {
		PredicateContext predCtx = (PredicateContext) ctx.parent;
		ParametersMap pmap = new ParametersMap();
		pmap.indexInSequence = rule_predicates_names.indexOf(predCtx.PRED_NAME().getText());
		pmap.eventId = predicates_map.get(predCtx.PRED_NAME().getText());
		pmap.attribute_name = ctx.ATTR_NAME().getText();
		if (!parameters_map.containsKey(ctx.PARAM_NAME().getText())) parameters_map.put(ctx.PARAM_NAME().getText(), pmap);
	}
	@Override
	public void exitParam_mapping(Param_mappingContext ctx) {

	}
	@Override
	public void enterAggregate_atom(Aggregate_atomContext ctx) {
		AggregateFun fun = getAggregateFun(ctx.AGGR_FUN().getText());
		TAggregate aggr = getAggregate(ctx.packet_reference().PRED_NAME().getText(), ctx.packet_reference().ATTR_NAME().getText(), fun);
		if (aggr!=null) {
			//already created, nothing to do
		}
		else {
			//i have to create it
			System.out.println("This aggr doesn't exist");
		}
		//Constraints for this predicate
		List<Attr_constraintContext> constraints = ctx.attr_constraint();
		for (Attr_constraintContext cons : constraints) {
			aggr.addConstraint(getConstraint(cons));
		}		
		List<Attr_parameterContext> params = ctx.attr_parameter();
		for (Attr_parameterContext par : params) {
			addParameter(par, rule_aggregates.indexOf(aggr), StateType.AGG);
		}
	}
	@Override
	public void exitAggregate_atom(Aggregate_atomContext ctx) {

	}
	@Override
	public void enterAgg_between(Agg_betweenContext ctx) {

	}
	@Override
	public void exitAgg_between(Agg_betweenContext ctx) {

	}
	@Override
	public void enterAgg_one_reference(Agg_one_referenceContext ctx) {

	}
	@Override
	public void exitAgg_one_reference(Agg_one_referenceContext ctx) {

	}

	private TAggregate getAggregate(String predName, String attrName, AggregateFun aggrFun) {
		for (TAggregate agg : rule_aggregates) {
			if (agg.getName().equals(attrName) && agg.getEventType()== predicates_map.get(predName) && agg.getFun().equals(aggrFun)) {
				return agg;
			}
		}
		return null;
	}

}
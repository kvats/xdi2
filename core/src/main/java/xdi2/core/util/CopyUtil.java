package xdi2.core.util;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xdi2.core.ContextNode;
import xdi2.core.Graph;
import xdi2.core.Literal;
import xdi2.core.Relation;
import xdi2.core.Statement;
import xdi2.core.features.nodetypes.XdiInnerRoot;
import xdi2.core.features.nodetypes.XdiCommonRoot;
import xdi2.core.features.nodetypes.XdiRoot;
import xdi2.core.syntax.XDIAddress;
import xdi2.core.syntax.XDIArc;

/**
 * Various utility methods for copying statements between graphs.
 * 
 * @author markus
 */
public final class CopyUtil {

	private static final Logger log = LoggerFactory.getLogger(CopyUtil.class);

	private static final CopyStrategy allCopyStrategy = new AllCopyStrategy();

	private CopyUtil() { }

	/**
	 * Copies a whole graph into a target graph.
	 * @param graph A graph.
	 * @param targetGraph The target graph.
	 * @param copyStrategy The strategy to determine what to copy.
	 */
	public static void copyGraph(Graph graph, Graph targetGraph, CopyStrategy copyStrategy) {

		if (graph == null) throw new NullPointerException();
		if (targetGraph == null) throw new NullPointerException();
		if (copyStrategy == null) copyStrategy = allCopyStrategy;

		copyContextNodeContents(graph.getRootContextNode(true), targetGraph.getRootContextNode(false), copyStrategy);
	}

	/*
	 * Methods for copying context nodes
	 */

	/**
	 * Copies a context node into a target graph.
	 * @param contextNode A context node from any graph.
	 * @param targetGraph The target graph.
	 * @param copyStrategy The strategy to determine what to copy.
	 * @return The copied context node in the target graph.
	 */
	public static ContextNode copyContextNode(ContextNode contextNode, Graph targetGraph, CopyStrategy copyStrategy) {

		if (contextNode == null) throw new NullPointerException();
		if (targetGraph == null) throw new NullPointerException();
		if (copyStrategy == null) copyStrategy = allCopyStrategy;

		if ((contextNode = copyStrategy.replaceContextNode(contextNode)) == null) return null;

		XDIAddress contextNodeAddress = contextNode.getXDIAddress();

		ContextNode targetContextNode;

		if (contextNode.isRootContextNode()) {

			targetContextNode = targetGraph.getRootContextNode(false);
		} else {

			targetContextNode = targetGraph.setDeepContextNode(contextNodeAddress);
		}

		copyContextNodeContents(contextNode, targetContextNode, copyStrategy);

		return targetContextNode;
	}

	/**
	 * Copies a context node into a target context node.
	 * @param contextNode A context node from any context node.
	 * @param targetContextNode The target context node.
	 * @param copyStrategy The strategy to determine what to copy.
	 * @return The copied context node in the target context node.
	 */
	public static ContextNode copyContextNode(ContextNode contextNode, ContextNode targetContextNode, CopyStrategy copyStrategy) {

		if (contextNode == null) throw new NullPointerException();
		if (targetContextNode == null) throw new NullPointerException();
		if (copyStrategy == null) copyStrategy = allCopyStrategy;

		if ((contextNode = copyStrategy.replaceContextNode(contextNode)) == null) return null;

		XDIArc contextNodeArc = contextNode.getXDIArc();

		ContextNode targetInnerContextNode = targetContextNode.setContextNode(contextNodeArc);

		copyContextNodeContents(contextNode, targetInnerContextNode, copyStrategy);

		return targetContextNode;
	}

	/**
	 * Copies all context nodes of a context node into a target context node.
	 * @param contextNode A context node from any graph.
	 * @param targetContextNode The target context node.
	 * @param copyStrategy The strategy to determine what to copy.
	 * @return The copied context nodes in the target graph.
	 */
	public static Iterator<ContextNode> copyContextNodes(ContextNode contextNode, ContextNode targetContextNode, CopyStrategy copyStrategy) {

		if (contextNode == null) throw new NullPointerException();
		if (targetContextNode == null) throw new NullPointerException();
		if (copyStrategy == null) copyStrategy = allCopyStrategy;

		for (Iterator<ContextNode> innerContextNodes = contextNode.getContextNodes(); innerContextNodes.hasNext(); ) {

			ContextNode innerContextNode = innerContextNodes.next();

			copyContextNode(innerContextNode, targetContextNode, copyStrategy);
		}

		return targetContextNode.getContextNodes();
	}

	/*
	 * Methods for copying relations
	 */

	/**
	 * Copies a relation into another graph.
	 * @param relation A relation from any graph.
	 * @param targetGraph The target graph.
	 * @param copyStrategy The strategy to determine what to copy.
	 * @return The copied relation in the target graph.
	 */
	public static Relation copyRelation(Relation relation, Graph targetGraph, CopyStrategy copyStrategy) {

		if (relation == null) throw new NullPointerException();
		if (targetGraph == null) throw new NullPointerException();
		if (copyStrategy == null) copyStrategy = allCopyStrategy;

		XDIAddress relationcontextNodeAddress = relation.getContextNode().getXDIAddress();
		ContextNode targetContextNode = targetGraph.setDeepContextNode(relationcontextNodeAddress);

		return copyRelation(relation, targetContextNode, copyStrategy);
	}

	/**
	 * Copies a relation into another context node.
	 * @param relation A relation from any context node.
	 * @param targetContextNode The target context node.
	 * @param copyStrategy The strategy to determine what to copy.
	 * @return The copied relation in the target context node.
	 */
	public static Relation copyRelation(Relation relation, ContextNode targetContextNode, CopyStrategy copyStrategy) {

		if (relation == null) throw new NullPointerException();
		if (targetContextNode == null) throw new NullPointerException();
		if (copyStrategy == null) copyStrategy = allCopyStrategy;

		if ((relation = copyStrategy.replaceRelation(relation)) == null) return null;

		XDIAddress relationcontextNodeAddress = relation.getContextNode().getXDIAddress();
		XDIAddress relationAddress = relation.getXDIAddress();
		XDIAddress relationtargetContextNodeAddress = relation.getTargetContextNodeXDIAddress();

		XDIAddress targetContextNodeAddress = targetContextNode.getXDIAddress();

		XdiRoot relationContextNodeXdiRoot = XdiCommonRoot.findCommonRoot(relation.getContextNode().getGraph()).getRoot(relationcontextNodeAddress, false);
		XdiRoot targetContextNodeXdiRoot = XdiCommonRoot.findCommonRoot(targetContextNode.getGraph()).getRoot(targetContextNodeAddress, false);

		XDIAddress relativeRelationcontextNodeAddress = relationContextNodeXdiRoot.absoluteToRelativeXDIAddress(relationcontextNodeAddress);
		XDIAddress relativeRelationtargetContextNodeAddress = relationContextNodeXdiRoot.absoluteToRelativeXDIAddress(relationtargetContextNodeAddress);
		XDIAddress relativetargetContextNodeAddress = targetContextNodeXdiRoot.absoluteToRelativeXDIAddress(targetContextNodeAddress);

		Relation targetRelation;

		// check if this relation establishes an inner root

		if (relativeRelationtargetContextNodeAddress != null &&
				relativeRelationtargetContextNodeAddress.getNumXDIArcs() == 1 &&
				XdiInnerRoot.isInnerRootXDIArc(relativeRelationtargetContextNodeAddress.getFirstXDIArc()) &&
				XdiInnerRoot.getSubjectOfInnerRootXDIArc(relativeRelationtargetContextNodeAddress.getFirstXDIArc()).equals(relativeRelationcontextNodeAddress) &&
				XdiInnerRoot.getPredicateOfInnerRootXDIArc(relativeRelationtargetContextNodeAddress.getFirstXDIArc()).equals(relationAddress)) {

			// if the target context node is not the same, we need to adjust the inner root

			if (! targetContextNodeAddress.equals(relationcontextNodeAddress)) {

				relativeRelationtargetContextNodeAddress = XDIAddress.fromComponent(XdiInnerRoot.createInnerRootXDIArc(relativetargetContextNodeAddress, relationAddress));
				relationtargetContextNodeAddress = targetContextNodeXdiRoot.relativeToAbsoluteXDIAddress(relativeRelationtargetContextNodeAddress);
			}

			targetRelation = targetContextNode.setRelation(relationAddress, relationtargetContextNodeAddress);

			// also need to copy the contents of the inner root

			copyContextNodeContents(relation.follow(), targetRelation.follow(), copyStrategy);
		} else {

			targetRelation = targetContextNode.setRelation(relationAddress, relationtargetContextNodeAddress);
		}

		return targetRelation;
	}

	/**
	 * Copies all relations of a context node into a target context node.
	 * @param contextNode A context node from any graph.
	 * @param targetContextNode The target context node.
	 * @param copyStrategy The strategy to determine what to copy.
	 * @return The copied relations in the target graph.
	 */
	public static Iterator<Relation> copyRelations(ContextNode contextNode, ContextNode targetContextNode, CopyStrategy copyStrategy) {

		if (contextNode == null) throw new NullPointerException();
		if (targetContextNode == null) throw new NullPointerException();
		if (copyStrategy == null) copyStrategy = allCopyStrategy;

		for (Iterator<Relation> relations = contextNode.getRelations(); relations.hasNext(); ) {

			Relation relation = relations.next();

			copyRelation(relation, targetContextNode, copyStrategy);
		}

		return targetContextNode.getRelations();
	}

	/*
	 * Methods for copying literals
	 */

	/**
	 * Copies a literal into another graph.
	 * @param literal A literal from any graph.
	 * @param targetGraph The target graph.
	 * @param copyStrategy The strategy to determine what to copy.
	 * @return The copied literal in the target graph.
	 */
	public static Literal copyLiteral(Literal literal, Graph targetGraph, CopyStrategy copyStrategy) {

		if (literal == null) throw new NullPointerException();
		if (targetGraph == null) throw new NullPointerException();
		if (copyStrategy == null) copyStrategy = allCopyStrategy;

		XDIAddress literalcontextNodeAddress = literal.getContextNode().getXDIAddress();
		ContextNode targetContextNode = targetGraph.setDeepContextNode(literalcontextNodeAddress);

		return copyLiteral(literal, targetContextNode, copyStrategy);
	}

	/**
	 * Copies a literal into another context node.
	 * @param literal A literal from any context node.
	 * @param targetContextNode The target context node.
	 * @param copyStrategy The strategy to determine what to copy.
	 * @return The copied literal in the target context node.
	 */
	public static Literal copyLiteral(Literal literal, ContextNode targetContextNode, CopyStrategy copyStrategy) {

		if (literal == null) throw new NullPointerException();
		if (targetContextNode == null) throw new NullPointerException();
		if (copyStrategy == null) copyStrategy = allCopyStrategy;

		if ((literal = copyStrategy.replaceLiteral(literal)) == null) return null;

		Object literalData = literal.getLiteralData();

		Literal targetLiteral = targetContextNode.setLiteral(literalData);

		return targetLiteral;
	}

	/**
	 * Copies a literal of a context node into a target context node.
	 * @param contextNode A context node from any graph.
	 * @param targetContextNode The target context node.
	 * @param copyStrategy The strategy to determine what to copy.
	 * @return The copied literal in the target graph.
	 */
	public static Literal copyLiteral(ContextNode contextNode, ContextNode targetContextNode, CopyStrategy copyStrategy) {

		if (contextNode == null) throw new NullPointerException();
		if (targetContextNode == null) throw new NullPointerException();
		if (copyStrategy == null) copyStrategy = allCopyStrategy;

		Literal literal = contextNode.getLiteral();
		if (literal == null) return null;

		return copyLiteral(literal, targetContextNode, copyStrategy);
	}

	/*
	 * Other copy methods
	 */

	/**
	 * Copies the contents of a context node (context nodes, relations, and the literal) into a target context node.
	 * @param contextNode A context node from any graph.
	 * @param targetContextNode The target context node.
	 * @param copyStrategy The strategy to determine what to copy.
	 */
	public static void copyContextNodeContents(ContextNode contextNode, ContextNode targetContextNode, CopyStrategy copyStrategy) {

		if (contextNode == null) throw new NullPointerException();
		if (targetContextNode == null) throw new NullPointerException();
		if (copyStrategy == null) copyStrategy = allCopyStrategy;

		copyContextNodes(contextNode, targetContextNode, copyStrategy);
		copyRelations(contextNode, targetContextNode, copyStrategy);
		copyLiteral(contextNode, targetContextNode, copyStrategy);
	}

	/**
	 * Copies a statement into another graph.
	 * @param statement A statement from any graph.
	 * @param targetGraph The target graph.
	 * @param copyStrategy The strategy to determine what to copy.
	 * @return The copied statement in the target graph.
	 */
	public static Statement copyStatement(Statement statement, Graph targetGraph, CopyStrategy copyStrategy) {

		if (statement == null) throw new NullPointerException();
		if (targetGraph == null) throw new NullPointerException();
		if (copyStrategy == null) copyStrategy = allCopyStrategy;

		targetGraph.setStatement(statement.getStatement());

		return null;
	}

	/*
	 * Helper classes
	 */

	/**
	 * An interface that can determine what to copy and what not.
	 */
	public static abstract class CopyStrategy {

		/**
		 * Strategies can replace a context node that is being copied.
		 * @param contextNode The original context node.
		 * @return The replacement (or null if it should not be copied).
		 */
		public ContextNode replaceContextNode(ContextNode contextNode) {

			if (log.isTraceEnabled()) log.trace("Copying context node " + contextNode);

			return contextNode;
		}

		/**
		 * Strategies can replace a relation that is being copied.
		 * @param relation The original relation.
		 * @return The replacement (or null if it should not be copied).
		 */
		public Relation replaceRelation(Relation relation) {

			if (log.isTraceEnabled()) log.trace("Copying relation " + relation);

			return relation;
		}

		/**
		 * Strategies can replace a literal that is being copied.
		 * @param literal The original literal.
		 * @return The replacement (or null if it should not be copied).
		 */
		public Literal replaceLiteral(Literal literal) {

			if (log.isTraceEnabled()) log.trace("Copying literal " + literal);

			return literal;
		}
	}

	/**
	 * The default strategy that copies everything.
	 */
	public static class AllCopyStrategy extends CopyStrategy {

	}

	/**
	 * A strategy that replaces certain XRI parts.
	 */
	public static class ReplaceXriCopyStrategy extends CopyStrategy {

		Map<XDIArc, XDIAddress> replacements;

		public ReplaceXriCopyStrategy(Map<XDIArc, XDIAddress> replacements) {

			this.replacements = replacements;
		}

		public ReplaceXriCopyStrategy(XDIArc oldXri, XDIAddress newXri) {

			this(Collections.singletonMap(oldXri, newXri));
		}

		@Override
		public ContextNode replaceContextNode(ContextNode contextNode) {

			XDIAddress contextNodeXri = contextNode.getXDIAddress();
			XDIArc contextNodeArcXri = contextNode.getXDIArc();

			XDIAddress replacedContextNodeXri = XDIAddress.fromComponent(contextNodeArcXri);

			for (Entry<XDIArc, XDIAddress> replacement : this.replacements.entrySet()) {

				replacedContextNodeXri = XDIAddressUtil.replaceXDIAddress(
						replacedContextNodeXri, 
						replacement.getKey(), 
						replacement.getValue());
			}

			replacedContextNodeXri = XDIAddressUtil.concatXDIAddresses(XDIAddressUtil.parentXDIAddress(contextNodeXri, -1), replacedContextNodeXri);

			if (log.isDebugEnabled()) log.debug("Replaced " + contextNodeXri + " with " + replacedContextNodeXri);

			if (contextNodeXri.equals(replacedContextNodeXri)) return super.replaceContextNode(contextNode);

			ContextNode replacedContextNode = GraphUtil.contextNodeFromComponents(replacedContextNodeXri);
			CopyUtil.copyContextNodeContents(contextNode, replacedContextNode, null);

			int additionalArcs = replacedContextNodeXri.getNumXDIArcs() - contextNodeXri.getNumXDIArcs();

			replacedContextNode = replacedContextNode.getContextNode(additionalArcs);

			return replacedContextNode;
		}

		@Override
		public Relation replaceRelation(Relation relation) {

			XDIAddress contextNodeXri = relation.getContextNode().getXDIAddress();
			XDIAddress arcXri = relation.getXDIAddress();
			XDIAddress targetContextNodeXri = relation.getTargetContextNodeXDIAddress();

			XDIAddress replacedTargetContextNodeXri = targetContextNodeXri;

			for (Entry<XDIArc, XDIAddress> replacement : this.replacements.entrySet()) {

				targetContextNodeXri = XDIAddressUtil.replaceXDIAddress(
						targetContextNodeXri, 
						replacement.getKey(), 
						replacement.getValue());
			}

			if (log.isDebugEnabled()) log.debug("Replaced " + targetContextNodeXri + " with " + replacedTargetContextNodeXri);

			if (targetContextNodeXri.equals(replacedTargetContextNodeXri)) return super.replaceRelation(relation);

			Relation replacedRelation = GraphUtil.relationFromComponents(contextNodeXri, arcXri, replacedTargetContextNodeXri);

			return replacedRelation;
		}
	};
}

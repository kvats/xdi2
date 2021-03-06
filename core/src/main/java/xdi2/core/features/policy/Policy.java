package xdi2.core.features.policy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xdi2.core.ContextNode;
import xdi2.core.Relation;
import xdi2.core.constants.XDIPolicyConstants;
import xdi2.core.features.nodetypes.XdiEntity;
import xdi2.core.features.nodetypes.XdiEntityCollection;
import xdi2.core.features.nodetypes.XdiEntityInstanceOrdered;
import xdi2.core.features.nodetypes.XdiEntityInstanceUnordered;
import xdi2.core.features.nodetypes.XdiEntitySingleton;
import xdi2.core.features.policy.evaluation.PolicyEvaluationContext;
import xdi2.core.features.policy.operator.Operator;
import xdi2.core.features.policy.operator.Operator.MappingRelationOperatorIterator;
import xdi2.core.syntax.XDIArc;
import xdi2.core.util.iterators.CompositeIterator;
import xdi2.core.util.iterators.MappingIterator;
import xdi2.core.util.iterators.NotNullIterator;
import xdi2.core.util.iterators.ReadOnlyIterator;
import xdi2.core.util.iterators.SingleItemIterator;

/**
 * An XDI policy, represented as a context node.
 * 
 * @author markus
 */
public abstract class Policy implements Serializable, Comparable<Policy> {

	private static final long serialVersionUID = 1604380462449272149L;

	private static final Logger log = LoggerFactory.getLogger(Policy.class);

	private XdiEntity xdiEntity;

	protected Policy(XdiEntity xdiEntity) {

		if (xdiEntity == null) throw new NullPointerException();

		this.xdiEntity = xdiEntity;
	}

	/**
	 * Checks if an XDI entity is a valid XDI policy.
	 * @param xdiEntity The XDI entity to check.
	 * @return True if the XDI entity is a valid XDI policy.
	 */
	public static boolean isValid(XdiEntity xdiEntity) {

		return
				PolicyRoot.isValid(xdiEntity) ||
				PolicyAnd.isValid(xdiEntity) ||
				PolicyOr.isValid(xdiEntity) ||
				PolicyNot.isValid(xdiEntity);
	}

	/**
	 * Factory method that creates an XDI policy bound to a given XDI entity.
	 * @param xdiEntity The XDI entity that is an XDI policy.
	 * @return The XDI entity.
	 */
	public static Policy fromXdiEntity(XdiEntity xdiEntity) {

		Policy policy;

		if ((policy = PolicyRoot.fromXdiEntity(xdiEntity)) != null) return policy;
		if ((policy = PolicyAnd.fromXdiEntity(xdiEntity)) != null) return policy;
		if ((policy = PolicyOr.fromXdiEntity(xdiEntity)) != null) return policy;
		if ((policy = PolicyNot.fromXdiEntity(xdiEntity)) != null) return policy;

		return null;
	}

	/**
	 * Factory method that casts a Policy to the right subclass, e.g. to a PolicyAnd.
	 * @param policy The Policy to be cast.
	 * @return The casted Policy.
	 */
	public static Policy castCondition(Policy policy) {

		if (policy == null) return null;

		return fromXdiEntity(policy.getXdiEntity());
	}

	/*
	 * Instance methods
	 */

	/**
	 * Returns the underlying XDI entity to which this XDI policy is bound.
	 * @return An XDI entity that represents the XDI policy.
	 */
	public XdiEntity getXdiEntity() {

		return this.xdiEntity;
	}

	/**
	 * Returns the underlying context node to which this XDI policy is bound.
	 * @return A context node that represents the XDI policy.
	 */
	public ContextNode getContextNode() {

		return this.getXdiEntity().getContextNode();
	}

	/**
	 * Returns the policy arc of the XDI policy (e.g. $and, $or).
	 * @return The policy arc of the XDI policy.
	 */
	public XDIArc getPolicyXDIArc() {

		if (this.getXdiEntity() instanceof XdiEntitySingleton)
			return ((XdiEntitySingleton) this.getXdiEntity()).getBaseXDIArc();
		else if (this.getXdiEntity() instanceof XdiEntityInstanceUnordered)
			return ((XdiEntityInstanceUnordered) this.getXdiEntity()).getXdiCollection().getBaseXDIArc();
		else if (this.getXdiEntity() instanceof XdiEntityInstanceOrdered)
			return ((XdiEntityInstanceOrdered) this.getXdiEntity()).getXdiCollection().getBaseXDIArc();

		return null;
	}

	/**
	 * Creates an XDI $and policy.
	 */
	public PolicyAnd createAndPolicy(boolean singleton) {

		XdiEntity policyAndXdiEntity; 

		if (singleton)
			policyAndXdiEntity = this.getXdiEntity().getXdiEntitySingleton(XdiEntitySingleton.createXDIArc(XDIPolicyConstants.XDI_ARC_AND), true);
		else
			policyAndXdiEntity = this.getXdiEntity().getXdiEntityCollection(XdiEntityCollection.createXDIArc(XDIPolicyConstants.XDI_ARC_AND), true).setXdiInstanceUnordered(true, false);

		return PolicyAnd.fromXdiEntity(policyAndXdiEntity);
	}

	/**
	 * Creates an XDI $or policy.
	 */
	public PolicyOr createOrPolicy(boolean singleton) {

		XdiEntity policyOrXdiEntity; 

		if (singleton)
			policyOrXdiEntity = this.getXdiEntity().getXdiEntitySingleton(XdiEntitySingleton.createXDIArc(XDIPolicyConstants.XDI_ARC_OR), true);
		else
			policyOrXdiEntity = this.getXdiEntity().getXdiEntityCollection(XdiEntityCollection.createXDIArc(XDIPolicyConstants.XDI_ARC_OR), true).setXdiInstanceUnordered(true, false);

		return PolicyOr.fromXdiEntity(policyOrXdiEntity);
	}

	/**
	 * Creates an XDI $not policy.
	 */
	public PolicyNot createNotPolicy(boolean singleton) {

		XdiEntity policyNotXdiEntity; 

		if (singleton)
			policyNotXdiEntity = this.getXdiEntity().getXdiEntitySingleton(XdiEntitySingleton.createXDIArc(XDIPolicyConstants.XDI_ARC_NOT), true);
		else
			policyNotXdiEntity = this.getXdiEntity().getXdiEntityCollection(XdiEntityCollection.createXDIArc(XDIPolicyConstants.XDI_ARC_NOT), true).setXdiInstanceUnordered(true, false);

		return PolicyNot.fromXdiEntity(policyNotXdiEntity);
	}

	/**
	 * Returns the XDI policies underneath this XDI policy.
	 */
	public ReadOnlyIterator<Policy> getPolicies() {

		List<Iterator<? extends Policy>> iterators = new ArrayList<Iterator<? extends Policy>> ();

		// add policies that are XDI entity singletons

		XdiEntitySingleton policyAndEntitySingleton = this.getXdiEntity().getXdiEntitySingleton(XdiEntitySingleton.createXDIArc(XDIPolicyConstants.XDI_ARC_AND), false);
		XdiEntitySingleton policyOrEntitySingleton = this.getXdiEntity().getXdiEntitySingleton(XdiEntitySingleton.createXDIArc(XDIPolicyConstants.XDI_ARC_OR), false);
		XdiEntitySingleton policyNotEntitySingleton = this.getXdiEntity().getXdiEntitySingleton(XdiEntitySingleton.createXDIArc(XDIPolicyConstants.XDI_ARC_NOT), false);

		if (policyAndEntitySingleton != null) iterators.add(new SingleItemIterator<Policy> (PolicyAnd.fromXdiEntity(policyAndEntitySingleton)));
		if (policyOrEntitySingleton != null) iterators.add(new SingleItemIterator<Policy> (PolicyOr.fromXdiEntity(policyOrEntitySingleton)));
		if (policyNotEntitySingleton != null) iterators.add(new SingleItemIterator<Policy> (PolicyNot.fromXdiEntity(policyNotEntitySingleton)));

		// add policies that are XDI entity instances

		XdiEntityCollection policyAndEntityCollection = this.getXdiEntity().getXdiEntityCollection(XdiEntityCollection.createXDIArc(XDIPolicyConstants.XDI_ARC_AND), false);
		XdiEntityCollection policyOrEntityCollection = this.getXdiEntity().getXdiEntityCollection(XdiEntityCollection.createXDIArc(XDIPolicyConstants.XDI_ARC_OR), false);
		XdiEntityCollection policyNotEntityCollection = this.getXdiEntity().getXdiEntityCollection(XdiEntityCollection.createXDIArc(XDIPolicyConstants.XDI_ARC_NOT), false);

		if (policyAndEntityCollection != null) iterators.add(new MappingXdiEntityPolicyAndIterator(policyAndEntityCollection.getXdiInstancesDeref()));
		if (policyOrEntityCollection != null) iterators.add(new MappingXdiEntityPolicyOrIterator(policyOrEntityCollection.getXdiInstancesDeref()));
		if (policyNotEntityCollection != null) iterators.add(new MappingXdiEntityPolicyNotIterator(policyNotEntityCollection.getXdiInstancesDeref()));

		return new CompositeIterator<Policy> (iterators.iterator());
	}

	/**
	 * Returns the XDI operators underneath this XDI policy.
	 */
	public ReadOnlyIterator<Operator> getOperators() {

		// get all relations that are valid XDI operators

		Iterator<Relation> relations = this.getContextNode().getRelations();

		return new MappingRelationOperatorIterator(relations);
	}

	/**
	 * Checks if the XDI policy evaluates to true or false.
	 * @param policyEvaluationContext An object that can locate context nodes.
	 * @return True or false.
	 */
	public final boolean evaluate(PolicyEvaluationContext policyEvaluationContext) {

		if (log.isDebugEnabled()) log.debug("Evaluating " + this.getClass().getSimpleName() + ": " + this.getContextNode());
		boolean result = this.evaluateInternal(policyEvaluationContext);
		if (log.isDebugEnabled()) log.debug("Evaluated " + this.getClass().getSimpleName() + ": " + this.getContextNode() + ": " + result);

		return result;
	}

	protected abstract boolean evaluateInternal(PolicyEvaluationContext policyEvaluationContext);

	/*
	 * Object methods
	 */

	@Override
	public String toString() {

		return this.getContextNode().toString();
	}

	@Override
	public boolean equals(Object object) {

		if (object == null || ! (object instanceof Policy)) return false;
		if (object == this) return true;

		Policy other = (Policy) object;

		return this.getContextNode().equals(other.getContextNode());
	}

	@Override
	public int hashCode() {

		int hashCode = 1;

		hashCode = (hashCode * 31) + this.getContextNode().hashCode();

		return hashCode;
	}

	@Override
	public int compareTo(Policy other) {

		if (other == this || other == null) return 0;

		return this.getContextNode().compareTo(other.getContextNode());
	}

	/*
	 * Helper classes
	 */

	public static class MappingXdiEntityPolicyIterator extends NotNullIterator<Policy> {

		public MappingXdiEntityPolicyIterator(Iterator<XdiEntity> xdiEntities) {

			super(new MappingIterator<XdiEntity, Policy> (xdiEntities) {

				@Override
				public Policy map(XdiEntity xdiEntity) {

					return Policy.fromXdiEntity(xdiEntity);
				}
			});
		}
	}

	public static class MappingXdiEntityPolicyRootIterator extends NotNullIterator<PolicyRoot> {

		public MappingXdiEntityPolicyRootIterator(Iterator<XdiEntity> xdiEntities) {

			super(new MappingIterator<XdiEntity, PolicyRoot> (xdiEntities) {

				@Override
				public PolicyRoot map(XdiEntity xdiEntity) {

					return PolicyRoot.fromXdiEntity(xdiEntity);
				}
			});
		}
	}

	public static class MappingXdiEntityPolicyAndIterator extends NotNullIterator<PolicyAnd> {

		public MappingXdiEntityPolicyAndIterator(Iterator<? extends XdiEntity> xdiEntities) {

			super(new MappingIterator<XdiEntity, PolicyAnd> (xdiEntities) {

				@Override
				public PolicyAnd map(XdiEntity xdiEntity) {

					return PolicyAnd.fromXdiEntity(xdiEntity);
				}
			});
		}
	}

	public static class MappingXdiEntityPolicyOrIterator extends NotNullIterator<PolicyOr> {

		public MappingXdiEntityPolicyOrIterator(Iterator<? extends XdiEntity> xdiEntities) {

			super(new MappingIterator<XdiEntity, PolicyOr> (xdiEntities) {

				@Override
				public PolicyOr map(XdiEntity xdiEntity) {

					return PolicyOr.fromXdiEntity(xdiEntity);
				}
			});
		}
	}

	public static class MappingXdiEntityPolicyNotIterator extends NotNullIterator<PolicyNot> {

		public MappingXdiEntityPolicyNotIterator(Iterator<? extends XdiEntity> xdiEntities) {

			super(new MappingIterator<XdiEntity, PolicyNot> (xdiEntities) {

				@Override
				public PolicyNot map(XdiEntity xdiEntity) {

					return PolicyNot.fromXdiEntity(xdiEntity);
				}
			});
		}
	}
}

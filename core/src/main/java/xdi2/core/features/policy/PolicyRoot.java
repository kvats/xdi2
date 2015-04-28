package xdi2.core.features.policy;

import java.util.Iterator;

import xdi2.core.constants.XDIPolicyConstants;
import xdi2.core.features.nodetypes.XdiEntity;
import xdi2.core.features.nodetypes.XdiEntityInstanceOrdered;
import xdi2.core.features.nodetypes.XdiEntityInstanceUnordered;
import xdi2.core.features.nodetypes.XdiEntitySingleton;
import xdi2.core.features.policy.evaluation.PolicyEvaluationContext;
import xdi2.core.features.policy.operator.Operator;

/**
 * An XDI root policy, represented as an XDI entity.
 * 
 * @author markus
 */
public final class PolicyRoot extends Policy {

	private static final long serialVersionUID = -9212794041490417047L;

	protected PolicyRoot(XdiEntity xdiEntity) {

		super(xdiEntity);
	}

	/*
	 * Static methods
	 */

	/**
	 * Checks if an XDI entity is a valid XDI root policy.
	 * @param xdiEntity The XDI entity to check.
	 * @return True if the XDI entity is a valid XDI root policy.
	 */
	public static boolean isValid(XdiEntity xdiEntity) {

		if (xdiEntity instanceof XdiEntitySingleton)
			return ((XdiEntitySingleton) xdiEntity).getBaseXDIArc().equals(XDIPolicyConstants.XDI_ARC_IF);
		else if (xdiEntity instanceof XdiEntityInstanceUnordered)
			return ((XdiEntityInstanceUnordered) xdiEntity).getXdiCollection().getBaseXDIArc().equals(XDIPolicyConstants.XDI_ARC_IF);
		else if (xdiEntity instanceof XdiEntityInstanceOrdered)
			return ((XdiEntityInstanceOrdered) xdiEntity).getXdiCollection().getBaseXDIArc().equals(XDIPolicyConstants.XDI_ARC_IF);

		return false;
	}

	/**
	 * Factory method that creates an XDI root policy bound to a given XDI subgraph.
	 * @param xdiEntity The XDI subgraph that is an XDI root policy.
	 * @return The XDI root policy.
	 */
	public static PolicyRoot fromXdiEntity(XdiEntity xdiEntity) {

		if (! isValid(xdiEntity)) return null;

		return new PolicyRoot(xdiEntity);
	}

	/*
	 * Instance methods
	 */

	@Override
	public Boolean evaluateInternal(PolicyEvaluationContext policyEvaluationContext) {

		for (Iterator<Policy> policies = this.getPolicies(); policies.hasNext(); ) {

			Policy policy = policies.next();
			if (Boolean.TRUE.equals(policy.evaluate(policyEvaluationContext))) return Boolean.TRUE;
		}

		for (Iterator<Operator> operators = this.getOperators(); operators.hasNext(); ) {

			Operator operator = operators.next();
			for (Boolean result : operator.evaluate(policyEvaluationContext)) if (Boolean.TRUE.equals(result)) return Boolean.TRUE;
		}

		return Boolean.FALSE;
	}
}

package xdi2.messaging.operations;

import xdi2.core.Relation;
import xdi2.core.features.nodetypes.XdiEntitySingleton;
import xdi2.core.util.XDIAddressUtil;
import xdi2.messaging.Message;
import xdi2.messaging.constants.XDIMessagingConstants;

/**
 * A $del XDI operation, represented as a relation.
 * 
 * @author markus
 */
public class DelOperation extends Operation {

	private static final long serialVersionUID = 5732150427365911411L;

	protected DelOperation(Message message, Relation relation) {

		super(message, relation);
	}

	/*
	 * Static methods
	 */

	/**
	 * Checks if an relation is a valid XDI $del operation.
	 * @param relation The relation to check.
	 * @return True if the relation is a valid XDI $del operation.
	 */
	public static boolean isValid(Relation relation) {

		if (XDIAddressUtil.startsWithXDIAddress(relation.getXDIAddress(), XDIMessagingConstants.XDI_ADD_DEL) == null) return false;
		if (! XdiEntitySingleton.createXDIArc(XDIMessagingConstants.XDI_ARC_DO).equals(relation.getContextNode().getXDIArc())) return false;

		return true;
	}

	/**
	 * Factory method that creates an XDI $del operation bound to a given relation.
	 * @param relation The relation that is an XDI $del operation.
	 * @return The XDI $del operation.
	 */
	public static DelOperation fromMessageAndRelation(Message message, Relation relation) {

		if (! isValid(relation)) return null;

		return new DelOperation(message, relation);
	}
}

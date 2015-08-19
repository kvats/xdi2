package xdi2.messaging.target.interceptor.impl;

import xdi2.core.Graph;
import xdi2.core.features.policy.PolicyRoot;
import xdi2.core.features.policy.evaluation.PolicyEvaluationContext;
import xdi2.core.util.GraphAware;
import xdi2.messaging.Message;
import xdi2.messaging.target.MessagingTarget;
import xdi2.messaging.target.Prototype;
import xdi2.messaging.target.exceptions.Xdi2MessagingException;
import xdi2.messaging.target.exceptions.Xdi2NotAuthorizedException;
import xdi2.messaging.target.execution.ExecutionContext;
import xdi2.messaging.target.execution.ExecutionResult;
import xdi2.messaging.target.interceptor.InterceptorResult;
import xdi2.messaging.target.interceptor.MessageInterceptor;
import xdi2.messaging.target.interceptor.impl.util.MessagePolicyEvaluationContext;

/**
 * This interceptor evaluates message policies.
 * 
 * @author markus
 */
public class MessagePolicyInterceptor extends AbstractInterceptor<MessagingTarget> implements GraphAware, MessageInterceptor, Prototype<MessagePolicyInterceptor> {

	private Graph messagePolicyGraph; 

	public MessagePolicyInterceptor(Graph messagePolicyGraph) {

		this.messagePolicyGraph = messagePolicyGraph;
	}

	public MessagePolicyInterceptor() {

		this.messagePolicyGraph = null;
	}

	/*
	 * Prototype
	 */

	@Override
	public MessagePolicyInterceptor instanceFor(PrototypingContext prototypingContext) {

		// create new interceptor

		MessagePolicyInterceptor interceptor = new MessagePolicyInterceptor();

		// set the graph

		interceptor.setMessagePolicyGraph(this.getMessagePolicyGraph());

		// done

		return interceptor;
	}

	/*
	 * GraphAware
	 */

	@Override
	public void setGraph(Graph graph) {

		if (this.getMessagePolicyGraph() == null) this.setMessagePolicyGraph(graph);
	}

	/*
	 * MessageInterceptor
	 */

	@Override
	public InterceptorResult before(Message message, ExecutionResult executionResult, ExecutionContext executionContext) throws Xdi2MessagingException {

		// evaluate the XDI policy of this message

		PolicyRoot policyRoot = message.getPolicyRoot(false);
		if (policyRoot == null) return InterceptorResult.DEFAULT;

		PolicyEvaluationContext policyEvaluationContext = new MessagePolicyEvaluationContext(message, this.getMessagePolicyGraph());

		if (! Boolean.TRUE.equals(policyRoot.evaluate(policyEvaluationContext))) {

			throw new Xdi2NotAuthorizedException("Message policy violation for message " + message.toString() + ".", null, executionContext);
		}

		// done

		return InterceptorResult.DEFAULT;
	}

	@Override
	public InterceptorResult after(Message message, ExecutionResult executionResult, ExecutionContext executionContext) throws Xdi2MessagingException {

		// done

		return InterceptorResult.DEFAULT;
	}

	/*
	 * Getters and setters
	 */

	public Graph getMessagePolicyGraph() {

		return this.messagePolicyGraph;
	}

	public void setMessagePolicyGraph(Graph messagePolicyGraph) {

		this.messagePolicyGraph = messagePolicyGraph;
	}
}

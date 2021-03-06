package xdi2.messaging.target.interceptor.impl.security.secrettoken;

import xdi2.core.syntax.XDIAddress;
import xdi2.messaging.target.MessagingTarget;

/**
 * The purpose of this interface is to validate the secret token for a given
 * sender XDI address. This is used by the SecretTokenInterceptor.
 */
public interface SecretTokenValidator {

	/*
	 * Init and shutdown
	 */

	public void init(MessagingTarget messagingTarget, SecretTokenInterceptor authenticationSecretTokenInterceptor) throws Exception;
	public void shutdown(MessagingTarget messagingTarget, SecretTokenInterceptor authenticationSecretTokenInterceptor) throws Exception;

	/**
	 * Validates a secret token.
	 */
	public boolean authenticate(String secretToken, XDIAddress senderXDIAddress);
}

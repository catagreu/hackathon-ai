package org.elavationlab.exception;

public class InsufficientFundsException extends WalletException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}


package net.sf.l2j.gameserver.model.cards;

public class SymbolNotFoundException extends RuntimeException {
    public SymbolNotFoundException() {
    }

    public SymbolNotFoundException(String message) {
        super(message);
    }

    public SymbolNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public SymbolNotFoundException(Throwable cause) {
        super(cause);
    }

    public SymbolNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

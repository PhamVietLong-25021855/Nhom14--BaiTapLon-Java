package userauth.common;

import java.io.Serial;
import java.io.Serializable;

public final class RemoteRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final RemoteAction action;
    private final Object[] arguments;

    public RemoteRequest(RemoteAction action, Object... arguments) {
        this.action = action;
        this.arguments = arguments == null ? new Object[0] : arguments.clone();
    }

    public RemoteAction getAction() {
        return action;
    }

    public Object[] getArguments() {
        return arguments.clone();
    }

    public Object argumentAt(int index) {
        return arguments[index];
    }
}

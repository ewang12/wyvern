package wyvern.tools.errors;

public class ToolError extends RuntimeException {
	
	public static void reportError(ErrorMessage message, HasLocation errorLocation) {
		throw new ToolError(message, errorLocation);
	}

	public static void reportError(ErrorMessage message, String arg1, HasLocation errorLocation) {
		throw new ToolError(message, arg1, errorLocation);
	}

	// may want to distinguish evaluation errors from typechecking errors at some point
	public static void reportEvalError(ErrorMessage message, String arg1, HasLocation errorLocation) {
		reportError(message, arg1, errorLocation);
	}

	public static void reportError(ErrorMessage message, String arg1, String arg2, HasLocation errorLocation) {
		throw new ToolError(message, arg1, arg2, errorLocation);
	}

	public static void reportError(ErrorMessage message, String arg1, String arg2, String arg3, HasLocation errorLocation) {
		throw new ToolError(message, arg1, arg2, arg3, errorLocation);
	}

	protected ToolError(ErrorMessage message, HasLocation errorLocation) {
		super(message.getErrorMessage());
		assert message.numberOfArguments() == 0;
		this.errorMessage = message;
		this.errorLocation = errorLocation;
		this.errorString = message.getErrorMessage();
	}

	protected ToolError(ErrorMessage message, String arg1, HasLocation errorLocation) {
		super(message.getErrorMessage(arg1));
		assert message.numberOfArguments() == 1;
		this.errorMessage = message;
		this.errorLocation = errorLocation;
		this.errorString = message.getErrorMessage(arg1);
	}

	protected ToolError(ErrorMessage message, String arg1, String arg2, HasLocation errorLocation) {
		super(message.getErrorMessage(arg1, arg2));
		assert message.numberOfArguments() == 2;
		this.errorMessage = message;
		this.errorLocation = errorLocation;
		this.errorString = message.getErrorMessage(arg1, arg2);
	}

	protected ToolError(ErrorMessage message, String arg1, String arg2, String arg3, HasLocation errorLocation) {
		super(message.getErrorMessage(arg1, arg2, arg3));
		assert message.numberOfArguments() == 3;
		this.errorMessage = message;
		this.errorLocation = errorLocation;
		this.errorString = message.getErrorMessage(arg1, arg2, arg3);
	}

	public HasLocation getErrorLocation() { return errorLocation; }
	public String getErrorString() { return errorString; }
	public ErrorMessage getTypecheckingErrorMessage() {
		return errorMessage;
	}
	
	private HasLocation errorLocation;	
	private String errorString;
	private ErrorMessage errorMessage;	
	
	/**
	 * For serialization
	 */
	private static final long serialVersionUID = -4348846559537743643L;

}
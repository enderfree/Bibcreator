
public class FileInvalidException extends Exception{
	//Default constructor
	public FileInvalidException() {
		super("Error:Input file cannot be parsed due to missing information");
	}
	
	//Construtor where we can give our own custom message
	public FileInvalidException(String message) {
		super(message);
	}
}

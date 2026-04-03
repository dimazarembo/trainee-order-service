package by.dzarembo.traineeorderservice.exception;

public class ItemInUseException extends RuntimeException {

    public ItemInUseException(String message) {
        super(message);
    }
}

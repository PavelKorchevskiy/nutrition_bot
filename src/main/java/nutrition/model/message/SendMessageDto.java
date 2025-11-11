package nutrition.model.message;

public record SendMessageDto(long chatId, String message, KeyboardType keyboardType) {
}

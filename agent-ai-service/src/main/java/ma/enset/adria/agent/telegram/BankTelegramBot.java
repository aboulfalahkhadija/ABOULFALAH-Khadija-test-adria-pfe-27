package ma.enset.adria.agent.telegram;

import jakarta.annotation.PostConstruct;
import ma.enset.adria.agent.service.BankAiAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Component
public class BankTelegramBot extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(BankTelegramBot.class);

    @Value("${telegram.token:}")
    private String telegramBotToken;

    @Value("${telegram.bot.username:EbankBot19}")
    private String botUsername;

    private final BankAiAgent aiAgent;

    public BankTelegramBot(BankAiAgent aiAgent) {
        this.aiAgent = aiAgent;
    }

    @Override
    public String getBotToken() {
        return this.telegramBotToken;
    }

    @Override
    public String getBotUsername() {
        return this.botUsername;
    }

    @PostConstruct
    public void registerTelegramBot() {
        if (telegramBotToken == null || telegramBotToken.isBlank() || telegramBotToken.startsWith("YOUR_")) {
            log.info("Token Telegram non configuré, le bot Telegram reste en attente.");
            return;
        }
        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(this);
            log.info("Bot Telegram {} démarré avec succès.", botUsername);
        } catch (TelegramApiException e) {
            log.warn("Impossible d'enregistrer le bot Telegram : {}", e.getMessage());
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;
        Long chatId = update.getMessage().getChatId();
        String query = update.getMessage().getText();

        SendChatAction typing = new SendChatAction();
        typing.setChatId(chatId.toString());
        typing.setAction(ActionType.TYPING);
        try { execute(typing); } catch (TelegramApiException ignored) {}

        String answer = aiAgent.chat(query, String.valueOf(chatId));

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(answer);
        try { execute(message); } catch (TelegramApiException e) {
            log.error("Erreur envoi message Telegram : {}", e.getMessage());
        }
    }
}

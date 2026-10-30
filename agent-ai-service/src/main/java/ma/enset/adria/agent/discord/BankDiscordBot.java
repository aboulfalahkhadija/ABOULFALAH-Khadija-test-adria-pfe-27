package ma.enset.adria.agent.discord;

import ma.enset.adria.agent.service.BankAiAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BankDiscordBot {

    private static final Logger log = LoggerFactory.getLogger(BankDiscordBot.class);

    @Value("${discord.token:}")
    private String discordToken;

    private final BankAiAgent bankAiAgent;

    public BankDiscordBot(BankAiAgent bankAiAgent) {
        this.bankAiAgent = bankAiAgent;
    }

    public String handleDiscordMessage(String channelId, String query) {
        return bankAiAgent.chat(query, channelId);
    }
}

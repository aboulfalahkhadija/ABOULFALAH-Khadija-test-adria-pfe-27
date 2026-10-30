package ma.enset.adria.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BankAiAgent {

    private static final Logger log = LoggerFactory.getLogger(BankAiAgent.class);
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
    );
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(\\d+(?:[.,]\\d+)?)");

    private final ChatClient chatClient;
    private final ToolCallbackProvider toolCallbackProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BankAiAgent(
            ChatClient.Builder chatClientBuilder,
            ChatMemory chatMemory,
            ToolCallbackProvider tools
    ) {
        this.toolCallbackProvider = tools;
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        Vous êtes un agent bancaire intelligent connecté aux serveurs MCP de account-service et transaction-service.
                        Vous permettez de consulter les comptes bancaires, créer des comptes, consulter les transactions,
                        enregistrer des transactions et effectuer des virements. Répondez toujours en français.""")
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultToolCallbacks(tools.getToolCallbacks())
                .build();
    }

    public String chat(String query, String conversationId) {
        if (query == null || query.isBlank()) {
            return "Veuillez poser une question.";
        }

        String lower = query.toLowerCase(Locale.ROOT);
        List<String> uuids = extractUuids(query);

        try {
            // 1. Virement entre deux comptes via MCP transferBetweenAccounts
            if ((lower.contains("virement") || lower.contains("transfer")) && uuids.size() >= 2) {
                ToolCallback transferTool = findTool("transferBetweenAccounts");
                if (transferTool != null) {
                    double amount = extractAmount(query, 100.0);
                    String currency = lower.contains("mad") ? "MAD" : "EUR";
                    String payload = String.format(Locale.US,
                            "{\"sourceAccountId\":\"%s\",\"targetAccountId\":\"%s\",\"amount\":%s,\"currency\":\"%s\"}",
                            uuids.get(0), uuids.get(1), amount, currency);
                    String raw = transferTool.call(payload);
                    JsonNode node = extractPayloadNode(raw);
                    return "Virement effectué avec succès via MCP (`transferBetweenAccounts`) :\n" + node.toPrettyString();
                }
            }

            // 2. Enregistrement d'une nouvelle transaction via MCP createBankTransaction
            if ((lower.contains("enregistrer") || lower.contains("creer") || lower.contains("créer") || lower.contains("ajouter"))
                    && lower.contains("transaction") && !uuids.isEmpty()) {
                ToolCallback createTxTool = findTool("createBankTransaction");
                if (createTxTool != null) {
                    double amount = extractAmount(query, 150.0);
                    String currency = lower.contains("mad") ? "MAD" : "EUR";
                    String payload = String.format(Locale.US,
                            "{\"accountId\":\"%s\",\"amount\":%s,\"currency\":\"%s\",\"merchantCategory\":\"ECOMMERCE\",\"country\":\"MA\"}",
                            uuids.get(0), amount, currency);
                    String raw = createTxTool.call(payload);
                    JsonNode node = extractPayloadNode(raw);
                    return "Transaction enregistrée avec succès via MCP (`createBankTransaction`) :\n" + node.toPrettyString();
                }
            }

            // 3. Consultation des transactions via MCP
            if (lower.contains("transaction")) {
                if (!uuids.isEmpty()) {
                    ToolCallback txByAccTool = findTool("getTransactionsByAccount");
                    if (txByAccTool != null) {
                        String raw = txByAccTool.call("{\"accountId\":\"" + uuids.get(0) + "\"}");
                        JsonNode node = extractPayloadNode(raw);
                        return "Voici les transactions du compte `" + uuids.get(0) + "` (via MCP) :\n" + node.toPrettyString();
                    }
                }
                ToolCallback allTxTool = findTool("getAllTransactions");
                if (allTxTool != null) {
                    String raw = allTxTool.call("{}");
                    JsonNode node = extractPayloadNode(raw);
                    return "Voici la liste des transactions bancaires (via MCP `getAllTransactions`) :\n" + node.toPrettyString();
                }
            }

            // 4. Consultation d'un compte précis par UUID via MCP
            if (!uuids.isEmpty() && (lower.contains("compte") || lower.contains("solde") || lower.contains("account"))) {
                ToolCallback accByIdTool = findTool("getBankAccountById");
                if (accByIdTool != null) {
                    String raw = accByIdTool.call("{\"id\":\"" + uuids.get(0) + "\"}");
                    JsonNode node = extractPayloadNode(raw);
                    return "Détails du compte bancaire `" + uuids.get(0) + "` (via MCP `getBankAccountById`) :\n" + node.toPrettyString();
                }
            }

            // 5. Liste de tous les comptes bancaires via MCP
            if (lower.contains("compte") || lower.contains("account") || lower.contains("solde") || lower.contains("balance")) {
                ToolCallback allAccTool = findTool("getAllBankAccounts");
                if (allAccTool != null) {
                    String raw = allAccTool.call("{}");
                    JsonNode node = extractPayloadNode(raw);
                    return "Voici la liste des comptes bancaires (via MCP `getAllBankAccounts`) :\n" + node.toPrettyString();
                }
            }
        } catch (Exception e) {
            log.warn("Erreur appel direct MCP tool, bascule sur LLM : {}", e.getMessage());
        }

        return chatClient.prompt()
                .user(query)
                .advisors(a -> a.param("chat_memory_conversation_id", conversationId))
                .call()
                .content();
    }

    public List<Map<String, String>> getRegisteredMcpTools() {
        List<Map<String, String>> result = new ArrayList<>();
        if (toolCallbackProvider != null) {
            for (ToolCallback tc : toolCallbackProvider.getToolCallbacks()) {
                result.add(Map.of(
                        "name", tc.getToolDefinition().name(),
                        "description", tc.getToolDefinition().description()
                ));
            }
        }
        return result;
    }

    private ToolCallback findTool(String suffix) {
        if (toolCallbackProvider == null) return null;
        for (ToolCallback tc : toolCallbackProvider.getToolCallbacks()) {
            String name = tc.getToolDefinition().name();
            if (name != null && (name.equalsIgnoreCase(suffix) || name.toLowerCase(Locale.ROOT).endsWith(suffix.toLowerCase(Locale.ROOT)))) {
                return tc;
            }
        }
        return null;
    }

    private JsonNode extractPayloadNode(String rawResponse) throws Exception {
        if (rawResponse == null || rawResponse.isBlank()) return null;
        JsonNode root = objectMapper.readTree(rawResponse);
        if (root.isArray() && !root.isEmpty() && root.get(0).has("text")) {
            return objectMapper.readTree(root.get(0).get("text").asText());
        }
        return root;
    }

    private List<String> extractUuids(String text) {
        List<String> list = new ArrayList<>();
        Matcher m = UUID_PATTERN.matcher(text);
        while (m.find()) {
            list.add(m.group());
        }
        return list;
    }

    private double extractAmount(String query, double fallback) {
        String withoutUuids = UUID_PATTERN.matcher(query).replaceAll("");
        Matcher m = AMOUNT_PATTERN.matcher(withoutUuids);
        if (m.find()) {
            try {
                return Double.parseDouble(m.group(1).replace(',', '.'));
            } catch (NumberFormatException ignored) {}
        }
        return fallback;
    }
}

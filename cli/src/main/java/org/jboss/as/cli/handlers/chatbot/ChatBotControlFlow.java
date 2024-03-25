/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.cli.handlers.chatbot;

import static java.util.Arrays.asList;

import dev.langchain4j.chain.ConversationalRetrievalChain;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.router.DefaultQueryRouter;
import dev.langchain4j.store.embedding.EmbeddingStore;
import java.io.File;

import static org.wildfly.common.Assert.checkNotNullParam;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandContext.Scope;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.CommandLineRedirection;

import static org.jboss.as.cli.handlers.chatbot.ChatBotHandler.loadEmbeddingStore;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.jboss.as.cli.operation.ParsedCommandLine;

/**
 * Implements the loop iteration logic.
 *
 * @author jfdenise
 */
public class ChatBotControlFlow implements CommandLineRedirection {

    private class Commands {

        String question;
        List<String> commands = new ArrayList<>();
    }
    private static final String CTX_KEY = "CHATBOT";
    private static final String CHAIN_KEY = "CHAIN";

    private ConversationalRetrievalChain chain;

    static ChatBotControlFlow get(CommandContext ctx) {
        return (ChatBotControlFlow) ctx.get(Scope.CONTEXT, CTX_KEY);
    }

    private CommandLineRedirection.Registration registration;

    private List<Commands> storedCommands = new ArrayList<>();
    Commands currentCommand;

    ChatBotControlFlow(CommandContext ctx) throws CommandLineException {
        checkNotNullParam("ctx", ctx);
        chain = (ConversationalRetrievalChain) ctx.get(Scope.CONTEXT, CHAIN_KEY);
        if (chain == null) {
            EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();
            EmbeddingStore<TextSegment> store = loadEmbeddingStore(
                    new File("").toPath().resolve("docs-wildfly-embedding.json"));
            ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                    .embeddingStore(store)
                    .embeddingModel(embeddingModel)
                    .maxResults(6) // on each interaction we will retrieve the 2 most relevant segments
                    .minScore(0.8) // we want to retrieve segments at least somewhat similar to user query
                    .build();
            ChatLanguageModel model = OpenAiChatModel
                    .builder()
                    .apiKey("demo")
                    .maxRetries(5)
                    .modelName(OpenAiChatModelName.GPT_3_5_TURBO)
                    .logRequests(Boolean.TRUE)
                    .logResponses(Boolean.TRUE)
                    .maxTokens(1000)
                    .build();
            String promptTemplate2 = "You are a chatbot that will provide assistance with questions about WildFly CLI.\n"
                    + "You will be given a question you need to answer and a context to provide you with information.\n"
                    + "You must answer the question based as much as possible on this context.\n"
                    + "You must answer the question with WildFly CLI commands.\n"
                    + "If a question does not make any sense, or is not factually coherent, explain why instead of answering something not correct.\n"
                    + "If you don't know the answer to a question, please don't share false information. Answer the user question delimited by  ---."
                    + "\n"
                    + "---\n"
                    + "{{userMessage}}\n"
                    + "---"
                    + "\n Here is a few data to help you:\n"
                    + "{{contents}}";

            chain = ConversationalRetrievalChain.builder()
                    .chatLanguageModel(model)
                    .chatMemory(MessageWindowChatMemory.builder().maxMessages(1).build())
                    .retrievalAugmentor(DefaultRetrievalAugmentor.builder()
                            .contentInjector(DefaultContentInjector.builder()
                                    .promptTemplate(PromptTemplate.from(promptTemplate2))
                                    .metadataKeysToInclude(asList("file_name", "url", "title"))
                                    .build())
                            .queryRouter(new DefaultQueryRouter(contentRetriever))
                            .build())
                    .build();
            ctx.set(Scope.CONTEXT, CTX_KEY, chain);
        }
        ctx.printLine("Hello, I am the WildFly chatbot, ready to help you setup your WildFly server. You can type your questions...\n" +
                "You can type 'store' to store the CLI commands contained in my answers.\n"+
                "You can type 'generate' to generate a CLI scrpt containing all the stored commands.\n"+
                "Type 'bye' to leave.\n");
        ctx.set(Scope.CONTEXT, CTX_KEY, this);
    }

    @Override
    public void set(CommandLineRedirection.Registration registration) {
        this.registration = registration;
    }

    @Override
    public void handle(CommandContext ctx) throws CommandLineException {
        final ParsedCommandLine line = ctx.getParsedCommandLine();
        final String cmd = line.getOperationName();

        if ("bye".equals(cmd) || "exit".equals(cmd)) {
            registration.handle(line);
            return;
        }
        if ("clear".equals(cmd)) {
            ctx.clearScreen();
            return;
        }
        if ("store".equals(cmd)) {
            storedCommands.add(currentCommand);
            ctx.printLine("Storing the following commands:");
            for (String s : currentCommand.commands) {
                ctx.printLine(s);
            }
            return;
        }
        if ("dump".equals(cmd)) {
            for (Commands cc : storedCommands) {
                ctx.printLine("#" + cc.question);
                for (String ccc : cc.commands) {
                    ctx.printLine(ccc);
                }
            }
            return;
        }
        if ("generate".equals(cmd)) {
            StringBuilder builder = new StringBuilder();
            for (Commands cc : storedCommands) {
                builder.append("#" + cc.question + "\n");
                for (String ccc : cc.commands) {
                    builder.append(ccc + "\n");
                }
            }
            try {
                Files.write(Paths.get("wildfly-chatbot-script.cli"), builder.toString().getBytes());
            } catch (IOException ioex) {
                throw new CommandLineException(ioex);
            }
            System.out.println("WildFly CLI script file wildfly-chatbot-script.cli generated.");
            return;
        }
        currentCommand = null;
        String s = chain.execute(line.getOriginalLine());
        String[] lst = s.split("\n");
        boolean enter = false;
        for (String l : lst) {
            if (enter) {
                l = l.trim();
                if (l.startsWith("```")) {
                    enter = false;
                } else {
                    currentCommand.commands.add(l);
                }
            } else {
                if (l.startsWith("```")) {
                    enter = true;
                    if (currentCommand == null) {
                        currentCommand = new Commands();
                        currentCommand.question = line.getOriginalLine();
                    }
                }
            }
        }
        ctx.print(s);
    }

    void run(CommandContext ctx) throws CommandLineException {
        try {
            registration.unregister();

        } finally {
            if (registration.isActive()) {
                registration.unregister();
            }
            ctx.remove(Scope.CONTEXT, CTX_KEY);
        }
    }
}

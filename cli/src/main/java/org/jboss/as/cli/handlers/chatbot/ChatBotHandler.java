/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.cli.handlers.chatbot;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import java.nio.file.Path;
import java.util.List;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.batch.BatchManager;
import org.jboss.as.cli.handlers.CommandHandlerWithHelp;

/**
 *
 * @author jfdenise
 */
public class ChatBotHandler extends CommandHandlerWithHelp {

    public ChatBotHandler() {
        super("chat", true);
    }

    @Override
    public boolean isAvailable(CommandContext ctx) {
        return ChatBotControlFlow.get(ctx) == null && !ctx.getBatchManager().isBatchActive();
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.handlers.CommandHandlerWithHelp#doHandle(org.jboss.as.cli.CommandContext)
     */
    @Override
    protected void doHandle(CommandContext ctx) throws CommandLineException {
        if (ChatBotControlFlow.get(ctx) != null) {
            throw new CommandFormatException("chat is not allowed while in for block");
        }
        final BatchManager batchManager = ctx.getBatchManager();
        if (batchManager.isBatchActive()) {
            throw new CommandFormatException("chat is not allowed while in batch mode.");
        }

        ctx.registerRedirection(new ChatBotControlFlow(ctx));
    }

    public static EmbeddingStore<TextSegment> createEmbeddingStore(List<Document> documents, EmbeddingModel embeddingModel) {
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(1500, 500))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();
        ingestor.ingest(documents);
        return embeddingStore;
    }

    public static EmbeddingStore<TextSegment> loadEmbeddingStore(Path filePath) {
        return InMemoryEmbeddingStore.fromFile(filePath);
    }

    /**
     * It has to accept everything since we don't know what kind of command will
     * be edited.
     */
    @Override
    public boolean hasArgument(CommandContext ctx, int index) {
        return true;
    }

    @Override
    public boolean hasArgument(CommandContext ctx, String name) {
        return true;
    }
}

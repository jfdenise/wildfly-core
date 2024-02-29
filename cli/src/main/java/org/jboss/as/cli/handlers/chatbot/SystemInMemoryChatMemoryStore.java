/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.cli.handlers.chatbot;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 *
 * @author Emmanuel Hugonnet (c) 2024 Red Hat, Inc.
 */
public class SystemInMemoryChatMemoryStore implements ChatMemoryStore {

    private final List<ChatMessage> systemMessages = new ArrayList<>();
    private InMemoryChatMemoryStore delegate;

    public SystemInMemoryChatMemoryStore(SystemMessage... messages) {
        if (messages != null) {
            Stream.of(messages).forEach(msg -> systemMessages.add(msg));
        }
        delegate = new InMemoryChatMemoryStore();
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        List<ChatMessage> result = new ArrayList<>();
        result.addAll(systemMessages);
        result.addAll(delegate.getMessages(memoryId));
        return result;
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        delegate.updateMessages(memoryId, messages);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        delegate.getMessages(memoryId);
    }
}

package com.bappul.matching.lunchmatch.agent;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AgentHistoryService {

    private final ConcurrentHashMap<Long, List<Map<String, Object>>> historyMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Boolean> loopActiveMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, List<String>> pendingMessages = new ConcurrentHashMap<>();

    public List<Map<String, Object>> getOrCreate(Long userId) {
        return historyMap.computeIfAbsent(userId, k -> new ArrayList<>());
    }

    public void clear(Long userId) {
        historyMap.remove(userId);
        loopActiveMap.remove(userId);
        pendingMessages.remove(userId);
    }

    public void markLoopStart(Long userId) {
        loopActiveMap.put(userId, true);
    }

    public void markLoopEnd(Long userId) {
        loopActiveMap.remove(userId);
        List<String> pending = pendingMessages.remove(userId);
        if (pending != null) {
            List<Map<String, Object>> history = getOrCreate(userId);
            for (String text : pending) {
                history.add(Map.of("role", "model", "parts", List.of(Map.of("text", text))));
            }
        }
    }

    public void pushModelMessage(Long userId, String text) {
        if (loopActiveMap.getOrDefault(userId, false)) {
            pendingMessages.computeIfAbsent(userId, k -> new ArrayList<>()).add(text);
        } else {
            List<Map<String, Object>> history = getOrCreate(userId);
            history.add(Map.of("role", "model", "parts", List.of(Map.of("text", text))));
        }
    }
}
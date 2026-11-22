package com.jerry.salesmanagement.service;

public interface AiLlmClient {
    String chat(String systemPrompt, String userPrompt);
}

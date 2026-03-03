package com.ragdesk.services;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class RecursiveChunkingService {

    // Legal-aware separators: prefer splitting on paragraph breaks before sentences
    private final List<String> separators = Arrays.asList("\n\n", "\n", ". ", " ", "");

    public List<String> splitText(String text, int chunkSize, int chunkOverlap) {
        if (text == null || text.isBlank())
            return new ArrayList<>();
        return recursiveSplit(text, separators, chunkSize, chunkOverlap);
    }

    private List<String> recursiveSplit(String text, List<String> currentSeparators, int chunkSize, int chunkOverlap) {
        List<String> finalChunks = new ArrayList<>();

        if (text.length() <= chunkSize) {
            finalChunks.add(text);
            return finalChunks;
        }

        String separator = currentSeparators.isEmpty() ? "" : currentSeparators.get(0);
        List<String> remainingSeparators = currentSeparators.subList(Math.min(1, currentSeparators.size()),
                currentSeparators.size());

        String[] parts;
        if (separator.isEmpty()) {
            parts = text.split("");
        } else {
            String regex = separator.equals(". ") ? "\\. " : separator;
            parts = text.split(regex);
        }

        List<String> currentGroup = new ArrayList<>();
        int currentGroupLen = 0;

        for (String part : parts) {
            if (currentGroupLen + part.length() + separator.length() > chunkSize && !currentGroup.isEmpty()) {
                String joinedChunk = String.join(separator, currentGroup);

                if (joinedChunk.length() > chunkSize && !remainingSeparators.isEmpty()) {
                    finalChunks.addAll(recursiveSplit(joinedChunk, remainingSeparators, chunkSize, chunkOverlap));
                } else {
                    finalChunks.add(joinedChunk);
                }

                int overlapLimit = chunkOverlap;
                List<String> newGroup = new ArrayList<>();
                int newLen = 0;
                for (int i = currentGroup.size() - 1; i >= 0; i--) {
                    String overlapPart = currentGroup.get(i);
                    if (newLen + overlapPart.length() + separator.length() <= overlapLimit) {
                        newGroup.add(0, overlapPart);
                        newLen += overlapPart.length() + separator.length();
                    } else {
                        break;
                    }
                }
                currentGroup = newGroup;
                currentGroupLen = newLen;
            }

            currentGroup.add(part);
            currentGroupLen += part.length() + separator.length();
        }

        if (!currentGroup.isEmpty()) {
            String joinedChunk = String.join(separator, currentGroup);
            if (joinedChunk.length() > chunkSize && !remainingSeparators.isEmpty()) {
                finalChunks.addAll(recursiveSplit(joinedChunk, remainingSeparators, chunkSize, chunkOverlap));
            } else {
                finalChunks.add(joinedChunk);
            }
        }

        return finalChunks;
    }
}

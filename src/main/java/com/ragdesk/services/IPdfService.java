package com.ragdesk.services;

import java.util.List;

public interface IPdfService {
    List<PageContent> extractText(String filePath);
}

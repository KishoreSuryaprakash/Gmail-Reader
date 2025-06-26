package com.gmail.reader;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.auth.oauth2.Credential;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.*;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.*;

public class GmailReader {

    private static final String APPLICATION_NAME = "Gmail API Java";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_READONLY);
    private static final String CREDENTIALS_FILE_PATH = "credentials.json";

    // Authorize and get credentials
    private static Credential getCredentials(final com.google.api.client.http.HttpTransport HTTP_TRANSPORT) throws Exception {
        InputStream in = new FileInputStream(CREDENTIALS_FILE_PATH);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();

        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }

    public static void main(String[] args) throws Exception {
        final com.google.api.client.http.HttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        // Fetch last 200 messages from inbox
        ListMessagesResponse messagesResponse = service.users().messages()
                .list("me")
                .setMaxResults(200L)
                .setQ("is:inbox")
                .execute();

        List<Message> messages = messagesResponse.getMessages();

        if (messages == null || messages.isEmpty()) {
            System.out.println("No messages found.");
        } else {
            System.out.println("Last 200 emails:");
            for (Message message : messages) {
                Message fullMessage = service.users().messages().get("me", message.getId())
                        .setFormat("metadata")
                        .execute();

                List<MessagePartHeader> headers = fullMessage.getPayload().getHeaders();
                String from = "", subject = "";

                for (MessagePartHeader header : headers) {
                    if (header.getName().equalsIgnoreCase("From")) {
                        from = header.getValue();
                    }
                    if (header.getName().equalsIgnoreCase("Subject")) {
                        subject = header.getValue();
                    }
                }

                System.out.println("From: " + from);
                System.out.println("Subject: " + subject);
                System.out.println("--------------------------------------------------");
            }
        }
    }
}

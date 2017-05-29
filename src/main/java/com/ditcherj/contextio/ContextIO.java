package com.ditcherj.contextio;

import com.ditcherj.contextio.dto.*;
import com.ditcherj.contextio.responses.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.*;
import com.github.scribejava.core.oauth.OAuthService;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Jonathan Ditcher on 01/04/2017.
 */
public class ContextIO {

    private static final Logger logger = LoggerFactory.getLogger(ContextIO.class);
    private static final String END_POINT = "https://api.context.io/2.0/";
    private String oauthKey;
    private String oauthSecret;

    /**
     * Instantiate a new ContextIO object. Your OAuth consumer key and secret can be
     * found under the "settings" tab of the developer console (https://console.context.io/#settings)
     * @param key Your Context.IO OAuth consumer key
     * @param secret Your Context.IO OAuth consumer secret
     */
    public ContextIO(String key, String secret) {
        this.oauthKey = key;
        this.oauthSecret = secret;
    }

    /**
     * https://context.io/docs/2.0/accounts/connect_tokens
     * @param account
     * @return
     */
    public ConnectTokensResponse getConnectTokens(String account) {
        if (StringUtils.isEmpty(account))
            throw new IllegalArgumentException("account must be string representing accountId");

        final String endpoint = "accounts/"+account+"/connect_tokens";
        Response response = this.get(endpoint, null);
        List<ConnectToken> tokens = new ResponseBuilder(response).decodeResponseAsList(new TypeReference<List<ConnectToken>>(){});

        ConnectTokensResponse connectTokensResponse = new ConnectTokensResponse();
        connectTokensResponse.setTokens(tokens);
        connectTokensResponse.setCode(response.getCode());

        return connectTokensResponse;
    }

    public CreateConnectTokenResponse createConnectToken(String account, String callback_url) {
        return this.createConnectToken(account, callback_url, null, null, null, null, null, null, null, null);
    }

    public CreateConnectTokenResponse createConnectToken(String account,
                                                         String callback_url,
                                                         String email,
                                                         String first_name,
                                                         String last_name,
                                                         String source_callback_url,
                                                         Boolean source_expunge_on_deleted_flag,
                                                         Boolean source_sync_all_folders,
                                                         Boolean source_raw_file_list,
                                                         String status_callback_url
    ) {
        logger.trace("");

        if (StringUtils.isEmpty(account))
            throw new IllegalArgumentException("account must be string representing accountId");
        if (StringUtils.isEmpty(callback_url))
            throw new IllegalArgumentException("callback_url required");

        Map<String, String> params = new HashMap<>();
        params.put("callback_url", callback_url);

        if(!StringUtils.isEmpty(email))
            params.put("email", email);
        if(!StringUtils.isEmpty(first_name))
            params.put("first_name", first_name);
        if(!StringUtils.isEmpty(last_name))
            params.put("last_name", last_name);
        if(!StringUtils.isEmpty(source_callback_url))
            params.put("source_callback_url", source_callback_url);
        if(source_expunge_on_deleted_flag != null)
            params.put("source_expunge_on_deleted_flag", source_expunge_on_deleted_flag ? "1" : "0");
        if(source_sync_all_folders != null)
            params.put("source_sync_all_folders", source_sync_all_folders ? "1" : "0");
        if(source_raw_file_list != null)
            params.put("source_raw_file_list", source_raw_file_list ? "1" : "0");
        if(!StringUtils.isEmpty(status_callback_url))
            params.put("status_callback_url", status_callback_url);

        final String endpoint = "accounts/"+account+"/connect_tokens";
        Response response = this.post(endpoint, params);

        return new ResponseBuilder(response).decodeResponse(CreateConnectTokenResponse.class);
    }

    public SimpleResponse deleteConnectToken(String account, String token) {
        if (StringUtils.isEmpty(account))
            throw new IllegalArgumentException("account must be string representing accountId");
        if (StringUtils.isEmpty(token))
            throw new IllegalArgumentException("token required");

        final String endpoint = "accounts/"+account+"/connect_tokens/" +token;
        Response response = this.delete(endpoint, null);
        return new ResponseBuilder(response).decodeResponse(SimpleResponse.class);
    }

    public ListEmailAddressesResponse getEmailAddresses(String account) {
        if (StringUtils.isEmpty(account))
            throw new IllegalArgumentException("account must be string representing accountId");

        final String endpoint = "accounts/" + account + "/email_addresses";
        Response response = this.get(endpoint, null);
        return new ResponseBuilder(response).decodeResponse(ListEmailAddressesResponse.class);
    }

    public AddEmailAddressResponse addEmailAddress(String account, String email) {
        if (StringUtils.isEmpty(account))
            throw new IllegalArgumentException("account must be string representing accountId");
        if (StringUtils.isEmpty(email))
            throw new IllegalArgumentException("email required");

        Map<String, String> params = Collections.singletonMap("email_address", email);

        final String endpoint = "accounts/" + account + "/email_addresses";
        Response response = this.post(endpoint, params);
        return new ResponseBuilder(response).decodeResponse(AddEmailAddressResponse.class);
    }

    public SimpleResponse modifyEmailAddress(String account, String email, Boolean primary) {
        if (StringUtils.isEmpty(account))
            throw new IllegalArgumentException("account must be string representing accountId");
        if (StringUtils.isEmpty(email))
            throw new IllegalArgumentException("email required");

        Map<String, String> params = new HashMap<>();
        if(primary != null)
            params.put("primary", primary ? "!" : "0");

        String endpoint = null;
        try {
            endpoint = "accounts/" + account + "/email_addresses/" + URLEncoder.encode(email, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        Response response = this.post(endpoint, params);
        return new ResponseBuilder(response).decodeResponse(SimpleResponse.class);
    }

    public SimpleResponse deleteEmailAddress(String account, String email) {
        if (StringUtils.isEmpty(account))
            throw new IllegalArgumentException("account must be string representing accountId");
        if (StringUtils.isEmpty(email))
            throw new IllegalArgumentException("email required");

        String endpoint = null;
        try {
            endpoint = "accounts/" + account + "/email_addresses/" + URLEncoder.encode(email, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        Response response = this.delete(endpoint, null);
        return new ResponseBuilder(response).decodeResponse(SimpleResponse.class);
    }

    /**
     * https://context.io/docs/2.0/accounts
     * @param account
     * @return
     */
    public AccountResponse getAccount(String account) {
        if (StringUtils.isEmpty(account))
            throw new IllegalArgumentException("account must be string representing accountId");

        final String endpoint = "accounts/" + account;
        Response response = this.get(endpoint, null);
        return new ResponseBuilder(response).decodeResponse(AccountResponse.class);
    }

    public AccountsResponse getAccounts() {
        final String endpoint = "accounts";

        Response response = this.get(endpoint, null);
        List<Account> accounts = new ResponseBuilder(response).decodeResponseAsList(new TypeReference<List<Account>>(){});

        AccountsResponse accountsResponse = new AccountsResponse();
        accountsResponse.setAccounts(accounts);
        accountsResponse.setCode(response.getCode());

        return accountsResponse;
    }

    /**
     * https://context.io/docs/2.0/accounts/contacts
     * @param account
     * @return
     */
    public ListContactsResponse listContacts(String account) {
        return this.listContacts(account, null, null, null, null, null, null, null);
    }

    public ListContactsResponse listContacts(String account,
                                             String search,
                                             Integer activeBefore,
                                             Integer activeAfter,
                                             String sortBy,
                                             SortOrder sortOrder,
                                             Integer limit,
                                             Integer offset) {
        logger.trace("");

        if (StringUtils.isEmpty(account))
            throw new IllegalArgumentException("account must be string representing accountId");

        Map<String, String> params = new HashMap<>();

        if(!StringUtils.isEmpty(search))
            params.put("search", search);
        if(activeBefore != null)
            params.put("active_before", String.valueOf(activeBefore));
        if(activeAfter != null)
            params.put("active_after", String.valueOf(activeAfter));
        if(!StringUtils.isEmpty(sortBy))
            params.put("sort_by", sortBy);
        if(sortOrder != null)
            params.put("sort_order", sortOrder.name().toLowerCase());
        if(limit != null)
            params.put("limit", String.valueOf(limit));
        if(offset != null)
            params.put("offset", String.valueOf(offset));

        final String endpoint = "accounts/"+account+"/contacts";
        Response response = this.get(endpoint, params);

        return new ResponseBuilder(response).decodeResponse(ListContactsResponse.class);
    }

    public ContactResponse getContact(String account, String email) {
        logger.trace("");

        if (StringUtils.isEmpty(account))
            throw new IllegalArgumentException("account must be string representing accountId");
        if (StringUtils.isEmpty(email))
            throw new IllegalArgumentException("email required");

        String endpoint = null;
        try {
            endpoint = "accounts/"+account+"/contacts/" + URLEncoder.encode(email, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Response response = this.get(endpoint, null);

        return new ResponseBuilder(response).decodeResponse(ContactResponse.class);
    }

    /**
     * https://context.io/docs/2.0/accounts/contacts/messages
     * @param account
     * @return
     */
    public ListMessagesResponse getMessages(String account) {
        if (StringUtils.isEmpty(account))
            throw new IllegalArgumentException("account must be string representing accountId");

        final String endpoint = "accounts/"+account+"/messages";
        Response response = this.get(endpoint, null);
        List<Message> messages = new ResponseBuilder(response).decodeResponseAsList(new TypeReference<List<Message>>(){});

        ListMessagesResponse messagesResponse = new ListMessagesResponse();
        messagesResponse.setMessages(messages);
        messagesResponse.setCode(response.getCode());

        return messagesResponse;
    }

    public MessageResponse getMessage(String account,
                                      String messageId) {
        return this.getMessage(account, messageId, null, null, null, null, null, null);
    }

    public MessageResponse getMessage(String account,
                                           String messageId,
                                           Boolean include_thread_size,
                                           Boolean include_body,
                                           Boolean include_headers,
                                           Boolean include_flags,
                                           Type body_type,
                                           Boolean include_source) {
        if (StringUtils.isEmpty(account))
            throw new IllegalArgumentException("account must be string representing accountId");
        if (StringUtils.isEmpty(messageId))
            throw new IllegalArgumentException("messageId required");

        Map<String, String> params = new HashMap<>();
        if(include_thread_size != null)
            params.put("include_thread_size", include_thread_size ? "1" : "0");
        if(include_body != null)
            params.put("include_body", include_body ? "1" : "0");
        if(include_headers != null)
            params.put("include_headers", include_headers ? "1" : "0");
        if(include_flags != null)
            params.put("include_flags", include_flags ? "1" : "0");
        if(body_type != null)
            params.put("body_type", body_type.name());
        if(include_source != null)
            params.put("flag_draft", include_source ? "1" : "0");

        final String endpoint = "accounts/"+account+"/messages/" + messageId;

        Response response = this.get(endpoint, null);
        return new ResponseBuilder(response).decodeResponse(MessageResponse.class);
    }

    public SimpleResponse addMessageToFolder(String account,
                                             String dst_source,
                                             String dst_folder,
                                             String message,
                                             Boolean flag_seen,
                                             Boolean flag_answered,
                                             Boolean flag_flagged,
                                             Boolean flag_deleted,
                                             Boolean flag_draft) {
        if (StringUtils.isEmpty(account))
            throw new IllegalArgumentException("account must be string representing accountId");
        if (StringUtils.isEmpty(dst_source))
            throw new IllegalArgumentException("source required");
        if (StringUtils.isEmpty(dst_folder))
            throw new IllegalArgumentException("folder required");
        if (StringUtils.isEmpty(message))
            throw new IllegalArgumentException("message required");

        Map<String, String> params = new HashMap<>();
        params.put("dst_source", dst_source);
        params.put("dst_folder", dst_folder);
        params.put("message", message);

        if(flag_seen != null)
            params.put("flag_seen", flag_seen ? "1" : "0");
        if(flag_answered != null)
            params.put("flag_answered", flag_answered ? "1" : "0");
        if(flag_flagged != null)
            params.put("flag_flagged", flag_flagged ? "1" : "0");
        if(flag_deleted != null)
            params.put("flag_deleted", flag_deleted ? "1" : "0");
        if(flag_draft != null)
            params.put("flag_draft", flag_draft ? "1" : "0");

        final String endpoint = "accounts/"+account+"/messages";
        Response response = this.post(endpoint, null);
        return new ResponseBuilder(response).decodeResponse(SimpleResponse.class);
    }

    public MoveMessageResponse moveMessageToFolder(String account, String messageId, String dst_folder) {
        return this.moveMessageToFolder(account, messageId, dst_folder, null, null, null, null, null, null, null);
    }

    public MoveMessageResponse moveMessageToFolder(String account,
                                                   String messageId,
                                                   String dst_folder,
                                                   String dst_source,
                                                   Boolean move,
                                                   Boolean flag_seen,
                                                   Boolean flag_answered,
                                                   Boolean flag_flagged,
                                                   Boolean flag_deleted,
                                                   Boolean flag_draft) {
        if (StringUtils.isEmpty(account))
            throw new IllegalArgumentException("account must be string representing accountId");
        if (StringUtils.isEmpty(messageId))
            throw new IllegalArgumentException("messageId required");
        if (StringUtils.isEmpty(dst_folder))
            throw new IllegalArgumentException("folder required");

        Map<String, String> params = new HashMap<>();
        params.put("dst_folder", dst_folder);

        if(!StringUtils.isEmpty(dst_source))
            params.put("dst_source", dst_source);
        if(move != null)
            params.put("move", flag_seen ? "1" : "0");
        if(flag_seen != null)
            params.put("flag_seen", flag_seen ? "1" : "0");
        if(flag_answered != null)
            params.put("flag_answered", flag_answered ? "1" : "0");
        if(flag_flagged != null)
            params.put("flag_flagged", flag_flagged ? "1" : "0");
        if(flag_deleted != null)
            params.put("flag_deleted", flag_deleted ? "1" : "0");
        if(flag_draft != null)
            params.put("flag_draft", flag_draft ? "1" : "0");

        final String endpoint = "accounts/"+account+"/messages/" +messageId;
        Response response = this.post(endpoint, null);
        return new ResponseBuilder(response).decodeResponse(MoveMessageResponse.class);
    }

    public SimpleResponse deleteMessage(String account, String messageId) {
        if (StringUtils.isEmpty(account))
            throw new IllegalArgumentException("account must be string representing accountId");
        if (StringUtils.isEmpty(messageId))
            throw new IllegalArgumentException("messageId required");

        final String endpoint = "accounts/"+account+"/messages/" +messageId;
        Response response = this.delete(endpoint, null);
        return new ResponseBuilder(response).decodeResponse(SimpleResponse.class);
    }

    public MessageBodyResponse getMessageBody(String account, String messageId, Type type){
        if (StringUtils.isEmpty(account))
            throw new IllegalArgumentException("account must be string representing accountId");
        if (StringUtils.isEmpty(messageId))
            throw new IllegalArgumentException("messageId required");

        Map<String, String> params = new HashMap<>();
        if(type != null)
            params.put("type", type.getMimeType());

        final String endpoint = "accounts/"+account+"/messages/" + messageId + "/body";
        Response response = this.get(endpoint, null);

        List<MessageBody> messageBodies = new ResponseBuilder(response).decodeResponseAsList(new TypeReference<List<MessageBody>>(){});

        MessageBodyResponse messagesResponse = new MessageBodyResponse();
        messagesResponse.setMessageBodies(messageBodies);
        messagesResponse.setCode(response.getCode());

        return messagesResponse;
    }

    public SourcesResponse getSourcesList(String account, SourceStatus status, Integer status_ok) {
        if (StringUtils.isEmpty(account))
            throw new IllegalArgumentException("account must be string representing accountId");

        Map<String, String> params = new HashMap<>();
        if(status != null)
            params.put("status", status.name());
        if(status_ok != null)
            params.put("status_ok", String.valueOf(status_ok));

        final String endpoint = "accounts/"+account+"/sources";
        Response response = this.get(endpoint, null);

        List<Source> sources = new ResponseBuilder(response).decodeResponseAsList(new TypeReference<List<Source>>(){});

        SourcesResponse sourcesResponse = new SourcesResponse();
        sourcesResponse.setSources(sources);
        sourcesResponse.setCode(response.getCode());

        return sourcesResponse;
    }

    public FoldersResponse getFolders(String account, String label, Integer include_extended_counts, Integer no_cache) {
        if (StringUtils.isEmpty(account))
            throw new IllegalArgumentException("account must be string representing accountId");

        if(StringUtils.isEmpty(label))
            label = "0";

        Map<String, String> params = new HashMap<>();
        if(include_extended_counts != null)
            params.put("include_extended_counts", String.valueOf(include_extended_counts));
        if(no_cache != null)
            params.put("no_cache", String.valueOf(no_cache));

        final String endpoint = "accounts/"+account+"/sources/" + label + "/folders";
        Response response = this.get(endpoint, null);

        List<Folder> folders = new ResponseBuilder(response).decodeResponseAsList(new TypeReference<List<Folder>>(){});

        FoldersResponse foldersResponse = new FoldersResponse();
        foldersResponse.setFolders(folders);
        foldersResponse.setCode(response.getCode());

        return foldersResponse;
    }

    public FlagsResponse getFlags(String account, String messageId) {
        if (StringUtils.isEmpty(account))
            throw new IllegalArgumentException("account must be string representing accountId");
        if (StringUtils.isEmpty(messageId))
            throw new IllegalArgumentException("messageId required");

        final String endpoint = "accounts/"+account+"/messages/" + messageId + "/flags";
        Response response = this.get(endpoint, null);
        return new ResponseBuilder(response).decodeResponse(FlagsResponse.class);
    }

    public SetFlagsResponse setFlags(String account, String messageId, Flags flags) {
        if (StringUtils.isEmpty(account))
            throw new IllegalArgumentException("account must be string representing accountId");
        if (StringUtils.isEmpty(messageId))
            throw new IllegalArgumentException("messageId required");

        Map<String, String> params = new HashMap<>();
        if(flags.getSeen() != null)
            params.put("seen", flags.getSeen() ? "1" : "0");
        if(flags.getAnswered() != null)
            params.put("answered", flags.getAnswered() ? "1" : "0");
        if(flags.getFlagged() != null)
            params.put("flagged", flags.getFlagged() ? "1" : "0");
        if(flags.getDeleted() != null)
            params.put("deleted", flags.getDeleted() ? "1" : "0");
        if(flags.getDraft() != null)
            params.put("draft", flags.getDraft() ? "1" : "0");

        final String endpoint = "accounts/"+account+"/messages/" + messageId + "/flags";
        Response response = this.post(endpoint, params);
        return new ResponseBuilder(response).decodeResponse(SetFlagsResponse.class);
    }

    public MessageThreadResponse getMessageThread(String account, String messageId) {
        return this.getMessageThread(account, messageId, null, null, null, null, null, null);
    }

    public MessageThreadResponse getMessageThread(String account,
                                                  String messageId,
                                                  Boolean include_body,
                                                  Boolean include_headers,
                                                  Boolean include_flags,
                                                  Type body_type,
                                                  Integer limit,
                                                  Integer offset
    ) {
        if (StringUtils.isEmpty(account))
            throw new IllegalArgumentException("account must be string representing accountId");
        if (StringUtils.isEmpty(messageId))
            throw new IllegalArgumentException("messageId required");

        Map<String, String> params = new HashMap<>();
        if(include_body != null)
            params.put("include_body", include_body ? "1" : "0");
        if(include_headers != null)
            params.put("include_headers", include_headers ? "1" : "0");
        if(include_flags != null)
            params.put("include_flags", include_flags ? "1" : "0");
        if(body_type != null)
            params.put("body_type", body_type.name());
        if(limit != null)
            params.put("limit", String.valueOf(limit));
        if(offset != null)
            params.put("offset", String.valueOf(offset));

        final String endpoint = "accounts/"+account+"/messages/" + messageId + "/thread";
        Response response = this.get(endpoint, params);
        return new ResponseBuilder(response).decodeResponse(MessageThreadResponse.class);
    }

    private Response get(String endpoint, Map<String, String> params) {
        return doCall(Verb.GET, endpoint, params);
    }

    private Response post(String endpoint, Map<String, String> params) {
        return doCall(Verb.POST, endpoint, params);
    }

    private Response delete(String endpoint, Map<String, String> params) {
        return doCall(Verb.DELETE, endpoint, params);
    }

    private Response doCall(Verb method, String endpoint, Map<String, String> params)  {
        Response response = null;

        try{
            if(params == null)
                params = new HashMap<>();

            String baseUrl = END_POINT + endpoint;

            if (Verb.GET.equals(method)) {
                URIBuilder uriBuilder = new URIBuilder(baseUrl);
                params.entrySet().stream().forEach(e -> uriBuilder.addParameter(e.getKey(), e.getValue()));
                baseUrl = uriBuilder.build().toString();
            }

            logger.trace("baseUrl["+baseUrl+"] method["+method+"]");

            OAuthService service = new ServiceBuilder()
                    .apiKey(this.oauthKey)
                    .apiSecret(this.oauthSecret)
                    .debugStream(System.out)
                    .build(new ContextIOApi());

            OAuthRequest request = new OAuthRequest(method, baseUrl);

            if(method.equals(Verb.POST)){
                if(endpoint.endsWith("messages"))
                    this.handlePostMessageRequest(request, params);
                else
                    params.entrySet().stream().forEach(e -> request.addBodyParameter(e.getKey(), e.getValue()));
            }

            Token nullToken = new OAuth1AccessToken("", "");
            service.signRequest(nullToken, request);

            response = service.execute(request);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return response;
    }

    private void handlePostMessageRequest(OAuthRequest request, Map<String, String> params) throws IOException {
        String boundary = generateBoundaryString();
        request.addHeader("Content-type", "multipart/form-data; boundary=" + boundary);

            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create().setBoundary(boundary);
            for(Map.Entry<String, String> entry : params.entrySet()){
                String param = entry.getKey();
                if(param.equals("message"))
                    entityBuilder = entityBuilder.addBinaryBody("message", params.get(param).getBytes(StandardCharsets.UTF_8), ContentType.create("message/rfc822"), "message.eml");
                else
                    entityBuilder = entityBuilder.addTextBody(param, params.get(param));
            }

            HttpEntity entity = entityBuilder.build();
            ByteArrayOutputStream baos = new ByteArrayOutputStream((int)entity.getContentLength());

            entity.writeTo(baos);
            String payload = new String(baos.toByteArray(), StandardCharsets.UTF_8);
            request.setPayload(payload.getBytes(StandardCharsets.UTF_8));
    }

    private static String generateBoundaryString() {
        return Long.toHexString(System.nanoTime());
    }
}

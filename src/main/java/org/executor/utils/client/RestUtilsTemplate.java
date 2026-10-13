package org.executor.utils.client;

import org.executor.exceptions.client.ClientException;
import org.executor.utils.client.http.HttpEntity;
import org.executor.utils.client.http.HttpHeader;
import org.executor.utils.client.http.HttpMethod;

import java.io.File;
import java.util.List;
import java.util.Map;

interface RestUtilsTemplate {

    <T> T exchange(String url, HttpMethod method, HttpEntity<?> requestEntity, Class<T> responseType, Map<String, ?> uriVariables)
            throws ClientException;

    File download(String url, List<HttpHeader> httpHeaders, Object resource, File storeLocation) throws ClientException;
}

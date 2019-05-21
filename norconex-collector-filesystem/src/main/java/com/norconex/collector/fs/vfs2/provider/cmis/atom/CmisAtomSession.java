/* Copyright 2019 Norconex Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.norconex.collector.fs.vfs2.provider.cmis.atom;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class CmisAtomSession {

    private static final Logger LOG =
            LogManager.getLogger(CmisAtomSession.class);

    private final CloseableHttpClient http;
    private String endpointURL;
    private String repoId;
    private String repoName;
    private String objectByPathTemplate;
    private String queryTemplate;
    private final XPath xpath = XPathFactory.newInstance().newXPath();

    public CmisAtomSession(CloseableHttpClient httpClient) {
        super();
        this.http = httpClient;
    }

    public String getEndpointURL() {
        return endpointURL;
    }
    void setEndpointURL(String endpointURL) {
        this.endpointURL = endpointURL;
    }

    public String getRepoId() {
        return repoId;
    }
    void setRepoId(String repoId) {
        this.repoId = repoId;
    }

    public String getRepoName() {
        return repoName;
    }
    void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    public String getObjectByPathTemplate() {
        return objectByPathTemplate;
    }
    void setObjectByPathTemplate(String urlTemplate) {
        this.objectByPathTemplate = urlTemplate;
    }

    public String getQueryTemplate() {
        return queryTemplate;
    }
    void setQueryTemplate(String queryTemplate) {
        this.queryTemplate = queryTemplate;
    }

    public CloseableHttpClient getHttpClient() {
        return http;
    }
    public HttpResponse httpGet(String url) throws FileSystemException {
        try {
            return http.execute(new HttpGet(url));
        } catch (IOException e) {
            throw new FileSystemException(
                    "Could not get document from " + url, e);
        }
    }
    public Document getDocumentByPath(String path) throws FileSystemException {
        try {
            return getDocument(objectByPathTemplate.replace("{path}",
                   URLEncoder.encode(path, StandardCharsets.UTF_8.toString())));
        } catch (UnsupportedEncodingException e) {
            throw new FileSystemException(
                    "Could not get document from path: " + path, e);
        }
    }
    public Document getDocument(String fullURL) throws FileSystemException {
        try (CloseableHttpResponse resp = http.execute(new HttpGet(fullURL))) {
            if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                String consumedContent = IOUtils.toString(
                        resp.getEntity().getContent(), StandardCharsets.UTF_8);
                LOG.debug("Could not get document. Response content: "
                        + consumedContent);
                throw new IOException("Invalid HTTP response \""
                        +  resp.getStatusLine() + "\" from " + fullURL);
            }
            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            try (InputStream is = resp.getEntity().getContent()) {
                return builder.parse(is);
            }
        } catch (ParserConfigurationException | UnsupportedOperationException
                | SAXException | IOException e) {
            throw new FileSystemException(
                    "Could not get document from " + fullURL, e);
        }
    }
    public InputStream getStream(String fullURL) throws FileSystemException {
        try {
            CloseableHttpResponse resp = http.execute(new HttpGet(fullURL));
            if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new IOException("Invalid HTTP response \""
                        +  resp.getStatusLine() + "\" from " + fullURL);
            }
            return resp.getEntity().getContent();
        } catch (UnsupportedOperationException | IOException e) {
            throw new FileSystemException(
                    "Could not get stream from " + fullURL, e);
        }
    }

    public int getInt(Node node, String exp, int defaultValue)
            throws FileSystemException {
        String val = getString(node, exp);
        return NumberUtils.toInt(val, defaultValue);
    }
    public String getString(Node node, String exp)
            throws FileSystemException {
        return evaluate(node, exp, XPathConstants.STRING);
    }
    public Node getNode(Node node, String exp)
            throws FileSystemException {
        return evaluate(node, exp, XPathConstants.NODE);
    }
    public NodeList getNodeList(Node node, String exp)
            throws FileSystemException {
        return evaluate(node, exp, XPathConstants.NODESET);
    }

    public void close() {
        if (http instanceof CloseableHttpClient) {
            try {
                http.close();
            } catch (IOException e) {
                throw new RuntimeException(
                        "Error closing CMIS Atom HTTP client", e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T evaluate(Node node, String exp, QName returnType)
            throws FileSystemException {
        XPathExpression expr = toExpression(exp);
        try {
            return (T) expr.evaluate(node, returnType);
        } catch (XPathExpressionException e) {
            throw new FileSystemException(
                    "Could not evaluate XPath expression: " + exp, e);
        }
    }
    private XPathExpression toExpression(String exp)
                throws FileSystemException {
        try {
            return xpath.compile(exp);
        } catch (XPathExpressionException e) {
            throw new FileSystemException(
                    "Could not compile XPath expression: " + exp, e);
        }
    }
}

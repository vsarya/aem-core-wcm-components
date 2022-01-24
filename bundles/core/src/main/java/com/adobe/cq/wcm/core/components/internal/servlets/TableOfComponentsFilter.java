/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2021 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package com.adobe.cq.wcm.core.components.internal.servlets;

import org.apache.commons.io.IOUtils;
import org.apache.sling.commons.html.HtmlParser;
import org.apache.sling.servlets.annotations.SlingServletFilter;
import org.apache.sling.servlets.annotations.SlingServletFilterScope;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Component(service = Filter.class,
    property = {Constants.SERVICE_RANKING + "Integer=999"})
@SlingServletFilter(scope = {SlingServletFilterScope.REQUEST},
    pattern = "/content/.*",
    extensions = {"html"},
    methods = {"GET"})
public class TableOfComponentsFilter implements Filter {

    @Reference
    private HtmlParser htmlParser;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        CharResponseWrapper wrapper = new CharResponseWrapper((HttpServletResponseWrapper) response);

        chain.doFilter(request, wrapper);

        long  startTime = System.currentTimeMillis();
        PrintWriter responseWriter = response.getWriter();

        if (wrapper.getContentType().contains("text/html")) {
//            CharArrayWriter charWriter = new CharArrayWriter();
            StringWriter sw = new StringWriter();

            String originalContent = wrapper.toString();

//            long s = System.currentTimeMillis();
            Document document = htmlParser.parse(
                null,
                IOUtils.toInputStream(originalContent, Charset.defaultCharset()),
                Charset.defaultCharset().displayName()
            );
////            Document document = Jsoup.parse(originalContent);
//            long e = System.currentTimeMillis();
//            System.out.println("Time, Jsoup, " + (e -  s));
//
//            Set<String> hTags = new HashSet<>();
//            hTags.add("h1");
//            hTags.add("h2");
//            hTags.add("h3");
//            hTags.add("h4");
//            hTags.add("h5");
//            hTags.add("h6");
//
//            Element toc = document.createElement("ol");
//            NodeList nodes = document.getElementsByTagName("*");
//            for (int i = 0; i < nodes.getLength(); i++) {
//                Element element = (Element) nodes.item(i);
//                if(!hTags.contains(element.getNodeName()) || element.getTextContent().length() == 0) {
//                    continue;
//                }
//                Element li = document.createElement("li");
//                String uniqueId = UUID.randomUUID().toString();
//                Element a = document.createElement("a");
//                a.setAttribute("href", "#" + uniqueId);
//                a.setTextContent(element.getTextContent());
//                li.appendChild(a);
//                toc.appendChild(li);
//                element.setAttribute("id", uniqueId);
//            }
//
//            s = System.currentTimeMillis();
//            NodeList elements = document.getElementsByTagName("tableofcomponents-placeholder");
//            e = System.currentTimeMillis();
//            System.out.println("Time, getTocByClass, " + (e -  s));
//
//            s = System.currentTimeMillis();
//            for (int i = 0; i < elements.getLength(); i++) {
//                Element element = (Element) nodes.item(i);
//                element.appendChild(toc);
//            }
//            e = System.currentTimeMillis();
//            System.out.println("Time, appendingToc, " + (e -  s));
//
//            s = System.currentTimeMillis();
            try {
                TransformerFactory tf = TransformerFactory.newInstance();
                Transformer trans = tf.newTransformer();
                trans.transform(new DOMSource(document), new StreamResult(sw));
            } catch (TransformerException transformerException) {
                transformerException.printStackTrace();
            }
//            charWriter.write(document.toString());
            String alteredContent = sw.toString();
            response.setContentLength(alteredContent.length());
            responseWriter.write(alteredContent);
//            e = System.currentTimeMillis();
//            System.out.println("Time, modifyingResponse, " + (e -  s));
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Time, Total, " +  (endTime - startTime));
    }

    @Override
    public void destroy() {

    }
}

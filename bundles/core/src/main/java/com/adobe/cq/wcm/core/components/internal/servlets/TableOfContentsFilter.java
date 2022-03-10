/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2022 Adobe
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

import com.adobe.cq.wcm.core.components.internal.models.v1.TableOfContentsImpl;
import com.adobe.cq.wcm.core.components.models.TableOfContents;
import com.day.cq.wcm.api.WCMMode;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.servlets.annotations.SlingServletFilter;
import org.apache.sling.servlets.annotations.SlingServletFilterScope;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

/**
 * Intercepts all the HTTP requests made to /editor.html or a html page inside /content/.
 * Creates a response wrapper - {@link CharResponseWrapper} in which all the servlets/filters after this filter,
 * stores the response content.
 * Gets the response content from this wrapper, modifies it and copies it into the original response object.
 */
@Component(
    service = Filter.class,
    property = {Constants.SERVICE_RANKING + "Integer=999"}
)
@SlingServletFilter(
    scope = {SlingServletFilterScope.REQUEST},
    pattern = "/content/.*",
    resourceTypes = "cq:Page",
    extensions = {"html"},
    methods = {"GET"}
)
public class TableOfContentsFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TableOfContentsFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void destroy() {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {

        CharResponseWrapper responseWrapper = new CharResponseWrapper((HttpServletResponseWrapper) response);
        chain.doFilter(request, responseWrapper);
        String originalContent = responseWrapper.toString();
        Boolean containsTableOfContents = (Boolean) request.getAttribute(TableOfContentsImpl.TOC_REQUEST_ATTR_FLAG);
        if (responseWrapper.getContentType().contains("text/html") &&
            (containsTableOfContents != null && containsTableOfContents)) {

            Document document = Jsoup.parse(originalContent);

            Elements tocPlaceholderElements = document.getElementsByClass(TableOfContentsImpl.TOC_PLACEHOLDER_CLASS);
            for (Element tocPlaceholderElement : tocPlaceholderElements) {
                Element tableOfContents = getTableOfContents(tocPlaceholderElement);
                tocPlaceholderElement.empty();
                tocPlaceholderElement.clearAttributes();
                tocPlaceholderElement.addClass(TableOfContentsImpl.TOC_CONTENT_CLASS);
                if(tableOfContents != null) {
                    tocPlaceholderElement.appendChild(tableOfContents);
                    WCMMode wcmMode = WCMMode.fromRequest(request);
                    if(wcmMode == WCMMode.EDIT || wcmMode == WCMMode.PREVIEW) {
                        Elements tocTemplatePlaceholderElement = tocPlaceholderElement
                            .parent()
                            .select("." + TableOfContentsImpl.TOC_TEMPLATE_PLACEHOLDER_CLASS);
                        tocTemplatePlaceholderElement.remove();
                    }
                }
            }

            CharArrayWriter charWriter = new CharArrayWriter();
            charWriter.write(document.outerHtml());
            String alteredContent = charWriter.toString();

            response.setContentLength(alteredContent.length());
            response.getWriter().write(alteredContent);
        } else {
            response.setContentLength(originalContent.length());
            response.getWriter().write(originalContent);
        }
    }

    /**
     * Creates a TOC {@link Element} by getting all the TOC config from the
     * TOC placeholder DOM {@link Element}' data attributes.
     * @param tocPlaceholderElement - TOC placeholder dom element
     * @return Independent TOC element, not attached to the DOM
     */
    private Element getTableOfContents(Element tocPlaceholderElement) {
        TableOfContents.ListType listType = tocPlaceholderElement.hasAttr(TableOfContentsImpl.TOC_DATA_ATTR_LIST_TYPE)
            ? TableOfContents.ListType.fromString(
                tocPlaceholderElement.attr(TableOfContentsImpl.TOC_DATA_ATTR_LIST_TYPE))
            : TableOfContentsImpl.DEFAULT_LIST_TYPE;
        String listTag = listType.getTagName();
        TableOfContents.HeadingLevel startLevel =
            tocPlaceholderElement.hasAttr(TableOfContentsImpl.TOC_DATA_ATTR_START_LEVEL)
                ? TableOfContents.HeadingLevel.fromStringOrDefault(
                    tocPlaceholderElement.attr(TableOfContentsImpl.TOC_DATA_ATTR_START_LEVEL),
                    TableOfContentsImpl.DEFAULT_START_LEVEL)
                : TableOfContentsImpl.DEFAULT_START_LEVEL;
        TableOfContents.HeadingLevel stopLevel =
            tocPlaceholderElement.hasAttr(TableOfContentsImpl.TOC_DATA_ATTR_STOP_LEVEL)
                ? TableOfContents.HeadingLevel.fromStringOrDefault(
                    tocPlaceholderElement.attr(TableOfContentsImpl.TOC_DATA_ATTR_STOP_LEVEL),
                    TableOfContentsImpl.DEFAULT_STOP_LEVEL)
                : TableOfContentsImpl.DEFAULT_STOP_LEVEL;
        String[] includeClasses = tocPlaceholderElement.hasAttr(TableOfContentsImpl.TOC_DATA_ATTR_INCLUDE_CLASSES)
            ? tocPlaceholderElement.attr(TableOfContentsImpl.TOC_DATA_ATTR_INCLUDE_CLASSES).split(",")
            : null;
        String[] ignoreClasses = tocPlaceholderElement.hasAttr(TableOfContentsImpl.TOC_DATA_ATTR_IGNORE_CLASSES)
            ? tocPlaceholderElement.attr(TableOfContentsImpl.TOC_DATA_ATTR_IGNORE_CLASSES).split(",")
            : null;

        if(startLevel.getValue() > stopLevel.getValue()) {
            LOGGER.warn("Invalid start and stop levels, startLevel={%d}, stopLevel={%d}",
                startLevel.getValue(), stopLevel.getValue());
            return null;
        }

        Document document = tocPlaceholderElement.ownerDocument();

        String includeCssSelector;
        if(includeClasses == null || includeClasses.length == 0) {
            List<String> selectors = new ArrayList<>();
            for(int level = startLevel.getValue(); level <= stopLevel.getValue(); level++) {
                selectors.add(getHeadingTagName(level));
            }
            includeCssSelector = StringUtils.join(selectors, ",");
        } else {
            includeCssSelector = getCssSelectorString(includeClasses, startLevel.getValue(), stopLevel.getValue());
        }
        Elements includeElements = document.select(includeCssSelector);

        if(ignoreClasses == null || ignoreClasses.length == 0) {
            return getNestedList(listTag, includeElements.listIterator(), 0);
        }
        String ignoreCssSelector = getCssSelectorString(ignoreClasses, startLevel.getValue(), stopLevel.getValue());
        Elements ignoreElements = document.select(ignoreCssSelector);

        Set<Element> ignoreElementsSet = new HashSet<>(ignoreElements);

        List<Element> validElements = new ArrayList<>();
        for(Element element : includeElements) {
            if(!ignoreElementsSet.contains(element)
                && !"".contentEquals(element.text().trim())) {
                validElements.add(element);
            }
        }
        return getNestedList(listTag, validElements.listIterator(), 0);
    }

    /**
     * Converts a list of ignore/include class names, heading start level and heading stop level
     * into a CSS selector string
     * @param classNames - an array of include or ignore class names of the TOC
     * @param startLevel - heading start level of the TOC
     * @param stopLevel - heading stop level of the TOC
     * @return CSS selector string
     */
    private String getCssSelectorString(String[] classNames, int startLevel, int stopLevel) {
        if(classNames == null || classNames.length == 0) {
            return "";
        }
        List<String> selectors = new ArrayList<>();
        for(String className: classNames) {
            for(int level = startLevel; level <= stopLevel; level++) {
                selectors.add("." + className + " " + getHeadingTagName(level));
                selectors.add(getHeadingTagName(level) + "." + className);
            }
        }
        return StringUtils.join(selectors, ",");
    }

    /**
     * Recursive method to create a nested list of TOC depending upon the heading levels of consecutive heading elements
     * @param listTag - 'ul' for unordered list or 'ol' for ordered list
     * @param headingElementsIterator - Iterator of list of heading elements to be included in TOC
     * @param parentHeadingLevel - Heading level of the parent of the current nesting level
     * @return Current nested TOC element containing all heading elements with levels >= parent heading level
     */
    private Element getNestedList(String listTag, ListIterator<Element> headingElementsIterator,
                                  int parentHeadingLevel) {
        if(!headingElementsIterator.hasNext()) {
            return null;
        }
        Element list = new Element(listTag);
        Element headingElement = headingElementsIterator.next();
        Element listItem = getListItemElement(headingElement);
        int previousHeadingLevel = getHeadingLevel(headingElement);
        list.appendChild(listItem);
        while(headingElementsIterator.hasNext()) {
            headingElement = headingElementsIterator.next();
            int currentHeadingLevel = getHeadingLevel(headingElement);
            if(currentHeadingLevel == previousHeadingLevel ||
                (currentHeadingLevel < previousHeadingLevel && currentHeadingLevel > parentHeadingLevel)) {
                listItem = getListItemElement(headingElement);
                list.appendChild(listItem);
                previousHeadingLevel = currentHeadingLevel;
            } else if(currentHeadingLevel > previousHeadingLevel) {
                headingElementsIterator.previous();
                list.children().last().appendChild(getNestedList(listTag, headingElementsIterator, previousHeadingLevel));
            } else if(currentHeadingLevel < previousHeadingLevel && currentHeadingLevel <= parentHeadingLevel) {
                headingElementsIterator.previous();
                return list;
            }
        }
        return list;
    }

    /**
     * Creates list item element('li') from the heading element.
     * Adds an internal link on the 'li' element to the provided heading element.
     * Adds 'id' attribute on heading element if not already present, using its text content.
     * @param headingElement - DOM heading element
     * @return Independent 'li' element, not attached to the DOM
     */
    private Element getListItemElement(Element headingElement) {
        String id = headingElement.attr("id");
        if("".contentEquals(id)) {
            id = headingElement.text()
                .trim()
                .toLowerCase()
                .replaceAll("\\s", "-");
            headingElement.attr("id", id);
        }
        Element listItem = new Element("li");
        Element anchorTag = new Element("a");
        anchorTag.attr("href", "#" + id);
        anchorTag.appendText(headingElement.text());
        listItem.appendChild(anchorTag);
        return listItem;
    }

    /**
     * Returns heading level('1' to '6') of the heading element
     * @param headingElement DOM heading element
     * @return Integer representing the heading level of the given heading element
     */
    private int getHeadingLevel(Element headingElement) {
        return headingElement.tagName().charAt(1) - '0';
    }

    /**
     * Returns heading tag name('h1' to 'h6') from integer level
     * @param level Integer representing the heading level
     * @return Heading tag name
     */
    private String getHeadingTagName(int level) {
        return "h" + level;
    }
}

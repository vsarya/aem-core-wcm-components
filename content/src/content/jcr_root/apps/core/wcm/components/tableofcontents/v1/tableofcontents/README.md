<!--
Copyright 2022 Adobe

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->
Table of Contents (v1)
====
Component written in HTL and HTTP Request Filter that renders a table of contents(TOC) to help navigate the page content.

## Features
* **List Type** - Whether to list items in the table of contents as an unordered list of bullet points, or as an ordered list of numbers.
* **Title Start Level** - The highest title level to report in the table of contents, where 1 corresponds to H1 and includes all top-level titles.
* **Title Stop Level** - The lowest title level to report in the table of contents, where 6 corresponds to H6 and includes all lowest-level titles.
* **Styles**

### Use Object
The Table of Contents component uses the `com.adobe.cq.wcm.core.components.models.TableOfContents` sling model as its use-object.

### Component Policy Configuration Properties
The following configuration properties are used:

1. `./restrictListType` - defines whether the author should be able to choose the list type or not - `norestriction` or `unordered` or `ordered`
2. `./restrictStartLevel` - defines whether the author should be able to choose the highest title level to report in the TOC - `norestriction` or `1` to `6`
3. `./restrictStopLevel` - defines whether the author should be able to choose the lowest title level to report in the TOC - `norestriction` or `6` to `1`
4. `./includeClasses` - defines an array of strings representing the configured class names to include in the TOC.
5. `./ignoreClasses` - defines an array of strings representing the configured class names to ignore in the TOC.

### Edit Dialog Properties
The following properties are written to JCR for this Table of Contents component and are expected to be available as `Resource` properties:

1. `./listType` - defines the list type of the TOC - `unordered` or `ordered`
2. `./startLevel` - defines the lowest title level to report in the TOC - `1` to `6`
3. `./stopLevel` - defines the highest title level to report in the TOC - `6` to `1`
4. `./id` - defines the component's HTML ID attribute.

## BEM Description
```
BLOCK cmp-toc__content
    ELEMENT <ol/> or <ul/>
```

## Information
* **Vendor**: Adobe
* **Version**: v1
* **Compatibility**: AEM 6.3
* **Status**: production-ready
* **Documentation**: [https://www.adobe.com/go/aem\_cmp\_tableofcontents\_v1](https://www.adobe.com/go/aem_cmp_tableofcontents_v1)
* **Component Library**: [https://www.adobe.com/go/aem\_cmp\_library\_tableofcontents](https://www.adobe.com/go/aem_cmp_library_tableofcontents)
* **Author**: [Vishal Singh Arya](https://github.com/vsarya)

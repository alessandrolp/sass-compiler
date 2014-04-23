/*
 * Copyright 2000-2014 Vaadin Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.sass.internal.parser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.vaadin.sass.internal.tree.Node;
import com.vaadin.sass.internal.tree.Node.BuildStringStrategy;
import com.vaadin.sass.internal.tree.VariableNode;

/**
 * SassList is a list that has a specified separator character (comma or space)
 * and data items. The data items can be lists.
 */
public class SassList implements SassListItem, Serializable {

    public enum Separator {
        COMMA(", "), SPACE(" ");
        private String separator;

        private Separator(String sep) {
            separator = sep;
        }

        @Override
        public String toString() {
            return separator;
        }
    }

    // The position where the list occurs in the source scss file.
    private int line = -1;
    private int column = -1;

    private Separator separator;

    private final List<SassListItem> items;

    public SassList() {
        this(Separator.SPACE);
    }

    public SassList(SassListItem... items) {
        this(Separator.SPACE, items);
    }

    public SassList(Separator sep, SassListItem... items) {
        separator = sep;
        this.items = Arrays.asList(items);
    }

    public SassList(Separator sep, List<SassListItem> items) {
        separator = sep;
        this.items = items;
    }

    @Override
    public int getLineNumber() {
        return line;
    }

    @Override
    public int getColumnNumber() {
        return column;
    }

    public void setSourcePosition(int line, int column) {
        this.line = line;
        this.column = column;
    }

    @Override
    public Separator getSeparator() {
        return separator;
    }

    public void setSeparator(Separator separator) {
        this.separator = separator;
    }

    /**
     * Returns the only LexicalUnitImpl contained in this list.
     * 
     * @throws ParseException
     *             if this.size() != 1 or if the type of this.get(0) is not
     *             LexicalUnitImpl.
     */
    @Override
    public LexicalUnitImpl getContainedValue() {
        if (size() != 1 || !(get(0) instanceof LexicalUnitImpl)) {
            throw new ParseException(
                    "Expected a one-element list, actual value: "
                            + getStringWithNesting());
        }
        return (LexicalUnitImpl) get(0);
    }

    // A helper method for sass interpolation
    public String unquotedString() {
        if (size() != 1) {
            // preserve quotes if the list contains several elements
            return printState();
        }
        if (get(0) instanceof SassList) {
            // A nested list may contain one or more elements, handle
            // recursively.
            return ((SassList) get(0)).unquotedString();
        }

        // Handle a list with one element that is not a list.
        String result = printState();
        if (result.length() >= 2
                && ((result.charAt(0) == '"' && result
                        .charAt(result.length() - 1) == '"') || (result
                        .charAt(0) == '\'' && result
                        .charAt(result.length() - 1) == '\''))) {
            result = result.substring(1, result.length() - 1);
        }
        return result;
    }

    /**
     * Returns a SassListItem whose textual (CSS) representation is the same as
     * that of this list. Any extra nesting is recursively removed. Nesting is
     * extra if a list contains only one element. A list with extra nesting is
     * replaced by its contents (a SassList or a SassListItem).
     */
    @Override
    public SassListItem flatten() {
        // Flatten this list (i.e., return its contents) if possible.
        if (size() == 1) {
            return get(0).flatten();
        }
        // Recursively flatten the elements of this list.
        List<SassListItem> list = new ArrayList<SassListItem>();
        for (SassListItem item : this) {
            list.add(item.flatten());
        }
        return new SassList(getSeparator(), list);
    }

    @Override
    public boolean containsArithmeticalOperator() {
        for (SassListItem item : this) {
            if (item.containsArithmeticalOperator()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public SassList evaluateArithmeticExpressions() {
        List<SassListItem> list = new ArrayList<SassListItem>();
        for (SassListItem item : this) {
            list.add(item.evaluateArithmeticExpressions());
        }
        return new SassList(getSeparator(), list);
    }

    @Override
    public SassList replaceVariables(Collection<VariableNode> variables) {
        // The actual replacing happens in LexicalUnitImpl, which also
        // implements SassListItem.
        List<SassListItem> list = new ArrayList<SassListItem>();
        for (SassListItem item : this) {
            list.add(item.replaceVariables(variables));
        }
        return new SassList(getSeparator(), list);
    }

    @Override
    public SassList replaceFunctions() {
        List<SassListItem> list = new ArrayList<SassListItem>();
        for (SassListItem item : this) {
            list.add(item.replaceFunctions());
        }
        return new SassList(getSeparator(), list);
    }

    @Override
    public String buildString(BuildStringStrategy strategy) {
        String result = "";
        for (int i = 0; i < size(); i++) {
            result += get(i).buildString(strategy);
            if (i < size() - 1) {
                result += separator;
            }
        }
        return result;
    }

    public String printState() {
        return buildString(Node.PRINT_STRATEGY);
    }

    @Override
    public String toString() {
        return "SassList [" + getStringWithNesting() + "]";
    }

    /**
     * Returns a string representation of the list where nesting is indicated
     * using parentheses. Such a representation is mainly useful for debugging.
     */
    public String getStringWithNesting() {
        String result = "(";
        for (int i = 0; i < size(); i++) {
            SassListItem item = get(i);
            if (item instanceof SassList) {
                result += ((SassList) item).getStringWithNesting();
            } else {
                result += item.printState();
            }
            if (i < size() - 1) {
                result += separator;
            }
        }
        result += ")";
        return result;
    }

    @Override
    public void updateUrl(String prefix) {
        for (SassListItem item : this) {
            item.updateUrl(prefix);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SassList)) {
            return false;
        }
        SassList other = (SassList) o;
        if (size() != other.size()) {
            return false;
        }
        for (int i = 0; i < size(); i++) {
            if (!get(i).equals(other.get(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = 1;
        for (int i = 0; i < size(); i++) {
            int currentHash = 0;
            if (get(i) != null) {
                currentHash = get(i).hashCode();
            }
            result = 41 * result + currentHash;
        }
        return result;
    }

    // The list modification methods return the modified list to allow treating
    // single LexicalUnitImpl objects similarly to lists.

    @Override
    public SassList addAllItems(SassListItem items) {
        ArrayList<SassListItem> values = new ArrayList<SassListItem>(this.items);
        SassList otherItems = getItemsAsList(items);
        values.addAll(otherItems.items);
        return new SassList(getSeparator(), values);
    }

    @Override
    public SassList removeAllItems(SassListItem items) {
        ArrayList<SassListItem> values = new ArrayList<SassListItem>(this.items);
        SassList otherItems = getItemsAsList(items);
        values.removeAll(otherItems.items);
        return new SassList(getSeparator(), values);
    }

    @Override
    public boolean containsAllItems(SassListItem items) {
        SassList otherItems = getItemsAsList(items);
        return this.items.containsAll(otherItems.items);
    }

    private SassList getItemsAsList(SassListItem items) {
        if (items instanceof SassList) {
            return (SassList) items;
        } else {
            return new SassList(items);
        }
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public SassListItem get(int index) {
        return items.get(index);
    }

    @Override
    public Iterator<SassListItem> iterator() {
        return (Collections.unmodifiableList(items)).iterator();
    }
}
